/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.configuration.ConfiguredGreenMail;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.smtp.SmtpManager;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.Store;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that manages a greenmail server with support for multiple protocols
 */
public class GreenMail extends ConfiguredGreenMail {
    final Logger log = LoggerFactory.getLogger(GreenMail.class);
    private Managers managers;
    private Map<String, AbstractServer> services;
    private ServerSetup[] config;

    /**
     * Creates a SMTP, SMTPS, POP3, POP3S, IMAP, and IMAPS server binding onto non-default ports.
     * The ports numbers are defined in {@link ServerSetupTest}
     */
    public GreenMail() {
        this(ServerSetupTest.ALL);
    }

    /**
     * Call this constructor if you want to run one of the email servers only
     *
     * @param config  Server setup to use
     */
    public GreenMail(ServerSetup config) {
        this(new ServerSetup[]{config});
    }

    /**
     * Call this constructor if you want to run more than one of the email servers
     *
     * @param config Server setup to use
     */
    public GreenMail(ServerSetup[] config) {
        this.config = config;
        // 2016-11-01 msaladin@ringworld.ch: Removed call to init() from here, because it is done in start as well, and only in
        // start we can be sure that the getStartupConfig() is up and ready, because the GreenMail user might change the config
        // between the call to the constructor and the start-call, e.g. like this:
        //   GreenMail greenMail = new GreenMail(ServerSetupTest.ALL);
        //   greenMail.withConfiguration(new PropertiesBasedGreenMailConfigurationBuilder().build(props));
        //   greenMail.start();
        // So the problem is that when init() is invoked in the constructor, the configuration is not yet ready and therefore
        // cannot be propagated easily to the managers.
    }

    /**
     * Initialize
     */
    private void init() {
        if (managers == null) {
            managers = new Managers(this.getStartupConfig());
        }
        if(services == null) {
            services = createServices(config, managers);
        }
    }

    @Override
    public synchronized void start() {
        init();

        final Collection<AbstractServer> servers = services.values();
        for (AbstractServer service : servers) {
            service.startService();
        }

        // Wait till all services are up and running
        for (AbstractServer service : servers) {
            try {
                service.waitTillRunning(service.getServerSetup().getServerStartupTimeout());
            } catch (InterruptedException ex) {
                throw new IllegalStateException("Could not start mail service " + service, ex);
            }

        }

        if (log.isDebugEnabled()) {
            log.debug("Started services, performing check if all up");
        }
        // Make sure if all services are up in a second loop, giving slow services more time.
        for (AbstractServer service : servers) {
            if (!service.isRunning()) {
                throw new IllegalStateException("Could not start mail server " + service
                        + ", try to set server startup timeout > " + service.getServerSetup().getServerStartupTimeout()
                        + " via " + ServerSetup.class.getSimpleName() + ".setServerStartupTimeout(timeoutInMs)");
            }
        }

        doConfigure();
    }

    @Override
    public synchronized void stop() {
        if (log.isDebugEnabled()) {
            log.debug("Stopping GreenMail ...");
        }

        if (services != null) {
            for (Service service : services.values()) {
                if (log.isDebugEnabled()) {
                    log.debug("Stopping service " + service.toString());
                }
                service.stopService();
            }
        }

        if (this.managers != null) {
            this.managers.stop();
        }

        managers = null;
        services = null;
    }

    @Override
    public void reset() {
        stop();
        start();
    }

    /**
     * Create the required services according to the server setup
     *
     * @param config Service configuration
     * @return Services map
     */
    private static Map<String, AbstractServer> createServices(ServerSetup[] config, Managers mgr) {
        Map<String, AbstractServer> srvc = new HashMap<>();
        for (ServerSetup setup : config) {
            if (srvc.containsKey(setup.getProtocol())) {
                throw new IllegalArgumentException("Server '" + setup.getProtocol() + "' was found at least twice in the array");
            }
            final String protocol = setup.getProtocol();
            if (protocol.startsWith(ServerSetup.PROTOCOL_SMTP)) {
                srvc.put(protocol, new SmtpServer(setup, mgr));
            } else if (protocol.startsWith(ServerSetup.PROTOCOL_POP3)) {
                srvc.put(protocol, new Pop3Server(setup, mgr));
            } else if (protocol.startsWith(ServerSetup.PROTOCOL_IMAP)) {
                srvc.put(protocol, new ImapServer(setup, mgr));
            }
        }
        return srvc;
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

    @Override
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
        return waitForIncomingEmail(5000L, emailCount);
    }

    @Override
    public MimeMessage[] getReceivedMessages() {
        List<StoredMessage> msgs = managers.getImapHostManager().getAllMessages();
        MimeMessage[] ret = new MimeMessage[msgs.size()];
        for (int i = 0; i < msgs.size(); i++) {
            StoredMessage storedMessage = msgs.get(i);
            ret[i] = storedMessage.getMimeMessage();
        }
        return ret;
    }

    /**
     * @deprecated As of 1.5 and to be removed in 1.6. Use {@link #getReceivedMessagesForDomain(String domain)} instead.
     */
    @Deprecated
    @Override
    public MimeMessage[] getReceviedMessagesForDomain(String domain) {
        return getReceivedMessagesForDomain(domain);
    }

    @Override
    public MimeMessage[] getReceivedMessagesForDomain(String domain) {
        List<StoredMessage> msgs = managers.getImapHostManager().getAllMessages();
        List<MimeMessage> ret = new ArrayList<>();
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
        if (this.managers == null) {
            throw new IllegalStateException("Please start GreenMail before calling setUser!");
        }
        GreenMailUser user = managers.getUserManager().getUser(login);
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

    @Override
    public GreenMail withConfiguration(GreenMailConfiguration config) {
        // Just overriding to return more specific type
        super.withConfiguration(config);
        return this;
    }

    @Override
    public void purgeEmailFromAllMailboxes() throws FolderException {
        ImapHostManager imaphost = getManagers().getImapHostManager();
        Store store = imaphost.getStore();
        Collection<MailFolder> mailboxes = store.listMailboxes("*");
        for (MailFolder folder : mailboxes) {
            folder.deleteAllMessages();
        }
    }

    /**
     * @deprecated As of 1.5 and to be removed in 1.6. No need to instantiate static helper class
     */
    @Deprecated
    public GreenMailUtil util() {
        return GreenMailUtil.instance();
    }

}
