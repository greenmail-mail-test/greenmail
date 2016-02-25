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

/**
 * {@link Managers} which uses the {@link StoredMessageCollectionFactory#MAP_BASED_FACTORY MAP_BASED_FACTORY} for
 * instantiating the {@link InMemoryStore}.
 *
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
public class MemorySafeManagers extends Managers {
    public MemorySafeManagers() {
        super(new InMemoryStore(StoredMessageCollectionFactory.MAP_BASED_FACTORY));
    }

    @Override
    protected Store createNewStore() {
        return new InMemoryStore(StoredMessageCollectionFactory.MAP_BASED_FACTORY);
    }
}
