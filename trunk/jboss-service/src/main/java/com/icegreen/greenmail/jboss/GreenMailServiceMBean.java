package com.icegreen.greenmail.jboss;

import org.jboss.system.ServiceMBean;

/**
 * Provides the GreenMail JMX interface.
 *
 * @author Marcel May
 */
public interface GreenMailServiceMBean extends ServiceMBean {
    /**
     * Configures the available user accounts.
     * <p/>
     * The user format is 'USER:PWD@DOMAIN'.
     *
     * @param theUsers the theUsers.
     */
    void setUsers(final String[] theUsers);

    /**
     * Get the list of configured users.
     *
     * @return Value for property 'users'.
     */
    public String[] getUsers();

    /**
     * Sets the hostname for the mail server.
     * <p/>
     * The default hostname is 'localhost'.
     *
     * @param theHostname the hostname.
     */
    void setHostname(final String theHostname);

    /**
     * Gets the hostname.
     *
     * @return the hostname.
     */
    String getHostname();

    /**
     * Toggles SMTP.
     *
     * @param theSmtpFlag true, if SMTP should be activated.
     */
    void setSmtpProtocol(final boolean theSmtpFlag);

    /**
     * Gets the SMTP status.
     *
     * @return true, if enabled.
     */
    boolean isSmtpProtocol();

    /**
     * Toggles SMTPS.
     *
     * @param theSmtpsFlag true, if SMTPS should be activated.
     */
    void setSmtpsProtocol(final boolean theSmtpsFlag);

    /**
     * Gets the SMTPS status.
     *
     * @return true, if enabled.
     */
    boolean isSmtpsProtocol();

    /**
     * Toggles POP3.
     *
     * @param thePop3Flag true, if POP3 should be activated.
     */
    void setPop3Protocol(final boolean thePop3Flag);

    /**
     * Gets the POP3 status.
     *
     * @return true, if enabled.
     */
    boolean isPop3Protocol();

    /**
     * Toggles POP3S.
     *
     * @param thePop3sFlag true, if POP3S should be activated.
     */
    void setPop3sProtocol(final boolean thePop3sFlag);

    /**
     * Gets the POP3S status.
     *
     * @return true, if enabled.
     */
    boolean isPop3sProtocol();

    /**
     * Toggles IMAP.
     *
     * @param theImapFlag true, if enabling.
     */
    void setImapProtocol(final boolean theImapFlag);

    /**
     * Gets the IMAP status.
     *
     * @return true, if enabled.
     */
    boolean isImapProtocol();

    /**
     * Toggles IMAPS.
     *
     * @param theImapsFlag true, if enabling.
     */
    void setImapsProtocol(final boolean theImapsFlag);

    /**
     * Gets the IMAPS status.
     *
     * @return true, if enabled.
     */
    boolean isImapsProtocol();

    /**
     * Sets the port offset.
     *
     * @param thePortOffset the port offset, defaults to {@link com.icegreen.greenmail.jboss.GreenMailService#DEFAULT_PORT_OFFSET}.
     */
    void setPortOffset(int thePortOffset);

    /**
     * Gets the port offset.
     *
     * @return the offset.
     */
    int getPortOffset();

    /**
     * Lists the configured users as HTML fragment.
     *
     * @return the users.
     */
    String listUsersHTML();

    /**
     * Sends a mail.
     *
     * @param theTo      the 'TO' field.
     * @param theFrom    the 'FROM' field.
     * @param theSubject the 'SUBJECT' field.
     * @param theContent the content.
     */
    public void sendMail(final String theTo,
                         final String theFrom,
                         final String theSubject,
                         final String theContent);

    /**
     * Starts the service.
     * <p/>
     * Starting and stopping reconfigures the service.
     *
     * @throws Exception
     */
    void startService() throws Exception;

    /**
     * Stops the service.
     *
     * @throws Exception on error.
     */
    void stopService() throws Exception;
}
