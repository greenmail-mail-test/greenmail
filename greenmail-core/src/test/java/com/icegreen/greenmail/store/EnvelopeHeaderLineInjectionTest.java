package com.icegreen.greenmail.store;

import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvelopeHeaderLineInjectionTest {
    @Test
    public void envelopeDropsCrLfFromDecodedPersonalName() throws Exception {
        // The personal name is an RFC 2047 encoded-word that decodes to bytes containing CRLF.
        // getPersonal() returns "A\r\n* 9 EXISTS\r\n"; encodeWord leaves it unchanged (ASCII).
        String raw = "Subject: hi\r\n" +
                "From: =?UTF-8?Q?A=0D=0A=2A_9_EXISTS=0D=0A?= <a@b.com>\r\n" +
                "\r\n" +
                "body\r\n";
        MimeMessage msg = GreenMailUtil.newMimeMessage(raw);
        SimpleMessageAttributes attrs = new SimpleMessageAttributes(msg, new Date());

        String envelope = attrs.getEnvelope();

        // A decoded personal name must not be able to forge a response line in the FETCH stream.
        assertThat(envelope).doesNotContain("\r").doesNotContain("\n");
        assertThat(envelope).contains("A* 9 EXISTS");
    }
}
