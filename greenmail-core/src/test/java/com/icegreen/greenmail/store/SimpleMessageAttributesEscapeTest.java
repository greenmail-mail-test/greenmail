package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleMessageAttributesEscapeTest {

    private SimpleMessageAttributes attributesFor(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        return new SimpleMessageAttributes(msg, new Date());
    }

    @Test
    public void textPartBodyStructureEscapesContentDescription() throws Exception {
        String raw = "From: a@b.com\r\n" +
                "Subject: x\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Description: he\"llo\\world\r\n" +
                "\r\n" +
                "body\r\n";
        String bs = attributesFor(raw).getBodyStructure(false);
        assertThat(bs).contains("\"he\\\"llo\\\\world\"");
    }

    @Test
    public void nonTextPartBodyStructureEscapesContentDescription() throws Exception {
        String raw = "From: a@b.com\r\n" +
                "Subject: x\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Description: ev\"il\r\n" +
                "\r\n" +
                "body\r\n";
        String bs = attributesFor(raw).getBodyStructure(false);
        assertThat(bs).contains("\"ev\\\"il\"");
    }
}
