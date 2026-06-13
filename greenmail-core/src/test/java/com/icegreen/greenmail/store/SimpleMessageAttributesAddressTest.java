package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleMessageAttributesAddressTest {

    private SimpleMessageAttributes attributesFor(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        return new SimpleMessageAttributes(msg, new Date());
    }

    @Test
    public void testAddressWithQuotes() throws Exception {
        // RFC 5322 allows quotes in local part if it's a quoted-string
        String raw = "From: \"personal\" <\"foo\\\"bar\"@host.com>\r\n" +
                "Subject: test\r\n" +
                "\r\n" +
                "body\r\n";
        String envelope = attributesFor(raw).getEnvelope();
        // The address part of ENVELOPE for From:
        // (personal-name NIL mailbox-name host-name)
        // Expected mailbox-name: "foo\"bar"
        
        assertThat(envelope).contains("\"foo\\\"bar\" \"host.com\"");
    }

    @Test
    public void testAddressWithBackslash() throws Exception {
        String raw = "From: <foo\\\\bar@host.com>\r\n" +
                "Subject: test\r\n" +
                "\r\n" +
                "body\r\n";
        String envelope = attributesFor(raw).getEnvelope();
        // Expected mailbox-name: "foo\\bar"
        assertThat(envelope).contains("\"foo\\\\\\\\bar\" \"host.com\"");
    }
}
