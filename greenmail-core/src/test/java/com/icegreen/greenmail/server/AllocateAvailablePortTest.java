package com.icegreen.greenmail.server;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class AllocateAvailablePortTest {

    private static final int AnyFreePort = 0;

    private final ServerSetup allocateAnyFreePortForAnSmtpServer = smtpServerAtPort(AnyFreePort);

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(allocateAnyFreePortForAnSmtpServer);

    @Test
    public void returnTheActuallyAllocatedPort() {
        assertThat(greenMail.getSmtp().getPort()).isNotEqualTo(0);
    }

    @Test
    public void ensureThatMailCanActuallyBeSentToTheAllocatedPort() {
        GreenMailUtil.sendTextEmail("to@localhost.com", "from@localhost.com", "subject", "body",
                smtpServerAtPort(greenMail.getSmtp().getPort()));

        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertThat(emails.length).isEqualTo(1);
    }

    private ServerSetup smtpServerAtPort(int port) {
        return new ServerSetup(port, null, ServerSetup.PROTOCOL_SMTP);
    }
}