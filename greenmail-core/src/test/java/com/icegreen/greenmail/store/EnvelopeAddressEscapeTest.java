package com.icegreen.greenmail.store;

import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvelopeAddressEscapeTest {
    @Test
    public void envelopeEscapesPersonalName() throws Exception {
        String raw = "Subject: hi\r\n" +
                "From: \"foo\\\"bar\" <a@b.com>\r\n" +
                "\r\n" +
                "body\r\n";
        MimeMessage msg = GreenMailUtil.newMimeMessage(raw);
        SimpleMessageAttributes attrs = new SimpleMessageAttributes(msg, new Date());

        String envelope = attrs.getEnvelope();

        // The sender-controlled personal name must not break the IMAP quoted string.
        assertThat(envelope).contains("foo\\\"bar");
        assertThat(envelope).doesNotContain("\"foo\"bar\"");
    }
}
