package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.base.GreenMailOperations;

/**
 * A version of GreenMailOperations that implements the configure() method.
 */
public abstract class ConfiguredGreenMail implements GreenMailOperations {
    private GreenMailConfiguration startupConfig;

    @Override
    public ConfiguredGreenMail withConfiguration(GreenMailConfiguration config) {
        this.startupConfig = config;
        return this;
    }

    /**
     * This method can be used by child classes to apply the configuration that is stored in startupConfig.
     */
    protected void doConfigure() {
        if (startupConfig != null) {
            for (UserBean user : startupConfig.getUsersToCreate()) {
                setUser(user.getEmail(), user.getLogin(), user.getPassword());
            }
            getManagers().getUserManager().setAuthRequired(!startupConfig.isAuthenticationDisabled());
        }
    }

    /**
     * @return the startup Configuration
     */
    protected GreenMailConfiguration getStartupConfig() {
        if (this.startupConfig == null) {
            // Provide default values
            this.startupConfig = new GreenMailConfiguration();
        }

        return this.startupConfig;
    }
}
