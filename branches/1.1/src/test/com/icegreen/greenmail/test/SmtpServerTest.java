/*
* Copyright (c) 2006 Your Corporation. All Rights Reserved.
*/
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Servers;
import com.icegreen.greenmail.util.ServerSetupTest;
import junit.framework.TestCase;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class SmtpServerTest extends TestCase {

    Servers servers;

    protected void tearDown() throws Exception {
        try {
            servers.stop();
        } catch (NullPointerException ignored) {
            //empty
        }
        super.tearDown();
    }

    public void testSmtpServerTimeout() throws Throwable {
        servers = new Servers(ServerSetupTest.SMTP);
        servers.start();
        assertEquals(0, servers.getReceivedMessages().length);
        long t0 = System.currentTimeMillis();
        servers.waitForIncomingEmail(500, 1);
        assertTrue(System.currentTimeMillis() - t0 > 500);
        MimeMessage[] emails = servers.getReceivedMessages();
        assertEquals(0, emails.length);
    }

    public void testSmtpServerReceiveWithSetup() throws Throwable {
        servers = new Servers(ServerSetupTest.SMTP);
        runSmtpServerReceive();
    }

    public void runSmtpServerReceive() throws Throwable {
        servers.start();
        assertEquals(0, servers.getReceivedMessages().length);

        String subject = servers.util().random();
        String body = servers.util().random();
        servers.util().sendTextEmailTest("test@localhost.com", "from@localhost.com", subject, body);
        servers.waitForIncomingEmail(1500, 1);
        MimeMessage[] emails = servers.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());
        assertEquals(body, servers.util().getBody(emails[0]).trim());
    }

    public void testSmtpsServerReceive() throws Throwable {
        servers = new Servers(ServerSetupTest.SMTPS);
        servers.start();
        assertEquals(0, servers.getReceivedMessages().length);

        String subject = servers.util().random();
        String body = servers.util().random();
        servers.util().sendTextEmailSecureTest("test@localhost.com", "from@localhost.com", subject, body);
        servers.waitForIncomingEmail(1500, 1);
        MimeMessage[] emails = servers.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());
        assertEquals(body, servers.util().getBody(emails[0]).trim());
    }

    public void testSmtpServerReceiveInThread() throws Throwable {
        servers = new Servers(ServerSetupTest.SMTP);
        servers.start();
        assertEquals(0, servers.getReceivedMessages().length);

        Thread sendThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(700);
                    servers.util().sendTextEmailTest("test@localhost.com", "from@localhost.com", "abc", "def");
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
        sendThread.start();
        servers.waitForIncomingEmail(3000, 1);
        MimeMessage[] emails = servers.getReceivedMessages();
        assertEquals(1, emails.length);
        sendThread.join(10000);
    }

    public void testSmtpServerReceiveMultipart() throws Exception {
        servers = new Servers(ServerSetupTest.SMTP);
        servers.start();
        assertEquals(0, servers.getReceivedMessages().length);

        String subject = servers.util().random();
        String body = servers.util().random();
        servers.util().sendAttachmentEmail("test@localhost.com", "from@localhost.com", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        servers.waitForIncomingEmail(1500, 1);
        Message[] emails = servers.getReceivedMessages();
        assertEquals(1, emails.length);
        assertEquals(subject, emails[0].getSubject());

        Object o = emails[0].getContent();
        assertTrue(o instanceof MimeMultipart);
        MimeMultipart mp = (MimeMultipart) o;
        assertEquals(2, mp.getCount());
        BodyPart bp;
        bp = mp.getBodyPart(0);
        assertEquals(body, servers.util().getBody(bp).trim());

        bp = mp.getBodyPart(1);
        assertEquals("AAEC", servers.util().getBody(bp).trim());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        servers.util().copyStream(bp.getInputStream(), bout);
        byte[] gif = bout.toByteArray();
        for (int i = 0; i < gif.length; i++) {
            assertEquals(i, gif[i]);
        }
    }
}
