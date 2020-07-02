package com.icegreen.greenmail.junit5;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailProxy;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * To use this extension you need to use {@code @RegisterExtension} Junit5 mechanism and made variable {@code static}.<br>
 * But if you want to remove {@code static} keyword annotate test class with {@code @TestInstance(TestInstance.Lifecycle.PER_CLASS)}.
 *
 * @see RegisterExtension
 * @see TestInstance
 */
public class GreenMailExtension extends GreenMailProxy implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    private final ServerSetup[] serverSetups;
    private GreenMail greenMail;
    private boolean perMethod = true;

    /**
     * Initialize with single server setups.
     *
     * @param serverSetup Setup to use
     */
    public GreenMailExtension(final ServerSetup serverSetup) {
        this.serverSetups = new ServerSetup[]{serverSetup};
    }

    /**
     * Initialize with all server setups.
     */
    public GreenMailExtension() {
        this(ServerSetupTest.ALL);
    }

    /**
     * Initialize with multiple server setups.
     *
     * @param serverSetups All setups to use
     */
    public GreenMailExtension(final ServerSetup[] serverSetups) {
        this.serverSetups = serverSetups;
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        if (perMethod) {
            greenMail = new GreenMail(serverSetups);
            start();
        }
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        if (perMethod) {
            stop();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!perMethod) {
            greenMail = new GreenMail(serverSetups);
            start();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (!perMethod) {
            stop();
        }
    }

    /**
     * Specify whether GreenMail should be set up and torn down before and after
     * each method or before and after all methods.
     * 
     * @param perMethod
     *            If <code>true</code>, per-method lifecycle is used, per-class
     *            otherwise.
     * @return The GreenMail extension for chaining calls.
     */
    public GreenMailExtension withPerMethodLifecycle(boolean perMethod) {
        this.perMethod = perMethod;
        return this;
    }

    @Override
    protected GreenMail getGreenMail() {
        return greenMail;
    }

    @Override
    public GreenMailExtension withConfiguration(final GreenMailConfiguration config) {
        super.withConfiguration(config);
        return this;
    }
}
