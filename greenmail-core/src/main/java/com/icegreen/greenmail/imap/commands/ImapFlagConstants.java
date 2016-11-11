package com.icegreen.greenmail.imap.commands;

import javax.mail.Flags;

/**
 * Created by saladin on 11/3/16.
 */
public class ImapFlagConstants {
	public static final Flags PERMANENT_FLAGS = new Flags();

	static {
		PERMANENT_FLAGS.add(Flags.Flag.ANSWERED);
		PERMANENT_FLAGS.add(Flags.Flag.DELETED);
		PERMANENT_FLAGS.add(Flags.Flag.DRAFT);
		PERMANENT_FLAGS.add(Flags.Flag.FLAGGED);
		PERMANENT_FLAGS.add(Flags.Flag.SEEN);
	}


}
