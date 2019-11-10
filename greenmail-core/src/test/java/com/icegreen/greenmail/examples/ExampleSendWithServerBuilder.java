package com.icegreen.greenmail.examples;

import static org.junit.Assert.assertEquals;

import javax.mail.MessagingException;

import org.junit.Test;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.smtp.SmtpState;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerBuilder;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

public class ExampleSendWithServerBuilder {
    @Test
    public void testSend() throws MessagingException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP, new Managers(), new ServerBuilder()); //uses test ports by default
        try {
            greenMail.start();
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "some subject",
                    "some body"); //replace this with your test message content
            assertEquals("some body", GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]));
        } finally {
            greenMail.stop();
        }
    }

    @Test
    public void testSendWithSetupBuilder() throws MessagingException {
    	ServerSetup[] setup = new ServerSetup.Builder()
    			.withPortOffset(3000)
    			.withBindAddress(ServerSetup.getLocalHostAddress())
    			.build(ServerSetup.SMTP_IMAP);
    	assertEquals(2, setup.length);
    	assertEquals(3000+ServerSetup.PORT_SMTP, setup[0].getPort());
    	
        GreenMail greenMail = new GreenMail(setup, new Managers(), new ServerBuilder()); //uses test ports by default
        try {
            greenMail.start();
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "some subject",
                    "some body"); //replace this with your test message content
            assertEquals("some body", GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]));
        } finally {
            greenMail.stop();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testSendWithSetupBuilderBindFailed() throws MessagingException {
    	final String InvalidBindAddress = "255.255.255.255";
		ServerSetup[] setup = new ServerSetup.Builder()
    			.withPortOffset(3000)
    			.withBindAddress(InvalidBindAddress)
    			.build(ServerSetup.SMTP_IMAP);
    	assertEquals(2, setup.length);
    	assertEquals(3000+ServerSetup.PORT_SMTP, setup[0].getPort());
    	assertEquals(InvalidBindAddress, setup[1].getBindAddress());
    	
        GreenMail greenMail = new GreenMail(setup, new Managers(), new ServerBuilder()); //uses test ports by default
        try {
            greenMail.start();
        } finally {
            greenMail.stop();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testSendAuthenticationFails() throws MessagingException {
    	ImapHostManager ihm = new ImapHostManagerImpl();
    	UserManager um = new FailingUserManager(ihm);
        Managers managers = new Managers(ihm, um, new FailingSmtpManager(ihm, um));
		GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP_IMAP, managers, new ServerBuilder()); //uses test ports by default
        try {
            greenMail.start();
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "some subject",
                    "some body"); //replace this with your test message content
        } finally {
            greenMail.stop();
        }
    }

    class FailingUserManager extends UserManager {

		public FailingUserManager(ImapHostManager imapHostManager) {
			super(imapHostManager);
		}

		@Override
		protected boolean checkPassword(String expectedPassword, String password) {
			return false;
		}
    }
    
    class FailingSmtpManager extends SmtpManager {

		public FailingSmtpManager(ImapHostManager imapHostManager, UserManager userManager) {
			super(imapHostManager, userManager);
		}
		
		@Override
		public String checkSender(SmtpState state, MailAddress sender) {
			return "InvalidSender: " + sender.getEmail();
		}
    	
		@Override
		public String checkRecipient(SmtpState state, MailAddress rcpt) {
			return "Invalid Recipient: " + rcpt.getEmail();
		}
		
		@Override
		public String checkData(SmtpState state) {
			try {
				return "Invalid Data: " + state.getMessage().getMessage().getContentType();
			} catch (MessagingException e) {
				return "Cannot handle Data: " + e;
			}
		}
    }
    
    class MyServerBuilder extends ServerBuilder {
    	@Override
    	public SmtpServer buildSmtpServer(ServerSetup setup, Managers managers) {
    		return super.buildSmtpServer(setup, managers);
    	}
    }
}
