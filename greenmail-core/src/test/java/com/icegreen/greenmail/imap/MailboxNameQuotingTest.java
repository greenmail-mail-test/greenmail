package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MailboxNameQuotingTest {
    private static final String CRLF = "\r\n";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    @Test
    public void listAndStatusEscapeQuoteInMailboxName() throws Exception {
        greenMail.setUser("foo@localhost", "pwd");
        String host = greenMail.getImap().getBindTo();
        int port = greenMail.getImap().getPort();
        try (Socket socket = new Socket(host, port);
             PrintStream out = new PrintStream(socket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            in.readLine(); // greeting
            out.print("a1 LOGIN foo@localhost pwd" + CRLF);
            readUntilTag(in, "a1");
            // Mailbox name containing a double quote: ab"cd
            out.print("a2 CREATE \"ab\\\"cd\"" + CRLF);
            readUntilTag(in, "a2");
            out.print("a3 LIST \"\" \"*\"" + CRLF);
            List<String> listLines = readUntilTag(in, "a3");
            out.print("a4 STATUS \"ab\\\"cd\" (MESSAGES)" + CRLF);
            List<String> statusLines = readUntilTag(in, "a4");

            String listLine = listLines.stream().filter(l -> l.contains("cd")).findFirst().orElseThrow();
            String statusLine = statusLines.stream().filter(l -> l.contains("cd")).findFirst().orElseThrow();
            assertThat(listLine).contains("\"ab\\\"cd\"");
            assertThat(statusLine).contains("\"ab\\\"cd\"");
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
