/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 *
 */
package com.icegreen.greenmail;

import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.store.InMemoryStore;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 27, 2006
 */
public class Managers {
    private ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
    private UserManager userManager = new UserManager(imapHostManager);
    private SmtpManager smtpManager = new SmtpManager(imapHostManager, userManager);

    public SmtpManager getSmtpManager() {
        return smtpManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public ImapHostManager getImapHostManager() {
        return imapHostManager;
    }
}
