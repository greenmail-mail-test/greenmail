package com.icegreen.greenmail.junit;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailProxy;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class GreenMailRule extends GreenMailProxy implements MethodRule, TestRule {
    private GreenMail greenMail;
    private final ServerSetup[] serverSetups;

    /**
     * Initialize with multiple server setups
     *
     * @param serverSetups All setups to use
     */
    public GreenMailRule(ServerSetup[] serverSetups) {
        this.serverSetups = serverSetups;
    }

    /**
     * Initialize with single server setups
     *
     * @param serverSetup Setup to use
     */
    public GreenMailRule(ServerSetup serverSetup) {
        this.serverSetups = new ServerSetup[]{serverSetup};
    }

    public GreenMailRule() {
        this(ServerSetupTest.ALL);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return apply(base, null, null);
    }

    @Override
    public Statement apply(final Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                greenMail = new GreenMail(serverSetups);
                try {
                    start();
                    base.evaluate();
                } finally {
                    stop();
                }
            }

        };
    }

    @Override
    public GreenMailRule withConfiguration(GreenMailConfiguration config) {
        // Just overriding to return more specific type
        super.withConfiguration(config);
        return this;
    }

    @Override
    protected GreenMail getGreenMail() {
        return greenMail;
    }
}

