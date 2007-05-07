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
     * Gets the configured users.
     *
     * @return the users.
     */
    String[] getUsers();

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
    void setSmtpProtocoll(final boolean theSmtpFlag);

    /**
     * Gets the SMTP status.
     *
     * @return true, if enabled.
     */
    boolean isSmtpProtocoll();

    /**
     * Toggles POP3.
     *
     * @param thePop3Flag true, if POP3 should be activated.
     */
    void setPop3Protocoll(final boolean thePop3Flag);

    /**
     * Gets the POP3 status.
     *
     * @return true, if enabled.
     */
    boolean isPop3Protocoll();

    /**
     * Toggles IMAP.
     *
     * @param theImapFlag true, if enabling.
     */
    void setImap(final boolean theImapFlag);

    /**
     * Gets the IMAP status.
     *
     * @return true, if enabled.
     */
    boolean isImap();

    /**
     * Sets the port offset.
     *
     * @param thePortOffset the port offset, defaults to {@link com.icegreen.greenmail.jboss.GreenMailService#DEFAULT_PORT_OFFSET}.
     */
    void setPortOffset(int thePortOffset);

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
