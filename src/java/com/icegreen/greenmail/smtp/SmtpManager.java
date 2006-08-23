/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;


import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.imap.ImapHostManager;

import java.util.*;


public class SmtpManager {
    Incoming _incomingQueue;
    UserManager userManager;
    private ImapHostManager imapHostManager;
    List notifyList;

    public SmtpManager(ImapHostManager imapHostManager, UserManager userManager) {
        this.imapHostManager = imapHostManager;
        this.userManager = userManager;
        _incomingQueue = new Incoming();
        notifyList = new ArrayList();
    }


    public String checkSender(SmtpState state, MailAddress sender) {
        //always ok
        return null;
    }

    public String checkRecipient(SmtpState state, MailAddress rcpt) {
        MailAddress sender = state.getMessage().getReturnPath();
        return null;
    }

    public String checkData(SmtpState state) {

        return null;
    }

    public void send(SmtpState state) {
        _incomingQueue.enqueue(state.getMessage());
        for (int i = 0; i < notifyList.size(); i++) {
            WaitObject o = (WaitObject) notifyList.get(i);
            o.setArrived();
            synchronized (o) {
                o.notify();
            }
        }
    }

    /**
     * @return null if no need to wait. Otherwise caller must call wait() on the returned object
     */
    public WaitObject createAndAddNewWaitObject(int emailCount) {
        if (imapHostManager.getAllMessages().size() >= emailCount) {
            return null;
        }
        WaitObject ret = new WaitObject(emailCount);
        notifyList.add(ret);
        return ret;
    }

    //~----------------------------------------------------------------------------------------------------------------
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

        void setArrived() {
            arrived = true;
        }
    }

    private class Incoming {
        boolean _stopping;
        List _queue = Collections.synchronizedList(new LinkedList());


        public void enqueue(MovingMessage msg) {
            Iterator iterator = msg.getRecipientIterator();
            while (iterator.hasNext()) {
                MailAddress username = (MailAddress) iterator.next();

                handle(msg, username);
            }

        }

        private void handle(MovingMessage msg, MailAddress mailAddress) {
            try {
                try {
                    GreenMailUser user = userManager.getUserByEmail(mailAddress.getEmail());
                    if (null == user) {
                        user = userManager.createUser(mailAddress.getEmail(),mailAddress.getEmail(), mailAddress.getEmail());
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