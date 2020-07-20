package com.icegreen.greenmail.junit4;

import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class GreenMailRuleTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    @Test
    public void testGreenMailStarted() {
        validateServicesRunning();

        // Send email and test in #testGreenMailStartedAgain() for "relicts"
        GreenMailUtil.sendTextEmail("to@localhost", "from@localhost", "subject", "content", greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(1);
        assertEquals(1, greenMail.getReceivedMessages().length);
    }

    @Test
    public void testGreenMailStartedAgain() {
        validateServicesRunning();

        // Expect no relict from previous test.
        assertEquals(0, greenMail.getReceivedMessages().length);
    }

    private void validateServicesRunning() {
        assertTrue(greenMail.getImap().isRunning());
        assertNull(greenMail.getImaps());
        assertTrue(greenMail.getSmtp().isRunning());
        assertNull(greenMail.getSmtps());
        assertNull(greenMail.getPop3());
        assertNull(greenMail.getPop3s());
    }
}