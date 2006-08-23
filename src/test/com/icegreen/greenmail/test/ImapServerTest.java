/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.icegreen.greenmail.util.Servers;
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
public class ImapServerTest extends TestCase {

    Servers servers;

    protected void tearDown() throws Exception {
        try {
            servers.stop();
        } catch (NullPointerException ignored) {
            //empty
        }
        super.tearDown();
    }

    public void testRetreiveSimple() throws Exception {
        servers = new Servers(ServerSetupTest.SMTP_IMAP);
        assertNotNull(servers.getImap());
        servers.start();
        final String subject = servers.util().random();
        final String body = servers.util().random() + "\r\n" + servers.util().random() + "\r\n" + servers.util().random();
        final String to = "test@localhost.com";
        servers.util().sendTextEmailTest(to, "from@localhost.com", subject, body);
        servers.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(servers.getImap());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    public void testImapsReceive() throws Throwable {
        servers = new Servers(ServerSetupTest.SMTPS_IMAPS);
        assertNull(servers.getImap());
        assertNotNull(servers.getImaps());
        servers.start();
        final String subject = servers.util().random();
        final String body = servers.util().random();
        String to = "test@localhost.com";
        servers.util().sendTextEmailSecureTest(to, "from@localhost.com", subject, body);
        servers.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(servers.getImaps());
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    public void testRetreiveSimpleWithNonDefaultPassword() throws Exception {
        servers = new Servers(ServerSetupTest.SMTP_IMAP);
        assertNotNull(servers.getImap());
        final String to = "test@localhost.com";
        final String password = "donotharmanddontrecipricateharm";
        servers.setUser(to, password);
        servers.start();
        final String subject = servers.util().random();
        final String body = servers.util().random();
        servers.util().sendTextEmailTest(to, "from@localhost.com", subject, body);
        servers.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(servers.getImap());
        boolean login_failed = false;
        try {
            retriever.getMessages(to, "wrongpassword");
        } catch (Throwable e) {
            login_failed = true;
        }
        assertTrue(login_failed);

        Message[] messages = retriever.getMessages(to, password);
        assertEquals(1, messages.length);
        assertEquals(subject, messages[0].getSubject());
        assertEquals(body, ((String) messages[0].getContent()).trim());
    }

    public void testRetriveMultipart() throws Exception {
        servers = new Servers(ServerSetupTest.SMTP_IMAP);
        assertNotNull(servers.getImap());
        servers.start();

        String subject = servers.util().random();
        String body = servers.util().random();
        String to = "test@localhost.com";
        servers.util().sendAttachmentEmail(to, "from@localhost.com", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        servers.waitForIncomingEmail(5000, 1);

        Retriever retriever = new Retriever(servers.getImap());
        Message[] messages = retriever.getMessages(to);

        Object o = messages[0].getContent();
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
        retriever.logout();
    }
}
