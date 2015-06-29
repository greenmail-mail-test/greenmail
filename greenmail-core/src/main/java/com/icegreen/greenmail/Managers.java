/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail;

import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.store.Store;
import com.icegreen.greenmail.user.UserManager;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 27, 2006
 */
public class Managers {
    private ImapHostManager imapHostManager;
    private UserManager userManager;
    private SmtpManager smtpManager;

    public Managers() {
        this(new InMemoryStore());
    }

    protected Managers(final Store imapHostManagerStore) {
        imapHostManager = new ImapHostManagerImpl(imapHostManagerStore);
        userManager = new UserManager(imapHostManager);
        smtpManager = new SmtpManager(imapHostManager, userManager);
    }

    public final SmtpManager getSmtpManager() {
        return smtpManager;
    }

    public final UserManager getUserManager() {
        return userManager;
    }

    public final ImapHostManager getImapHostManager() {
        return imapHostManager;
    }

    public final void reset() {
        imapHostManager = new ImapHostManagerImpl(createNewStore());
        userManager = new UserManager(imapHostManager);
        smtpManager = new SmtpManager(imapHostManager, userManager);
    }

    protected Store createNewStore() {
        return new InMemoryStore();
    }
}
