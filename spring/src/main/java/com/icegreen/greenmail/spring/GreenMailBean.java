package com.icegreen.greenmail.spring;

import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Spring bean for GreenMail server.
 * <p/>
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
    private boolean smtpProtocoll = true;
    /** SMTPS server */
    private boolean smtpsProtocoll = false;
    /** POP3 server */
    private boolean pop3Protocoll = true;
    /** POP3S server */
    private boolean pop3sProtocoll = false;
    /** IMAP server. */
    private boolean imapProtocoll = false;
    /** IMAPS server. */
    private boolean imapsProtocoll = false;
    /** Users. */
    private List users;
    /** Port offset (default is 3000) */
    private int portOffset = 3000;
    /** Hostname. Default is null (= localhost). */
    private String hostname;
    /** If the server is started. */
    private boolean started;
    /** Outoing mail server setup. */
    private ServerSetup smtpServerSetup;
    /** Outoing secure mail server setup. */
    private ServerSetup smtpsServerSetup;

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied (and satisfied
     * BeanFactoryAware and ApplicationContextAware). <p>This method allows the bean instance to
     * perform initialization only possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such as failure to set an essential
     *                   property) or if initialization fails.
     */
    public void afterPropertiesSet() throws Exception {
        greenMail = new GreenMail(createServerSetup());
        if (null != users) {
            for (int i = 0; i < users.size(); i++) {
                String user = (String) users.get(i);
                int posColon = user.indexOf(':');
                int posAt = user.indexOf('@');
                String login = user.substring(0, posColon);
                String pwd = user.substring(posColon + 1, posAt);
                String domain = user.substring(posAt + 1);
                if (log.isDebugEnabled()) {
                    log.debug("Adding user " + login + ':' + pwd + '@' + domain);
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
     * Creates the server setup, depending on the protocoll flags.
     *
     * @return the configured server setups.
     */
    private ServerSetup[] createServerSetup() {
        List setups = new ArrayList();
        if (smtpProtocoll) {
            smtpServerSetup = createTestServerSetup(ServerSetup.SMTP);
            setups.add(smtpServerSetup);
        }
        if (smtpsProtocoll) {
            smtpsServerSetup = createTestServerSetup(ServerSetup.SMTPS);
            setups.add(smtpsServerSetup);
        }
        if (pop3Protocoll) {
            setups.add(createTestServerSetup(ServerSetup.POP3));
        }
        if (pop3sProtocoll) {
            setups.add(createTestServerSetup(ServerSetup.POP3S));
        }
        if (imapProtocoll) {
            setups.add(createTestServerSetup(ServerSetup.IMAP));
        }
        if (imapsProtocoll) {
            setups.add(createTestServerSetup(ServerSetup.IMAPS));
        }
        return (ServerSetup[]) setups.toArray(new ServerSetup[0]);
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
                started = true;
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
        return new ServerSetup(portOffset + theSetup.getPort(),
                               hostname,
                               theSetup.getProtocol());
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
     * Setter for property 'smtpProtocoll'.
     *
     * @param theSmtpProtocoll Value to set for property 'smtpProtocoll'.
     */
    public void setSmtpProtocoll(final boolean theSmtpProtocoll) {
        smtpProtocoll = theSmtpProtocoll;
    }

    /**
     * Getter for property 'smtpProtocoll'.
     *
     * @return Value for property 'smtpProtocoll'.
     */
    public boolean isSmtpProtocoll() {
        return smtpProtocoll;
    }

    /**
     * Setter for property 'smtpsProtocoll'.
     *
     * @param theSmtpsProtocoll Value to set for property 'smtpsProtocoll'.
     */
    public void setSmtpsProtocoll(final boolean theSmtpsProtocoll) {
        smtpsProtocoll = theSmtpsProtocoll;
    }

    /**
     * Getter for property 'smtpsProtocoll'.
     *
     * @return Value for property 'smtpsProtocoll'.
     */
    public boolean isSmtpsProtocoll() {
        return smtpsProtocoll;
    }

    /**
     * Setter for property 'pop3Protocoll'.
     *
     * @param thePop3Protocoll Value to set for property 'pop3Protocoll'.
     */
    public void setPop3Protocoll(final boolean thePop3Protocoll) {
        pop3Protocoll = thePop3Protocoll;
    }

    /**
     * Getter for property 'pop3Protocoll'.
     *
     * @return Value for property 'pop3Protocoll'.
     */
    public boolean isPop3Protocoll() {
        return pop3Protocoll;
    }

    /**
     * Setter for property 'pop3sProtocoll'.
     *
     * @param thePop3sProtocoll Value to set for property 'pop3sProtocoll'.
     */
    public void setPop3sProtocoll(final boolean thePop3sProtocoll) {
        pop3sProtocoll = thePop3sProtocoll;
    }

    /**
     * Getter for property 'pop3sProtocoll'.
     *
     * @return Value for property 'pop3sProtocoll'.
     */
    public boolean isPop3sProtocoll() {
        return pop3sProtocoll;
    }

    /**
     * Setter for property 'imapProtocoll'.
     *
     * @param theImapProtocoll Value to set for property 'imapProtocoll'.
     */
    public void setImapProtocoll(final boolean theImapProtocoll) {
        imapProtocoll = theImapProtocoll;
    }

    /**
     * Getter for property 'imapProtocoll'.
     *
     * @return Value for property 'imapProtocoll'.
     */
    public boolean isImapProtocoll() {
        return imapProtocoll;
    }

    /**
     * Setter for property 'imapsProtocoll'.
     *
     * @param theImapsProtocoll Value to set for property 'imapsProtocoll'.
     */
    public void setImapsProtocoll(final boolean theImapsProtocoll) {
        imapsProtocoll = theImapsProtocoll;
    }

    /**
     * Getter for property 'imapsProtocoll'.
     *
     * @return Value for property 'imapsProtocoll'.
     */
    public boolean isImapsProtocoll() {
        return imapsProtocoll;
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
    public void setUsers(final List theUsers) {
        users = theUsers;
    }

    /**
     * Getter for property 'users'.
     *
     * @return Value for property 'users'.
     */
    public List getUsers() {
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
     * <p/>
     * Note: SMTP or SMTPS must be configured.
     *
     * @param theTo      the <em>TO</em> field.
     * @param theFrom    the <em>FROM</em>field.
     * @param theSubject the subject.
     * @param theContent the message content.
     */
    public void sendEmail(final String theTo, final String theFrom, final String theSubject,
                          final String theContent) {
        ServerSetup serverSetup = smtpServerSetup;
        if (null == serverSetup) {
            serverSetup = smtpsServerSetup;
        }
        if (null == serverSetup) {
            throw new IllegalStateException("Can not send mail, no SMTP or SMTPS setup found");
        }
        GreenMailUtil.sendTextEmail(theTo, theFrom, theSubject, theContent, serverSetup);
    }

    public void setBeanName(final String pName) {
        name = pName;
    }
}
