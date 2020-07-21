package com.icegreen.greenmail.test.commands;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.smtp.commands.AuthCommand;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.smtp.SMTPTransport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class SMTPCommandTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    private int port;
    private String hostAddress;
    private URLName smtpURL;

    @Before
    public void setUp() {
        hostAddress = greenMail.getSmtp().getBindTo();
        port = greenMail.getSmtp().getPort();
        smtpURL = new URLName(hostAddress);
    }

    @Test
    public void mailSenderEmpty() throws IOException, MessagingException {
        Session smtpSession = greenMail.getSmtp().createSession();
        SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);

        try {
            Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
            smtpTransport.connect(smtpSocket);
            assertThat(smtpTransport.isConnected()).isTrue();
            smtpTransport.issueCommand("MAIL FROM: <>", -1);
            assertThat("250 OK").isEqualToNormalizingWhitespace(smtpTransport.getLastServerResponse());
        } finally {
            smtpTransport.close();
        }
    }

    @Test
    public void authPlain() throws IOException, MessagingException, UserException {
        {
            Session smtpSession = greenMail.getSmtp().createSession();
            SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);
            try {
                Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
                smtpTransport.connect(smtpSocket);
                assertThat(smtpTransport.isConnected()).isTrue();

                // Should fail, as user does not exist
                smtpTransport.issueCommand("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_CREDENTIALS_INVALID);

                // Try again but create user
                greenMail.getManagers().getUserManager().createUser("test@localhost", "test", "testpass");
                smtpTransport.issueCommand("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_SUCCEDED);
            } finally {
                smtpTransport.close();
            }
        }

        // With continuation
        {
            Session smtpSession = greenMail.getSmtp().createSession();
            SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);
            try {
                Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
                smtpTransport.connect(smtpSocket);
                assertThat(smtpTransport.isConnected()).isTrue();

                smtpTransport.issueCommand("AUTH PLAIN", -1);
                assertThat(smtpTransport.getLastServerResponse().startsWith(AuthCommand.SMTP_SERVER_CONTINUATION)).isTrue();
                smtpTransport.issueCommand("dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_SUCCEDED);
            } finally {
                smtpTransport.close();
            }
        }
    }

    @Test
    public void authLogin() throws IOException, MessagingException, UserException {
        Session smtpSession = greenMail.getSmtp().createSession();
        SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);
        try {
            Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
            smtpTransport.connect(smtpSocket);
            assertThat(smtpTransport.isConnected()).isTrue();;

            // Should fail, as user does not exist
            smtpTransport.issueCommand("AUTH LOGIN ", 334);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("334 VXNlciBOYW1lAA==" /* Username */);
            smtpTransport.issueCommand(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.US_ASCII)), -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("334 UGFzc3dvcmQA" /* Password */);
            smtpTransport.issueCommand(Base64.getEncoder().encodeToString("testpass".getBytes(StandardCharsets.US_ASCII)), -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_CREDENTIALS_INVALID);

            // Try again but create user
            greenMail.getManagers().getUserManager().createUser("test@localhost", "test", "testpass");
            smtpTransport.issueCommand("AUTH LOGIN ", 334);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("334 VXNlciBOYW1lAA==" /* Username */);
            smtpTransport.issueCommand(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.US_ASCII)), -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("334 UGFzc3dvcmQA" /* Password */);
            smtpTransport.issueCommand(Base64.getEncoder().encodeToString("testpass".getBytes(StandardCharsets.US_ASCII)), -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_SUCCEDED);
        } finally {
            smtpTransport.close();
        }
    }

    @Test
    public void mailSenderAUTHSuffix() throws IOException, MessagingException {
        Session smtpSession = greenMail.getSmtp().createSession();
        SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);

        try {
            Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
            smtpTransport.connect(smtpSocket);
            assertThat(smtpTransport.isConnected()).isTrue();
            smtpTransport.issueCommand("MAIL FROM: <test.test@test.net> AUTH <>", -1);
            assertThat("250 OK").isEqualToNormalizingWhitespace(smtpTransport.getLastServerResponse());
        } finally {
            smtpTransport.close();
        }
    }

}
