/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.test;

import java.io.ByteArrayOutputStream;
import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3Store;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Wael Chatila
 */
public class Pop3ServerTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(new ServerSetup[]{
        ServerSetupTest.SMTP, ServerSetupTest.SMTPS,
        ServerSetupTest.POP3, ServerSetupTest.POP3S});

    @Test
    public void testPop3Capabillities() throws MessagingException, UserException {
        final POP3Store store = greenMail.getPop3().createStore();
        greenMail.getUserManager().createUser("testPop3Capabillities@localhost",
                "testPop3Capabillities@localhost", "pwd");
        store.connect("testPop3Capabillities@localhost", "pwd");
        try {
            assertThat(store.capabilities().containsKey("UIDL")).isTrue();
        } finally {
            store.close();
        }
    }

    @Test
    public void testRetrieve() throws Exception {
        assertThat(greenMail.getPop3()).isNotNull();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random() + "\r\n" + GreenMailUtil.random() + "\r\n" + GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getPop3())) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages.length).isEqualTo(1);
            final Message message = messages[0];
            assertThat(message.getSubject()).isEqualTo(subject);
            assertThat(message.getContent().toString().trim()).isEqualTo(body);

            // UID
            POP3Folder f = (POP3Folder) message.getFolder();
            assertThat(f.getUID(message)).isNotEqualTo("UNKNOWN");
        }
    }

    @Test
    public void testPop3sReceive() throws Throwable {
        assertThat(greenMail.getPop3s()).isNotNull();
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendTextEmailSecureTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getPop3s())) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages.length).isEqualTo(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(messages[0].getContent().toString().trim()).isEqualTo(body);
        }
    }

    @Test
    public void testRetrieveWithNonDefaultPassword() throws Exception {
        assertThat(greenMail.getPop3()).isNotNull();
        final String to = "test@localhost";
        final String password = "donotharmanddontrecipricateharm";
        greenMail.setUser(to, password);
        final String subject = GreenMailUtil.random();
        final String body = GreenMailUtil.random();
        GreenMailUtil.sendTextEmailTest(to, "from@localhost", subject, body);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getPop3())) {
            assertThatThrownBy(() -> retriever.getMessages(to, "wrongpassword"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(AuthenticationFailedException.class);

            Message[] messages = retriever.getMessages(to, password);
            assertThat(messages.length).isEqualTo(1);
            assertThat(messages[0].getSubject()).isEqualTo(subject);
            assertThat(messages[0].getContent().toString().trim()).isEqualTo(body);
        }
    }

    @Test
    public void testRetrieveMultipart() throws Exception {
        assertThat(greenMail.getPop3()).isNotNull();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        String to = "test@localhost";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost", subject, body, new byte[]{0, 1, 2},
            "image/gif", "testimage_filename", "testimage_description",
            ServerSetupTest.SMTP);
        greenMail.waitForIncomingEmail(5000, 1);

        try (Retriever retriever = new Retriever(greenMail.getPop3())) {
            Message[] messages = retriever.getMessages(to);

            Object o = messages[0].getContent();
            assertThat(o instanceof MimeMultipart).isTrue();
            MimeMultipart mp = (MimeMultipart) o;
            assertThat(mp.getCount()).isEqualTo(2);
            BodyPart bp;
            bp = mp.getBodyPart(0);
            assertThat(bp.getContent()).isEqualTo(body);

            bp = mp.getBodyPart(1);
            assertThat(GreenMailUtil.getBody(bp)).isEqualTo("AAEC");

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            GreenMailUtil.copyStream(bp.getInputStream(), bout);
            byte[] gif = bout.toByteArray();
            for (int i = 0; i < gif.length; i++) {
                assertThat((int)gif[i]).isEqualTo(i);
            }
        }
    }
}
