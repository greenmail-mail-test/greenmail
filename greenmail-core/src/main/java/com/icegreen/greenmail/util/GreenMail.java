/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Utility class that manages a greenmail server with support for multiple protocols
 */
public class GreenMail implements GreenMailOperations {
    Managers managers;
    HashMap<String, Service> services;

    /**
     * Creates a SMTP, SMTPS, POP3, POP3S, IMAP, and IMAPS server binding onto non-default ports.
     * The ports numbers are defined in {@link ServerSetupTest}
     */
    public GreenMail() {
        this(ServerSetupTest.ALL);
    }

    /**
     * Call this constructor if you want to run one of the email servers only
     * @param config
     */
    public GreenMail(ServerSetup config) {
        this(new ServerSetup[]{config});
    }

    /**
     * Call this constructor if you want to run more than one of the email servers
     * @param config
     */
    public GreenMail(ServerSetup[] config) {
        managers = new Managers();
        services = new HashMap<String, Service>();
        for (ServerSetup setup : config) {
            if (services.containsKey(setup.getProtocol())) {
                throw new IllegalArgumentException("Server '" + setup.getProtocol() + "' was found at least twice in the array");
            }
            final String protocol = setup.getProtocol();
            if (protocol.startsWith(ServerSetup.PROTOCOL_SMTP)) {
                services.put(protocol, new SmtpServer(setup, managers));
            } else if (protocol.startsWith(ServerSetup.PROTOCOL_POP3)) {
                services.put(protocol, new Pop3Server(setup, managers));
            } else if (protocol.startsWith(ServerSetup.PROTOCOL_IMAP)) {
                services.put(protocol, new ImapServer(setup, managers));
            }
        }
    }


    public synchronized void start() {
        for (Service service : services.values()) {
            service.startService(null);
        }
        //quick hack
        boolean allup = false;
        for (int i=0;i<200 && !allup;i++) {
            allup = true;
            for (Service service : services.values()) {
                allup = allup && service.isRunning();
            }
            if (!allup) {
                try {
                    wait(5);
                } catch (InterruptedException e) {
                    // We don't care
                }
            }
        }
        if (!allup) {
            throw new RuntimeException("Couldnt start at least one of the mail services.");
        }
    }

    public synchronized void stop() {
        for (Service service : services.values()) {
            service.stopService(null);
        }
    }

    @Override
    public SmtpServer getSmtp() {
        return (SmtpServer) services.get(ServerSetup.PROTOCOL_SMTP);
    }

    @Override
    public ImapServer getImap() {
        return (ImapServer) services.get(ServerSetup.PROTOCOL_IMAP);

    }

    @Override
    public Pop3Server getPop3() {
        return (Pop3Server) services.get(ServerSetup.PROTOCOL_POP3);
    }

    @Override
    public SmtpServer getSmtps() {
        return (SmtpServer) services.get(ServerSetup.PROTOCOL_SMTPS);
    }

    @Override
    public ImapServer getImaps() {
        return (ImapServer) services.get(ServerSetup.PROTOCOL_IMAPS);

    }

    @Override
    public Pop3Server getPop3s() {
        return (Pop3Server) services.get(ServerSetup.PROTOCOL_POP3S);
    }

    public Managers getManagers() {
        return managers;
    }

    //~ Convenience Methods, often needed while testing ---------------------------------------------------------------
    @Override
    public boolean waitForIncomingEmail(long timeout, int emailCount) {
        final SmtpManager.WaitObject o = managers.getSmtpManager().createAndAddNewWaitObject(emailCount);
        if (null == o) {
            return true;
        }

        synchronized (o) {
            long t0 = System.currentTimeMillis();
            while (!o.isArrived()) {
                //this loop is necessary to insure correctness, see documentation on Object.wait()
                try {
                    o.wait(timeout);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread was interrupted while waiting", e);
                }
                if ((System.currentTimeMillis() - t0) > timeout) {
                    return false;
                }

            }
        }
        return true;
    }

    @Override
    public boolean waitForIncomingEmail(int emailCount) {
        return waitForIncomingEmail(5000l,emailCount);
    }

    @Override
    public MimeMessage[] getReceivedMessages() {
        List msgs = managers.getImapHostManager().getAllMessages();
        MimeMessage[] ret = new MimeMessage[msgs.size()];
        for (int i = 0; i < msgs.size(); i++) {
            StoredMessage storedMessage = (StoredMessage) msgs.get(i);
            ret[i] = storedMessage.getMimeMessage();
        }
        return ret;
    }

    @Override
    public MimeMessage[] getReceviedMessagesForDomain(String domain) {
        List<StoredMessage> msgs = managers.getImapHostManager().getAllMessages();
        List<MimeMessage> ret = new ArrayList<MimeMessage>();
        try {
            for (StoredMessage msg : msgs) {
                String tos = GreenMailUtil.getAddressList(msg.getMimeMessage().getAllRecipients());
                if (tos.toLowerCase().contains(domain)) {
                    ret.add(msg.getMimeMessage());
                }
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return ret.toArray(new MimeMessage[ret.size()]);
    }

    @Override
    public GreenMailUser setUser(String login, String password) {
        return setUser(login, login, password);
    }

    @Override
    public GreenMailUser setUser(String email, String login, String password) {
        GreenMailUser user = managers.getUserManager().getUser(email);
        if (null == user) {
            try {
                user = managers.getUserManager().createUser(email, login, password);
            } catch (UserException e) {
                throw new RuntimeException(e);
            }
        } else {
            user.setPassword(password);
        }
        return user;
    }

    @Override
    public void setQuotaSupported(boolean isEnabled) {
        managers.getImapHostManager().getStore().setQuotaSupported(isEnabled);
    }

    @Override
    public void setUsers(Properties users) {
        for (Object o : users.keySet()) {
            String email = (String) o;
            String password = users.getProperty(email);
            setUser(email, email, password);
        }
    }

    public GreenMailUtil util() {
        return GreenMailUtil.instance();
    }
}
