/**
 * 
 */
package com.icegreen.greenmail.smtp.commands;

import java.io.IOException;

import com.icegreen.greenmail.smtp.SmtpConnection;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpState;

/**
 * @author pdcoxhead
 *
 */
public class SSLEhloCommand extends SmtpCommand {

	private static final String TEA_ANYONEQM = " offers a sturdy cup of tea and a scone with TLS jam";

	/* (non-Javadoc)
	 * @see com.icegreen.greenmail.smtp.commands.SmtpCommand#execute(com.icegreen.greenmail.smtp.SmtpConnection, com.icegreen.greenmail.smtp.SmtpState, com.icegreen.greenmail.smtp.SmtpManager, java.lang.String)
	 */
	@Override
	public void execute(SmtpConnection conn, SmtpState state, SmtpManager manager, String commandLine)
			throws IOException {
		conn.send(RESPONSE_PREFIX_NO_MORE_COMING + conn.getServerGreetingsName() + TEA_ANYONEQM);

	}

}
