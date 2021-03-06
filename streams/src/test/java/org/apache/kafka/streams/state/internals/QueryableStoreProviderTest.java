/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.streams.state.internals;


import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.state.NoOpWindowStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.test.NoOpReadOnlyStore;
import org.apache.kafka.test.StateStoreProviderStub;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertNotNull;

public class QueryableStoreProviderTest {

    private final String keyValueStore = "key-value";
    private final String windowStore = "window-store";
    private QueryableStoreProvider storeProvider;
    private HashMap<String, StateStore> globalStateStores;
    private final int numStateStorePartitions = 2;

    @Before
    public void before() {
        final StateStoreProviderStub theStoreProvider = new StateStoreProviderStub(false);
        for (int partition = 0; partition < numStateStorePartitions; partition++) {
            theStoreProvider.addStore(keyValueStore, partition, new NoOpReadOnlyStore<>());
            theStoreProvider.addStore(windowStore, partition, new NoOpWindowStore());
        }
        globalStateStores = new HashMap<>();
        storeProvider =
            new QueryableStoreProvider(
                Collections.singletonList(theStoreProvider),
                new GlobalStateStoreProvider(globalStateStores)
            );
    }

    @Test(expected = InvalidStateStoreException.class)
    public void shouldThrowExceptionIfKVStoreDoesntExist() {
        storeProvider.getStore(StoreQueryParameters.fromNameAndType("not-a-store", QueryableStoreTypes.keyValueStore()));
    }

    @Test(expected = InvalidStateStoreException.class)
    public void shouldThrowExceptionIfWindowStoreDoesntExist() {
        storeProvider.getStore(StoreQueryParameters.fromNameAndType("not-a-store", QueryableStoreTypes.windowStore()));
    }

    @Test
    public void shouldReturnKVStoreWhenItExists() {
        assertNotNull(storeProvider.getStore(StoreQueryParameters.fromNameAndType(keyValueStore, QueryableStoreTypes.keyValueStore())));
    }

    @Test
    public void shouldReturnWindowStoreWhenItExists() {
        assertNotNull(storeProvider.getStore(StoreQueryParameters.fromNameAndType(windowStore, QueryableStoreTypes.windowStore())));
    }

    @Test(expected = InvalidStateStoreException.class)
    public void shouldThrowExceptionWhenLookingForWindowStoreWithDifferentType() {
        storeProvider.getStore(StoreQueryParameters.fromNameAndType(windowStore, QueryableStoreTypes.keyValueStore()));
    }

    @Test(expected = InvalidStateStoreException.class)
    public void shouldThrowExceptionWhenLookingForKVStoreWithDifferentType() {
        storeProvider.getStore(StoreQueryParameters.fromNameAndType(keyValueStore, QueryableStoreTypes.windowStore()));
    }

    @Test
    public void shouldFindGlobalStores() {
        globalStateStores.put("global", new NoOpReadOnlyStore<>());
        assertNotNull(storeProvider.getStore(StoreQueryParameters.fromNameAndType("global", QueryableStoreTypes.keyValueStore())));
    }

    @Test
    public void shouldReturnKVStoreWithPartitionWhenItExists() {
        assertNotNull(storeProvider.getStore(StoreQueryParameters.fromNameAndType(keyValueStore, QueryableStoreTypes.keyValueStore()).withPartition(numStateStorePartitions - 1)));
    }

    @Test
    public void shouldThrowExceptionWhenKVStoreWithPartitionDoesntExists() {
        final int partition = numStateStorePartitions + 1;
        final InvalidStateStoreException thrown = assertThrows(InvalidStateStoreException.class, () ->
                storeProvider.getStore(
                        StoreQueryParameters
                                .fromNameAndType(keyValueStore, QueryableStoreTypes.keyValueStore())
                                .withPartition(partition))
        );
        assertThat(thrown.getMessage(), equalTo(String.format("The specified partition %d for store %s does not exist.", partition, keyValueStore)));
    }

    @Test
    public void shouldReturnWindowStoreWithPartitionWhenItExists() {
        assertNotNull(storeProvider.getStore(StoreQueryParameters.fromNameAndType(windowStore, QueryableStoreTypes.windowStore()).withPartition(numStateStorePartitions - 1)));
    }

    @Test
    public void shouldThrowExceptionWhenWindowStoreWithPartitionDoesntExists() {
        final int partition = numStateStorePartitions + 1;
        final InvalidStateStoreException thrown = assertThrows(InvalidStateStoreException.class, () ->
                storeProvider.getStore(
                        StoreQueryParameters
                                .fromNameAndType(windowStore, QueryableStoreTypes.windowStore())
                                .withPartition(partition))
        );
        assertThat(thrown.getMessage(), equalTo(String.format("The specified partition %d for store %s does not exist.", partition, windowStore)));
    }
}