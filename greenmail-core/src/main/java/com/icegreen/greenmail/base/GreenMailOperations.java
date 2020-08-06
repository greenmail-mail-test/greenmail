package com.icegreen.greenmail.base;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Interface that contains all operations provided by the Greenmail main class
 */
public interface GreenMailOperations {
    /**
     * @return SMTP server operated by this GreenMail instance or null if there is none
     */
    SmtpServer getSmtp();

    /**
     * @return SMTP server operated by this GreenMail instance or null if there is none
     */
    ImapServer getImap();

    /**
     * @return SMTP server operated by this GreenMail instance or null if there is none
     */
    Pop3Server getPop3();

    /**
     * @return SMTP server operated by this GreenMail instance or null if there is none
     */
    SmtpServer getSmtps();

    /**
     * @return SMTP server operated by this GreenMail instance or null if there is none
     */
    ImapServer getImaps();

    /**
     * @return SMTP server operated by this GreenMail instance or null if there is none
     */
    Pop3Server getPop3s();

    /**
     * @return Greenmail protocol managers
     */
    Managers getManagers();

    /**
     * Use this method if you are sending email in a different thread from the one you're testing from.
     * Block waits for an email to arrive in any mailbox for any user.
     * Implementation Detail: No polling wait implementation
     *
     * @param timeout    maximum time in ms to wait for emailCount of messages to arrive before giving up and returning false
     * @param emailCount waits for these many emails to arrive before returning
     * @return Returns false if timeout period was reached, otherwise true.
     */
    boolean waitForIncomingEmail(long timeout, int emailCount);

    /**
     * Does the same thing as {@link #waitForIncomingEmail(long, int)} but with a default timeout of 5000ms
     *
     * @param emailCount waits for these many emails to arrive before returning
     * @return Returns false if timeout period was reached, otherwise true.
     */
    boolean waitForIncomingEmail(int emailCount);

    /**
     * @return Returns all messags in all folders for all users
     * {@link com.icegreen.greenmail.util.GreenMailUtil} has a bunch of static helper methods to extract body text etc.
     */
    MimeMessage[] getReceivedMessages();

    /**
     * Gets all messages containing given domain.
     *
     * @param domain the domain, such as 'icegreen.com' or 'some.example.com'
     * @return Returns all received messages for given domain.
     */
    MimeMessage[] getReceivedMessagesForDomain(String domain);

    /**
     * Sets the password for the account linked to email. If no account exits, one is automatically created when an email is received
     * The automatically created account has the account login and password equal to the email address.
     *
     * @param login    Login for which the password should be set. This is assumed to be the same as the email address.
     * @param password New password
     * @return the user created
     */
    GreenMailUser setUser(String login, String password);

    /**
     * Sets the password for the account linked to email. If no account exits, one is automatically created when an email is received
     * The automatically created account has the account login and password equal to the email address.
     *
     * @param email    Email address for which the password should be set
     * @param login    Login name for login. This may be different to the email address. E.g. the email address could be
     *                 "info@localhost", the login could be "info".
     * @param password New password
     * @return the user created
     */
    GreenMailUser setUser(String email, String login, String password);

    /**
     * Sets up accounts with password based on a properties map where the key is the email/login and the value the password
     *
     * @param users User/password map
     */
    void setUsers(Properties users);

    /**
     * Toggles the IMAP quota support. Quotas are enabled by default.
     *
     * @param isEnabled true, if quotas should be supported.
     */
    void setQuotaSupported(boolean isEnabled);

    /**
     * Configure GreenMail instance using the given configuration
     *
     * @param config Configuration to use
     * @return self (for method chaining)
     */
    GreenMailOperations withConfiguration(GreenMailConfiguration config);

    /**
     * Start the GreenMail server
     */
    void start();

    /**
     * Stop the GreenMail server. Clear all data (send messages, users, ...).
     */
    void stop();

    /**
     * Restart the GreenMail server. Clear all data (send messages, users, ...)
     */
    void reset();

    /**
     * Remove/purge all data from all mail stores (POP3/IMAP)
     *
     * @throws FolderException on error
    */
    void purgeEmailFromAllMailboxes() throws FolderException;
}
