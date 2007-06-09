package com.icegreen.greenmail.jboss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Implements the GreenMailServiceMBean.
 *
 * @author Marcel May
 */
public class GreenMailService extends ServiceMBeanSupport implements GreenMailServiceMBean {
    /** New logger. */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** Default port offset is {@value #DEFAULT_PORT_OFFSET}. */
    public static final int DEFAULT_PORT_OFFSET = 3000;
    /** The mail server. */
    private GreenMail mGreenMail;

    /** SMTP server */
    private boolean mSmtpProtocoll = true;
    /** SMTPS server */
    private boolean mSmtpsProtocoll;
    /** POP3 server */
    private boolean mPop3Protocoll = true;
    /** POP3S server */
    private boolean mPop3sProtocoll;
    /** IMAP server. */
    private boolean mImapProtocoll;
    /** IMAPS server. */
    private boolean mImapsProtocoll;
    /** Users. */
    private String[] mUsers;
    /** Port offset (default is {@value #DEFAULT_PORT_OFFSET}) */
    private int mPortOffset = DEFAULT_PORT_OFFSET;
    /** Hostname. Default is null (= localhost). */
    private String mHostname = "localhost";
    /** Helper array. */
    private static final ServerSetup[] SERVER_SETUP_ARRAY = new ServerSetup[0];
    /** All server setups configured. */
    private List<ServerSetup> mServerSetups;

    // ****** GreenMail service methods

    /** {@inheritDoc} */
    public void setSmtpProtocoll(final boolean theSmtpProtocoll) {
        mSmtpProtocoll = theSmtpProtocoll;
    }

    /** {@inheritDoc} */
    public boolean isSmtpProtocoll() {
        return mSmtpProtocoll;
    }

    /** {@inheritDoc} */
    public void setPop3Protocoll(final boolean thePop3Protocoll) {
        mPop3Protocoll = thePop3Protocoll;
    }

    /** {@inheritDoc} */
    public boolean isPop3Protocoll() {
        return mPop3Protocoll;
    }

    /** {@inheritDoc} */
    public void setImap(final boolean theImapFlag) {
        mImapProtocoll = theImapFlag;
    }

    /** {@inheritDoc} */
    public boolean isImap() {
        return mImapProtocoll;
    }

    /** {@inheritDoc} */
    public void setHostname(final String pHostname) {
        mHostname = pHostname;
    }

    /** {@inheritDoc} */
    public String getHostname() {
        return mHostname;
    }

    /** {@inheritDoc} */
    public void setUsers(final String[] theUsers) {
        if (log.isDebugEnabled()) {
            log.debug("Configuring mail users " + Arrays.asList(theUsers));
        }
        mUsers = theUsers;
    }


    /** {@inheritDoc} */
    public void sendMail(final String theTo,
                         final String theFrom,
                         final String theSubject,
                         final String theBody) {
        if (log.isDebugEnabled()) {
            log.debug("Sending mail, TO: <" + theTo + ">, FROM: <" + theFrom +
                    ">, SUBJECT: <" + theSubject + ">, CONTENT: <" + theBody+'>');
        }

        try {
            Session session = GreenMailUtil.getSession(getSmtpServerSetup());

            Address[] tos = new InternetAddress[]{new InternetAddress(theTo)};
            Address from = new InternetAddress(theFrom);
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setSubject(theSubject);
            mimeMessage.setFrom(from);

            mimeMessage.setText(theBody);
            Transport.send(mimeMessage, tos);
        } catch (Throwable e) {
            throw new RuntimeException("Can not send mail", e);
        }
    }

    private ServerSetup getSmtpServerSetup() {
        // TODO: allow the user to choose
        for (ServerSetup setup : mServerSetups) {
            if (setup.getProtocol().toUpperCase().startsWith("SMTP")) {
                return setup;
            }
        }
        throw new IllegalStateException("No SMTP(S) server setup found");
    }

    /** {@inheritDoc} */
    public String[] getUsers() {
        return mUsers;
    }

    // ****** JBoss Service methods

    @Override
    public void startService() throws Exception {
        super.start();
        createGreenMail();
    }

    @Override
    public void stopService() {
        if (null != mGreenMail) {
            mGreenMail.stop();
            if (log.isDebugEnabled()) {
                log.debug("GreenMailService stopped.");
            }
        } else {
            log.info("Already stopped, ignoring.");
        }
        super.stop();
    }

    // ****** Helper
    private void createGreenMail() throws Exception {
        if (null == mGreenMail) {
            mServerSetups = createServerSetup();
            mGreenMail = new GreenMail(mServerSetups.toArray(SERVER_SETUP_ARRAY));
            if (log.isInfoEnabled()) {
                StringBuffer msg = new StringBuffer("Starting greenmail service ( ");
                if (isSmtpProtocoll()) {
                    msg.append("SMTP:").append(mGreenMail.getSmtp().getPort()).append(' ');
                }
                if (isPop3Protocoll()) {
                    msg.append("POP3:").append(mGreenMail.getPop3().getPort()).append(' ');
                }
                msg.append(" )");
                log.info(msg.toString());
            }
            if (null != mUsers) {
                for (String user : mUsers) {
                    addMailUser(user);
                }
            }
            mGreenMail.start();
        } else {
            log.info("Already started, ignoring.");
        }
    }

    private void addMailUser(final String user) {
        int posColon = user.indexOf(':');
        int posAt = user.indexOf('@');
        String login = user.substring(0, posColon);
        String pwd = user.substring(posColon + 1, posAt);
        String domain = user.substring(posAt + 1);
        if (log.isDebugEnabled()) {
            // This is a test system, so we do not care about pwd in the log file.
            log.debug("Adding user " + login + ':' + pwd + '@' + domain);
        }
        mGreenMail.setUser(login + '@' + domain, login, pwd);
    }

    /**
     * Creates the server setup, depending on the protocoll flags.
     *
     * @return the configured server setups.
     */
    private List<ServerSetup> createServerSetup() {
        List<ServerSetup> setups = new ArrayList<ServerSetup>();
        if (mSmtpProtocoll) {
            setups.add(createTestServerSetup(ServerSetup.SMTP));
        }
        if (mSmtpsProtocoll) {
            setups.add(createTestServerSetup(ServerSetup.SMTPS));
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
        return setups;
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
     * Setter for property 'smtpsProtocoll'.
     *
     * @param theSmtpsProtocoll Value to set for property 'smtpsProtocoll'.
     */
    public void setSmtpsProtocoll(final boolean theSmtpsProtocoll) {
        mSmtpsProtocoll = theSmtpsProtocoll;
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
     * Setter for property 'imapProtocoll'.
     *
     * @param theImapProtocoll Value to set for property 'imapProtocoll'.
     */
    public void setImapProtocoll(final boolean theImapProtocoll) {
        mImapProtocoll = theImapProtocoll;
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
     * Setter for property 'portOffset'.
     *
     * @param thePortOffset Value to set for property 'portOffset'.
     */
    public void setPortOffset(final int thePortOffset) {
        mPortOffset = thePortOffset;
    }


}

