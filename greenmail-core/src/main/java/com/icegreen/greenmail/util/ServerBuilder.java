package com.icegreen.greenmail.util;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.smtp.SmtpServer;

public class ServerBuilder {

	public SmtpServer buildSmtpServer(ServerSetup setup, Managers managers) {
		return new SmtpServer(setup, managers);
	}
	
	public Pop3Server buildPop3Server(ServerSetup setup, Managers managers) {
		return new Pop3Server(setup, managers);
	}
	
	public ImapServer buildImapServer(ServerSetup setup, Managers managers) {
		return new ImapServer(setup, managers);
	}
	
}
