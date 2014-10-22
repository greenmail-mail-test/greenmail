package com.icegreen.greenmail.test.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Tests sending and receiving large messages
 */
public class LargeMessageTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    public void testLargeMessageTextAndAttachment() throws MessagingException, IOException {
        String to = "to@localhost";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost", "Subject", createLargeString(),
                createLargeByteArray(), "application/blubb", "file", "descr",
                greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheck(new Retriever(greenMail.getPop3()), to);
        retrieveAndCheck(new Retriever(greenMail.getImap()), to);
    }

    @Test
    public void testLargeMessageBody() throws MessagingException, IOException {
        String to = "to@localhost";
        GreenMailUtil.sendMessageBody(to, "from@localhost", "Subject", createLargeByteArray(), "application/blubb",
                greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheckBody(new Retriever(greenMail.getPop3()), to);
        retrieveAndCheckBody(new Retriever(greenMail.getImap()), to);
    }

    /**
     * Retrieve message from retriever and check the attachment and text content
     *
     * @param retriever Retriever to read from
     * @param to        Account to retrieve
     */
    private void retrieveAndCheck(Retriever retriever, String to) throws MessagingException, IOException {
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        Message message = messages[0];
        assertTrue(message.getContentType().startsWith("multipart/mixed"));
        MimeMultipart body = (MimeMultipart) message.getContent();
        assertTrue(body.getContentType().startsWith("multipart/mixed"));
        assertEquals(2, body.getCount());

        // Message text
        final BodyPart textPart = body.getBodyPart(0);
        String text = (String) textPart.getContent();
        assertEquals(createLargeString(), text);

        final BodyPart attachment = body.getBodyPart(1);
        assertTrue(attachment.getContentType().equalsIgnoreCase("application/blubb; name=file"));
        InputStream attachmentStream = (InputStream) attachment.getContent();
        byte[] bytes = IOUtils.toByteArray(attachmentStream);
        assertArrayEquals(createLargeByteArray(), bytes);
    }

    /**
     * Retrieve message from retriever and check the body content
     *
     * @param retriever Retriever to read from
     * @param to        Account to retrieve
     */
    private void retrieveAndCheckBody(Retriever retriever, String to) throws MessagingException, IOException {
        Message[] messages = retriever.getMessages(to);
        assertEquals(1, messages.length);
        Message message = messages[0];
        assertTrue(message.getContentType().equalsIgnoreCase("application/blubb"));

        // Check content
        InputStream contentStream = (InputStream) message.getContent();
        byte[] bytes = IOUtils.toByteArray(contentStream);
        assertArrayEquals(createLargeByteArray(), bytes);

        // Dump complete mail message. This leads to a FETCH command without section or "len" specified.
        message.writeTo(new ByteArrayOutputStream());
    }

    /**
     * @return approx 100 kb array filled with the number 100
     */
    private byte[] createLargeByteArray() {
        byte[] bytes = new byte[100 * 1024 + 100];
        Arrays.fill(bytes, (byte) 100);
        return bytes;
    }

    /**
     * @return approx 100 kb String filled with the letter 'a'
     */
    private String createLargeString() {
        return StringUtils.repeat('a', 100000);
    }
}
