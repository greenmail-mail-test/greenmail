package com.icegreen.greenmail.test.commands;

import java.io.IOException;
import java.net.Socket;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.smtp.commands.AuthCommand;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.smtp.SMTPTransport;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.junit.Assert.assertTrue;

public class SMTPCommandTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void mailSenderEmpty() throws IOException, MessagingException {
        Socket smtpSocket;
        String hostAddress = greenMail.getSmtp().getBindTo();
        int port = greenMail.getSmtp().getPort();

        Session smtpSession = greenMail.getSmtp().createSession();
        URLName smtpURL = new URLName(hostAddress);
        SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);

        try {
            smtpSocket = new Socket(hostAddress, port);
            smtpTransport.connect(smtpSocket);
            assertThat(smtpTransport.isConnected(), is(equalTo(true)));
            smtpTransport.issueCommand("MAIL FROM: <>", -1);
            assertThat("250 OK", equalToCompressingWhiteSpace(smtpTransport.getLastServerResponse()));
        } finally {
            smtpTransport.close();
        }
    }

    @Test
    public void authPlain() throws IOException, MessagingException, UserException {
        Socket smtpSocket;
        String hostAddress = greenMail.getSmtp().getBindTo();
        int port = greenMail.getSmtp().getPort();

        URLName smtpURL = new URLName(hostAddress);
        {
            Session smtpSession = greenMail.getSmtp().createSession();
            SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);
            try {
                smtpSocket = new Socket(hostAddress, port);
                smtpTransport.connect(smtpSocket);
                assertThat(smtpTransport.isConnected(), is(equalTo(true)));

                // Should fail, as user does not exist
                smtpTransport.issueCommand("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse(), equalToCompressingWhiteSpace(AuthCommand.AUTH_CREDENTIALS_INVALID));

                // Try again but create user
                greenMail.getManagers().getUserManager().createUser("test@localhost", "test", "testpass");
                smtpTransport.issueCommand("AUTH PLAIN dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse(), equalToCompressingWhiteSpace(AuthCommand.AUTH_SUCCEDED));
            } finally {
                smtpTransport.close();
            }
        }

        // With continuation
        {
            Session smtpSession = greenMail.getSmtp().createSession();
            SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);
            try {
                smtpSocket = new Socket(hostAddress, port);
                smtpTransport.connect(smtpSocket);
                assertThat(smtpTransport.isConnected(), is(equalTo(true)));

                smtpTransport.issueCommand("AUTH PLAIN", -1);
                assertTrue(smtpTransport.getLastServerResponse().startsWith(AuthCommand.SMTP_SERVER_CONTINUATION));
                smtpTransport.issueCommand("dGVzdAB0ZXN0AHRlc3RwYXNz" /* test / test / testpass */, -1);
                assertThat(smtpTransport.getLastServerResponse(), equalToCompressingWhiteSpace(AuthCommand.AUTH_SUCCEDED));
            } finally {
                smtpTransport.close();
            }
        }
    }

    @Test
    public void mailSenderAUTHSuffix() throws IOException, MessagingException {
        Socket smtpSocket;
        String hostAddress = greenMail.getSmtp().getBindTo();
        int port = greenMail.getSmtp().getPort();

        Session smtpSession = greenMail.getSmtp().createSession();
        URLName smtpURL = new URLName(hostAddress);
        SMTPTransport smtpTransport = new SMTPTransport(smtpSession, smtpURL);

        try {
            smtpSocket = new Socket(hostAddress, port);
            smtpTransport.connect(smtpSocket);
            assertThat(smtpTransport.isConnected(), is(equalTo(true)));
            smtpTransport.issueCommand("MAIL FROM: <test.test@test.net> AUTH <>", -1);
            assertThat("250 OK", equalToCompressingWhiteSpace(smtpTransport.getLastServerResponse()));
        } finally {
            smtpTransport.close();
        }
    }

}
