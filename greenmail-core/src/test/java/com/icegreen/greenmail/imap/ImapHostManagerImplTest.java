/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.store.InMemoryStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.easymock.PowerMock.*;

/**
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ImapHostManagerImpl.class)
public class ImapHostManagerImplTest {

    @Test
    public void shouldUseDefaultInMemoryStoreIfNoneGiven() throws Exception {
        // Given
        final InMemoryStore mockStore = createMockAndExpectNew(InMemoryStore.class);
        replay(mockStore, InMemoryStore.class);

        // When
        new ImapHostManagerImpl();

        // Then
        verify(mockStore, InMemoryStore.class);
    }
}