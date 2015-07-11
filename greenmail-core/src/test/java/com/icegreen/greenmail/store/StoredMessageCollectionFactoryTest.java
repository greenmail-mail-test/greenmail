/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@RunWith(Theories.class)
public class StoredMessageCollectionFactoryTest {
    @DataPoints
    public static FactoryWithMatchingClass[] createFactoryPoints() {
        return new FactoryWithMatchingClass[]{
                new FactoryWithMatchingClass(StoredMessageCollectionFactory.LIST_BASED_FACTORY, ListBasedStoredMessageCollection.class),
                new FactoryWithMatchingClass(StoredMessageCollectionFactory.MAP_BASED_FACTORY, MapBasedStoredMessageCollection.class)};
    }

    @Theory
    public void shouldCreateMatchingCollection(final FactoryWithMatchingClass factoryWithMatchingClass) {
        // When
        final StoredMessageCollection collection = factoryWithMatchingClass.factory.createCollection();

        // Then
        assertThat(collection, is(Matchers.<StoredMessageCollection>instanceOf(factoryWithMatchingClass.collectionClass)));
    }

    private static class FactoryWithMatchingClass {
        private final StoredMessageCollectionFactory factory;
        private final Class<? extends StoredMessageCollection> collectionClass;

        private FactoryWithMatchingClass(StoredMessageCollectionFactory factory, Class<? extends StoredMessageCollection> collectionClass) {
            this.factory = factory;
            this.collectionClass = collectionClass;
        }
    }

    @Test
    public void shouldRejectUnknownProperty() {
        for (final StoredMessageCollectionFactory factory : StoredMessageCollectionFactory.values()) {
            try {
                factory.withConfigurationValue("foo", "bar");
                fail("Should have thrown exception");
            } catch (IllegalArgumentException e) {
                // This is expected.
            }
        }
    }
}