package com.icegreen.greenmail.base;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserManager;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
     * @return the user manager for
     */
    UserManager getUserManager();

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
     * @return Returns all messages in all folders for all users
     * {@link com.icegreen.greenmail.util.GreenMailUtil} has a bunch of static helper methods to extract body text etc.
     */
    MimeMessage[] getReceivedMessages();

    /**
     * Gets all messages containing given domain.
     * <p>
     * Note:
     * This operates on the raw messages ignoring the post-box user.
     * A CC-ed email would therefore show up multiple times for each receiving user.
     *
     * @param domain the domain, such as 'icegreen.com' or 'some.example.com'
     * @return Returns all received messages for given domain.
     */
    MimeMessage[] getReceivedMessagesForDomain(String domain);

    /**
     * Finds all messages matching the user account and message predicate.
     *
     * @param userPredicate  matches user accounts
     * @param messagePredicate  matches message
     * @return a stream of matching messages, matching both predicates (logical AND)
     * @since 2.1.0
     */
    Stream<MimeMessage> findReceivedMessages(Predicate<GreenMailUser> userPredicate,
                                             Predicate<MimeMessage> messagePredicate);

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
     * Checks if GreenMail is up and running.
     *
     * @return true if ready to serve.
     */
    boolean isRunning();

    /**
     * Remove/purge all data from all mail stores (POP3/IMAP)
     *
     * @throws FolderException on error
     */
    void purgeEmailFromAllMailboxes() throws FolderException;

    /**
     * Loads emails from given path.
     *
     * <ul>
     *   <li>
     *     Expected structure in provided path, containing EML (rfc0822) mail files
     *     <p>Pattern: <pre>&lt;EMAIL&gt; / &lt;FOLDER*&gt; / &lt;*.eml&gt;</pre>
     *     <p>Example:
     *     <pre>
     *    ├── bar@localhost (directory)
     *    │   └── INBOX (directory)
     *    │       └── test-5.eml (file)
     *    └── foo@localhost (directory)
     *        └── Drafts (directory)
     *            └── draft.eml (file)
     *     </pre>
     *   </li>
     *   <li>Creates user of given email if missing (by convention, with email as login and password)</li>
     *   <li>Creates intermediate mail folder if missing</li>
     * </ul>
     *
     * @param path base path with email structure
     * @throws IOException on IO error
     * @throws FolderException if e.g. fails to create intermediate folder
     * @since 2.1-alpha-3 / 2.0.1 / 1.6.15
     */
    GreenMailOperations loadEmails(Path path) throws IOException, FolderException;
}
