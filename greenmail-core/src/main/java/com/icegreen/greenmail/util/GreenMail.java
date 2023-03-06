/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.configuration.ConfiguredGreenMail;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.InMemoryStore;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility class that manages a greenmail server with support for multiple protocols
 */
public class GreenMail extends ConfiguredGreenMail {
    protected final Logger log = LoggerFactory.getLogger(GreenMail.class);
    protected Managers managers;
    protected final Map<String, AbstractServer> services = new HashMap<>();
    protected ServerSetup[] config;

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
     * @param config Server setup to use
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

        // Log support information including JVM and default file encoding
        if (log.isDebugEnabled()) {
            log.debug("GreenMail version: {}", BuildInfo.INSTANCE.getProjectVersion());
            log.debug("{} {} {}",
                System.getProperty("java.vm.name", "java.vm.name"),
                System.getProperty("java.vm.vendor", "java.vm.vendor"),
                System.getProperty("java.runtime.version", "java.runtime.version")
            );
            log.debug("file.encoding : {}", System.getProperty("file.encoding", "file.encoding"));
        }

        init();
    }

    /**
     * Initialize
     */
    private void init() {
        if (managers == null) {
            managers = new Managers();
        }

        services.clear();
        services.putAll( createServices(config, managers) );
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
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Could not start mail service " + service, ex);
            }

        }

        log.debug("Started services, performing check if all up");
        // Make sure if all services are up in a second loop, giving slow services more time.
        for (AbstractServer service : servers) {
            if (!service.isRunning()) {
                throw new IllegalStateException("Could not start mail server " + service
                        + ", try to set server startup timeout > " + service.getServerSetup().getServerStartupTimeout()
                        + " via " + ServerSetup.class.getSimpleName() + ".setServerStartupTimeout(timeoutInMs) or " +
                        "-Dgreenmail.startup.timeout");
            }
        }

        doConfigure();
    }

    @Override
    public synchronized void stop() {
        log.debug("Stopping GreenMail ...");

        for (Service service : services.values()) {
            log.debug("Stopping service {}", service);
            service.stopService();
        }
        services.clear();

        managers = new Managers();
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
    protected Map<String, AbstractServer> createServices(ServerSetup[] config, Managers mgr) {
        Map<String, AbstractServer> srvc = new HashMap<>();
        for (ServerSetup setup : config) {
            if (srvc.containsKey(setup.getProtocol())) {
                throw new IllegalArgumentException("Server '" + setup.getProtocol() + "' was found at least twice in setup config");
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
    public synchronized Managers getManagers() {
        return managers;
    }

    @Override
    public UserManager getUserManager() {
        return getManagers().getUserManager();
    }

    //~ Convenience Methods, often needed while testing ---------------------------------------------------------------
    @Override
    public boolean waitForIncomingEmail(long timeout, int emailCount) {
        final CountDownLatch waitObject = getManagers().getSmtpManager().createAndAddNewWaitObject(emailCount);
        final long endTime = System.currentTimeMillis() + timeout;
            while (waitObject.getCount() > 0) {
                final long waitTime = endTime - System.currentTimeMillis();
                if (waitTime < 0L) {
                    return waitObject.getCount() == 0;
                }
                try {
                    waitObject.await(waitTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // Continue loop, in case of premature interruption
                }
            }
        return waitObject.getCount() == 0;
    }

    @Override
    public boolean waitForIncomingEmail(int emailCount) {
        return waitForIncomingEmail(5000L, emailCount);
    }

    @Override
    public MimeMessage[] getReceivedMessages() {
        List<StoredMessage> msgs = getManagers().getImapHostManager().getAllMessages();
        MimeMessage[] ret = new MimeMessage[msgs.size()];
        for (int i = 0; i < msgs.size(); i++) {
            StoredMessage storedMessage = msgs.get(i);
            ret[i] = storedMessage.getMimeMessage();
        }
        return ret;
    }

    @Override
    public MimeMessage[] getReceivedMessagesForDomain(String domain) {
        List<StoredMessage> msgs = getManagers().getImapHostManager().getAllMessages();
        List<MimeMessage> ret = new ArrayList<>();
        try {
            for (StoredMessage msg : msgs) {
                String tos = GreenMailUtil.getAddressList(msg.getMimeMessage().getAllRecipients());
                if (null != tos && tos.toLowerCase().contains(domain.toLowerCase())) {
                    ret.add(msg.getMimeMessage());
                }
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return ret.toArray(new MimeMessage[0]);
    }

    @Override
    public GreenMailUser setUser(String login, String password) {
        return setUser(login, login, password);
    }

    @Override
    public GreenMailUser setUser(String email, String login, String password) {
        final UserManager userManager = getUserManager();
        GreenMailUser user = userManager.getUser(login);
        if (null == user) {
            try {
                user = userManager.createUser(email, login, password);
            } catch (UserException e) {
                throw new IllegalStateException(e);
            }
        } else {
            user.setPassword(password);
        }
        return user;
    }

    @Override
    public void setQuotaSupported(boolean isEnabled) {
        getManagers().getImapHostManager().getStore().setQuotaSupported(isEnabled);
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
        ImapHostManager imapHostManager = getManagers().getImapHostManager();
        InMemoryStore store = (InMemoryStore) imapHostManager.getStore();
        Collection<MailFolder> mailboxes = store.listMailboxes("*");
        for (MailFolder folder : mailboxes) {
            folder.deleteAllMessages();
        }
    }

    @Override
    public boolean isRunning() {
        for (AbstractServer service : services.values()) {
            if (!service.isRunning()) {
                log.debug("Service {} is not running", service);
                return false;
            }
        }
        return !services.isEmpty();
    }
}
