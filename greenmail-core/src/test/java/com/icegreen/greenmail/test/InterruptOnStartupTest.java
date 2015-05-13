package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

public class InterruptOnStartupTest {

    /**
     * Tests that the servers are shut down gracefully when greenmail is
     * interrupted while starting the mail services.
     */
    @Test
    public void testCleanShutdownWhenInterruptedWhileStartingServices() throws Exception {
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                GreenMail greenMail = new GreenMail(ServerSetupTest.SMTPS_IMAPS);
                greenMail.start();
            }
        });
        t.start();
        Thread.sleep(500);
        t.interrupt();
    }
}
