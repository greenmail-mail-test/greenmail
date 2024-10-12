package com.icegreen.greenmail.smtp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.eclipse.angus.mail.smtp.SMTPTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.smtp.commands.AuthCommand;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.URLName;

class SMTPCommandTest {

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private int port;
    private String hostAddress;
    private URLName smtpURL;

    @BeforeEach
    void setUp() {
        hostAddress = greenMail.getSmtp().getBindTo();
        port = greenMail.getSmtp().getPort();
        smtpURL = new URLName(hostAddress);
    }

    @Test
    void mailSenderEmpty() throws IOException, MessagingException {
        Session smtpSession = greenMail.getSmtp().createSession();

        try (SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL)) {
            Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
            smtpTransport.connect(smtpSocket);
            assertThat(smtpTransport.isConnected()).isTrue();
            smtpTransport.issueCommand("MAIL FROM: <>", -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("250 OK");
        }
    }

    @Test
    void authPlain() throws IOException, MessagingException, UserException {
        {
            Session smtpSession = greenMail.getSmtp().createSession();
            try (SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL)) {
                Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
                smtpTransport.connect(smtpSocket);
                assertThat(smtpTransport.isConnected()).isTrue();

                // Should fail, as user does not exist
                smtpTransport.issueCommand("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_CREDENTIALS_INVALID);

                // Try again but create user
                greenMail.getUserManager().createUser("test@localhost", "test", "testpass");
                smtpTransport.issueCommand("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_SUCCEDED);
            }
        }

        // With continuation
        {
            Session smtpSession = greenMail.getSmtp().createSession();
            try (SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL)) {
                Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
                smtpTransport.connect(smtpSocket);
                assertThat(smtpTransport.isConnected()).isTrue();

                smtpTransport.issueCommand("AUTH PLAIN", -1);
                assertThat(smtpTransport.getLastServerResponse()).startsWith(AuthCommand.SMTP_SERVER_CONTINUATION);
                smtpTransport.issueCommand("dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_SUCCEDED);
            }
        }
    }

    @Test
    void authLogin() throws IOException, MessagingException, UserException {
        Session smtpSession = greenMail.getSmtp().createSession();
        try (SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL)) {
            Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
            smtpTransport.connect(smtpSocket);
            assertThat(smtpTransport.isConnected()).isTrue();

            // Should fail, as user does not exist
            smtpTransport.issueCommand("AUTH LOGIN ", 334);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("334 VXNlcm5hbWU6" /* Username: */);
            smtpTransport.issueCommand(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.US_ASCII)), -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("334 UGFzc3dvcmQ6" /* Password: */);
            smtpTransport.issueCommand(Base64.getEncoder().encodeToString("testpass".getBytes(StandardCharsets.US_ASCII)), -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_CREDENTIALS_INVALID);

            // Try again but create user
            greenMail.getUserManager().createUser("test@localhost", "test", "testpass");
            smtpTransport.issueCommand("AUTH LOGIN ", 334);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("334 VXNlcm5hbWU6" /* Username: */);
            smtpTransport.issueCommand(Base64.getEncoder().encodeToString("test".getBytes(StandardCharsets.US_ASCII)), -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("334 UGFzc3dvcmQ6" /* Password: */);
            smtpTransport.issueCommand(Base64.getEncoder().encodeToString("testpass".getBytes(StandardCharsets.US_ASCII)), -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace(AuthCommand.AUTH_SUCCEDED);
        }
    }

    @Test
    void mailSenderAUTHSuffix() throws IOException, MessagingException {
        Session smtpSession = greenMail.getSmtp().createSession();

        try (SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL)) {
            Socket smtpSocket = new Socket(hostAddress, port); // Closed by transport
            smtpTransport.connect(smtpSocket);
            assertThat(smtpTransport.isConnected()).isTrue();
            smtpTransport.issueCommand("MAIL FROM: <test.test@test.net> AUTH <>", -1);
            assertThat(smtpTransport.getLastServerResponse()).isEqualToNormalizingWhitespace("250 OK");
        }
    }

}
