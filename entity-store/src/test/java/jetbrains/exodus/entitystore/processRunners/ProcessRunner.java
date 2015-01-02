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
package jetbrains.exodus.entitystore.processRunners;

import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityStore;
import jetbrains.exodus.entitystore.EntityStoreTestBase;
import jetbrains.exodus.entitystore.StoreTransaction;
import jetbrains.exodus.util.ForkedLogic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class ProcessRunner extends ForkedLogic {

    private static final Log log = LogFactory.getLog(ProcessRunner.class);

    protected EntityStore store;

    protected StoreTransaction txn;

    @Override
    public void forked(String[] args) throws Exception {
        log.info("Process started");

        store = EntityStoreTestBase.createStore(EntityStoreTestBase.randomTempFolder() + "-forked");
        txn = store.beginTransaction();

        step();
        oneMoreStep();

        getStreamer().writeString(store.getLocation());
        log.info("Euthanasia requested");
        Thread.sleep(180 * 1000);
        close();
    }

    private void step() throws Exception {
        Entity entity = txn.newEntity("Person");
        entity.setProperty("name", "Vadim");
        entity.setProperty("password", "dummypassword");
        byte[] blob = new byte[1024 * 1024];
        Arrays.fill(blob, (byte) 1);
        entity.setBlob("weight", new ByteArrayInputStream(blob));
        txn.flush();
    }

    protected void oneMoreStep() throws Exception {
    }

}
