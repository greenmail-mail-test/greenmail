package com.icegreen.greenmail.test.commands;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

import java.io.IOException;
import java.net.Socket;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.smtp.SMTPTransport;
import org.junit.Rule;
import org.junit.Test;

public class SMTPCommandTest {

	@Rule
	public final GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(ServerSetupTest.SMTP);

	@Test
	@StoreChooser(store="file,memory")
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
			assertThat(smtpTransport.isConnected(),is(equalTo(true)));
			smtpTransport.issueCommand("MAIL FROM: <>", -1);
			assertThat("250 OK", equalToIgnoringWhiteSpace(smtpTransport.getLastServerResponse()));
		} finally {
			smtpTransport.close();
		}
	}
}
