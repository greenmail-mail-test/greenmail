package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MailboxNameLiteralTest {
    private static final String CRLF = "\r\n";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    /**
     * A mailbox name is an astring, so the quoted and the literal form of the same octets
     * must address the same mailbox, and two different payloads must stay different names.
     */
    @Test
    public void quotedAndLiteralMailboxNameOfSameOctetsAddressSameMailbox() throws Exception {
        greenMail.setUser("foo@localhost", "pwd");
        try (Socket socket = new Socket(greenMail.getImap().getBindTo(), greenMail.getImap().getPort());
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))) {
            in.readLine(); // greeting
            send(out, "a1 LOGIN foo@localhost pwd" + CRLF);
            assertThat(lastLine(in, "a1")).startsWith("a1 OK");

            send(out, "a2 CREATE \"ä\"" + CRLF);
            assertThat(lastLine(in, "a2")).startsWith("a2 OK");

            // Same single octet, now sent as a literal
            send(out, "a3 CREATE {1+}" + CRLF + "ä" + CRLF);
            assertThat(lastLine(in, "a3")).startsWith("a3 NO");

            // A different octet is a different mailbox
            send(out, "a4 CREATE {1+}" + CRLF + "ü" + CRLF);
            assertThat(lastLine(in, "a4")).startsWith("a4 OK");

            send(out, "a5 LIST \"\" \"*\"" + CRLF);
            List<String> listed = readUntilTag(in, "a5");
            // Modified UTF-7 of U+00E4 and U+00FC
            assertThat(listed).filteredOn(l -> l.startsWith("* LIST"))
                .anySatisfy(l -> assertThat(l).endsWith("\"&AOQ-\""))
                .anySatisfy(l -> assertThat(l).endsWith("\"&APw-\""))
                .hasSize(3); // INBOX and the two created mailboxes
        }
    }

    private static void send(OutputStream out, String command) throws IOException {
        out.write(command.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }

    private static String lastLine(BufferedReader in, String tag) throws IOException {
        List<String> lines = readUntilTag(in, tag);
        return lines.get(lines.size() - 1);
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
