package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleContentDispositionBodyStructureTest {

    private SimpleMessageAttributes attributes(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
            new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        return new SimpleMessageAttributes(msg, new Date());
    }

    @Test
    public void repeatedContentDispositionDoesNotFailStoring() throws Exception {
        // Two Content-Disposition headers made Header.create throw IllegalArgumentException
        // during construction, crashing message store and FETCH BODYSTRUCTURE.
        String bs = attributes("From: a@b.com\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Disposition: attachment; filename=\"a.txt\"\r\n"
            + "Content-Disposition: inline\r\n"
            + "\r\nbody\r\n").getBodyStructure(true);
        // First occurrence is used for the disposition extension field.
        assertThat(bs).startsWith("(\"text\" \"plain\"");
        assertThat(bs).contains("(\"attachment\" (\"filename\" \"a.txt\"))");
    }

    @Test
    public void singleContentDispositionIsUnchanged() throws Exception {
        String bs = attributes("From: a@b.com\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Disposition: inline\r\n"
            + "\r\nbody\r\n").getBodyStructure(true);
        assertThat(bs).contains("\"inline\"");
    }
}
