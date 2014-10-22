/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;


import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;


public class SmtpManager {
    protected final static Logger log = LoggerFactory.getLogger(SmtpManager.class);

    Incoming _incomingQueue;
    UserManager userManager;
    private ImapHostManager imapHostManager;
    Vector<WaitObject> notifyList;

    public SmtpManager(ImapHostManager imapHostManager, UserManager userManager) {
        this.imapHostManager = imapHostManager;
        this.userManager = userManager;
        _incomingQueue = new Incoming();
        notifyList = new Vector<WaitObject>();
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
                this.notifyAll();
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
            try {
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

                } catch (UserException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            msg.releaseContent();
        }
    }
}