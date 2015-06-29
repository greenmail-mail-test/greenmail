package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.Managers;

import java.util.ArrayList;
import java.util.List;

/**
 * Properties that can be defined to configure a GreenMail instance or GreenMailRule.
 */
public class GreenMailConfiguration {
    private final List<UserBean> usersToCreate = new ArrayList<UserBean>();
    private Class<? extends Managers> managersClass = Managers.class;

    /**
     * @return New GreenMail configuration
     */
    public static GreenMailConfiguration aConfig() {
        return new GreenMailConfiguration();
    }

    /**
     * The given {@link com.icegreen.greenmail.user.GreenMailUser} will be created when servers will start
     *
     * @param login User id and email addres
     * @param password Password of user that belongs to login name
     * @return Modified configuration
     */
    public GreenMailConfiguration withUser(final String login, final String password) {
        this.usersToCreate.add(new UserBean(login, login, password));
        return this;
    }

    /**
     * The given {@link com.icegreen.greenmail.user.GreenMailUser} will be created when servers will start
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

    public GreenMailConfiguration withManagersClass(final Class<? extends Managers> managersClass) {
        if (managersClass == null) {
            throw new NullPointerException("managersClass may not be null.");
        }
        this.managersClass = managersClass;
        return this;
    }

    /**
     * @return Users that should be created on server startup
     */
    public List<UserBean> getUsersToCreate() {
        return usersToCreate;
    }

    /**
     * @return The {@link Managers} class to should be used
     */
    public Class<? extends Managers> getManagersClass() {
        return managersClass;
    }
}
