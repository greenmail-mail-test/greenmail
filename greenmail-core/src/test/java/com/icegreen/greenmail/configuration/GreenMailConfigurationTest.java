package com.icegreen.greenmail.configuration;

import static com.icegreen.greenmail.configuration.GreenMailConfigurationTestBase.testUsersAccessibleConfig;

import java.nio.file.Paths;

import com.icegreen.greenmail.internal.TestHelper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

/**
 * Tests if the configuration is applied correctly to GreenMail instances.
 */
public class GreenMailConfigurationTest {


    @Test
    public void testUsersAccessibleWithMemoryStore() {
        GreenMail greenMail = new GreenMail(ServerSetupTest.IMAP).withConfiguration(testUsersAccessibleConfig());
        greenMail.start();
        try {
            GreenMailConfigurationTestBase.testUsersAccessible(greenMail);
        } finally {
            greenMail.stop();
        }
    }

    @Test
    public void testUsersAccessibleWithFileStore() {
        GreenMail greenMail = new GreenMail(ServerSetupTest.IMAP).withConfiguration(testUsersAccessibleConfig());
        greenMail.getStartupConfig().withStoreClassImplementation("com.icegreen.greenmail.filestore.MBoxFileStore");

        String toDeleteDir = TestHelper.getRandomDirectory();
        greenMail.getStartupConfig().withFileStoreRootDirectory(toDeleteDir);
        greenMail.start();
        try {
            GreenMailConfigurationTestBase.testUsersAccessible(greenMail);
        } finally {
            greenMail.stop();
            if (toDeleteDir != null) {
                TestHelper.deleteDirectoryWithContentAndIgnoreExceptions(Paths.get(toDeleteDir));
            }
        }
    }

}