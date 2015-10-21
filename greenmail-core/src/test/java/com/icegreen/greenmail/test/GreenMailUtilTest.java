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
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 29, 2006
 */
public class GreenMailUtilTest {
    @Test
    public void testMimeMessageLoading() throws MessagingException {
        MimeMessage message = GreenMailUtil.newMimeMessage(SAMPLE_EMAIL);
        assertEquals("wassup", message.getSubject());
    }

    @Test
    public void testGetBody() throws MessagingException, IOException {
        MimeMessage message = GreenMailUtil.newMimeMessage(SAMPLE_EMAIL);
        String body = GreenMailUtil.getBody(message);
        assertEquals("Yo wassup Bertil", body.trim());
    }

    @Test
    public void testSendTextEmailTest() throws Exception {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP);
        try {
            greenMail.setUser("foo@localhost", "pwd");
            greenMail.start();
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost",
                    "Test subject", "Test message", ServerSetupTest.SMTP);
            greenMail.waitForIncomingEmail(1);

            Store store = greenMail.getImap().createStore();
            store.connect("foo@localhost", "pwd");
            try {
                Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);
                Message[] msgs = folder.getMessages();
                assertTrue(null != msgs && msgs.length == 1);
                Message m = msgs[0];
                assertEquals("Test subject", m.getSubject());
                Address a[] = m.getRecipients(Message.RecipientType.TO);
                assertTrue(null != a && a.length == 1
                        && a[0].toString().equals("foo@localhost"));
                a = m.getFrom();
                assertTrue(null != a && a.length == 1
                        && a[0].toString().equals("bar@localhost"));
                assertTrue(m.getContentType().toLowerCase()
                        .startsWith("text/plain"));
                assertEquals("Test message", m.getContent());
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

            assertEquals(0, GreenMailUtil.getQuota(user, testQuota.quotaRoot).length);
            GreenMailUtil.setQuota(user, testQuota);

            final Quota[] quota = GreenMailUtil.getQuota(user, testQuota.quotaRoot);
            assertEquals(1, quota.length);
            assertEquals(2, quota[0].resources.length);

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
