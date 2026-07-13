package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class EnvelopeMissingDateTest {

    private SimpleMessageAttributes attributes(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
            new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        return new SimpleMessageAttributes(msg, new Date());
    }

    @Test
    public void rfc822PartWithoutInnerDateFetchesBodyStructure() throws Exception {
        // The inner message has no Date header, so the embedded part carries no envelope date.
        // BODYSTRUCTURE embeds that part's ENVELOPE and must not fail on the missing date.
        String inner = "From: inner@x.com\r\nSubject: hi\r\n\r\nbody\r\n";
        String bs = attributes("From: a@b.com\r\n"
            + "Subject: fwd\r\n"
            + "Content-Type: message/rfc822\r\n"
            + "\r\n" + inner).getBodyStructure(false);
        assertThat(bs).startsWith("(\"MESSAGE\" \"RFC822\"");
    }

    @Test
    public void rfc822PartWithoutInnerDateEmitsNilEnvelopeDate() throws Exception {
        String inner = "From: inner@x.com\r\nSubject: hi\r\n\r\nbody\r\n";
        SimpleMessageAttributes attrs = attributes("From: a@b.com\r\n"
            + "Subject: fwd\r\n"
            + "Content-Type: message/rfc822\r\n"
            + "\r\n" + inner);
        // The embedded ENVELOPE begins with a NIL date instead of a quoted empty string.
        assertThat(attrs.getBodyStructure(false)).contains("(NIL \"hi\"");
    }

    @Test
    public void rfc822AttachmentInMultipartFetchesBodyStructure() throws Exception {
        // Common case: a forwarded mail attached as message/rfc822 whose inner message has no Date.
        String inner = "From: inner@x.com\r\nSubject: hi\r\n\r\nbody\r\n";
        String bs = attributes("From: a@b.com\r\n"
            + "Content-Type: multipart/mixed; boundary=\"B\"\r\n"
            + "\r\n"
            + "--B\r\nContent-Type: text/plain\r\n\r\nsee attached\r\n"
            + "--B\r\nContent-Type: message/rfc822\r\n\r\n" + inner
            + "--B--\r\n").getBodyStructure(false);
        assertThat(bs).contains("\"MESSAGE\" \"RFC822\"");
    }
}
