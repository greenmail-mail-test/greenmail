package com.icegreen.greenmail.test;

import java.util.concurrent.TimeUnit;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConcurrentCloseIT {

    @Test
    public void concurrentCloseTest() throws Exception {
        for (int i = 0; i < 5000; i++) {
            testThis();
            // Avoid TIME_WAIT issues.
            TimeUnit.MILLISECONDS.sleep(2);
        }
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
            assertThat(1).isEqualTo(emails.length);
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

        public void run() {
            try {
                GreenMailUtil.sendTextEmail("test@localhost", "from@localhost", "abc", "def", ServerSetupTest.SMTP);
            } catch (final Throwable e) {
                exc = new IllegalStateException(e);
            }
        }
    }
}
