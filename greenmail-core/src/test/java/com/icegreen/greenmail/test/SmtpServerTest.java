/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test;

import static com.icegreen.greenmail.util.GreenMailUtil.createTextEmail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

import org.junit.Rule;
import org.junit.Test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.smtp.commands.AuthCommand;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class SmtpServerTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(new ServerSetup[]{ServerSetupTest.SMTP, ServerSetupTest.SMTPS});

    @Test
    public void testSmtpServerBasic() throws MessagingException {
        GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "subject", "body");
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals("subject", emails[0].getSubject());
        assertEquals("body", GreenMailUtil.getBody(emails[0]));
    }

    @Test
    public void testSmtpServerTimeout() throws Throwable {
        assertEquals(0, greenMail.getReceivedMessages().length);
        long t0 = System.currentTimeMillis();
        greenMail.waitForIncomingEmail(500, 1);
        assertTrue(System.currentTimeMillis() - t0 > 500);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(0, emails.length);
    }

    @Test
    public void testSmtpServerReceiveWithSetup() throws Throwable {
        assertEquals(0, greenMail.getReceivedMessages().length);

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest("test@localhost.com", "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(1500, 1);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(emails[0]).trim());
    }

    @Test
    public void testSmtpsServerReceive() throws Throwable {
        assertEquals(0, greenMail.getReceivedMessages().length);

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailSecureTest("test@localhost.com", "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(1500, 1);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(emails[0]).trim());
    }

    @Test
    public void testSmtpServerReceiveInThread() throws Throwable {
        assertEquals(0, greenMail.getReceivedMessages().length);

        Thread sendThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(700);
                    GreenMailUtil.sendTextEmailTest("test@localhost.com", "from@localhost", "abc", "def");
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
        sendThread.start();
        greenMail.waitForIncomingEmail(3000, 1);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        sendThread.join(10000);
    }

    @Test
    public void testSmtpServerReceiveMultipart() throws Exception {
        assertEquals(0, greenMail.getReceivedMessages().length);

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        GreenMailUtil.sendAttachmentEmail("test@localhost.com", "from@localhost.com", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(1500, 1);
        Message[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());

        Object o = emails[0].getContent();
        assertTrue(o instanceof MimeMultipart);
        MimeMultipart mp = (MimeMultipart) o;
        assertEquals(2, mp.getCount());
        BodyPart bp;
        bp = mp.getBodyPart(0);
        assertEquals(body, GreenMailUtil.getBody(bp).trim());

        bp = mp.getBodyPart(1);
        assertEquals("AAEC", GreenMailUtil.getBody(bp).trim());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GreenMailUtil.copyStream(bp.getInputStream(), bout);
        byte[] gif = bout.toByteArray();
        for (int i = 0; i < gif.length; i++) {
            assertEquals(i, gif[i]);
        }
    }

    @Test
    public void testSmtpServerLeadingPeriods() throws MessagingException {
        String body = ". body with leading period";
        GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "subject", body);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals("subject", emails[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(emails[0]));
    }

    @Test
    public void testSendAndWaitForIncomingMailsInBcc() throws Throwable {
        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        final MimeMessage message = createTextEmail("test@localhost", "from@localhost", subject, body, greenMail.getSmtp().getServerSetup());
        message.addRecipients(Message.RecipientType.BCC, "bcc1@localhost,bcc2@localhost");

        assertEquals(0, greenMail.getReceivedMessages().length);

        GreenMailUtil.sendMimeMessage(message);

        assertTrue(greenMail.waitForIncomingEmail(1500, 3));

        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(3, emails.length);
    }

    @Test
    public void testAuth() throws Throwable {
        assertEquals(0, greenMail.getReceivedMessages().length);

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        final MimeMessage message = GreenMailUtil.createTextEmail("test@localhost", "from@localhost",
                subject, body, greenMail.getSmtp().getServerSetup());
        Transport.send(message);
        try {
            Transport.send(message, "foo", "bar");
        } catch (AuthenticationFailedException ex) {
            assertTrue(ex.getMessage().contains(AuthCommand.AUTH_CREDENTIALS_INVALID));
        }
        greenMail.setUser("foo", "bar");
        Transport.send(message, "foo", "bar");

        greenMail.waitForIncomingEmail(1500, 3);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(2, emails.length);
        for (MimeMessage receivedMsg : emails) {
            assertEquals(subject, receivedMsg.getSubject());
            assertEquals(body, GreenMailUtil.getBody(receivedMsg).trim());
        }
    }

    @Test
    public void testSmtpServerReceiveWithAUTHSuffix() throws Throwable {
        assertEquals(0, greenMail.getReceivedMessages().length);

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        
        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.from", "<test@localhost.com> AUTH <somethingidontknow>");
        Session session = GreenMailUtil.getSession(ServerSetupTest.SMTP, mailProps);

        MimeMessage message = new MimeMessage(session);
        message.setContent("body1", "text/plain");
        message.setFrom("from@localhost");
        message.setRecipients(RecipientType.TO, InternetAddress.parse("to@localhost"));
        message.setSubject(subject);

        GreenMailUtil.sendMimeMessage(message);
        System.setProperty("mail.smtp.from", "<test@localhost.com> AUTH <somethingidontknow>");
        

        greenMail.waitForIncomingEmail(1500, 1);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());
    }

}
