package com.icegreen.greenmail.standalone;

import com.icegreen.greenmail.configuration.PropertiesBasedGreenMailConfigurationBuilder;
import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.PropertiesBasedServerSetupBuilder;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Enables GreenMail to run in standalone mode.
 * <p>
 * Example: java  -Dgreenmail.smtp -Dgreenmail.users=test1:pwd1 -jar greenmail.jar
 *
 * @see PropertiesBasedServerSetupBuilder
 * @see PropertiesBasedGreenMailConfigurationBuilder
 */
public class GreenMailStandaloneRunner {
    private final Logger log = LoggerFactory.getLogger(GreenMailStandaloneRunner.class);
    private GreenMail greenMail;

    /**
     * Start and configure GreenMail using given properties.
     *
     * @param properties the properties such as System.getProperties()
     */
    public void doRun(Properties properties) {
        ServerSetup[] serverSetup = new PropertiesBasedServerSetupBuilder().build(properties);

        if (serverSetup.length == 0) {
            printUsage(System.out);

        } else {
            greenMail = new GreenMail(serverSetup);
            log.info("Starting GreenMail standalone v{} using {}",
                    BuildInfo.INSTANCE.getProjectVersion(), Arrays.toString(serverSetup));
            greenMail.withConfiguration(new PropertiesBasedGreenMailConfigurationBuilder().build(properties))
                    .start();
        }
    }

    protected static void configureLogging(Properties properties) {
        // Init logging: Try standard log4j configuration mechanism before falling back to
        // provided logging configuration
        String log4jConfig = System.getProperty("log4j.configuration");
        if (null == log4jConfig) {
            if (properties.containsKey(PropertiesBasedServerSetupBuilder.GREENMAIL_VERBOSE)) {
                DOMConfigurator.configure(GreenMailStandaloneRunner.class.getResource("/log4j-verbose.xml"));
            } else {
                DOMConfigurator.configure(GreenMailStandaloneRunner.class.getResource("/log4j.xml"));
            }
        } else {
            if (log4jConfig.toLowerCase().endsWith(".xml")) {
                DOMConfigurator.configure(log4jConfig);
            } else {
                PropertyConfigurator.configure(log4jConfig);
            }
        }
    }

    private void printUsage(PrintStream out) {
        // Don't use logger
        out.println("Usage: java OPTIONS -jar greenmail.jar");
        out.println("\nOPTIONS:");
        String[][] options = {
                {"-Dgreenmail.setup.<protocol|all>", "Specifies mail server to start using default port and bind " +
                        "address 127.0.0.1"},
                {"Note: Protocol can be one of smtp,smtps,imap,imaps,pop3 or pop3s"},
                {"-Dgreenmail.setup.test.<protocol|all>", "Specifies mail server to start using default port plus " +
                        "offset 3000 and bind address 127.0.0.1"},
                {"-Dgreenmail.<protocol|all>.hostname=...",
                        "Specifies bind address. Requires additional port parameter."},
                {"-Dgreenmail.<protocol|all>.port=...", "Specifies port. Requires additional hostname parameter."},
                {"-Dgreenmail.users=<logon:pwd@domain>[,...]", "Specifies mail users, eg foo:pwd@bar.com,foo2:pwd@bar2.com."},
                {"Note: domain must be DNS resolvable!"},
                {"-Dgreenmail.auth.disabled ", "Disables authentication check so that any password works."},
                {"Also automatically provisions previously non-existent users."},
                {"-Dgreenmail.verbose ", "Enables verbose mode, including JavaMail debug output"},
                {"-Dgreenmail.startup.timeout=<TIMEOUT_IN_MILLISECS>",
                        "Overrides the default server startup timeout of 1000ms."},
        };
        for (String[] opt : options) {
            if (opt.length == 1) {
                out.println(String.format("%1$44s %2$s", " ", opt[0]));
            } else {
                out.println(String.format("%1$-42s : %2$s", (Object[]) opt)); // NOSONAR
            }
        }
        out.println();
        out.println("Example: ");
        out.println(" java -Dgreenmail.setup.test.all -Dgreenmail.users=foo:pwd@bar.com -jar greenmail.jar");
        out.println("       Starts SMTP,SMTPS,IMAP,IMAPS,POP3,POP3S" +
                "with default ports plus offset 3000 on 127.0.0.1 and user foo@bar.com.");
        out.println("       Note: bar.com domain for user must be DNS resolvable!");
        out.println(" java -Dgreenmail.setup.test.smtp -Dgreenmail.setup.test.imap -Dgreenmail.auth.disabled -jar greenmail.jar");
        out.println("       Starts SMTP on 127.0.01:3025 and IMAP on 127.0.0.1:3143, disabling user authentication");
        out.println(" java -Dgreenmail.smtp.hostname=0.0.0.0 -Dgreenmail.smtp.port=3025 " +
                "-Dgreenmail.imap.hostname=0.0.0.0 -Dgreenmail.imap.port=3143 -jar greenmail.jar");
        out.println("       Starts SMTP on 0.0.0.0:3025 and IMAP on 0.0.0.0:3143");
    }

    GreenMail getGreenMail() {
        return greenMail;
    }

    public static void main(String[] args) {
        final Properties properties = System.getProperties();
        configureLogging(properties);
        new GreenMailStandaloneRunner().doRun(properties);
    }
}
