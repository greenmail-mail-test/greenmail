package com.icegreen.greenmail.spring;

import java.util.ArrayList;
import java.util.List;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring bean for GreenMail server.
 * <p>
 * By default,
 * <ul>
 *  <li>SMTP, POP3 services are activated</li>
 *  <li>autostart is enabled</li>
 *  <li>port offset is 3000</li>
 * </ul>
 *
 * @author Marcel May (mm)
 */
public class GreenMailBean implements InitializingBean, DisposableBean, BeanNameAware {
    /** New logger. */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** Spring bean name. */
    private String name;

    /** The mail server. */
    private GreenMail greenMail;
    /** Automatically start the servers. */
    private boolean autostart = true;
    /** SMTP server */
    private boolean smtpProtocol = true;
    /** SMTPS server */
    private boolean smtpsProtocol = false;
    /** POP3 server */
    private boolean pop3Protocol = true;
    /** POP3S server */
    private boolean pop3sProtocol = false;
    /** IMAP server. */
    private boolean imapProtocol = false;
    /** IMAPS server. */
    private boolean imapsProtocol = false;
    /** Users. */
    private List<String> users;
    /** Port offset (default is 3000) */
    private int portOffset = 3000;
    /** Hostname. Default is null (= localhost). */
    private String hostname;
    /** If the server is started. */
    private boolean started;
    /** Outgoing mail server setup. */
    private ServerSetup smtpServerSetup;
    /** Outgoing secure mail server setup. */
    private ServerSetup smtpsServerSetup;
    /** Timeout to wait for server startup in millis */
    private long serverStartupTimeout = 1000L;

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied (and satisfied
     * BeanFactoryAware and ApplicationContextAware). <p>This method allows the bean instance to
     * perform initialization only possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such as failure to set an essential
     *                   property) or if initialization fails.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        greenMail = new GreenMail(createServerSetup());
        if (null != users) {
            for (String user : users) {
                int posColon = user.indexOf(':');
                int posAt = user.indexOf('@');
                String login = user.substring(0, posColon);
                String pwd = user.substring(posColon + 1, posAt);
                String domain = user.substring(posAt + 1);
                if (log.isDebugEnabled()) {
                    log.debug("Adding user {}:{}@{}" ,login, pwd, domain);
                }
                greenMail.setUser(login + '@' + domain, login, pwd);
            }
        }
        if (autostart) {
            greenMail.start();
            started = true;
        }
    }

    /**
     * Creates the server setup, depending on the protocol flags.
     *
     * @return the configured server setups.
     */
    private ServerSetup[] createServerSetup() {
        List<ServerSetup> setups = new ArrayList<>();
        if (smtpProtocol) {
            smtpServerSetup = createTestServerSetup(ServerSetup.SMTP);
            setups.add(smtpServerSetup);
        }
        if (smtpsProtocol) {
            smtpsServerSetup = createTestServerSetup(ServerSetup.SMTPS);
            setups.add(smtpsServerSetup);
        }
        if (pop3Protocol) {
            setups.add(createTestServerSetup(ServerSetup.POP3));
        }
        if (pop3sProtocol) {
            setups.add(createTestServerSetup(ServerSetup.POP3S));
        }
        if (imapProtocol) {
            setups.add(createTestServerSetup(ServerSetup.IMAP));
        }
        if (imapsProtocol) {
            setups.add(createTestServerSetup(ServerSetup.IMAPS));
        }
        return setups.toArray(new ServerSetup[setups.size()]);
    }

    /** Starts the server. */
    public void start() {
        synchronized (greenMail) {
            if (started) {
                log.warn("Can not start server (already started)");
            } else {
                greenMail.start();
                started = true;
            }
        }
    }

    /** Stops the server. */
    public void stop() {
        synchronized (greenMail) {
            if (started) {
                greenMail.stop();
                started = false;
            } else {
                log.warn("Can not stop server (not started).");
            }
        }
    }

    /**
     * Creates a test server setup with configured offset.
     *
     * @param theSetup the server setup.
     * @return the test server setup.
     */
    private ServerSetup createTestServerSetup(final ServerSetup theSetup) {
        ServerSetup serverSetup =
                new ServerSetup(portOffset + theSetup.getPort(),
                               hostname,
                               theSetup.getProtocol());
        serverSetup.setServerStartupTimeout(serverStartupTimeout);
        return serverSetup;
    }

    /**
     * Gets the currently received messages.
     *
     * @return the available messages.
     */
    public MimeMessage[] getReceivedMessages() {
        return greenMail.getReceivedMessages();
    }

    /**
     * Invoked by a BeanFactory on destruction of a singleton.
     *
     * @throws Exception in case of shutdown errors. Exceptions will get logged but not rethrown to
     *                   allow other beans to release their resources too.
     */
    @Override
    public void destroy() throws Exception {
        greenMail.stop();
    }


    /**
     * Sets the autostart flag.
     *
     * @param theAutostart the flag.
     */
    public void setAutostart(final boolean theAutostart) {
        autostart = theAutostart;
    }

    /**
     * Getter for property 'autostart'.
     *
     * @return Value for property 'autostart'.
     */
    public boolean isAutostart() {
        return autostart;
    }

    /**
     * Setter for property 'smtpProtocol'.
     *
     * @param theSmtpProtocoll Value to set for property 'smtpProtocol'.
     */
    public void setSmtpProtocol(final boolean theSmtpProtocoll) {
        smtpProtocol = theSmtpProtocoll;
    }

    /**
     * Getter for property 'smtpProtocol'.
     *
     * @return Value for property 'smtpProtocol'.
     */
    public boolean isSmtpProtocol() {
        return smtpProtocol;
    }

    /**
     * Setter for property 'smtpsProtocol'.
     *
     * @param theSmtpsProtocoll Value to set for property 'smtpsProtocol'.
     */
    public void setSmtpsProtocol(final boolean theSmtpsProtocoll) {
        smtpsProtocol = theSmtpsProtocoll;
    }

    /**
     * Getter for property 'smtpsProtocol'.
     *
     * @return Value for property 'smtpsProtocol'.
     */
    public boolean isSmtpsProtocol() {
        return smtpsProtocol;
    }

    /**
     * Setter for property 'pop3Protocol'.
     *
     * @param thePop3Protocoll Value to set for property 'pop3Protocol'.
     */
    public void setPop3Protocol(final boolean thePop3Protocoll) {
        pop3Protocol = thePop3Protocoll;
    }

    /**
     * Getter for property 'pop3Protocol'.
     *
     * @return Value for property 'pop3Protocol'.
     */
    public boolean isPop3Protocol() {
        return pop3Protocol;
    }

    /**
     * Setter for property 'pop3sProtocol'.
     *
     * @param thePop3sProtocoll Value to set for property 'pop3sProtocol'.
     */
    public void setPop3sProtocol(final boolean thePop3sProtocoll) {
        pop3sProtocol = thePop3sProtocoll;
    }

    /**
     * Getter for property 'pop3sProtocol'.
     *
     * @return Value for property 'pop3sProtocol'.
     */
    public boolean isPop3sProtocol() {
        return pop3sProtocol;
    }

    /**
     * Setter for property 'imapProtocol'.
     *
     * @param theImapProtocoll Value to set for property 'imapProtocol'.
     */
    public void setImapProtocol(final boolean theImapProtocoll) {
        imapProtocol = theImapProtocoll;
    }

    /**
     * Getter for property 'imapProtocol'.
     *
     * @return Value for property 'imapProtocol'.
     */
    public boolean isImapProtocol() {
        return imapProtocol;
    }

    /**
     * Setter for property 'imapsProtocol'.
     *
     * @param theImapsProtocoll Value to set for property 'imapsProtocol'.
     */
    public void setImapsProtocol(final boolean theImapsProtocoll) {
        imapsProtocol = theImapsProtocoll;
    }

    /**
     * Getter for property 'imapsProtocol'.
     *
     * @return Value for property 'imapsProtocol'.
     */
    public boolean isImapsProtocol() {
        return imapsProtocol;
    }

    /**
     * Setter for property 'hostname'.
     *
     * @param pHostname Value to set for property 'hostname'.
     */
    public void setHostname(final String pHostname) {
        hostname = pHostname;
    }

    /**
     * Getter for property 'hostname'.
     *
     * @return Value for property 'hostname'.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Setter for property 'portOffset'.
     *
     * @param pPortOffset Value to set for property 'portOffset'.
     */
    public void setPortOffset(final int pPortOffset) {
        portOffset = pPortOffset;
    }

    /**
     * Getter for property 'portOffset'.
     *
     * @return Value for property 'portOffset'.
     */
    public int getPortOffset() {
        return portOffset;
    }

    /**
     * Setter for property 'users'.
     *
     * @param theUsers Value to set for property 'users'.
     */
    public void setUsers(final List<String> theUsers) {
        users = theUsers;
    }

    /**
     * Getter for property 'users'.
     *
     * @return Value for property 'users'.
     */
    public List<String> getUsers() {
        return users;
    }

    /**
     * Getter for property 'started'.
     *
     * @return Value for property 'started'.
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Getter for property 'greenMail'.
     *
     * @return Value for property 'greenMail'.
     */
    public GreenMail getGreenMail() {
        return greenMail;
    }

    /**
     * Sends a mail message to the GreenMail server.
     * <p>
     * Note: SMTP or SMTPS must be configured.
     *
     * @param theTo      the <em>TO</em> field.
     * @param theFrom    the <em>FROM</em>field.
     * @param theSubject the subject.
     * @param theContent the message content.
     */
    public void sendEmail(final String theTo, final String theFrom, final String theSubject,
                          final String theContent) {
        if (null == smtpServerSetup) {
            throw new IllegalStateException("Can not send mail, no SMTP or SMTPS setup found");
        }
        GreenMailUtil.sendTextEmail(theTo, theFrom, theSubject, theContent, smtpServerSetup);
    }

    @Override
    public void setBeanName(final String pName) {
        name = pName;
    }

    /**
     * Timeout to wait for server startup in millis
     *
     * @return Value for property 'serverStartupTimeout'
     */
    public long getServerStartupTimeout() {
        return serverStartupTimeout;
    }

    public void setServerStartupTimeout(long serverStartupTimeout) {
        this.serverStartupTimeout = serverStartupTimeout;
    }
}
