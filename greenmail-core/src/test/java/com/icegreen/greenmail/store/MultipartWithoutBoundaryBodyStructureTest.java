package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class MultipartWithoutBoundaryBodyStructureTest {

    private SimpleMessageAttributes attributes(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
            new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        return new SimpleMessageAttributes(msg, new Date());
    }

    @Test
    public void multipartWithoutBoundaryFetchesBodyStructure() throws Exception {
        // A sender declares multipart/* but the body can not be split into parts (no boundary).
        // The message stores fine but leaves the parts array unset, so BODYSTRUCTURE must not
        // dereference it and fail the whole FETCH.
        SimpleMessageAttributes attrs = attributes("From: a@b.com\r\n"
            + "Content-Type: multipart/mixed\r\n"
            + "\r\nnot really multipart\r\n");
        assertThatCode(() -> attrs.getBodyStructure(true)).doesNotThrowAnyException();
        assertThat(attrs.getBodyStructure(true)).contains("\"mixed\"");
    }

    @Test
    public void multipartAlternativeWithoutBoundaryFetchesBodyStructure() throws Exception {
        SimpleMessageAttributes attrs = attributes("From: a@b.com\r\n"
            + "Content-Type: multipart/alternative\r\n"
            + "\r\nbody\r\n");
        assertThatCode(() -> attrs.getBodyStructure(false)).doesNotThrowAnyException();
        assertThat(attrs.getBodyStructure(false)).contains("\"alternative\"");
    }
}
