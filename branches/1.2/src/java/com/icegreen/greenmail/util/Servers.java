/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.icegreen.greenmail.util;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.store.SimpleStoredMessage;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 28, 2006
 * @deprecated Use GreenMail.java instead
 */
public class Servers extends GreenMail {

    public Servers() {
        super();
    }

    public Servers(ServerSetup config) {
        super(config);
    }

    public Servers(ServerSetup[] config) {
        super(config);
    }
}
