package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for https://github.com/greenmail-mail-test/greenmail/issues/1105
 *
 * When session B expunges a message while session A has the same folder selected,
 * session A's message sequence numbers must remain stable until the server sends
 * session A the EXPUNGE response. Per RFC 3501 §7.4.1 (and RFC 9051 §7.5.1),
 * EXPUNGE may not be sent during FETCH, STORE or SEARCH at all.
 */
public class ImapSequenceNumberExpungeTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    /**
     * Session A has INBOX selected with 10 messages. Session B expunges message 1.
     * Session A then fetches messages by UID range 6-10. The fetched messages must
     * retain their original UIDs (UID 6 maps to "message 6", not "message 5").
     *
     * The bug: after session B expunges message 1, session A's FETCH returns
     * renumbered sequence numbers (msn 6 carries UID 7, etc.) before the EXPUNGE
     * is sent to session A. This causes the client to associate UIDs with the
     * wrong messages.
     */
    @Test
    public void fetchByUidRange_preservesUidMapping_afterOtherSessionExpunges() throws Exception {
        // Setup: deliver 10 messages
        GreenMailUser user = greenMail.setUser("a@localhost", "a", "secret");
        for (int n = 1; n <= 10; n++) {
            user.deliver(GreenMailUtil.createTextEmail("a@localhost", "b@localhost",
                "message " + n, "body " + n, greenMail.getSmtp().getServerSetup()));
        }
        greenMail.waitForIncomingEmail(1000, 10);

        Properties props = new Properties();
        Session session = Session.getInstance(props);
        int port = greenMail.getImap().getPort();

        // Session A: open folder and fetch messages by UID range 6-10
        Store storeA = session.getStore("imap");
        storeA.connect("127.0.0.1", port, "a", "secret");
        IMAPFolder folderA = (IMAPFolder) storeA.getFolder("INBOX");
        folderA.open(Folder.READ_WRITE);

        // Session B: expunge message 1
        Store storeB = session.getStore("imap");
        storeB.connect("127.0.0.1", port, "a", "secret");
        Folder folderB = storeB.getFolder("INBOX");
        folderB.open(Folder.READ_WRITE);
        folderB.getMessage(1).setFlag(jakarta.mail.Flags.Flag.DELETED, true);
        folderB.close(true);

        // Session A: fetch by UID range 6-10
        Message[] messages = ((UIDFolder) folderA).getMessagesByUID(6, 10);

        // Each message should retain its original UID-to-subject mapping
        // UID 6 -> "message 6", UID 7 -> "message 7", etc.
        for (int i = 0; i < messages.length; i++) {
            long uid = ((UIDFolder) folderA).getUID(messages[i]);
            String subject = messages[i].getSubject();
            int expectedUid = 6 + i;
            assertThat(subject)
                .as("UID %d should map to 'message %d', but got '%s'", uid, expectedUid, subject)
                .isEqualTo("message " + expectedUid);
        }

        folderA.close(false);
        storeA.close();
        storeB.close();
    }

    /**
     * Session A has INBOX selected with 10 messages. Session B expunges message 1.
     * Session A then fetches with UID FETCH 1:10 (UID). The EXPUNGE response must
     * arrive before any FETCH response that uses the new (renumbered) sequence numbers,
     * or the FETCH responses must use the old (pre-expunge) sequence numbers.
     *
     * After the fetch, getMessageByUID(6) should return the message with subject
     * "message 6", not "message 5".
     */
    @Test
    public void uidFetch_preservesUidMapping_afterOtherSessionExpunges() throws Exception {
        // Setup: deliver 10 messages
        GreenMailUser user = greenMail.setUser("a@localhost", "a", "secret");
        for (int n = 1; n <= 10; n++) {
            user.deliver(GreenMailUtil.createTextEmail("a@localhost", "b@localhost",
                "message " + n, "body " + n, greenMail.getSmtp().getServerSetup()));
        }
        greenMail.waitForIncomingEmail(1000, 10);

        Properties props = new Properties();
        Session session = Session.getInstance(props);
        int port = greenMail.getImap().getPort();

        // Session A: open folder
        Store storeA = session.getStore("imap");
        storeA.connect("127.0.0.1", port, "a", "secret");
        IMAPFolder folderA = (IMAPFolder) storeA.getFolder("INBOX");
        folderA.open(Folder.READ_WRITE);

        // Session B: expunge message 1
        Store storeB = session.getStore("imap");
        storeB.connect("127.0.0.1", port, "a", "secret");
        Folder folderB = storeB.getFolder("INBOX");
        folderB.open(Folder.READ_WRITE);
        folderB.getMessage(1).setFlag(jakarta.mail.Flags.Flag.DELETED, true);
        folderB.close(true);

        // Session A: fetch with UID profile to trigger UID FETCH
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(UIDFolder.FetchProfileItem.UID);

        // First fetch a range to trigger the renumbering issue
        Message[] range = ((UIDFolder) folderA).getMessagesByUID(6, 10);
        folderA.fetch(range, fp);

        // Now getMessagesByUID(1, 10) triggers UID FETCH 1:10 (UID)
        // which sends FETCH lines in new numbers, then * 1 EXPUNGE
        ((UIDFolder) folderA).getMessagesByUID(1, 10);

        // getMessageByUID(6) should return "message 6", not "message 5"
        Message m = ((UIDFolder) folderA).getMessageByUID(6);
        assertThat(m.getSubject())
            .as("getMessageByUID(6) should return 'message 6', not 'message 5'")
            .isEqualTo("message 6");

        folderA.close(false);
        storeA.close();
        storeB.close();
    }
}
