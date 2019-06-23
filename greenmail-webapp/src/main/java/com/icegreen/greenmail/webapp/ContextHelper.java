package com.icegreen.greenmail.webapp;

import javax.servlet.ServletContext;

import com.icegreen.greenmail.Managers;

/**
 * Helps accessing servlet context attributes.
 */
public class ContextHelper {
    static final String ATTRIBUTE_NAME_MANAGERS = "greenmail_managers";
    static final String ATTRIBUTE_NAME_CONFIGURATION = "greenmail_configuration";

    private ContextHelper() {
        // Nothing
    }

    public static void initAttributes(ServletContext ctx, Managers managers, Configuration configuration) {
        ctx.setAttribute(ATTRIBUTE_NAME_MANAGERS, managers);
        ctx.setAttribute(ATTRIBUTE_NAME_CONFIGURATION, configuration);
    }

    public static Managers getManagers(ServletContext ctx) {
        return (Managers) ctx.getAttribute(ATTRIBUTE_NAME_MANAGERS);
    }

    public static Configuration getConfiguration(ServletContext ctx) {
        return (Configuration) ctx.getAttribute(ATTRIBUTE_NAME_CONFIGURATION);
    }
}
