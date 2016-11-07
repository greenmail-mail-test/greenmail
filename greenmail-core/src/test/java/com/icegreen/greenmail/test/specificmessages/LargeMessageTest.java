package com.icegreen.greenmail.test.specificmessages;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests sending and receiving large messages
 */
public class LargeMessageTest {
    @Rule
    public GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    @StoreChooser(store="file,memory")
    public void testLargeMessageTextAndAttachment() throws MessagingException, IOException {
        String to = "to@localhost";
        GreenMailUtil.sendAttachmentEmail(to, "from@localhost", "Subject", createLargeString(),
                createLargeByteArray(), "application/blubb", "file", "descr",
                greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheck(greenMail.getPop3(), to);
        retrieveAndCheck(greenMail.getImap(), to);
    }

    @Test
    @StoreChooser(store="file,memory")
    public void testLargeMessageBody() throws MessagingException, IOException {
        String to = "to@localhost";
        GreenMailUtil.sendMessageBody(to, "from@localhost", "Subject", createLargeByteArray(), "application/blubb",
                greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheckBody(greenMail.getPop3(), to);
        retrieveAndCheckBody(greenMail.getImap(), to);
    }

    /**
     * Retrieve message from retriever and check the attachment and text content
     *
     * @param server Server to read from
     * @param to     Account to retrieve
     */
    private void retrieveAndCheck(AbstractServer server, String to) throws MessagingException, IOException {
        try (Retriever retriever = new Retriever(server)) {
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
    }

    /**
     * Retrieve message from retriever and check the body content
     *
     * @param server Server to read from
     * @param to     Account to retrieve
     */
    private void retrieveAndCheckBody(AbstractServer server, String to) throws MessagingException, IOException {
        try (Retriever retriever = new Retriever(server)) {
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
        return new String(new char[100000]).replace("\0", "a");
    }
}
