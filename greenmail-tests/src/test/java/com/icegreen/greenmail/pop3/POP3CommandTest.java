package com.icegreen.greenmail.pop3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.pop3.commands.AuthCommand;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.ServerSetupTest;

class POP3CommandTest {
    private static final String CRLF = "\r\n";

    @RegisterExtension
    public static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.POP3);

    private int port;
    private String hostAddress;

    @BeforeEach
    public void setUp() {
        hostAddress = greenMail.getPop3().getBindTo();
        port = greenMail.getPop3().getPort();
    }

    @FunctionalInterface
    public interface ConnectionHandler {
        void apply(PrintStream printStream, BufferedReader reader) throws IOException;
    }

    private void withConnection(ConnectionHandler handler) throws IOException {
        try (Socket socket = new Socket(hostAddress, port)) {
            assertThat(socket.isConnected()).isTrue();
            try (PrintStream printStream = new PrintStream(socket.getOutputStream());
                 final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                handler.apply(printStream, reader);
                // Gracefully close connection
                printStream.print("QUIT"+ CRLF);
            }
        }
    }

    @Test
    void authPlain() throws IOException {
        withConnection((printStream, reader) -> {
            // No such user
            assertThat(reader.readLine()).startsWith("+OK POP3 GreenMail Server v");
            printStream.print("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" + CRLF /* test / test / testpass */);
            assertThat(reader.readLine()).isEqualTo("-ERR Authentication failed: User <test> doesn't exist");

            try {
                greenMail.getUserManager()
                        .createUser("test@localhost", "test", "testpass");
            } catch (UserException e) {
                throw new IllegalStateException(e);
            }

            // Invalid pwd
            printStream.print("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwY" + CRLF /* test / test / <invalid> */);
            assertThat(reader.readLine()).isEqualTo("-ERR Authentication failed, expected base64 encoding : Last unit does not have enough valid bits");

            // Successful auth
            printStream.print("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" + CRLF /* test / test / <invalid> */);
            assertThat(reader.readLine()).isEqualTo("+OK");
        });
    }

    @Test
    void authPlainWithContinuation() throws IOException, UserException {
        greenMail.getUserManager()
                .createUser("test@localhost", "test", "testpass");
        withConnection((printStream, reader) -> {
            assertThat(reader.readLine()).startsWith("+OK POP3 GreenMail Server v");
            printStream.print("AUTH PLAIN" + CRLF /* test / test / testpass */);
            assertThat(reader.readLine()).isEqualTo(AuthCommand.CONTINUATION);
            printStream.print("dGVzdAB0ZXN0AHRlc3RwYXNz" + CRLF /* test / test / <invalid> */);
            assertThat(reader.readLine()).isEqualTo("+OK");
        });
    }

    @Test
    void authDisabled() throws IOException {
        greenMail.getUserManager().setAuthRequired(false);
        withConnection((printStream, reader) -> {
            assertThat(reader.readLine()).startsWith("+OK POP3 GreenMail Server v");
            printStream.print("USER blar@blar.com" + CRLF);
            assertThat(reader.readLine()).isEqualTo("+OK");
        });
    }

    @Test
    void authEnabled() throws IOException {
        greenMail.getUserManager().setAuthRequired(true);
        withConnection((printStream, reader) -> {
            assertThat(reader.readLine()).startsWith("+OK POP3 GreenMail Server v");
            printStream.print("USER blar@blar.com" + CRLF);
            assertThat(reader.readLine()).isNotEqualTo("+OK");
        });
    }

}
