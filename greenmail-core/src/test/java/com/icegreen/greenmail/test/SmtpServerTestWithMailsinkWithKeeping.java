package com.icegreen.greenmail.test;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.configuration.PropertiesBasedGreenMailConfigurationBuilder;
import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * Created by saladin on 11/1/16.
 */
public class SmtpServerTestWithMailsinkWithKeeping {

	private static Properties mailsinkPropsWithoutKeeping = new Properties();
	static {
		mailsinkPropsWithoutKeeping.put("greenmail.mailsink.user", "mailsink:mailsink");
		mailsinkPropsWithoutKeeping.put("greenmail.mailsink.keep.in.original.mailboxes", "true");
	}

	@Rule
	public final GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(new ServerSetup[]{ServerSetupTest.SMTP,
			ServerSetupTest.SMTPS}, new PropertiesBasedGreenMailConfigurationBuilder().build(mailsinkPropsWithoutKeeping));

	@Test
	@StoreChooser(store="file,memory")
	public void testSmtpSendingtoMailsink() throws MessagingException {
		GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "subject", "body");
		MimeMessage[] emails = greenMail.getReceivedMessages();
		assertEquals(2, emails.length);
		assertEquals("subject", emails[0].getSubject());
		assertEquals("body", GreenMailUtil.getBody(emails[0]));
		assertEquals("subject", emails[1].getSubject());
		assertEquals("body", GreenMailUtil.getBody(emails[1]));
	}

}
