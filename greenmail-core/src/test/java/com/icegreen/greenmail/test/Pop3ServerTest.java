/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Store;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 */
public class Pop3ServerTest {
    @Rule
    public final GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(ServerSetupTest.ALL);

    @Test
    @StoreChooser(store="file,memory")
    public void testPop3Capabillities() throws MessagingException, UserException {
        final POP3Store store = greenMail.getPop3().createStore();
        greenMail.getManagers().getUserManager().createUser("testPop3Capabillities@localhost.com",
                "testPop3Capabillities@localhost.com", "pwd");
        store.connect("testPop3Capabillities@localhost.com", "pwd");
        try {
            assertTrue(store.capabilities().containsKey("UIDL"));
        } finally {
            store.close();
        }
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testRetrieve() throws Exception {
        assertNotNull(greenMail.getPop3());
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random() + "\r\n" + GreenMailUtil.random() + "\r\n" + GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getPop3())) {
            Message[] messages = retriever.getMessages(to);
            assertEquals(1, messages.length);
            assertEquals(subject, messages[0].getSubject());
            assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());

            // UID
            POP3Folder f = (POP3Folder) messages[0].getFolder();
            assertNotEquals("UNKNOWN", f.getUID(messages[0]));
        }
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testPop3sReceive() throws Throwable {
        assertNotNull(greenMail.getPop3s());
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendTextEmailSecureTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getPop3s())) {
            Message[] messages = retriever.getMessages(to);
            assertEquals(1, messages.length);
            assertEquals(subject, messages[0].getSubject());
            assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());
        }
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testRetrieveWithNonDefaultPassword() throws Exception {
        assertNotNull(greenMail.getPop3());
        final String to = "test@localhost.com";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getPop3())) {
            try {
                retriever.getMessages(to, "wrongpassword");
                fail("Expected authentication failure");
            } catch (Throwable e) {
                // ok
            }

            Message[] messages = retriever.getMessages(to, password);
            assertEquals(1, messages.length);
            assertEquals(subject, messages[0].getSubject());
            assertEquals(body, GreenMailUtil.getBody(messages[0]).trim());
        }
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testRetrieveMultipart() throws Exception {
        assertNotNull(greenMail.getPop3());

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        String to = "test@localhost.com";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost.com", subject, body, new byte[]{0, 1, 2}, "image/gif", "testimage_filename", "testimage_description", ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getPop3())) {
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
}
