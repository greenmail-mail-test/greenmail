package com.icegreen.greenmail.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.imap.ImapConstants;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;


public class UserManagerTest {
    @Test
    public void testListUsers() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);

        assertThat(userManager.listUser()).isEmpty();

        GreenMailUser u1 = userManager.createUser("foo@bar.com", "foo", "pwd");

        assertThat(userManager.listUser()).hasSize(1);
        assertThat(userManager.listUser().contains(u1)).isTrue();

        GreenMailUser u2 = userManager.createUser("foo2@bar.com", "foo2", "pwd");
        assertThat(userManager.listUser()).hasSize(2);
        assertThat(userManager.listUser().contains(u1)).isTrue();
        assertThat(userManager.listUser().contains(u2)).isTrue();
    }

    @Test
    public void testFindByEmailAndLogin() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        GreenMailUser u1 = userManager.createUser("foo@bar.com", "foo", "pwd");

        assertThat(userManager.getUserByEmail(u1.getEmail())).isEqualTo(u1);
        assertThat(userManager.getUser(u1.getLogin())).isEqualTo(u1);

        GreenMailUser u2 = userManager.createUser("foo2@bar.com", "foo2", "pwd");
        assertThat(userManager.getUserByEmail(u1.getEmail())).isEqualTo(u1);
        assertThat(userManager.getUserByEmail(u2.getEmail())).isEqualTo(u2);
        assertThat(userManager.getUser(u1.getLogin())).isEqualTo(u1);
        assertThat(userManager.getUser(u2.getLogin())).isEqualTo(u2);
    }

    @Test
    public void testCreateAndDeleteUser() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);

        assertThat(userManager.listUser().isEmpty()).isTrue();

        GreenMailUser user = userManager.createUser("foo@bar.com", "foo", "pwd");
        assertThat(userManager.listUser()).hasSize(1);

        userManager.deleteUser(user);
        assertThat(userManager.listUser().isEmpty()).isTrue();
    }

    @Test
    public void testDeleteUserShouldDeleteMail() throws Exception {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);

        GreenMailUser user = userManager.createUser("foo@bar.com", "foo", "pwd");
        assertThat(userManager.listUser()).hasSize(1);

        imapHostManager.createPrivateMailAccount(user);
        MailFolder otherfolder = imapHostManager.createMailbox(user, "otherfolder");
        MailFolder inbox = imapHostManager.getFolder(user, ImapConstants.INBOX_NAME);

        ServerSetup ss = ServerSetupTest.IMAP;
        MimeMessage m1 = GreenMailUtil.createTextEmail("there@localhost", "here@localhost", "sub1", "msg1", ss);
        MimeMessage m2 = GreenMailUtil.createTextEmail("there@localhost", "here@localhost", "sub1", "msg1", ss);

        inbox.store(m1);
        otherfolder.store(m2);
 
        userManager.deleteUser(user);
        assertThat(imapHostManager.getAllMessages().isEmpty()).isTrue();
    }

    @Test
    public void testNoAuthRequired() {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        userManager.setAuthRequired(false);

        assertThat(userManager.test("foo@localhost", null)).isTrue();
        assertThat(userManager.listUser()).hasSize(1);
    }

    @Test
    public void testNoAuthRequiredWithExistingUser() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        userManager.setAuthRequired(false);

        userManager.createUser("foo@example.com", "foo", null);
        assertThat(userManager.listUser().isEmpty()).isFalse();
        assertThat(userManager.test("foo", null)).isTrue();
    }

    @Test
    public void testAuthRequired() {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        userManager.setAuthRequired(true);

        assertThat(userManager.test("foo@localhost", null)).isFalse();
        assertThat(userManager.listUser().isEmpty()).isTrue();
    }

    @Test
    public void testAuthRequiredWithExistingUser() throws UserException {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        userManager.setAuthRequired(true);
        userManager.createUser("foo@example.com", "foo", "bar");

        assertThat(userManager.listUser().isEmpty()).isFalse();
        assertThat(userManager.test("foo", "bar")).isTrue();
    }

    @Test
    public void testMultithreadedUserCreationAndDeletionWithSync() {
        ConcurrencyTest concurrencyTest = new ConcurrencyTest(true);
        concurrencyTest.performTest();
    }

    @Test
    public void testMultithreadedUserCreationAndDeletion() {
        ConcurrencyTest concurrencyTest = new ConcurrencyTest(false);
        concurrencyTest.performTest();
    }

    static class ConcurrencyTest {
        private static final int NO_THREADS = 5;
        private static final int NO_ACCOUNTS_PER_THREAD = 20;

        private final UserManager userManager;
        private final ImapHostManager imapHostManager;
        private final boolean creationSynchronized;

        public ConcurrencyTest(boolean creationSynchronized) {
            imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
            userManager = new UserManager(imapHostManager);
            this.creationSynchronized = creationSynchronized;
        }

        private void createMailbox(String email) throws UserException {
            userManager.createUser(email, email, email);
        }

        private void deleteMailbox(String email) throws FolderException {
            GreenMailUser user = userManager.getUserByEmail(email);
            MailFolder inbox = imapHostManager.getInbox(user);
            inbox.deleteAllMessages();
            userManager.deleteUser(user);
        }

        public void performTest() {
            final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    for (int counter = 0; counter < NO_ACCOUNTS_PER_THREAD; counter++) {
                        String email = "email_" + Thread.currentThread().getName() + "_" + counter;
                        try {
                            if (creationSynchronized) {
                                synchronized (ConcurrencyTest.class) {
                                    createMailbox(email);
                                }
                            } else {
                                createMailbox(email);
                            }
                            deleteMailbox(email);
                        } catch (Exception e) {
                            exceptions.add(e);
                        }
                    }
                }
            };

            Thread[] threads = new Thread[NO_THREADS];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(task);
                threads[i].start();
            }

            for (int i = 0; i < threads.length; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }

            if (!exceptions.isEmpty()) {
                fail("Exception was thrown: " + exceptions);
            }
        }
    }
}
