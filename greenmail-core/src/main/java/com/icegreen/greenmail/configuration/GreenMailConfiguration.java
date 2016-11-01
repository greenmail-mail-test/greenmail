package com.icegreen.greenmail.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Properties that can be defined to configure a GreenMail instance or GreenMailRule.
 */
public class GreenMailConfiguration {
    private final List<UserBean> usersToCreate = new ArrayList<>();
    private boolean disableAuthenticationCheck = false;
    private UserBean mailsinkUser = null;
    private boolean mailsinkKeepInOriginalMailboxes = true;

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
     * @return
     */
    public GreenMailConfiguration withMailsinkKeepInOriginalMailboxes(boolean booleanProperty) {
        this.mailsinkKeepInOriginalMailboxes = booleanProperty;
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

        b.append("}");
        return b.toString();
    }

}
