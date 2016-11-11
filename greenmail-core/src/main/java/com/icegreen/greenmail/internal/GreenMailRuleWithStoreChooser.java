package com.icegreen.greenmail.internal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;

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

/**
 * Internal GreenMailRule which accepts an annotation on the test method StoreChooser. When you have a test which you
 * want to run against the InMemoryStorage and the filebased storage, you can add the annotation and then the test
 * will be run twice, once with the memory storage and once with the file-storage.
 *
 * Example in your test file:
 *   @Test
 *   @StoreChoice(store="file,memory")
 *   public void yourTestMethod(....) {}
 *
 * Created by saladin on 11/4/16.
 */
public class GreenMailRuleWithStoreChooser extends GreenMailProxy implements MethodRule, TestRule {
    private GreenMail greenMail;
    private final ServerSetup[] serverSetups;
    private GreenMailConfiguration ruleStartupConfig;
    private String appliesToStore = null;

    /**
     * Initialize with multiple server setups
     *
     * @param serverSetups All setups to use
     */
    public GreenMailRuleWithStoreChooser(ServerSetup[] serverSetups, GreenMailConfiguration config) {
        this.ruleStartupConfig = config;
        this.serverSetups = serverSetups;
    }


    /**
     * Initialize with multiple server setups
     *
     * @param serverSetups All setups to use
     */
    public GreenMailRuleWithStoreChooser(ServerSetup[] serverSetups) {
        this.ruleStartupConfig = null;
        this.serverSetups = serverSetups;
    }

    /**
     * Initialize with single server setups
     *
     * @param serverSetup Setup to use
     */
    public GreenMailRuleWithStoreChooser(ServerSetup serverSetup) {
        this.ruleStartupConfig = null;
        this.serverSetups = new ServerSetup[]{serverSetup};
    }

    /**
     * Initialize with single server setups
     *
     * @param serverSetup Setup to use
     */
    public GreenMailRuleWithStoreChooser(ServerSetup serverSetup, GreenMailConfiguration config) {
        this.ruleStartupConfig = config;
        this.serverSetups = new ServerSetup[]{serverSetup};
    }

    public GreenMailRuleWithStoreChooser() {
        this(ServerSetupTest.ALL);
    }

    public GreenMailRuleWithStoreChooser(GreenMailConfiguration config) {
        this(ServerSetupTest.ALL, config);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        if (description != null) {
            StoreChooser a = description.getAnnotation(StoreChooser.class);
            if (a  != null) {
                appliesToStore = a.store();
            }
        }
        return apply(base, null, null);
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
        if (method != null) {
            StoreChooser a = method.getAnnotation(StoreChooser.class);
            appliesToStore = a.store();
        }
        return new MyStatement(base);
    }

    private class MyStatement extends  Statement {
        private final Statement base;

        private MyStatement(final Statement theBase) {
            this.base = theBase;
        }

        @Override
        public void evaluate() throws Throwable {
            boolean useMemoryStore = false;
            boolean useFileStore = false;
            if (appliesToStore != null) {
                StringTokenizer t = new StringTokenizer(appliesToStore, ",");
                while (t.hasMoreTokens()) {
                    String tokenValue = t.nextToken().trim();
                    if ("memory".equals(tokenValue)) {
                        useMemoryStore = true;
                    } else if ("file".equals(tokenValue)) {
                        useFileStore = true;
                    }
                }
            }

            String deleteAtEnd = null;
            try {
                if ((!useMemoryStore) && (!useFileStore)) {
                    // No annotation, so just invoke the unit-test as it is:
                    this.invokeIt();
                } else {
                    // Otherwise, we might invoke it twice.
                    if (useMemoryStore) {
                        if (ruleStartupConfig == null) {
                            ruleStartupConfig = new GreenMailConfiguration();
                        }
                        ruleStartupConfig.withStoreClassImplementation("com.icegreen.greenmail.store.InMemoryStore");
                        this.invokeIt();
                    }
                    if (useFileStore) {
                        if (ruleStartupConfig == null) {
                            ruleStartupConfig = new GreenMailConfiguration();
                        }
                        // Make sure to use the Filestore:
                        deleteAtEnd = TestHelper.getRandomDirectory();
                        ruleStartupConfig.withStoreClassImplementation("com.icegreen.greenmail.filestore.MBoxFileStore");
                        ruleStartupConfig.withFileStoreRootDirectory(deleteAtEnd);

                        this.invokeIt();
                    }
                }
            } finally {
                // Greenmail is stopped now... Let's check whether we created a temporary directory for
                // with the file-store. If yes, delete it.
                if (deleteAtEnd != null) {
                    Path directory = Paths.get(deleteAtEnd);
                    TestHelper.deleteDirectoryWithContentAndIgnoreExceptions(directory);
                }
            }
        }

        private void invokeIt() throws Throwable {
            greenMail = new GreenMail(serverSetups);
            if (ruleStartupConfig != null) {
                greenMail.withConfiguration(ruleStartupConfig);
            }
            try {
                start();
                base.evaluate();
            } finally {
                stop();
            }
        }

    }

    @Override
    protected GreenMail getGreenMail() {
        return greenMail;
    }
}
