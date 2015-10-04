package com.icegreen.greenmail.standalone;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.PropertyServerSetupBuilder;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Enables GreenMail to run in standalone mode.
 * <p/>
 * Example: java -jar greenmail.jar -Dgreenmail.smtp
 *
 * @see com.icegreen.greenmail.util.PropertyServerSetupBuilder
 */
public class GreenMailStandaloneRunner {
    private static final Logger log = LoggerFactory.getLogger(GreenMailStandaloneRunner.class);

    public static void main(String[] args) {
        String log4jConfig = System.getProperty("log4j.configuration");
        if (null == log4jConfig) {
            PropertyConfigurator.configure(GreenMailStandaloneRunner.class.getResource("/log4j.xml"));
        }

        ServerSetup[] serverSetup = new PropertyServerSetupBuilder().create(System.getProperties());

        if (serverSetup.length == 0) {
            // Don't use logger
            System.out.println("Usage: java OPTIONS -jar greenmail.jar");
            System.out.println("OPTIONS: [-Dgreenmail.setup.all | -Dgreenmail.setup.test.all | " +
                    "[-Dgreenmail.setup[.test].[smtp[s]|imap[s]|pop3[s]]] " +
                    "[-Dgreenmail.[smtp[s]|imap[s]|pop3[s]].hostname -Dgreenmail.[smtp[s]|imap[s]|pop3[s]].port");
            System.out.println();
            System.out.println("Example: ");
            System.out.println(" java -Dgreenmail.setup.test.all -jar greenmail.jar");
            System.out.println(" java -Dgreenmail.smtp.hostname=0.0.0.0 -Dgreenmail.smtp.port=3025 " +
                    "-Dgreenmail.imap.hostname=0.0.0.0 -Dgreenmail.imap.port=3143 -jar greenmail.jar");
        } else {
            GreenMail greenMail = new GreenMail(serverSetup);
            log.info("Starting GreenMail standalone using " + Arrays.toString(serverSetup));
            greenMail.start();
        }
    }

}
