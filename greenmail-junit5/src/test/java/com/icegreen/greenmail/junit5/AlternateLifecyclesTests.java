package com.icegreen.greenmail.junit5;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMailUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("GreenMail with alternate lifecycles tests")
class AlternateLifecyclesTests {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Per-Method lifecycle")
    class PerMethodLifecycle {
        @RegisterExtension
        GreenMailExtension greenMail = new GreenMailExtension()
            .withConfiguration(GreenMailConfiguration.aConfig()
                .withUser("to@localhost.com", "login-id", "password"));

        @Test
        @DisplayName("Send test 1")
        void testSend1() {
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "some subject", "some body");
            assertEquals(1, greenMail.getReceivedMessages().length);
        }

        @Test
        @DisplayName("Send test 2")
        void testSend2() {
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "some subject", "some body");
            assertEquals(1, greenMail.getReceivedMessages().length);
        }

        @AfterAll
        public void afterAll() {
            assertEquals(0, greenMail.getReceivedMessages().length);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Per-Class lifecycle")
    class PerClassLifecycle {
        @RegisterExtension
        GreenMailExtension greenMail = new GreenMailExtension()
            .withPerMethodLifecycle(false)
            .withConfiguration(GreenMailConfiguration.aConfig()
                .withUser("to@localhost.com", "login-id", "password"));

        @Test
        @DisplayName("Send test 1")
        void testSend1() {
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "some subject", "some body");
        }

        @Test
        @DisplayName("Send test 2")
        void testSend2() {
            GreenMailUtil.sendTextEmailTest("to@localhost.com", "from@localhost.com", "some subject", "some body");
        }

        @AfterAll
        public void afterAll() {
            assertEquals(2, greenMail.getReceivedMessages().length);
        }
    }
}
