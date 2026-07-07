package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentTypeWithoutSubtypeTest {

    private SimpleMessageAttributes attributes(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
            new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        return new SimpleMessageAttributes(msg, new Date());
    }

    @Test
    public void contentTypeWithoutSlashDoesNotFailStoring() throws Exception {
        // Content-Type header carries no subtype ("no '/'"). Parsing must not throw.
        String bs = attributes("From: a@b.com\r\n"
            + "Content-Type: nonsense\r\n"
            + "\r\nbody\r\n").getBodyStructure(true);
        assertThat(bs).startsWith("(\"text\" \"plain\"");
    }

    @Test
    public void emptyContentTypeDoesNotFailStoring() throws Exception {
        String bs = attributes("From: a@b.com\r\n"
            + "Content-Type: \r\n"
            + "\r\nbody\r\n").getBodyStructure(true);
        assertThat(bs).startsWith("(\"text\" \"plain\"");
    }

    @Test
    public void contentTypeWithoutSlashButWithParametersDoesNotFailStoring() throws Exception {
        String bs = attributes("From: a@b.com\r\n"
            + "Content-Type: weird; charset=us-ascii\r\n"
            + "\r\nbody\r\n").getBodyStructure(true);
        assertThat(bs).startsWith("(\"text\" \"plain\"");
    }
}
