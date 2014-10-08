/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.*;
import junit.framework.TestCase;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class Pop3ServerTest extends TestCase {

    GreenMail greenMail;

    protected void tearDown() throws Exception {
        try {
            greenMail.stop();
        } catch (NullPointerException ignored) {
            //empty
        }
        super.tearDown();
    }

    public void testRetrieve() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_POP3);
        assertNotNull(greenMail.getPop3());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random() + "\r\n" + GreenMailUtil.random() + "\r\n" + GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getPop3());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());
    }

    public void testPop3sReceive() throws Throwable {
        greenMail = new GreenMail(new ServerSetup[]{ServerSetupTest.SMTPS, ServerSetupTest.POP3S});
        assertNull(greenMail.getPop3());
        assertNotNull(greenMail.getPop3s());
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailSecureTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getPop3s());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());
    }

    public void testRetrieveWithNonDefaultPassword() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_POP3);
        assertNotNull(greenMail.getPop3());
        final String to = "test@localhost.com";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
        greenMail.start();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getPop3());
        boolean login_failed = false;
        try {
            Message[] messages = retriever.getMessages(to, "wrongpassword");
        } catch (Throwable e) {
            login_failed = true;
        }
        assertTrue(login_failed);

        Message[] messages = retriever.getMessages(to, password);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());
    }

    public void testRetrieveMultipart() throws Exception {
        greenMail = new GreenMail(ServerSetupTest.SMTP_POP3);
        assertNotNull(greenMail.getPop3());
        greenMail.start();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost.com", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(greenMail.getPop3());
        Message[] messages = retriever.getMessages(to);

        Object o = messages[0].getContent();
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
