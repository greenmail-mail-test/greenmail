/*
 *  -------------------------------------------------------------------
 *  This software is released under the Apache license 2.0
 *  -------------------------------------------------------------------
 * /
 */

package com.icegreen.greenmail.store;

import org.hamcrest.Matchers;
import org.junit.Ignore;
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
@PrepareForTest(StoredMessageCollectionFactory.class)
public class StoredMessageCollectionFactoryPowerMockTest {
    // The following tests require a fix for PowerMock issue #552:
    // https://code.google.com/p/powermock/issues/detail?id=552

    @Test
    @Ignore
    public void shouldCreateMapBasedCollectionWithSize5000ByDefault() throws Exception {
        // Given
        final MapBasedStoredMessageCollection mockCollection = createMockAndExpectNew(MapBasedStoredMessageCollection
                .class, new Class<?>[]{int.class}, 5000);
        replay(mockCollection, MapBasedStoredMessageCollection.class);

        // When
        final StoredMessageCollection createdCollection = StoredMessageCollectionFactory.MAP_BASED_FACTORY
                .createCollection();

        // Then
        assertThat(createdCollection, is(Matchers.<StoredMessageCollection>sameInstance(mockCollection)));
        verify(mockCollection, MapBasedStoredMessageCollection.class);
    }

    @Test
    @Ignore
    public void shouldCreateMapBasedCollectionWithGivenSizeIfPropertyIsSet() throws Exception {
        // Given
        final int expectedMapSize = 200;
        StoredMessageCollectionFactory.MAP_BASED_FACTORY.withConfigurationValue(StoredMessageCollectionFactory
                .Constants.CONFIGURATION_KEY_MAXIMUM_MAP_SIZE, expectedMapSize);
        final MapBasedStoredMessageCollection mockCollection = createMockAndExpectNew(MapBasedStoredMessageCollection
                .class, new Class<?>[]{int.class}, expectedMapSize);
        replay(mockCollection, MapBasedStoredMessageCollection.class);

        // When
        final StoredMessageCollection createdCollection = StoredMessageCollectionFactory.MAP_BASED_FACTORY
                .createCollection();

        // Then
        assertThat(createdCollection, is(Matchers.<StoredMessageCollection>sameInstance(mockCollection)));
        verify(mockCollection, MapBasedStoredMessageCollection.class);
    }
}
