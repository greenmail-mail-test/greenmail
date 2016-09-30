package com.icegreen.greenmail.smtp.commands;

import java.io.IOException;

import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;

public class StartTlsCommand extends SmtpCommand {

	private static final String STARTTLS = "STARTTLS";
	private static final String _501_SYNTAX_ERROR_NO_PARAMETERS_ALLOWED = "501 Syntax error (no parameters allowed)";
	private static final String _220_READY_TO_START_TLS = "220 Ready to start TLS";

	@Override
	public void execute(SmtpConnection conn, SmtpState state, SmtpManager manager, String commandLine)
			throws IOException {
        //Check for any parameters/extra illegal characters sent
        if (!commandLine.trim().toUpperCase().equals(STARTTLS)) {
            conn.send(_501_SYNTAX_ERROR_NO_PARAMETERS_ALLOWED);
        } else {
            conn.send(_220_READY_TO_START_TLS);
        }

	}

}
