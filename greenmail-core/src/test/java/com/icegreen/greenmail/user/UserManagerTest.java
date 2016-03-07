package com.icegreen.greenmail.user;

import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.store.InMemoryStore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class UserManagerTest {
    @Test
    public void testListUsers() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);

        assertEquals(0, userManager.listUser().size());

        GreenMailUser u1 = userManager.createUser("foo@bar.com", "foo", "pwd");

        assertEquals(1, userManager.listUser().size());
        assertTrue(userManager.listUser().contains(u1));

        GreenMailUser u2 = userManager.createUser("foo2@bar.com", "foo2", "pwd");
        assertEquals(2, userManager.listUser().size());
        assertTrue(userManager.listUser().contains(u1));
        assertTrue(userManager.listUser().contains(u2));
    }

    @Test
    public void testFindByEmailAndLogin() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        GreenMailUser u1 = userManager.createUser("foo@bar.com", "foo", "pwd");

        assertEquals(u1, userManager.getUserByEmail(u1.getEmail()));
        assertEquals(u1, userManager.getUser(u1.getLogin()));

        GreenMailUser u2 = userManager.createUser("foo2@bar.com", "foo2", "pwd");
        assertEquals(u1, userManager.getUserByEmail(u1.getEmail()));
        assertEquals(u2, userManager.getUserByEmail(u2.getEmail()));
        assertEquals(u1, userManager.getUser(u1.getLogin()));
        assertEquals(u2, userManager.getUser(u2.getLogin()));
    }

    @Test
    public void testCreateAndDeleteUser() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);

        assertTrue(userManager.listUser().isEmpty());

        GreenMailUser user = userManager.createUser("foo@bar.com", "foo", "pwd");
        assertEquals(1, userManager.listUser().size());

        userManager.deleteUser(user);
        assertTrue(userManager.listUser().isEmpty());
    }

    @Test
    public void testNoAuthRequired() {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        userManager.setAuthRequired(false);

        assertTrue(userManager.listUser().isEmpty());
        assertTrue(userManager.test("foo@localhost",null));

    }

    @Test
    public void testNoAuthRequiredWithExistingUser() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        userManager.setAuthRequired(false);

        userManager.createUser("foo@example.com","foo",null);
        assertFalse(userManager.listUser().isEmpty());
        assertTrue(userManager.test("foo",null));

    }

    @Test
    public void testAuthRequired() {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        userManager.setAuthRequired(true);

        assertTrue(userManager.listUser().isEmpty());
        assertFalse(userManager.test("foo@localhost",null));

    }

    @Test
    public void testAuthRequiredWithExistingUser() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        userManager.setAuthRequired(true);
        userManager.createUser("foo@example.com","foo","bar");

        assertFalse(userManager.listUser().isEmpty());
        assertTrue(userManager.test("foo","bar"));

    }
}
