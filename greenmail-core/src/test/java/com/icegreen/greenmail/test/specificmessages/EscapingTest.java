package com.icegreen.greenmail.test.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests escaping of message parts
 */
public class EscapingTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    public void testEscapeSubject() throws MessagingException, IOException {
        String to = "to@localhost";
        String subject = "Subject";
        greenMail.setUser(to, to);
        GreenMailUtil.sendTextEmail(to, "from@localhost", subject, "msg", greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheck(new Retriever(greenMail.getPop3()), to, subject);
        retrieveAndCheck(new Retriever(greenMail.getImap()), to, subject);
    }

    /**
     * Retrieve message from retriever and check content
     *
     * @param retriever Retriever to read from
     * @param to        Account to retrieve
     * @param subject   Subject of message
     */
    private void retrieveAndCheck(Retriever retriever, String to, String subject) throws MessagingException, IOException {
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        Message message = messages[0];

        // Message subject
        assertThat(message.getSubject(), is(subject));
    }
}
