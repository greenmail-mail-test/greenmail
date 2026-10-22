package com.icegreen.greenmail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Protocol commands are case-insensitive, and some clients send them in lower case
 * (for example, Python's smtplib sends "mail FROM:" and "rcpt TO:").
 * <p>
 * In the Turkish locale, "i".toUpperCase() is the dotted capital I (U+0130), so commands
 * like "mail", "quit", "list" or "login" must not be upper-cased using the default locale.
 */
public class LocaleIndependentCommandTest {
    private Locale defaultLocale;
    private GreenMail greenMail;

    @Before
    public void setup() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));

        greenMail = new GreenMail(ServerSetupTest.SMTP_POP3_IMAP);
        greenMail.start();
        greenMail.setUser("to@localhost", "to", "secret");
    }

    @After
    public void tearDown() {
        try {
            greenMail.stop();
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void smtpLowerCaseCommands() throws Exception {
        try (Session session = new Session(ServerSetupTest.SMTP)) {
            assertThat(session.readLine()).startsWith("220");

            session.send("helo localhost");
            assertThat(session.readLine()).startsWith("250");

            session.send("mail FROM:<from@localhost>");
            assertThat(session.readLine()).startsWith("250");

            session.send("rcpt TO:<to@localhost>");
            assertThat(session.readLine()).startsWith("250");

            session.send("data");
            assertThat(session.readLine()).startsWith("354");

            session.send("Subject: test\r\n\r\nbody\r\n.");
            assertThat(session.readLine()).startsWith("250");

            session.send("quit");
            assertThat(session.readLine()).startsWith("221");
        }

        assertThat(greenMail.waitForIncomingEmail(1)).isTrue();
    }

    @Test
    public void pop3LowerCaseCommands() throws Exception {
        try (Session session = new Session(ServerSetupTest.POP3)) {
            assertThat(session.readLine()).startsWith("+OK");

            session.send("user to");
            assertThat(session.readLine()).startsWith("+OK");

            session.send("pass secret");
            assertThat(session.readLine()).startsWith("+OK");

            session.send("list");
            assertThat(session.readLine()).startsWith("+OK");
            assertThat(session.readLine()).isEqualTo(".");

            session.send("quit");
            assertThat(session.readLine()).startsWith("+OK");
        }
    }

    @Test
    public void imapLowerCaseCommands() throws Exception {
        try (Session session = new Session(ServerSetupTest.IMAP)) {
            assertThat(session.readLine()).startsWith("* OK");

            session.send("a1 login to secret");
            assertThat(session.readTaggedResponse("a1")).startsWith("a1 OK");

            session.send("a2 list \"\" \"*\"");
            assertThat(session.readTaggedResponse("a2")).startsWith("a2 OK");

            session.send("a3 logout");
            assertThat(session.readTaggedResponse("a3")).startsWith("a3 OK");
        }
    }

    /**
     * Minimal line-based client for talking to a protocol server over a plain socket.
     */
    private static class Session implements AutoCloseable {
        private final Socket socket;
        private final OutputStream out;
        private final BufferedReader in;

        Session(ServerSetup setup) throws IOException {
            socket = new Socket(setup.getBindAddress(), setup.getPort());
            out = socket.getOutputStream();
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        }

        void send(String line) throws IOException {
            out.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        String readLine() throws IOException {
            return in.readLine();
        }

        String readTaggedResponse(String tag) throws IOException {
            String line;
            do {
                line = in.readLine();
            } while (line != null && !line.startsWith(tag + " "));
            return line;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
