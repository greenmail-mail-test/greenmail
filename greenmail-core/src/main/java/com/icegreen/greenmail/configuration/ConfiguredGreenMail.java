package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.base.GreenMailOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A version of GreenMailOperations that implements the configure() method.
 */
public abstract class ConfiguredGreenMail implements GreenMailOperations {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private GreenMailConfiguration config;

    protected final Managers createManagers() {
        if (config == null)
            config = GreenMailConfiguration.aConfig();
        Class<? extends Managers> configuredManagersClass = config.getManagersClass();
        try {
            return configuredManagersClass.newInstance();
        } catch (InstantiationException e) {
            log.warn(String.format("Could not instantiate Managers class '%s'. Will run with default class '%s'.",
                    configuredManagersClass, Managers.class), e);
        } catch (IllegalAccessException e) {
            log.warn(String.format("Illegal access while instantiating Managers class '%s'. Will run with default " +
                    "class '%s'.", configuredManagersClass, Managers.class), e);
        } catch (Exception e) {
            log.warn(String.format("General exception while instantiating Managers class '%s'. Will run with default " +
                    "class '%s'.", configuredManagersClass, Managers.class), e);
        }
        return new Managers();
    }

    @Override
    public ConfiguredGreenMail withConfiguration(GreenMailConfiguration config) {
        this.config = config;
        return this;
    }

    /**
     * This method can be used by child classes to apply the configuration that is stored in config.
     */
    protected void doConfigure() {
        if (config != null) {
            for (UserBean user : config.getUsersToCreate()) {
                setUser(user.getEmail(), user.getLogin(), user.getPassword());
            }
        }
    }
}
