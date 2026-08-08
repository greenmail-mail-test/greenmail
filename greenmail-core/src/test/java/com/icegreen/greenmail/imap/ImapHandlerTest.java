package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.user.UserManager;
import org.junit.Test;

import java.net.Socket;

/**
 * Regression for GH-902: deleteUser can notify IMAP sessions after the handler
 * already cleared its response during close.
 */
public class ImapHandlerTest {

    @Test
    public void forceConnectionCloseWhenResponseNullDoesNotThrow() {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        ImapHandler handler = new ImapHandler(userManager, imapHostManager, new Socket());

        // response is still null before run() starts (or after close() cleared it)
        handler.forceConnectionClose("Mailbox INBOX has been deleted");
    }

    @Test
    public void forceConnectionCloseAfterCloseDoesNotThrow() {
        ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        UserManager userManager = new UserManager(imapHostManager);
        ImapHandler handler = new ImapHandler(userManager, imapHostManager, new Socket());

        handler.close();
        handler.forceConnectionClose("Mailbox INBOX has been deleted");
    }
}
