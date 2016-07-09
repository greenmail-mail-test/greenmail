package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.IMAPStore;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

/**
 * Example using plain JavaMail for sending / receiving mails via GreenMail server.
 */
public class ExampleJavaMailTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    @Test
    public void testSendAndReceive() throws UnsupportedEncodingException, MessagingException, UserException {
        Session smtpSession = greenMail.getSmtp().createSession();

        Message msg = new MimeMessage(smtpSession);
        msg.setFrom(new InternetAddress("foo@example.com"));
        msg.addRecipient(Message.RecipientType.TO,
                new InternetAddress("bar@example.com"));
        msg.setSubject("Email sent to GreenMail via plain JavaMail");
        msg.setText("Fetch me via IMAP");
        Transport.send(msg);

        // Create user, as connect verifies pwd
        greenMail.setUser("bar@example.com", "bar@example.com", "secret-pwd");

        // Alternative 1: Create session and store or ...
        Session imapSession = greenMail.getImap().createSession();
        Store store = imapSession.getStore("imap");
        store.connect("bar@example.com", "secret-pwd");
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        Message msgReceived = inbox.getMessage(1);
        assertEquals(msg.getSubject(), msgReceived.getSubject());

        // Alternative 2: ... let GreenMail create and configure a store:
        IMAPStore imapStore = greenMail.getImap().createStore();
        imapStore.connect("bar@example.com", "secret-pwd");
        inbox = imapStore.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        msgReceived = inbox.getMessage(1);
        assertEquals(msg.getSubject(), msgReceived.getSubject());

        // Alternative 3: ... directly fetch sent message using GreenMail API
        assertEquals(1, greenMail.getReceivedMessagesForDomain("bar@example.com").length);
        msgReceived = greenMail.getReceivedMessagesForDomain("bar@example.com")[0];
        assertEquals(msg.getSubject(), msgReceived.getSubject());

        store.close();
        imapStore.close();
    }
}
