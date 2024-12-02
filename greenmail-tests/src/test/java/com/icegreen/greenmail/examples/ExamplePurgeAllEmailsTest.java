package com.icegreen.greenmail.examples;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;

/**
 * Created by Youssuf ElKalay.
 * Example using GreenMail.purgeEmailFromAllMailboxes() to test removing emails from all configured mailboxes - either
 * POP3 or IMAP.
 */
class ExamplePurgeAllEmailsTest {
    @RegisterExtension
    static final GreenMailExtension greenMailRule = new GreenMailExtension(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    void testRemoveAllMessagesInImapMailbox() throws FolderException {
        try (Retriever retriever = new Retriever(greenMailRule.getImap())) {
            greenMailRule.setUser("foo@localhost", "pwd");
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost",
                    "Test subject", "Test message", ServerSetupTest.SMTP);
            assertThat(greenMailRule.waitForIncomingEmail(1)).isTrue();
            greenMailRule.purgeEmailFromAllMailboxes();
            Message[] messages = retriever.getMessages("foo@localhost", "pwd");
            assertThat(messages).isEmpty();
        }
    }

}
