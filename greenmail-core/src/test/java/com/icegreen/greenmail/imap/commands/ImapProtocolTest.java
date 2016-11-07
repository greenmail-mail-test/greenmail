package com.icegreen.greenmail.imap.commands;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.mail.Folder;
import javax.mail.MessagingException;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPProtocol;
import org.junit.Rule;
import org.junit.Test;

/**
 * Low level IMAP protocol test cases.
 */
public class ImapProtocolTest {
    @Rule
    public final GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(ServerSetupTest.SMTP_IMAP);

    @Test
    @StoreChooser(store="file,memory")
    public void testFetchUidsAndSize() throws MessagingException {
        greenMail.setUser("foo@localhost", "pwd");
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test UIDFolder",
                "Test message", ServerSetupTest.SMTP);

        final IMAPStore store = greenMail.getImap().createStore();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1:* RFC822.SIZE", null);
                }
            });
            FetchResponse fetchResponse = (FetchResponse) ret[0];
            assertFalse(fetchResponse.isBAD());
            assertTrue(fetchResponse.getItemCount() > 0); // At least UID and SIZE
        } finally {
            store.close();
        }
    }
}
