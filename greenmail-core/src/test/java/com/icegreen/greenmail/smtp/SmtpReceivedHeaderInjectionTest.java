package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The DATA command prepends a {@code Return-Path}/{@code Received} trace header built from the
 * client-supplied HELO/EHLO argument and the MAIL FROM return path. Neither value may contain
 * CR or LF; if it does, the trace header is split and the sender can inject additional header
 * lines into the stored message that a recipient later retrieves.
 */
public class SmtpReceivedHeaderInjectionTest {
    private GreenMail greenMail;

    @Before
    public void setup() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @After
    public void tearDown() {
        greenMail.stop();
    }

    @Test
    public void heloArgumentCanNotInjectHeaderViaBareLineFeed() throws Exception {
        String host = ServerSetupTest.SMTP.getBindAddress();
        int port = ServerSetupTest.SMTP.getPort();

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            assertThat(in.readLine()).startsWith("220");

            // A bare LF inside the HELO argument is not a CRLF, so the command reader keeps
            // reading until the real CRLF; the argument carries the embedded line break.
            send(out, "HELO injected\nX-Injected: bar\r\n");
            assertThat(in.readLine()).startsWith("250");

            send(out, "MAIL FROM:<sender@localhost>\r\n");
            assertThat(in.readLine()).startsWith("250");

            send(out, "RCPT TO:<to@localhost>\r\n");
            assertThat(in.readLine()).startsWith("250");

            send(out, "DATA\r\n");
            assertThat(in.readLine()).startsWith("354");

            send(out, "Subject: hi\r\n\r\nbody\r\n.\r\n");
            assertThat(in.readLine()).startsWith("250");

            send(out, "QUIT\r\n");
        }

        assertThat(greenMail.waitForIncomingEmail(1)).isTrue();
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getHeader("X-Injected")).isNull();
    }

    @Test
    public void mailFromReturnPathCanNotInjectHeaderViaEncodedWord() throws Exception {
        String host = ServerSetupTest.SMTP.getBindAddress();
        int port = ServerSetupTest.SMTP.getPort();

        // decodeText() decodes the RFC 2047 encoded-word, turning the address into
        // "a\r\nX-Injected2: bar" before it is written into the Return-Path header.
        String encodedWord = "=?utf-8?B?YQ0KWC1JbmplY3RlZDI6IGJhcg==?=";

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            assertThat(in.readLine()).startsWith("220");

            send(out, "HELO localhost\r\n");
            assertThat(in.readLine()).startsWith("250");

            send(out, "MAIL FROM:<" + encodedWord + ">\r\n");
            assertThat(in.readLine()).startsWith("250");

            send(out, "RCPT TO:<to@localhost>\r\n");
            assertThat(in.readLine()).startsWith("250");

            send(out, "DATA\r\n");
            assertThat(in.readLine()).startsWith("354");

            send(out, "Subject: hi\r\n\r\nbody\r\n.\r\n");
            assertThat(in.readLine()).startsWith("250");

            send(out, "QUIT\r\n");
        }

        assertThat(greenMail.waitForIncomingEmail(1)).isTrue();
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages[0].getHeader("X-Injected2")).isNull();
    }

    private static void send(OutputStream out, String data) throws Exception {
        out.write(data.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
