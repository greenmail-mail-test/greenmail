package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Youssuf ElKalay.
 * Example using GreenMail.purgeEmailFromAllMailboxes() to test removing emails from all configured mailboxes - either
 * POP3 or IMAP.
 */
public class ExamplePurgeAllEmailsTest {
    @Rule
    public final GreenMailRule greenMailRule = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP);


    @Test
    public void testremoveAllMessagesInImapMailbox() throws FolderException {
        try (Retriever retriever = new Retriever(greenMailRule.getImap())) {
            greenMailRule.setUser("foo@localhost", "pwd");
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost",
                    "Test subject", "Test message", ServerSetupTest.SMTP);
            assertThat(greenMailRule.waitForIncomingEmail(1)).isTrue();
            greenMailRule.purgeEmailFromAllMailboxes();
            Message[] messages = retriever.getMessages("foo@localhost", "pwd");
            assertThat(messages.length).isEqualTo(0);
        }

    }

}
