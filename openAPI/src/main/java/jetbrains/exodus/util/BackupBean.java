/**
 * Copyright 2010 - 2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.exodus.util;

import jetbrains.exodus.BackupStrategy;
import jetbrains.exodus.Backupable;
import jetbrains.exodus.core.dataStructures.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class BackupBean implements Backupable {

    private static final Log log = LogFactory.getLog(BackupBean.class);

    private final Backupable[] targets;
    private volatile long backupStartTicks;
    private String backupPath;
    private boolean backupToZip;
    private String backupNamePrefix;
    private String commandAfterBackup;
    private Throwable backupException;

    public BackupBean(final Backupable target) {
        targets = new Backupable[]{target};
    }

    public BackupBean(final List<Backupable> targets) {
        final int targetsCount = targets.size();
        if (targetsCount < 1) {
            throw new IllegalArgumentException();
        }
        this.targets = targets.toArray(new Backupable[targetsCount]);
    }

    public void setBackupPath(@NotNull final String backupPath) {
        this.backupPath = backupPath;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public boolean getBackupToZip() {
        return backupToZip;
    }

    public void setBackupToZip(boolean zip) {
        backupToZip = zip;
    }

    public String getBackupNamePrefix() {
        return backupNamePrefix;
    }

    public void setBackupNamePrefix(String prefix) {
        backupNamePrefix = prefix;
    }

    public void setCommandAfterBackup(@Nullable final String command) {
        commandAfterBackup = command;
    }

    public String getCommandAfterBackup() {
        return commandAfterBackup;
    }

    public void setBackupStartTicks(long backupStartTicks) {
        this.backupStartTicks = backupStartTicks;
    }

    public long getBackupStartTicks() {
        return backupStartTicks;
    }

    public void setBackupException(Throwable backupException) {
        this.backupException = backupException;
    }

    public Throwable getBackupException() {
        return backupException;
    }

    @Override
    public BackupStrategy getBackupStrategy() {
        final int targetsCount = targets.length;
        final BackupStrategy[] wrapped = new BackupStrategy[targetsCount];
        for (int i = 0; i < targetsCount; i++) {
            wrapped[i] = targets[i].getBackupStrategy();
        }
        return new BackupStrategy() {
            @Override
            public void beforeBackup() throws Exception {
                backupStartTicks = System.currentTimeMillis();
                log.info("Backing up database...");
                for (final BackupStrategy strategy : wrapped) {
                    strategy.beforeBackup();
                }
            }

            @Override
            public Iterable<Pair<File, String>> listFiles() {
                return new Iterable<Pair<File, String>>() {
                    @Override
                    public Iterator<Pair<File, String>> iterator() {
                        return new Iterator<Pair<File, String>>() {

                            @Nullable
                            private Pair<File, String> next = null;
                            private int i = 0;
                            @NotNull
                            private Iterator<Pair<File, String>> it = EMPTY.listFiles().iterator();

                            @Override
                            public boolean hasNext() {
                                return getNext() != null;
                            }

                            @Override
                            public Pair<File, String> next() {
                                try {
                                    return getNext();
                                } finally {
                                    next = null;
                                }
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException("remove");
                            }

                            private Pair<File, String> getNext() {
                                if (next == null) {
                                    while (!it.hasNext()) {
                                        if (i >= targetsCount) {
                                            return next;
                                        }
                                        it = wrapped[i++].listFiles().iterator();
                                    }
                                    next = it.next();
                                }
                                return next;
                            }
                        };
                    }
                };
            }

            @Override
            public void afterBackup() throws Exception {
                try {
                    for (final BackupStrategy strategy : wrapped) {
                        strategy.afterBackup();
                    }
                } finally {
                    backupStartTicks = 0;
                }
                if (commandAfterBackup != null) {
                    log.info("Executing \"" + commandAfterBackup + "\"...");
                    //noinspection CallToRuntimeExecWithNonConstantString,CallToRuntimeExec
                    Runtime.getRuntime().exec(commandAfterBackup);
                }
                log.info("Backup finished.");
            }

            @Override
            public void onError(Throwable t) {
                backupException = t;
            }
        };
    }
}
