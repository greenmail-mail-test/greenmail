/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail;

import java.nio.file.Paths;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.filestore.MBoxFileStore;
import com.icegreen.greenmail.filestore.UncheckedFileStoreException;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.store.Store;
import com.icegreen.greenmail.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 27, 2006
 */
public class Managers {
    final Logger log = LoggerFactory.getLogger(Managers.class);

    private ImapHostManager imapHostManager = null;
    private UserManager userManager = null;
    private SmtpManager smtpManager = null;
    private Store storeToUse = null;

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
        log.info("Starting managers with the following startup configuration:");
        startupConfig.logConfiguration();

        //"/tmp/filestore-test/" + Long.toString(System.currentTimeMillis()

        // TODO: Use Reflection to invoke correct class:

        if ("com.icegreen.greenmail.store.InMemoryStore".equals(startupConfig.getStoreClassImplementation())) {
            this.storeToUse = new InMemoryStore();
        } else if ("com.icegreen.greenmail.filestore.MBoxFileStore".equals(startupConfig.getStoreClassImplementation())) {
            this.storeToUse = new MBoxFileStore(Paths.get(startupConfig.getFileStoreRootDirectory()));
        } else {
            throw new UncheckedFileStoreException("Cannot create the Store implementation class: '" + startupConfig
                    .getStoreClassImplementation() + "'. This class is unknown.");
        }

        this.imapHostManager = new ImapHostManagerImpl(this.storeToUse);
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

    public void stop() {
        this.storeToUse.stop();
    }
}
