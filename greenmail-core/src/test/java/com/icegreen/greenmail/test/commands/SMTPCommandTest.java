package com.icegreen.greenmail.test.commands;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.Rule;
import org.junit.Test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;

public class SMTPCommandTest {
	
	@Rule
	public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.ALL);

	@Test
	public void MailSenderEmpty() throws UnknownHostException, IOException {
		Socket smtpSocket = null;
		DataOutputStream dos = null; 
		BufferedReader br = null;
		String hostAddress = greenMail.getSmtp().getBindTo();
		int port = greenMail.getSmtp().getPort();
		
		try {
			smtpSocket = new Socket(hostAddress,port);
			dos = new DataOutputStream(smtpSocket.getOutputStream());
			br = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
		} finally {
			
		}
		
		assertEquals("220 /" + hostAddress + " GreenMail SMTP Service Ready at port " + port,br.readLine());
		dos.writeBytes("HELO " + hostAddress);
		dos.writeBytes("\n");
		assertEquals("250 /" + hostAddress,br.readLine());
		dos.writeBytes("MAIL FROM: <>");
		dos.writeBytes("\n");
		assertEquals("250 OK",br.readLine());
		dos.flush();
		br.close();
		dos.close();

		smtpSocket.close();
		
	}

}
