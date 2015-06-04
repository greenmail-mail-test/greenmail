/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.store;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
enum StoredMessageCollectionFactory {
    LIST_BASED_FACTORY {
        @Override
        StoredMessageCollection createCollection() {
            return new ListBasedStoredMessageCollection();
        }
    },
    MAP_BASED_FACTORY {
        @Override
        StoredMessageCollection createCollection() {
            return new MapBasedStoredMessageCollection(5000);
        }
    };

    abstract StoredMessageCollection createCollection();
}
