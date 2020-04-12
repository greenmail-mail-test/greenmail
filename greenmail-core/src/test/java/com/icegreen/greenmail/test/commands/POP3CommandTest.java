package com.icegreen.greenmail.test.commands;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import javax.mail.MessagingException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.pop3.commands.AuthCommand;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.ServerSetupTest;

public class POP3CommandTest {
    private static final String CRLF = "\r\n";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.POP3);

    private int port;
    private String hostAddress;

    @Before
    public void setUp() {
        hostAddress = greenMail.getPop3().getBindTo();
        port = greenMail.getPop3().getPort();
    }

    @Test
    public void authPlain() throws IOException, MessagingException, UserException {
        try (Socket socket = new Socket(hostAddress, port)) {
            assertThat(socket.isConnected(), is(equalTo(true)));
            PrintStream printStream = new PrintStream(socket.getOutputStream());
            final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // No such user
            assertThat(reader.readLine(), is(startsWith("+OK POP3 GreenMail Server v")));
            printStream.print("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" + CRLF /* test / test / testpass */);
            assertThat(reader.readLine(), is(equalTo("-ERR Authentication failed: User <test> doesn't exist")));

            greenMail.getManagers().getUserManager().createUser("test@localhost", "test", "testpass");

            // Invalid pwd
            printStream.print("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwY" + CRLF /* test / test / <invalid> */);
            assertThat(reader.readLine(), is(equalTo("-ERR Authentication failed: Invalid password")));

            // Successful auth
            printStream.print("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" + CRLF /* test / test / <invalid> */);
            assertThat(reader.readLine(), is(equalTo("+OK")));
        }
    }

    @Test
    public void authPlainWithContinuation() throws IOException, UserException {
        try (Socket socket = new Socket(hostAddress, port)) {
            assertThat(socket.isConnected(), is(equalTo(true)));
            PrintStream printStream = new PrintStream(socket.getOutputStream());
            final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            greenMail.getManagers().getUserManager().createUser("test@localhost", "test", "testpass");

            assertThat(reader.readLine(), is(startsWith("+OK POP3 GreenMail Server v")));
            printStream.print("AUTH PLAIN" + CRLF /* test / test / testpass */);
            assertThat(reader.readLine(), is(equalTo(AuthCommand.CONTINUATION)));
            printStream.print("dGVzdAB0ZXN0AHRlc3RwYXNz" + CRLF /* test / test / <invalid> */);
            assertThat(reader.readLine(), is(equalTo("+OK")));
        }
    }

    @Test
    public void authDisabled() throws IOException, UserException {
        try (Socket socket = new Socket(hostAddress, port)) {
            assertThat(socket.isConnected(), is(equalTo(true)));
            PrintStream printStream = new PrintStream(socket.getOutputStream());
            final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            greenMail.getManagers().getUserManager().setAuthRequired(false);

            assertThat(reader.readLine(), is(startsWith("+OK POP3 GreenMail Server v")));
            printStream.print("USER blar@blar.com" + CRLF);
            assertThat(reader.readLine(), is(equalTo("+OK")));
        }
    }

    @Test
    public void authEnabled() throws IOException, UserException {
        try (Socket socket = new Socket(hostAddress, port)) {
            assertThat(socket.isConnected(), is(equalTo(true)));
            PrintStream printStream = new PrintStream(socket.getOutputStream());
            final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            greenMail.getManagers().getUserManager().setAuthRequired(true);

            assertThat(reader.readLine(), is(startsWith("+OK POP3 GreenMail Server v")));
            printStream.print("USER blar@blar.com" + CRLF);
            assertThat(reader.readLine(), is(not(equalTo("+OK"))));
        }
    }

}
