package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

/**
 * Test that checks if greenmail start, stop and reset works correctly
 */
public class ServerStartStopTest {
    @Test
    public void testStartStop() {
        GreenMail service = new GreenMail(ServerSetupTest.ALL);
        // Try to stop before start: Nothing happens
        service.stop();
        service.start();
        service.stop();
        // Now the server is stopped, should be started by reset command
        service.reset();
        // Start again
        service.reset();
        // ANd finally stop
        service.stop();
    }
}
