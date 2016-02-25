/*
 *  -------------------------------------------------------------------
 *  This software is released under the Apache license 2.0
 *  -------------------------------------------------------------------
 * /
 */

package com.icegreen.greenmail;

import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.store.Store;
import com.icegreen.greenmail.user.UserManager;
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
@PrepareForTest(Managers.class)
public class ManagersTest {

    @Test
    public void shouldUseDefaultInMemoryStoreIfNoneGiven() throws Exception {
        // Given
        final InMemoryStore mockStore = createMockAndExpectNew(InMemoryStore.class);
        replay(mockStore, InMemoryStore.class);

        // When
        new Managers();

        // Then
        verify(mockStore, InMemoryStore.class);
    }

    @Test
    public void shouldCreateNewImapHostManagerImpl() throws Exception {
        // Given
        final Store store = new InMemoryStore();
        final ImapHostManagerImpl mockHostManager = createMockAndExpectNew(ImapHostManagerImpl.class, store);
        replay(mockHostManager, ImapHostManagerImpl.class);

        // When
        new Managers(store);

        // Then
        verify(mockHostManager, ImapHostManagerImpl.class);
    }

    @Test
    public void shouldCreateNewUserManager() throws Exception {
        // Given
        final Store store = new InMemoryStore();
        final ImapHostManagerImpl mockHostManager = createMock(ImapHostManagerImpl.class, store);
        expectNew(ImapHostManagerImpl.class, store).andReturn(mockHostManager);
        final UserManager mockUserManager = createMockAndExpectNew(UserManager.class, mockHostManager);
        replay(mockHostManager, ImapHostManagerImpl.class, mockUserManager, UserManager.class);

        // When
        new Managers(store);

        // Then
        verify(mockUserManager, UserManager.class);
    }

    @Test
    public void shouldCreateNewSmtpManager() throws Exception {
        // Given
        final Store store = new InMemoryStore();
        final ImapHostManagerImpl mockHostManager = createMock(ImapHostManagerImpl.class, store);
        expectNew(ImapHostManagerImpl.class, store).andReturn(mockHostManager);
        final UserManager mockUserManager = createMock(UserManager.class, mockHostManager);
        expectNew(UserManager.class, mockHostManager).andReturn(mockUserManager);
        final SmtpManager mockSmtpManager = createMock(SmtpManager.class,
                mockHostManager, mockUserManager);
        replay(mockHostManager, ImapHostManagerImpl.class, mockUserManager, UserManager.class, mockSmtpManager,
                SmtpManager.class);

        // When
        new Managers(store);

        // Then
        verify(mockSmtpManager, SmtpManager.class);
    }

    @Test
    public void shouldCreateDefaultInMemoryStore() throws Exception {
        // Given
        final Managers managers = new Managers();
        final Store mockStore = createMockAndExpectNew(InMemoryStore.class);
        replay(mockStore, InMemoryStore.class);

        // When
        final Store createdStore = managers.createNewStore();

        // Then
        verify(mockStore, InMemoryStore.class);
        assertThat(createdStore, is(mockStore));
    }

    @Test
    public void shouldRecreateStoreAndAllSubManagers() throws Exception {
        // Given
        final Managers managers = new Managers();
        final InMemoryStore mockStore = createMockAndExpectNew(InMemoryStore.class);
        final ImapHostManagerImpl mockHostManager = createMockAndExpectNew(ImapHostManagerImpl.class, mockStore);
        final UserManager mockUserManager = createMockAndExpectNew(UserManager.class, mockHostManager);
        replay(mockStore, InMemoryStore.class, mockHostManager, ImapHostManagerImpl.class, mockUserManager,
                UserManager.class);

        // When
        managers.reset();

        // Then
        verify(mockStore, InMemoryStore.class, mockHostManager, ImapHostManagerImpl.class, mockUserManager,
                UserManager.class);
    }
}