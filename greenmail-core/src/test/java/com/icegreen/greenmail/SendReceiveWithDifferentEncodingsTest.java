package com.icegreen.greenmail;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Rule;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class SendReceiveWithDifferentEncodingsTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_POP3);

    @Test
    public void testSendUtf8EncodedMessage() throws MessagingException, IOException {
        testSendingAndRetrievingMaintainsEncoding(StandardCharsets.UTF_8);
    }

    @Test
    public void testSendIso8859EncodedMessage() throws MessagingException, IOException {
        testSendingAndRetrievingMaintainsEncoding(StandardCharsets.ISO_8859_1);
    }

    private void testSendingAndRetrievingMaintainsEncoding(Charset charset) throws MessagingException, IOException {
        Session session = GreenMailUtil.getSession(ServerSetupTest.SMTP, new Properties());
        MimeMessage mimeMessage = new MimeMessage(session, mailDataInputStream(charset));
        GreenMailUtil.sendMimeMessage(mimeMessage);
        String sentMailText = new String(getBytes(mimeMessage), charset);
        assertThat(sentMailText).contains("Schön");
        greenMail.waitForIncomingEmail(1000, 1);
        try (Retriever retriever = new Retriever(greenMail.getPop3())) {
            MimeMessage receivedMessage = (MimeMessage) retriever.getMessages("bar@example.com")[0];

            // Verify that the Message's raw data is in the correct encoding
            String receivedPureMessage = new String(getBytes(receivedMessage), charset);
            assertThat(receivedPureMessage).contains("Schön");

            // Verify that the Message's 'getContent' method correctly determines the charset when returning the content.
            // Note that here, we retrieve a String without explicitly providing the encoding.
            String content = (String) receivedMessage.getContent();
            assertThat(content).contains("Schön");
        }
    }

    /**
     * @param charset the Charset which should be used to encode the sample email
     * @return an InputStream to create a MimeMessage from, which uses the specified encoding and
     * sets the corresponding email-header "Content-Type" accordingly.
     */
    private InputStream mailDataInputStream(Charset charset) {
        return new ByteArrayInputStream(String.format(RAW_MAIL_STRING, charset.toString()).getBytes(charset));
    }

    private byte[] getBytes(MimeMessage message) throws MessagingException, IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        message.writeTo(bos);
        return bos.toByteArray();
    }

    private static final String RAW_MAIL_STRING =
        "From - Fri Feb 18 10:28:22 2022\r\n" +
        "Return-Path: <foo@example.com>\r\n" +
        "Received: from 127.0.0.1 (HELO [127.0.0.1]); Fri Feb 18 10:28:21 CET 2022\r\n" +
        "Message-ID: <bf6c9320-9c2b-2063-de0a-bfde973b481e@example.com>\r\n" +
        "Date: Fri, 18 Feb 2022 10:28:21 +0100\r\n" +
        "MIME-Version: 1.0\r\n" +
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101\r\n" +
        " Thunderbird/91.6.1\r\n" +
        "To: bar@example.com\r\n" +
        "From: \"Franz O. Oskar\" <foo@example.com>\r\n" +
        "Subject: =?UTF-8?B?U3ViasOka3Qy?=\r\n" +
        "Content-Type: text/plain; charset=%s; format=flowed\r\n" +
        "Content-Transfer-Encoding: 8bit\r\n" +
        "\r\n" +
        "Schön\r\n";

}
