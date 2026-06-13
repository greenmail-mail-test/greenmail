package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleMessageAttributesInReplyToTest {

    private SimpleMessageAttributes attributesFor(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        return new SimpleMessageAttributes(msg, new Date());
    }

    @Test
    public void testInReplyToQuotingAndEscaping() throws Exception {
        String raw = "From: a@b.com\r\n" +
                "Subject: test\r\n" +
                "In-Reply-To: <some-id@host.com> (my comment)\r\n" +
                "\r\n" +
                "body\r\n";
        String envelope = attributesFor(raw).getEnvelope();
        // Envelope structure: (date subject from sender reply-to to cc bcc in-reply-to message-id)
        
        assertThat(envelope).contains("\"<some-id@host.com> (my comment)\"");
    }

    @Test
    public void testInReplyToWithQuotes() throws Exception {
        String raw = "From: a@b.com\r\n" +
                "Subject: test\r\n" +
                "In-Reply-To: \"quoted string\" <id@host.com>\r\n" +
                "\r\n" +
                "body\r\n";
        String envelope = attributesFor(raw).getEnvelope();
        assertThat(envelope).contains("\"\\\"quoted string\\\" <id@host.com>\"");
    }
}
