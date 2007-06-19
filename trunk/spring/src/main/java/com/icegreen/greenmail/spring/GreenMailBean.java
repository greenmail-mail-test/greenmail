package com.icegreen.greenmail.spring;

import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.GreenMailUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring bean for GreenMail server.
 * <p/>
 * By default, <ul> <li>SMTP, POP3 services are activated</li> <li>autostart is enabled</li>
 * <li>port offset is 3000</li> </ul>
 *
 * @author Marcel May (mm)
 */
public class GreenMailBean implements InitializingBean, DisposableBean {
    /** New logger. */
    protected final Log log = LogFactory.getLog(getClass());

    /** The mail server. */
    private GreenMail mGreenMail;
    /** Automatically start the servers. */
    private boolean mAutostart = true;
    /** SMTP server */
    private boolean mSmtpProtocoll = true;
    /** SMTPS server */
    private boolean mSmtpsProtocoll = false;
    /** POP3 server */
    private boolean mPop3Protocoll = true;
    /** POP3S server */
    private boolean mPop3sProtocoll = false;
    /** IMAP server. */
    private boolean mImapProtocoll = false;
    /** IMAPS server. */
    private boolean mImapsProtocoll = false;
    /** Users. */
    private List mUsers;
    /** Port offset (default is 3000) */
    private int mPortOffset = 3000;
    /** Hostname. Default is null (= localhost). */
    private String mHostname;
    /** If the server is started. */
    private boolean mStarted;
    /** Outoing mail server setup. */
    private ServerSetup mSmtpServerSetup;
    /** Outoing secure mail server setup. */
    private ServerSetup mSmtpsServerSetup;

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
        mGreenMail = new GreenMail(createServerSetup());
        if (null != mUsers) {
            for (int i = 0; i < mUsers.size(); i++) {
                String user = (String) mUsers.get(i);
                int posColon = user.indexOf(':');
                int posAt = user.indexOf('@');
                String login = user.substring(0, posColon);
                String pwd = user.substring(posColon + 1, posAt);
                String domain = user.substring(posAt + 1);
                if (log.isDebugEnabled()) {
                    log.debug("Adding user " + login + ':' + pwd + '@' + domain);
                }
                mGreenMail.setUser(login + '@' + domain, login, pwd);
            }
        }
        if (mAutostart) {
            mGreenMail.start();
            mStarted = true;
        }
    }

    /**
     * Creates the server setup, depending on the protocoll flags.
     *
     * @return the configured server setups.
     */
    private ServerSetup[] createServerSetup() {
        List setups = new ArrayList();
        if (mSmtpProtocoll) {
            mSmtpServerSetup = createTestServerSetup(ServerSetup.SMTP);
            setups.add(mSmtpServerSetup);
        }
        if (mSmtpsProtocoll) {
            mSmtpsServerSetup = createTestServerSetup(ServerSetup.SMTPS);
            setups.add( mSmtpsServerSetup );
        }
        if (mPop3Protocoll) {
            setups.add(createTestServerSetup(ServerSetup.POP3));
        }
        if (mPop3sProtocoll) {
            setups.add(createTestServerSetup(ServerSetup.POP3S));
        }
        if (mImapProtocoll) {
            setups.add(createTestServerSetup(ServerSetup.IMAP));
        }
        if (mImapsProtocoll) {
            setups.add(createTestServerSetup(ServerSetup.IMAPS));
        }
        return (ServerSetup[]) setups.toArray(new ServerSetup[0]);
    }

    /** Starts the server. */
    public void start() {
        synchronized (mGreenMail) {
            if (mStarted) {
                log.warn("Can not start server (already started)");
            } else {
                mGreenMail.start();
                mStarted = true;
            }
        }
    }

    /** Stops the server. */
    public void stop() {
        synchronized (mGreenMail) {
            if (mStarted) {
                mGreenMail.stop();
                mStarted = true;
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
        return new ServerSetup(mPortOffset + theSetup.getPort(),
                               mHostname,
                               theSetup.getProtocol());
    }

    /**
     * Gets the currently received messages.
     *
     * @return the available messages.
     */
    public MimeMessage[] getReceivedMessages() {
        return mGreenMail.getReceivedMessages();
    }

    /**
     * Invoked by a BeanFactory on destruction of a singleton.
     *
     * @throws Exception in case of shutdown errors. Exceptions will get logged but not rethrown to
     *                   allow other beans to release their resources too.
     */
    public void destroy() throws Exception {
        mGreenMail.stop();
    }


    /**
     * Sets the autostart flag.
     *
     * @param theAutostart the flag.
     */
    public void setAutostart(final boolean theAutostart) {
        mAutostart = theAutostart;
    }

    /**
     * Getter for property 'autostart'.
     *
     * @return Value for property 'autostart'.
     */
    public boolean isAutostart() {
        return mAutostart;
    }

    /**
     * Setter for property 'smtpProtocoll'.
     *
     * @param theSmtpProtocoll Value to set for property 'smtpProtocoll'.
     */
    public void setSmtpProtocoll(final boolean theSmtpProtocoll) {
        mSmtpProtocoll = theSmtpProtocoll;
    }

    /**
     * Getter for property 'smtpProtocoll'.
     *
     * @return Value for property 'smtpProtocoll'.
     */
    public boolean isSmtpProtocoll() {
        return mSmtpProtocoll;
    }

    /**
     * Setter for property 'smtpsProtocoll'.
     *
     * @param theSmtpsProtocoll Value to set for property 'smtpsProtocoll'.
     */
    public void setSmtpsProtocoll(final boolean theSmtpsProtocoll) {
        mSmtpsProtocoll = theSmtpsProtocoll;
    }

    /**
     * Getter for property 'smtpsProtocoll'.
     *
     * @return Value for property 'smtpsProtocoll'.
     */
    public boolean isSmtpsProtocoll() {
        return mSmtpsProtocoll;
    }

    /**
     * Setter for property 'pop3Protocoll'.
     *
     * @param thePop3Protocoll Value to set for property 'pop3Protocoll'.
     */
    public void setPop3Protocoll(final boolean thePop3Protocoll) {
        mPop3Protocoll = thePop3Protocoll;
    }

    /**
     * Getter for property 'pop3Protocoll'.
     *
     * @return Value for property 'pop3Protocoll'.
     */
    public boolean isPop3Protocoll() {
        return mPop3Protocoll;
    }

    /**
     * Setter for property 'pop3sProtocoll'.
     *
     * @param thePop3sProtocoll Value to set for property 'pop3sProtocoll'.
     */
    public void setPop3sProtocoll(final boolean thePop3sProtocoll) {
        mPop3sProtocoll = thePop3sProtocoll;
    }

    /**
     * Getter for property 'pop3sProtocoll'.
     *
     * @return Value for property 'pop3sProtocoll'.
     */
    public boolean isPop3sProtocoll() {
        return mPop3sProtocoll;
    }

    /**
     * Setter for property 'imapProtocoll'.
     *
     * @param theImapProtocoll Value to set for property 'imapProtocoll'.
     */
    public void setImapProtocoll(final boolean theImapProtocoll) {
        mImapProtocoll = theImapProtocoll;
    }

    /**
     * Getter for property 'imapProtocoll'.
     *
     * @return Value for property 'imapProtocoll'.
     */
    public boolean isImapProtocoll() {
        return mImapProtocoll;
    }

    /**
     * Setter for property 'imapsProtocoll'.
     *
     * @param theImapsProtocoll Value to set for property 'imapsProtocoll'.
     */
    public void setImapsProtocoll(final boolean theImapsProtocoll) {
        mImapsProtocoll = theImapsProtocoll;
    }

    /**
     * Getter for property 'imapsProtocoll'.
     *
     * @return Value for property 'imapsProtocoll'.
     */
    public boolean isImapsProtocoll() {
        return mImapsProtocoll;
    }

    /**
     * Setter for property 'hostname'.
     *
     * @param pHostname Value to set for property 'hostname'.
     */
    public void setHostname(final String pHostname) {
        mHostname = pHostname;
    }

    /**
     * Getter for property 'hostname'.
     *
     * @return Value for property 'hostname'.
     */
    public String getHostname() {
        return mHostname;
    }

    /**
     * Setter for property 'portOffset'.
     *
     * @param pPortOffset Value to set for property 'portOffset'.
     */
    public void setPortOffset(final int pPortOffset) {
        mPortOffset = pPortOffset;
    }

    /**
     * Getter for property 'portOffset'.
     *
     * @return Value for property 'portOffset'.
     */
    public int getPortOffset() {
        return mPortOffset;
    }

    /**
     * Setter for property 'users'.
     *
     * @param theUsers Value to set for property 'users'.
     */
    public void setUsers(final List theUsers) {
        mUsers = theUsers;
    }

    /**
     * Getter for property 'users'.
     *
     * @return Value for property 'users'.
     */
    public List getUsers() {
        return mUsers;
    }

    /**
     * Getter for property 'started'.
     *
     * @return Value for property 'started'.
     */
    public boolean isStarted() {
        return mStarted;
    }

    /**
     * Getter for property 'greenMail'.
     *
     * @return Value for property 'greenMail'.
     */
    public GreenMail getGreenMail() {
        return mGreenMail;
    }

    /**
     * Sends a mail message to the GreenMail server.
     *
     * Note: SMTP or SMTPS must be configured.
     *
     * @param theTo the <em>TO</em> field.
     * @param theFrom the <em>FROM</em>field.
     * @param theSubject the subject.
     * @param theContent the message content.
     */
    public void sendEmail(final String theTo, final String theFrom, final String theSubject,
                          final String theContent) {
        ServerSetup serverSetup = mSmtpServerSetup;
        if(null==serverSetup) {
            serverSetup = mSmtpsServerSetup;
        }
        if(null==serverSetup) {
            throw new IllegalStateException("Can not send mail, no SMTP or SMTPS setup found");
        }
        GreenMailUtil.sendTextEmail(theTo, theFrom, theSubject, theContent, serverSetup);
    }
}
