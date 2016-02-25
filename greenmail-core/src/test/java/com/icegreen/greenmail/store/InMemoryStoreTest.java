/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.easymock.PowerMock.*;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(InMemoryStore.class)
public class InMemoryStoreTest {
    @Test
    public void shouldUsedListBasedFactoryByDefault() throws Exception {
        // Given
        final RootFolder mockFolder = createMockAndExpectNew(RootFolder.class, StoredMessageCollectionFactory
                .LIST_BASED_FACTORY);
        replay(mockFolder, RootFolder.class);

        // When
        new InMemoryStore();

        // Then
        verify(mockFolder, RootFolder.class);
    }

    @Test
    public void shouldForwardGivenFactory() throws Exception {
        for (final StoredMessageCollectionFactory factory : StoredMessageCollectionFactory.values()) {
            // Given
            final RootFolder mockFolder = createMockAndExpectNew(RootFolder.class, factory);
            replay(mockFolder, RootFolder.class);

            // When
            new InMemoryStore(factory);

            // Then
            verify(mockFolder, RootFolder.class);
            reset(RootFolder.class);
        }
    }
}