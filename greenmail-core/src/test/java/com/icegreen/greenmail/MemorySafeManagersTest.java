/*
 *  -------------------------------------------------------------------
 *  This software is released under the Apache license 2.0
 *  -------------------------------------------------------------------
 * /
 */

package com.icegreen.greenmail;

import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.store.Store;
import com.icegreen.greenmail.store.StoredMessageCollectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.*;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MemorySafeManagers.class)
public class MemorySafeManagersTest {
    @Test
    public void shouldUseMapBasedFactoryInConstructor() throws Exception {
        // Given
        final InMemoryStore mockStore = createMockAndExpectNew(InMemoryStore.class, StoredMessageCollectionFactory.MAP_BASED_FACTORY);
        replay(mockStore, InMemoryStore.class);

        // When
        new MemorySafeManagers();

        // Then
        verify(mockStore, InMemoryStore.class);
    }

    @Test
    public void shouldCreateInMemoryStoreWithMapBasedFactory() throws Exception {
        // Given
        final Managers managers = new MemorySafeManagers();
        final Store mockStore = createMockAndExpectNew(InMemoryStore.class, StoredMessageCollectionFactory.MAP_BASED_FACTORY);
        replay(mockStore, InMemoryStore.class);

        // When
        final Store createdStore = managers.createNewStore();

        // Then
        verify(mockStore, InMemoryStore.class);
        assertThat(createdStore, is(mockStore));
    }
}