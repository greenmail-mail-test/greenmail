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
    private final ImapHostManager imapHostManager;
    private final UserManager userManager;
    private final SmtpManager smtpManager;

    public Managers() {
        imapHostManager = new ImapHostManagerImpl(new InMemoryStore());
        userManager = new UserManager(imapHostManager);
        smtpManager = new SmtpManager(imapHostManager, userManager);
	}
    
    public Managers(ImapHostManager imapHostManager) {
        this.imapHostManager = imapHostManager;
        userManager = new UserManager(imapHostManager);
        smtpManager = new SmtpManager(imapHostManager, userManager);
	}
    
    public Managers(ImapHostManager imapHostManager, UserManager userManager) {
        this.imapHostManager = imapHostManager;
        this.userManager = userManager;
        smtpManager = new SmtpManager(imapHostManager, userManager);
	}
    
    public Managers(ImapHostManager imapHostManager, UserManager userManager, SmtpManager smtpManager) {
		this.imapHostManager = imapHostManager;
		this.userManager = userManager;
		this.smtpManager = smtpManager;
	}
    
    public static Managers build(ImapHostManager imapHostManager) {
    	UserManager userManager = new UserManager(imapHostManager);
		return new Managers(imapHostManager, userManager, new SmtpManager(imapHostManager, userManager));
    }

    public static Managers build(ImapHostManager imapHostManager, UserManager userManager) {
		return new Managers(imapHostManager, userManager, new SmtpManager(imapHostManager, userManager));
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
