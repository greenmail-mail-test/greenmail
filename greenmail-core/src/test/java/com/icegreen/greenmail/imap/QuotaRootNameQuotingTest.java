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

public class QuotaRootNameQuotingTest {
    private static final String CRLF = "\r\n";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.IMAP);

    @Test
    public void getQuotaRootDoesNotInjectResponseLines() throws IOException {
        greenMail.setUser("foo@localhost", "pwd");

        String host = greenMail.getImap().getBindTo();
        int port = greenMail.getImap().getPort();
        try (Socket socket = new Socket(host, port)) {
            OutputStream os = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));

            assertThat(reader.readLine()).startsWith("* OK");

            os.write(("a1 LOGIN foo@localhost pwd" + CRLF).getBytes(StandardCharsets.US_ASCII));
            os.flush();
            String line;
            while ((line = reader.readLine()) != null && !line.startsWith("a1 ")) {
                // skip
            }
            assertThat(line).startsWith("a1 OK");

            // Mailbox name supplied as a non-synchronizing literal carrying a CRLF
            // ("INBOX\r\nX" == 8 bytes).
            os.write(("a2 GETQUOTAROOT {8+}" + CRLF).getBytes(StandardCharsets.US_ASCII));
            os.write(("INBOX" + CRLF + "X" + CRLF).getBytes(StandardCharsets.US_ASCII));
            os.flush();

            List<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null && !line.startsWith("a2 ")) {
                lines.add(line);
            }

            // The CRLF carried in the mailbox name must not split the response into
            // a forged "X" line; the name is emitted as a single quoted token.
            assertThat(lines).doesNotContain("X");
            assertThat(lines).containsExactly("* QUOTAROOT \"INBOXX\"");
        }
    }
}
