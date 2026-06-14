package com.icegreen.greenmail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class SmtpUtf8IT {
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
    public void testSmtpUtf8() throws Exception {
        String host = ServerSetupTest.SMTP.getBindAddress();
        int port = ServerSetupTest.SMTP.getPort();

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            assertThat(in.readLine()).startsWith("220");

            out.print("EHLO localhost\r\n");
            out.flush();
            boolean hasSmtpUtf8 = false;
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("250 SMTPUTF8")) {
                    hasSmtpUtf8 = true;
                }
                if (line.startsWith("250 ")) {
                    break;
                }
            }
            assertThat(hasSmtpUtf8).isTrue();

            String utf8Email = "tést@localhost";
            out.print("MAIL FROM:<" + utf8Email + "> SMTPUTF8\r\n");
            out.flush();
            assertThat(in.readLine()).startsWith("250 OK");

            out.print("RCPT TO:<" + utf8Email + ">\r\n");
            out.flush();
            assertThat(in.readLine()).startsWith("250 OK");

            out.print("DATA\r\n");
            out.flush();
            assertThat(in.readLine()).startsWith("354");

            out.print("Subject: UTF-8 test\r\n");
            out.print("From: <" + utf8Email + ">\r\n");
            out.print("Content-Type: text/plain; charset=utf-8\r\n");
            out.print("\r\n");
            out.print("Content with UTF-8: tést\r\n");
            out.print(".\r\n");
            out.flush();
            assertThat(in.readLine()).startsWith("250 OK");

            out.print("QUIT\r\n");
            out.flush();
        }

        assertThat(greenMail.waitForIncomingEmail(1)).isTrue();
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        MimeMessage msg = messages[0];
        assertThat(msg.getFrom()[0].toString()).contains("tést@localhost");
        assertThat(msg.getContent().toString()).contains("Content with UTF-8: tést");
    }

    @Test
    public void testSmtpUtf8ViaJavaMail() throws MessagingException {
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", ServerSetupTest.SMTP.getBindAddress());
        props.setProperty("mail.smtp.port", String.valueOf(ServerSetupTest.SMTP.getPort()));
        props.setProperty("mail.mime.allowutf8", "true");
        props.setProperty("mail.smtp.smtputf8", "true");

        Session session = Session.getInstance(props);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("fóó@localhost"));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("bár@localhost"));
        message.setSubject("SMTPUTF8 test");
        message.setText("Content with UTF-8: tést");

        Transport.send(message);

        assertThat(greenMail.waitForIncomingEmail(1)).isTrue();
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertThat(messages).hasSize(1);
        MimeMessage msg = messages[0];
        assertThat(msg.getFrom()[0].toString()).contains("fóó@localhost");
    }
}
