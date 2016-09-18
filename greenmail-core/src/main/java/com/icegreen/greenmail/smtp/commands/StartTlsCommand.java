package com.icegreen.greenmail.smtp.commands;

import java.io.IOException;

import com.icegreen.greenmail.smtp.SmtpConnection;
//import com.icegreen.greenmail.smtp.SmtpHandler;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;

public class StartTlsCommand extends SmtpCommand {

	@Override
	public void execute(SmtpConnection conn, SmtpState state, SmtpManager manager, String commandLine)
			throws IOException {
        //Check for any parameters/extra illegal characters sent
        if (!commandLine.toUpperCase().equals("STARTTLS")) {
            conn.send("501 Syntax error (no parameters allowed)");
        } else {
            conn.send("220 Ready to start TLS");
        }

	}

}
