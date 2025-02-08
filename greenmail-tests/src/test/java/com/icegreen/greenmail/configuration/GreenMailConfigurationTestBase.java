package com.icegreen.greenmail.configuration;

import com.icegreen.greenmail.base.GreenMailOperations;
import com.icegreen.greenmail.util.Retriever;

import jakarta.mail.Message;

import static com.icegreen.greenmail.configuration.GreenMailConfiguration.aConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test methods that are used in both GreenMailConfiguration tests
 */
public final class GreenMailConfigurationTestBase {
    /**
     * Util
     */
    private GreenMailConfigurationTestBase() {
    }

    /**
     * Check if both user accounts can be accessed
     */
    public static void testUsersAccessible(GreenMailOperations greenMail) {
        // Checks if the user that is registered in the config is actually accessible
        try (Retriever retriever = new Retriever(greenMail.getImap())) {
            final Message[] messages = retriever.getMessages("user@localhost", "password");
            // if getMessage is successful this means that the user account has been created
            assertThat(messages).isEmpty();

            // Now check second user. this one has a different user id
            final Message[] messages2 = retriever.getMessages("secondUserLogin", "password2");
            assertThat(messages2).isEmpty();
        }
    }

    /**
     * @return Configuration that has to be used for testUsersAccessible
     */
    public static GreenMailConfiguration testUsersAccessibleConfig() {
        return aConfig()
                .withUser("user@localhost", "password")
                .withUser("secondUser@localhost", "secondUserLogin", "password2");
    }
}
