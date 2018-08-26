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
import java.util.concurrent.CountDownLatch;

import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SmtpManager {
    protected static final Logger log = LoggerFactory.getLogger(SmtpManager.class);

    Incoming incomingQueue;
    UserManager userManager;
    private ImapHostManager imapHostManager;
    List<CountDownLatch> notifyList;

    public SmtpManager(ImapHostManager imapHostManager, UserManager userManager) {
        this.imapHostManager = imapHostManager;
        this.userManager = userManager;
        incomingQueue = new Incoming();
        notifyList = Collections.synchronizedList(new ArrayList<CountDownLatch>());
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
        incomingQueue.enqueue(state.getMessage());
        for (CountDownLatch o : notifyList) {
            o.countDown();
        }
    }

    /**
     * @return null if no need to wait. Otherwise caller must call wait() on the returned object
     */
    public synchronized CountDownLatch createAndAddNewWaitObject(int emailCount) {
        final int existingCount = imapHostManager.getAllMessages().size();
        if (existingCount >= emailCount) {
            return new CountDownLatch(0); // Requires no count down, therefore not added to notification list
        }
        CountDownLatch latch = new CountDownLatch(emailCount - existingCount);
        notifyList.add(latch);
        return latch;
    }

    //~----------------------------------------------------------------------------------------------------------------


    private class Incoming {
        public void enqueue(MovingMessage msg) {
            for (MailAddress address : msg.getToAddresses()) {
                handle(msg, address);
            }

        }

        private void handle(MovingMessage msg, MailAddress mailAddress) {
            try {
                GreenMailUser user = userManager.getUserByEmail(mailAddress.getEmail());
                if (null == user) {
                    String login = mailAddress.getEmail();
                    String email = mailAddress.getEmail();
                    String password = mailAddress.getEmail();
                    user = userManager.createUser(email, login, password);
                    log.info("Created user login {} for address {} with password {} because it didn't exist before.",
                            login, email, password);
                }

                user.deliver(msg);
            } catch (Exception e) {
                throw new IllegalStateException("Can not deliver message " + msg + " to " + mailAddress, e);
            }

            msg.releaseContent();
        }
    }

    public UserManager getUserManager() {
        return userManager;
    }
}