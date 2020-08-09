package com.icegreen.greenmail.test.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.pop3.commands.AuthCommand;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class POP3CommandTest {
    private static final String CRLF = "\r\n";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.POP3){
        @Override
        public void start() {
            super.start();
            System.out.println("Started");
        }
    };

    private int port;
    private String hostAddress;

    @Before
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
            }
        }
    }

    @Test
    public void authPlain() throws IOException {
        withConnection((printStream, reader) -> {
            // No such user
            assertThat(reader.readLine()).startsWith("+OK POP3 GreenMail Server v");
            printStream.print("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" + CRLF /* test / test / testpass */);
            assertThat(reader.readLine()).isEqualTo("-ERR Authentication failed: User <test> doesn't exist");

            try {
                greenMail.getManagers().getUserManager()
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
    public void authPlainWithContinuation() throws IOException, UserException {
        greenMail.getManagers().getUserManager()
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
    public void authDisabled() throws IOException {
        greenMail.getManagers().getUserManager().setAuthRequired(false);
        withConnection((printStream, reader) -> {
            assertThat(reader.readLine()).startsWith("+OK POP3 GreenMail Server v");
            printStream.print("USER blar@blar.com" + CRLF);
            assertThat(reader.readLine()).isEqualTo("+OK");
        });
    }

    @Test
    public void authEnabled() throws IOException {
        greenMail.getManagers().getUserManager().setAuthRequired(true);
        withConnection((printStream, reader) -> {
            assertThat(reader.readLine()).startsWith("+OK POP3 GreenMail Server v");
            printStream.print("USER blar@blar.com" + CRLF);
            assertThat(reader.readLine()).isNotEqualTo("+OK");
        });
    }

}
