package com.icegreen.greenmail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentCloseIT {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private int limitIterations;

    @BeforeEach
    void setUp() {
        final String envValue = System.getenv("ConcurrentCloseIT_ITERATIONS");
        if (null == envValue) {
            limitIterations = 2500;
        } else
            limitIterations = Integer.parseInt(envValue);
    }

    @Test
    void concurrentCloseTest() throws Exception {
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < limitIterations; i++) {
            testThis();
            if (i > 0 && i % 500 == 0) {
                log.info("Performed {} of {} iterations in {}ms",
                    i, limitIterations, System.currentTimeMillis() - startTime);
            }
        }
        log.info("Performed {} iterations in {}ms", limitIterations, System.currentTimeMillis() - startTime);
    }

    private void testThis() throws InterruptedException {
        final GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.setUser("test@localhost", "test@localhost");
        greenMail.start();
        final SenderThread sendThread = new SenderThread();
        try {
            sendThread.start();
            greenMail.waitForIncomingEmail(3000, 1);
            final MimeMessage[] emails = greenMail.getReceivedMessages();
            assertThat(emails).hasSize(1);
            sendThread.join(10000);
        } finally {
            greenMail.stop();
        }

        if (sendThread.exc != null) {
            throw sendThread.exc;
        }
    }

    private static class SenderThread extends Thread {
        RuntimeException exc;

        @Override
        public void run() {
            try {
                GreenMailUtil.sendTextEmail("test@localhost", "from@localhost", "abc", "def",
                    ServerSetupTest.SMTP);
            } catch (final Throwable e) {
                exc = new IllegalStateException(e);
            }
        }
    }
}
