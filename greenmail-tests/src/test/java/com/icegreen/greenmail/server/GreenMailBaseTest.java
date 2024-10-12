package com.icegreen.greenmail.server;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * Base GreenMail test (no pre-configured GreenMail server)
 */
class GreenMailBaseTest {
    @Test
    void testCustomMailSessionProperties() {
        GreenMail greenMail = new GreenMail(new ServerSetup[]{
            ServerSetupTest.SMTP,
            ServerSetupTest.IMAP.mailSessionProperty("a.key","a.value")});
        assertThat(greenMail.getImap().createSession().getProperties()).contains(entry("a.key","a.value"));
        assertThat(greenMail.getSmtp().createSession().getProperties()).doesNotContain(entry("a.key","a.value"));
    }
}
