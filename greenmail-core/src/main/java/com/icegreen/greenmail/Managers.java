/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail;

import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.user.UserManager;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 27, 2006
 */
public class Managers {
    private final ImapHostManager imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
    private final UserManager userManager = new UserManager(imapHostManager);
    private final SmtpManager smtpManager = new SmtpManager(imapHostManager, userManager);

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
