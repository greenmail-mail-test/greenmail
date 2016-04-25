package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static org.junit.Assert.assertEquals;

public class ConcurrentCloseTest {
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
        greenMail.start();
        final Thread sendThread = new Thread() {
            public void run() {
                try {
                    GreenMailUtil.sendTextEmailTest("test@localhost.com", "from@localhost.com", "abc", "def");
                } catch (final Throwable e) {
                    exc = new RuntimeException(e);
                }
            }
        };
        sendThread.start();
        greenMail.waitForIncomingEmail(3000, 1);
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        greenMail.stop();
        sendThread.join(10000);
        if (exc != null) {
            throw exc;
        }
    }
}
