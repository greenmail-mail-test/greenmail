package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
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

public class ImapIdleExpungeTest {
    private static final String CRLF = "\r\n";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    /**
     * An EXPUNGE response renumbers the messages on the client, so an expunge by another
     * session must reach an idling session exactly once, and after the flag update which
     * still refers to the expunged message.
     */
    @Test
    public void idleReportsExpungeOfOtherSessionOnlyOnce() throws Exception {
        assertThat(idleWhileOtherSessionExpunges(false)).containsExactly(
            "* 1 FETCH (FLAGS (\\Deleted))",
            "* 1 EXPUNGE",
            "a3 OK IDLE completed.");
    }

    /**
     * A new message, which is still to be reported with its RECENT count once the client is
     * done with idling, must not get in the way of reporting the expunge.
     */
    @Test
    public void idleReportsExpungeOnlyOnceAfterNewMessage() throws Exception {
        assertThat(idleWhileOtherSessionExpunges(true))
            .filteredOn(line -> line.endsWith("EXPUNGE"))
            .containsExactly("* 1 EXPUNGE");
    }

    private List<String> idleWhileOtherSessionExpunges(boolean newMessageWhileIdling) throws IOException {
        greenMail.setUser("foo@localhost", "pwd");
        for (int i = 0; i < 3; i++) {
            sendMessage();
        }
        greenMail.waitForIncomingEmail(3);

        try (Socket idleSocket = new Socket(greenMail.getImap().getBindTo(), greenMail.getImap().getPort());
             OutputStream idleOut = idleSocket.getOutputStream();
             BufferedReader idleIn = new BufferedReader(
                 new InputStreamReader(idleSocket.getInputStream(), StandardCharsets.ISO_8859_1));
             Socket socket = new Socket(greenMail.getImap().getBindTo(), greenMail.getImap().getPort());
             OutputStream out = socket.getOutputStream();
             BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1))) {
            idleIn.readLine(); // greeting
            send(idleOut, "a1 LOGIN foo@localhost pwd" + CRLF);
            assertThat(lastLine(idleIn, "a1")).startsWith("a1 OK");
            send(idleOut, "a2 SELECT INBOX" + CRLF);
            assertThat(lastLine(idleIn, "a2")).startsWith("a2 OK");
            send(idleOut, "a3 IDLE" + CRLF);
            assertThat(idleIn.readLine()).startsWith("+");

            if (newMessageWhileIdling) {
                sendMessage();
            }

            // Another session deletes the first message while the first session is idling
            in.readLine(); // greeting
            send(out, "b1 LOGIN foo@localhost pwd" + CRLF);
            assertThat(lastLine(in, "b1")).startsWith("b1 OK");
            send(out, "b2 SELECT INBOX" + CRLF);
            assertThat(lastLine(in, "b2")).startsWith("b2 OK");
            send(out, "b3 STORE 1 +FLAGS (\\Deleted)" + CRLF);
            assertThat(lastLine(in, "b3")).startsWith("b3 OK");
            send(out, "b4 EXPUNGE" + CRLF);
            assertThat(lastLine(in, "b4")).startsWith("b4 OK");

            send(idleOut, "DONE" + CRLF);
            List<String> idleResponses = readUntilTag(idleIn, "a3");

            // Nothing is left over for the next command
            send(idleOut, "a4 NOOP" + CRLF);
            assertThat(readUntilTag(idleIn, "a4")).containsExactly("a4 OK NOOP completed.");

            return idleResponses;
        }
    }

    private void sendMessage() {
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test subject", "Test message",
            greenMail.getSmtp().getServerSetup());
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
