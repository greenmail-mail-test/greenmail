package com.icegreen.greenmail.util;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.pop3.commands.Pop3CommandRegistry;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;

public class ServerBuilder {

	public SmtpServer buildSmtpServer(ServerSetup setup, Managers managers) {
		return new SmtpServer(setup, managers);
	}
	
	public SmtpServer buildSmtpServer(ServerSetup setup, Managers managers, SmtpCommandRegistry smtpCommandRegistry) {
		return new SmtpServer(setup, managers, smtpCommandRegistry);
	}
	
	public Pop3Server buildPop3Server(ServerSetup setup, Managers managers) {
		return new Pop3Server(setup, managers);
	}
	
	public Pop3Server buildPop3Server(ServerSetup setup, Managers managers, Pop3CommandRegistry pop3CommandRegistry) {
		return new Pop3Server(setup, managers, pop3CommandRegistry);
	}
	
	public ImapServer buildImapServer(ServerSetup setup, Managers managers) {
		return new ImapServer(setup, managers);
	}
	
}
