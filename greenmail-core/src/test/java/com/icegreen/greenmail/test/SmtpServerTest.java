/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import junit.framework.TestCase;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class SmtpServerTest extends TestCase {

    GreenMail greenMail;

    protected void tearDown() throws Exception {
        try {
            greenMail.stop();
        } catch (NullPointerException ignored) {
            //empty
        }
        super.tearDown();
    }

    public void testSmtpServerBasic() throws MessagingException {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
        GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "subject", "body");
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals("subject", emails[0].getSubject());
        assertEquals("body", GreenMailUtil.getBody(emails[0]));
    }

    public void testSmtpServerTimeout() throws Throwable {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
        assertEquals(0, greenMail.getReceivedMessages().length);
        long t0 = System.currentTimeMillis();
        greenMail.waitForIncomingEmail(500, 1);
        assertTrue(System.currentTimeMillis() - t0 > 500);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(0, emails.length);
    }

    public void testSmtpServerReceiveWithSetup() throws Throwable {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        runSmtpServerReceive();
    }

    public void runSmtpServerReceive() throws Throwable {
        greenMail.start();
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

    public void testSmtpsServerReceive() throws Throwable {
        greenMail = new GreenMail(ServerSetupTest.SMTPS);
        greenMail.start();
        assertEquals(0, greenMail.getReceivedMessages().length);

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailSecureTest("test@localhost.com", "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(1500, 1);
        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(emails[0]).trim());
    }

    public void testSmtpServerReceiveInThread() throws Throwable {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
        assertEquals(0, greenMail.getReceivedMessages().length);

        Thread sendThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(700);
                    GreenMailUtil.sendTextEmailTest("test@localhost.com", "from@localhost.com", "abc", "def");
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

    public void testSmtpServerReceiveMultipart() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
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
}
