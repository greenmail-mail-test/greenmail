package com.icegreen.greenmail.store;

import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class StoredMessageIdTest {
    private static final String ID = "<custom@example.com>";

    private MimeMessage message() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom("sender@example.com");
        message.setText("hello");
        return message;
    }

    @Test
    public void preservesCustomIdDuringDelivery() throws Exception {
        GreenMail greenMail = new GreenMail();
        MimeMessage message = message();
        message.setHeader("Message-ID", ID);
        greenMail.setUser("recipient@example.com", "password").deliver(message);
        assertThat(greenMail.getReceivedMessages()[0].getMessageID()).isEqualTo(ID);
    }

    @Test
    public void preservesCustomIdAcrossDeliveries() throws Exception {
        GreenMail greenMail = new GreenMail();
        MimeMessage message = message();
        message.setHeader("Message-ID", ID);
        greenMail.setUser("first@example.com", "password").deliver(message);
        greenMail.setUser("second@example.com", "password").deliver(message);
        assertThat(message.getMessageID()).isEqualTo(ID);
        for (MimeMessage received : greenMail.getReceivedMessages()) {
            assertThat(received.getMessageID()).isEqualTo(ID);
        }
    }

    @Test
    public void preservesCustomIdInEnvelope() throws Exception {
        MimeMessage message = message();
        message.setHeader("Message-ID", ID);
        StoredMessage stored = new StoredMessage(message, new Date(), 1);
        assertThat(stored.getAttributes().getEnvelope()).endsWith("\"" + ID + "\")");
    }

    @Test
    public void generatesIdWhenAbsent() throws Exception {
        StoredMessage stored = new StoredMessage(message(), new Date(), 1);
        String id = stored.getMimeMessage().getMessageID();
        assertThat(id).isNotNull();
        assertThat(stored.getAttributes().getEnvelope()).endsWith("\"" + id + "\")");
    }

    @Test
    public void preservesSavedId() throws Exception {
        MimeMessage message = message();
        message.saveChanges();
        String id = message.getMessageID();
        StoredMessage stored = new StoredMessage(message, new Date(), 1);
        assertThat(stored.getMimeMessage().getMessageID()).isEqualTo(id);
    }

    @Test
    public void preservesReceivedId() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(("Message-ID: " + ID + "\r\nFrom: sender@example.com\r\n\r\nhello")
                        .getBytes(StandardCharsets.US_ASCII)));
        StoredMessage stored = new StoredMessage(message, new Date(), 1);
        assertThat(stored.getMimeMessage().getMessageID()).isEqualTo(ID);
    }

    @Test
    public void retainsContentAndMetadata() throws Exception {
        MimeMessage message = message();
        message.setHeader("Message-ID", ID);
        Date received = new Date(123456789L);
        StoredMessage stored = new StoredMessage(message, received, 42);
        assertThat(stored.getMimeMessage().getContent().toString().trim()).isEqualTo("hello");
        assertThat(stored.getMimeMessage().getReceivedDate()).isEqualTo(received);
        assertThat(stored.getUid()).isEqualTo(42);
        assertThat(stored.getMimeMessage().getFrom()[0].toString()).isEqualTo("sender@example.com");
    }
}
