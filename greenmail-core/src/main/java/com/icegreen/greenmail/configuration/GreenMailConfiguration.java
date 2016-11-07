package com.icegreen.greenmail.configuration;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties that can be defined to configure a GreenMail instance or GreenMailRule.
 */
public class GreenMailConfiguration {
    final Logger log = LoggerFactory.getLogger(GreenMailConfiguration.class);

    private final List<UserBean> usersToCreate = new ArrayList<>();
    private boolean disableAuthenticationCheck = false;
    private UserBean mailsinkUser = null;
    private boolean mailsinkKeepInOriginalMailboxes = true;
    private String storeClassImplementation = "com.icegreen.greenmail.store.InMemoryStore";
    private String fileStoreRootDirectory = "greenmailstore";

    public void logConfiguration() {
        log.info("  Startup property greenmail.auth.disabled                       : " + disableAuthenticationCheck);
        log.info("  Startup property greenmail.users                               : #" + usersToCreate.size());
        for (UserBean u : this.usersToCreate) {
            log.info("    User                                                         : #" + u.toString());
        }
        log.info("  Startup property greenmail.mailsink.user                       : " + mailsinkUser);
        log.info("  Startup property greenmail.mailsink.keep.in.original.mailboxes : " + mailsinkKeepInOriginalMailboxes);
        log.info("  Startup property greenmail.mailstore.impl.class                : " + storeClassImplementation);
        log.info("  Startup property greenmail.filestore.rootdir                   : " + Paths.get(fileStoreRootDirectory)
                .toAbsolutePath().toString());
    }

    /**
     * The given {@link com.icegreen.greenmail.user.GreenMailUser} will be created when servers will start.
     *
     * @param login User id and email address
     * @param password Password of user that belongs to login name
     * @return Modified configuration
     */
    public GreenMailConfiguration withUser(final String login, final String password) {
        return withUser(login, login, password);
    }

    /**
     * The given {@link com.icegreen.greenmail.user.GreenMailUser} will be created when servers will start.
     *
     * @param email Email address
     * @param login Login name of user
     * @param password Password of user that belongs to login name
     * @return Modified configuration
     */
    public GreenMailConfiguration withUser(final String email, final String login, final String password) {
        this.usersToCreate.add(new UserBean(email, login, password));
        return this;
    }

    /**
     * @return New GreenMail configuration
     */
    public static GreenMailConfiguration aConfig() {
        return new GreenMailConfiguration();
    }

    /**
     * @return Users that should be created on server startup
     */
    public List<UserBean> getUsersToCreate() {
        return usersToCreate;
    }

    /**
     * @return true, if authentication is disabled.
     *
     * @see GreenMailConfiguration#withDisabledAuthentication()
     */
    public boolean isAuthenticationDisabled() {
        return disableAuthenticationCheck;
    }

    /**
     * @return Returns the currently defined mailsink user. Can be null.
     */
    public UserBean getMailsinkUser() {
        return this.mailsinkUser;
    }

    /**
     * @return true when the mailsink user exists.
     */
    public boolean hasMailsinkUser() {
        return this.mailsinkUser != null;
    }

    /**
     * @return whether to keep the mails in their original mailboxes (true) or not (false).
     */
    public boolean keepMailsinkInOriginalMailboxes() {
        return this.mailsinkKeepInOriginalMailboxes;
    }

    /**
     * @return the name of the implementation class which implements the Store interface.
     */
    public String getStoreClassImplementation() {
        return this.storeClassImplementation;
    }

    /**
     * @return the root directory of the store when the store is of type MBoxFileStore. This is where all mailboxes and
     * messages are persisted.
     */
    public String getFileStoreRootDirectory() {
        return this.fileStoreRootDirectory;
    }

    /**
     * Disables authentication.
     *
     * Useful if you want to avoid setting up users up front.
     *
     * @return Modified configuration.
     */
    public GreenMailConfiguration withDisabledAuthentication() {
        disableAuthenticationCheck = true;
        return this;
    }

    /**
     * Sets the mailsink user who will receive a copy of all incoming emails.
     *
     * @param login Login name of user
     * @param password Password of user that belongs to login name
     */
    public GreenMailConfiguration withMailsinkUser(final String login, final String password) {
        return withMailsinkUser(login, login, password);
    }

    /**
     * Sets the mailsink user who will receive a copy of all incoming emails.
     *
     * @param email Email address
     * @param login Login name of user
     * @param password Password of user that belongs to login name
     */
    public GreenMailConfiguration withMailsinkUser(final String email, final String login, final String password) {
        this.mailsinkUser = new UserBean(email, login, password);
        return this;
    }

    /**
     * Sets the boolean property whether to only copy all the mails to the mailsink user (true) or whether to
     * move the mails to the mailsink user (original users don't receive the mails at all, only the mailsink
     * user receives the mails).
     *
     * This property is only evaluated when a valid mailsink user is defined.
     *
     * @param booleanProperty - true for keeping the mails in the original mailboxes
     */
    public GreenMailConfiguration withMailsinkKeepInOriginalMailboxes(boolean booleanProperty) {
        this.mailsinkKeepInOriginalMailboxes = booleanProperty;
        return this;
    }

    /**
     * Sets the implementation class for the Store interface. Possible values:
     *   * com.icegreen.greenmail.store.InMemoryStore
     *   * com.icegreen.greenmail.filestore.MBoxFileStore
     *
     * @param implClassname - the name of the class
     */
    public GreenMailConfiguration withStoreClassImplementation(String implClassname) {
        this.storeClassImplementation = implClassname;
        return this;
    }

    /**
     * When the store implementation is of type MBoxFileStore, this is the root directory where mailboxes and messages are
     * persited to.
     *
     * @param rootDir - the root directory, will be created it if does not exist.
     */
    public GreenMailConfiguration withFileStoreRootDirectory(String rootDir) {
        this.fileStoreRootDirectory = rootDir;
        return this;
    }



    public String toString() {
        StringBuilder b = new StringBuilder();

        b.append("GreenMailConfiguration{usersToCreate=(");
        for (UserBean u : this.usersToCreate) {
            b.append(u);
            b.append("),");
        }
        b.append(";");

        b.append("disableAuthenticationCheck=");
        b.append(this.disableAuthenticationCheck);
        b.append(";");

        b.append("mailsinkUser=");
        b.append(this.mailsinkUser);
        b.append(";");

        b.append("mailsinkKeepInOriginalMailboxes=");
        b.append(this.mailsinkKeepInOriginalMailboxes);
        b.append(";");

        b.append("storeClassImplementation=");
        b.append(this.storeClassImplementation);
        b.append(";");

        b.append("fileStoreRootDirectory=");
        b.append(this.fileStoreRootDirectory);

        b.append("}");
        return b.toString();
    }

}
