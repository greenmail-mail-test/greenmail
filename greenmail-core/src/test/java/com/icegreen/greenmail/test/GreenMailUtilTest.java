/* -------------------------------------------------------------------
 * Copyright (c) 2007 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 29, 2006
 */
public class GreenMailUtilTest {
    @Test
    public void testMimeMessageLoading() throws MessagingException {
        MimeMessage message = GreenMailUtil.newMimeMessage(SAMPLE_EMAIL);
        assertThat(message.getSubject()).isEqualTo("wassup");
    }

    @Test
    public void testGetBody() throws MessagingException {
        MimeMessage message = GreenMailUtil.newMimeMessage(SAMPLE_EMAIL);
        String body = GreenMailUtil.getBody(message);
        assertThat(body.trim()).isEqualTo("Yo wassup Bertil");
    }

    @Test
    public void testGetEmptyBodyAndHeader() throws Exception {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        try {
            greenMail.start();

            String subject = GreenMailUtil.random();
            String body = ""; // Provokes https://github.com/greenmail-mail-test/greenmail/issues/151
            String to = "test@localhost";
            final byte[] gifAttachment = {0, 1, 2};
            GreenMailUtil.sendAttachmentEmail(to, "from@localhost", subject, body, gifAttachment,
                    "image/gif", "testimage_filename", "testimage_description",
                    greenMail.getSmtp().getServerSetup());
            greenMail.waitForIncomingEmail(5000, 1);

            try (Retriever retriever = new Retriever(greenMail.getImap())) {
                MimeMultipart mp = (MimeMultipart) retriever.getMessages(to)[0].getContent();
                BodyPart bp;
                bp = mp.getBodyPart(0);
                assertThat(body).isEqualTo(GreenMailUtil.getBody(bp).trim());
                assertThat(
                        "Content-Type: text/plain; charset=us-ascii\r\n" +
                        "Content-Transfer-Encoding: 7bit").isEqualTo(
                        GreenMailUtil.getHeaders(bp).trim());

                bp = mp.getBodyPart(1);
                assertThat("AAEC").isEqualTo(GreenMailUtil.getBody(bp).trim());
                assertThat(
                        "Content-Type: image/gif; name=testimage_filename\r\n" +
                        "Content-Transfer-Encoding: base64\r\n" +
                        "Content-Disposition: attachment; filename=testimage_filename\r\n" +
                        "Content-Description: testimage_description").isEqualTo(GreenMailUtil.getHeaders(bp).trim());

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                GreenMailUtil.copyStream(bp.getInputStream(), bout);
                assertThat(bout.toByteArray()).isEqualTo(gifAttachment);
            }
        } finally {
            greenMail.stop();
        }
    }

    @Test
    public void testSendTextEmailTest() throws Exception {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        try {
            greenMail.setUser("foo@localhost", "pwd");
            greenMail.start();
            GreenMailUtil.sendTextEmail("\"Foo, Bar\" <foo@localhost>", "\"Bar, Foo\" <bar@localhost>",
                    "Test subject", "Test message", ServerSetupTest.SMTP);
            greenMail.waitForIncomingEmail(1);

            Store store = greenMail.getImap().createStore();
            store.connect("foo@localhost", "pwd");
            try {
                Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);
                Message[] msgs = folder.getMessages();
                assertThat(null != msgs && msgs.length == 1).isTrue();
                Message m = msgs[0];
                assertThat(m.getSubject()).isEqualTo("Test subject");
                Address[] a = m.getRecipients(Message.RecipientType.TO);
                assertThat(null != a && a.length == 1
                        && a[0].toString().equals("\"Foo, Bar\" <foo@localhost>")).isTrue();
                a = m.getFrom();
                assertThat(null != a && a.length == 1
                        && a[0].toString().equals("\"Bar, Foo\" <bar@localhost>")).isTrue();
                assertThat(m.getContentType().toLowerCase().startsWith("text/plain")).isTrue();
                assertThat(m.getContent()).isEqualTo("Test message");
            } finally {
                store.close();
            }
        } finally {
            greenMail.stop();
        }
    }

    @Test
    public void testSetAndGetQuota() throws MessagingException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        try {
            greenMail.start();

            final GreenMailUser user = greenMail.setUser("foo@localhost", "pwd");

            Store store = greenMail.getImap().createStore();
            store.connect("foo@localhost", "pwd");

            Quota testQuota = new Quota("INBOX");
            testQuota.setResourceLimit("STORAGE", 1024L * 42L);
            testQuota.setResourceLimit("MESSAGES", 5L);

            assertThat(0).isEqualTo(GreenMailUtil.getQuota(user, testQuota.quotaRoot).length);
            GreenMailUtil.setQuota(user, testQuota);

            final Quota[] quota = GreenMailUtil.getQuota(user, testQuota.quotaRoot);
            assertThat(quota.length).isEqualTo(1);
            assertThat(quota[0].resources.length).isEqualTo(2);

            store.close();
        } finally {
            greenMail.stop();
        }
    }

    final static String SAMPLE_EMAIL = "From - Thu Jan 19 00:30:34 2006\r\n"
            + "X-Account-Key: account245\r\n"
            + "X-UIDL: 11332317636080.2607.mail5,S=833\r\n"
            + "X-Mozilla-Status: 0001\r\n"
            + "X-Mozilla-Status2: 00000000\r\n"
            + "Return-Path: <bertil@surstomming.com>\r\n"
            + "Delivered-To: eivar@blastigen.com\r\n"
            + "Received: (qmail 2376 invoked from network); 19 Jan 2006 02:01:05 -0000\r\n"
            + "Received: from unknown (HELO [192.168.0.5]) (hej@66.245.216.76)\r\n"
            + "\tby mail5.hotmail.com with (RC4-MD5 encrypted) SMTP; Wed, 18 Jan 2006 18:01:05 -0800\r\n"
            + "Message-ID: <43CEF322.7080702@hotmail.com>\r\n"
            + "Date: Wed, 18 Jan 2006 18:02:10 -0800\r\n"
            + "From: Wael Chatila <wael@localhost.com>\r\n"
            + "User-Agent: Mozilla Thunderbird 1.0.7 (Windows/20050923)\r\n"
            + "X-Accept-Language: en-us, en\r\n" + "MIME-Version: 1.0\r\n"
            + "To: Bertil <bertil@localhost.com>\r\n" + "Subject: wassup\r\n"
            + "Content-Type: text/plain; charset=ISO-8859-1; format=flowed\r\n"
            + "Content-Transfer-Encoding: 7bit\r\n" + "\r\n"
            + "Yo wassup Bertil\r\n";
}
