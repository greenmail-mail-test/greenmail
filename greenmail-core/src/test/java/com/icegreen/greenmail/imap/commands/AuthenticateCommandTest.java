package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticateCommandTest {
    private static final String CRLF = "\r\n";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.IMAP);

    @Test
    public void malformedXoauth2InitialResponseReturnsNoAndKeepsConnection() throws IOException {
        greenMail.setUser("foo@localhost", "pwd");
        String host = greenMail.getImap().getBindTo();
        int port = greenMail.getImap().getPort();
        try (Socket socket = new Socket(host, port);
             PrintStream out = new PrintStream(socket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            in.readLine(); // Greeting

            // base64("user=x"): a decoded XOAUTH2 message without the ^A separator, so
            // SaslXoauth2Message.parse rejects it. The server must answer with a tagged
            // NO instead of closing the connection.
            out.print("a1 AUTHENTICATE XOAUTH2 " + Base64.getEncoder().encodeToString(
                "user=x".getBytes(StandardCharsets.UTF_8)) + CRLF);
            List<String> authLines = readUntilTag(in, "a1");
            assertThat(authLines).anyMatch(line ->
                line.startsWith("a1 NO ") && line.contains("Invalid XOAUTH2 authentication string"));

            // The connection must survive the failed attempt so the client can fall back to LOGIN.
            out.print("a2 LOGIN foo@localhost pwd" + CRLF);
            List<String> loginLines = readUntilTag(in, "a2");
            assertThat(loginLines).anyMatch(line -> line.startsWith("a2 OK "));
        }
    }

    private static List<String> readUntilTag(BufferedReader in, String tag) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            lines.add(line);
            if (line.startsWith(tag + " ")) {
                break;
            }
        }
        return lines;
    }
}
