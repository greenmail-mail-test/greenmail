package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The body type and subtype emitted in a FETCH BODYSTRUCTURE response originate
 * from the sender's Content-Type header. They must be escaped before being
 * wrapped in IMAP quoted strings, otherwise a double-quote terminates the
 * quoted string early and injects tokens into the response.
 */
public class BodyStructureTypeQuotingTest {

    private String bodyStructure(String contentType) throws Exception {
        Session session = Session.getInstance(new Properties());
        String raw = "From: a@b.com\r\nTo: c@d.com\r\nSubject: x\r\n"
            + "Content-Type: " + contentType + "\r\n\r\nbody\r\n";
        MimeMessage message = new MimeMessage(session,
            new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        return new SimpleMessageAttributes(message, new Date()).getBodyStructure(true);
    }

    @Test
    public void escapesQuoteInTextSubtype() throws Exception {
        assertThat(bodyStructure("text/pl\"ain"))
            .contains("\"pl\\\"ain\"")
            .doesNotContain("\"pl\"ain\"");
    }

    @Test
    public void escapesQuoteInTypeAndSubtype() throws Exception {
        assertThat(bodyStructure("appli\"cation/oct\"et"))
            .contains("\"APPLI\\\"CATION\"")
            .contains("\"OCT\\\"ET\"");
    }
}
