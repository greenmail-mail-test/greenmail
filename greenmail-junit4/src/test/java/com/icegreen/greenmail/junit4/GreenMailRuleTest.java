package com.icegreen.greenmail.junit4;

import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class GreenMailRuleTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    @Test
    public void testGreenMailStarted() {
        validateServicesRunning();

        // Send email and test in #testGreenMailStartedAgain() for "relicts"
        GreenMailUtil.sendTextEmail("to@localhost", "from@localhost", "subject", "content", greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(1);
        assertThat(greenMail.getReceivedMessages().length).isEqualTo(1);
    }

    @Test
    public void testGreenMailStartedAgain() {
        validateServicesRunning();

        // Expect no relict from previous test.
        assertThat(greenMail.getReceivedMessages().length).isEqualTo(0);
    }

    private void validateServicesRunning() {
        assertThat(greenMail.getImap().isRunning()).isTrue();
        assertThat(greenMail.getImaps()).isNull();
        assertThat(greenMail.getSmtp().isRunning()).isTrue();
        assertThat(greenMail.getSmtps()).isNull();
        assertThat(greenMail.getPop3()).isNull();
        assertThat(greenMail.getPop3s()).isNull();
    }
}