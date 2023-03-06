package com.icegreen.greenmail.server;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Base GreenMail test (no pre-configured GreenMail server)
 */
public class GreenMailBaseTest {
    @Test
    public void testCustomMailSessionProperties() {
        GreenMail greenMail = new GreenMail(new ServerSetup[]{
            ServerSetupTest.SMTP,
            ServerSetupTest.IMAP.mailSessionProperty("a.key","a.value")});
        assertThat(greenMail.getImap().createSession().getProperties()).contains(entry("a.key","a.value"));
        assertThat(greenMail.getSmtp().createSession().getProperties()).doesNotContain(entry("a.key","a.value"));
    }
}
