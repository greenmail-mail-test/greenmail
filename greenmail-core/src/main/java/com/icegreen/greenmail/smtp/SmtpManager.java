/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.configuration.UserBean;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SmtpManager {
    protected static final Logger log = LoggerFactory.getLogger(SmtpManager.class);

    Incoming _incomingQueue;
    UserManager userManager;
    GreenMailConfiguration startupConfig;
    private ImapHostManager imapHostManager;
    List<WaitObject> notifyList;

    public SmtpManager(ImapHostManager imapHostManager, UserManager userManager, GreenMailConfiguration startupConfig) {
        this.imapHostManager = imapHostManager;
        this.userManager = userManager;
        this.startupConfig = startupConfig;
        _incomingQueue = new Incoming();
        notifyList = Collections.synchronizedList(new ArrayList<WaitObject>());
    }


    public String checkSender(SmtpState state, MailAddress sender) {
        //always ok
        return null;
    }

    public String checkRecipient(SmtpState state, MailAddress rcpt) {
        // todo?
//        MailAddress sender = state.getMessage().getReturnPath();
        return null;
    }

    public String checkData(SmtpState state) {
        return null;
    }

    public synchronized void send(SmtpState state) {
        _incomingQueue.enqueue(state.getMessage());
        for (WaitObject o : notifyList) {
            synchronized (o) {
                o.emailReceived();
            }
        }
    }

    /**
     * @return null if no need to wait. Otherwise caller must call wait() on the returned object
     */
    public synchronized WaitObject createAndAddNewWaitObject(int emailCount) {
        final int existingCount = imapHostManager.getAllMessages().size();
        if (existingCount >= emailCount) {
            return null;
        }
        WaitObject ret = new WaitObject(emailCount - existingCount);
        notifyList.add(ret);
        return ret;
    }

    //~----------------------------------------------------------------------------------------------------------------

    /**
     * This Object is used by a thread to wait until a number of emails have arrived.
     * (for example Server's waitForIncomingEmail method)
     * <p/>
     * Every time an email has arrived, the method emailReceived() must be called.
     * <p/>
     * The notify() or notifyALL() methods should not be called on this object unless
     * you want to notify waiting threads even if not all the required emails have arrived.
     */
    public static class WaitObject {
        private boolean arrived = false;
        private int emailCount;

        public WaitObject(int emailCount) {
            this.emailCount = emailCount;
        }

        public int getEmailCount() {
            return emailCount;
        }

        public boolean isArrived() {
            return arrived;
        }

        private void setArrived() {
            arrived = true;
        }

        public void emailReceived() {
            emailCount--;
            if (emailCount <= 0) {
                setArrived();
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

    private class Incoming {
        public void enqueue(MovingMessage msg) {
            for (MailAddress address : msg.getToAddresses()) {
                handle(msg, address);
            }

        }

        private void handle(MovingMessage msg, MailAddress mailAddress) {
            if (startupConfig.hasMailsinkUser()) {
                if (startupConfig.keepMailsinkInOriginalMailboxes()) {
                    // Keep the mail in the original mailbox as well
                    deliverContentToMailAddress(msg, mailAddress);
                }
                deliverContentToMailUser(msg, startupConfig.getMailsinkUser());
            }
            else {
                deliverContentToMailAddress(msg, mailAddress);
            }
            msg.releaseContent();
        }

        private void deliverContentToMailAddress(MovingMessage msg, MailAddress mailAddress) {
            try {
                GreenMailUser user = userManager.getUserByEmail(mailAddress.getEmail());
                if (null == user) {
                    String login = mailAddress.getEmail();
                    String email = mailAddress.getEmail();
                    String password = mailAddress.getEmail();
                    user = userManager.createUser(email, login, password);
                    log.info("Created user login {} for address {} with password {} because it didn't exist before.", login, email, password);
                }

                user.deliver(msg);
            } catch (Exception e) {
                log.error("Can not deliver message " + msg + " to " + mailAddress, e);
                throw new RuntimeException(e);
            }
        }

        private void deliverContentToMailUser(MovingMessage msg, UserBean mailUser) {
            try {
                GreenMailUser user = userManager.getUserByEmail(mailUser.getEmail());
                if (null == user) {
                    String login = mailUser.getLogin();
                    String email = mailUser.getEmail();
                    String password = mailUser.getPassword();
                    user = userManager.createUser(email, login, password);
                    log.info("Created user login {} for address {} with password {} because it didn't exist before.", login, email, password);
                }
                user.deliver(msg);
            } catch (Exception e) {
                log.error("Can not deliver message " + msg + " to " + mailUser, e);
                throw new RuntimeException(e);
            }
        }
    }
}