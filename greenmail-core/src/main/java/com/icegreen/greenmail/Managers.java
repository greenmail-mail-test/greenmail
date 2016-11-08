/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.filestore.UncheckedFileStoreException;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapHostManagerImpl;
import com.icegreen.greenmail.smtp.SmtpManager;
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

        // Depending on the startup confifguration, construct the correct Store class using Reflection:
        this.constructStore(startupConfig);

        this.imapHostManager = new ImapHostManagerImpl(this.storeToUse);
        this.userManager = new UserManager(imapHostManager);
        this.smtpManager = new SmtpManager(imapHostManager, userManager, startupConfig);
    }

    /**
     * Creates the store class (InMemory or File-based), depending on the Startup Properties.
     *
     * @param startupConfig
     */
    private void constructStore(GreenMailConfiguration startupConfig) {
        Class<?> storeClass = null;
        try {
            storeClass = Class.forName(startupConfig.getStoreClassImplementation());
            Constructor[] ctors = storeClass.getDeclaredConstructors();
            Constructor chosenConstructor = null;
            for (Constructor cstr : ctors) {
                if (cstr.getGenericParameterTypes().length == 1) {
                    Type firstParamType = cstr.getGenericParameterTypes()[0];
                    if (firstParamType.equals(startupConfig.getClass())) {
                        chosenConstructor = cstr;
                        break;
                    }
                }
            }
            if (chosenConstructor == null) {
                throw new UncheckedFileStoreException("Cannot find correct constructor of class '" + startupConfig
                        .getStoreClassImplementation() + "'. The constructor must have one parameter of type "
                        + "GreenMailConfiguration.");
            }

            this.storeToUse = (Store)chosenConstructor.newInstance(startupConfig);
        }
        catch (ClassNotFoundException e) {
            throw new UncheckedFileStoreException("ClassNotFoundException while trying to create instance of store class '" +
                    startupConfig.getStoreClassImplementation() + "'.", e);
        }
        catch (InstantiationException e) {
            throw new UncheckedFileStoreException("InstantiationException while trying to create instance of store class '" +
                    startupConfig.getStoreClassImplementation() + "'.", e);
        }
        catch (IllegalAccessException e) {
            throw new UncheckedFileStoreException("IllegalAccessException while trying to create instance of store class '" +
                    startupConfig.getStoreClassImplementation() + "'.", e);
        }
        catch (InvocationTargetException e) {
            throw new UncheckedFileStoreException("InvocationTargetException while trying to create instance of store class '" +
                    startupConfig.getStoreClassImplementation() + "'.", e);
        }
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
