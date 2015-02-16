package com.icegreen.greenmail.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailProxy;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

public class GreenMailRule extends GreenMailProxy implements MethodRule, TestRule {
    private GreenMail greenMail;
    private final ServerSetup[] serverSetups;

    private final List<UserBean> usersToCreate = new ArrayList<UserBean>();

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
                greenMail.start();
                for (final UserBean user : usersToCreate) {
                    if (user.getEmail() != null) {
                        greenMail.setUser(user.getEmail(), user.getLogin(), user.getPassword());
                    } else {
                        greenMail.setUser(user.getLogin(), user.getPassword());
                    }
                }
                try {
                    base.evaluate();
                } finally {
                    greenMail.stop();
                }
            }

        };
    }

    @Override
    protected GreenMail getGreenMail() {
        return greenMail;
    }

    /**
     * A user to create when servers start
     * 
     * @author Christophe DENEUX - Linagora
     *
     */
    private class UserBean {
        final String email;

        final String login;

        final String password;

        public UserBean(final String email, final String login, final String password) {
            this.email = email;
            this.login = login;
            this.password = password;
        }

        public String getEmail() {
            return this.email;
        }

        public String getLogin() {
            return this.login;
        }

        public String getPassword() {
            return this.password;
        }
    }

    /**
     * The given {@link GreenMailUser} will be created when servers will start
     */
    public GreenMailRule addUser(final String login, final String password) {
        this.usersToCreate.add(new UserBean(null, login, password));
        return this;
    }

    /**
     * The given {@link GreenMailUser} will be created when servers will start
     */
    public GreenMailRule addUser(final String email, final String login, final String password) {
        this.usersToCreate.add(new UserBean(email, login, password));
        return this;
    }
}

