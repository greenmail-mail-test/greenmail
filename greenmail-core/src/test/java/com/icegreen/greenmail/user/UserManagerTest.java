package com.icegreen.greenmail.user;

import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.store.InMemoryStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void testFindByEmail() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        GreenMailUser u1 = userManager.createUser("foo@bar.com", "foo", "pwd");

        assertEquals(u1, userManager.getUserByEmail(u1.getEmail()));

        GreenMailUser u2 = userManager.createUser("foo2@bar.com", "foo2", "pwd");
        assertEquals(u1, userManager.getUserByEmail(u1.getEmail()));
        assertEquals(u2, userManager.getUserByEmail(u2.getEmail()));
    }
}
