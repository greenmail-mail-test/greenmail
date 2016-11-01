/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
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
    private ImapHostManager imapHostManager = null;
    private UserManager userManager = null;
    private SmtpManager smtpManager = null;

    /**
     * Public constructor wihtout Startup Configuration
     *
     * @deprecated Please use the constructor with a valid Startup Configuration
     */
    public Managers() {
        this(new GreenMailConfiguration());
    }

    /**
     * Public constructor with startupConfiguration
     *
     * @param startupConfig - the startup configuration
     */
    public Managers(GreenMailConfiguration startupConfig) {
        this.imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        this.userManager = new UserManager(imapHostManager);
        this.smtpManager = new SmtpManager(imapHostManager, userManager, startupConfig);
    }

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
