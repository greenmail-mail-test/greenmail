package com.icegreen.greenmail.test;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConcurrentCloseIT {
    @Test
    public void concurrentCloseTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            testThis();
        }
    }

    private volatile RuntimeException exc;
    private void testThis() throws InterruptedException {
        exc = null;
        final GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.setUser("test@localhost","test@localhost");
        greenMail.start();
        try {
            final Thread sendThread = new Thread() {
                public void run() {
                    try {
                        GreenMailUtil.sendTextEmail("test@localhost", "from@localhost", "abc", "def", ServerSetupTest.SMTP);
                    } catch (final Throwable e) {
                        exc = new IllegalStateException(e);
                    }
                }
            };
            sendThread.start();
            greenMail.waitForIncomingEmail(3000, 1);
            final MimeMessage[] emails = greenMail.getReceivedMessages();
            assertEquals(1, emails.length);
            sendThread.join(10000);
        } finally {
            greenMail.stop();
        }
        if (exc != null) {
            throw exc;
        }
    }
}
