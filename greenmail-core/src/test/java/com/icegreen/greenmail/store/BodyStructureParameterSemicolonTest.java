package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class BodyStructureParameterSemicolonTest {

    private String bodyStructure(String raw) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
            new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        return new SimpleMessageAttributes(msg, new Date()).getBodyStructure(true);
    }

    @Test
    public void semicolonInsideQuotedFilenameKeepsSingleParameter() throws Exception {
        String bs = bodyStructure("From: a@b.com\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Disposition: attachment; filename=\"Invoice; final.pdf\"\r\n"
            + "\r\nbody\r\n");
        assertThat(bs).contains("\"filename\" \"Invoice; final.pdf\"");
    }

    @Test
    public void semicolonInsideQuotedContentTypeParameterKeepsSingleParameter() throws Exception {
        String bs = bodyStructure("From: a@b.com\r\n"
            + "Content-Type: text/plain; name=\"a;b\"\r\n"
            + "\r\nbody\r\n");
        assertThat(bs).contains("\"name\" \"a;b\"");
    }

    @Test
    public void parameterWithoutValueIsIgnored() throws Exception {
        String bs = bodyStructure("From: a@b.com\r\n"
            + "Content-Type: text/plain; charset=us-ascii; broken\r\n"
            + "\r\nbody\r\n");
        assertThat(bs).contains("\"charset\" \"us-ascii\"");
    }
}
