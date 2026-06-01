package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class BodyStructureParameterQuotingTest {

    private String bodyStructure(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
            new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        return new SimpleMessageAttributes(msg, new Date()).getBodyStructure(true);
    }

    @Test
    public void dispositionParameterTrailingBackslashIsEscaped() throws Exception {
        String bs = bodyStructure("From: a@b.com\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Disposition: attachment; filename=\"evil\\\"\r\n"
            + "\r\nbody\r\n");
        assertThat(bs).contains("\"filename\" \"evil\\\\\"");
    }

    @Test
    public void contentTypeParameterQuoteIsEscaped() throws Exception {
        String bs = bodyStructure("From: a@b.com\r\n"
            + "Content-Type: text/plain; name=\"ev\\\"il\"\r\n"
            + "\r\nbody\r\n");
        assertThat(bs).contains("\"name\" \"ev\\\\\\\"il\"");
    }

    @Test
    public void plainParameterIsUnchanged() throws Exception {
        String bs = bodyStructure("From: a@b.com\r\n"
            + "Content-Type: text/plain; charset=\"utf-8\"\r\n"
            + "\r\nbody\r\n");
        assertThat(bs).contains("\"charset\" \"utf-8\"");
    }
}
