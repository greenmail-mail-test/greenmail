package com.icegreen.greenmail.server;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class AllocateAvailablePortTest {

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetup.SMTP.dynamicPort());

    @Test
    public void returnTheActuallyAllocatedPort() {
        assertThat(greenMail.getSmtp().getPort()).isNotZero();
    }

    @Test
    public void ensureThatMailCanActuallyBeSentToTheAllocatedPort() {
        GreenMailUtil.sendTextEmail("to@localhost", "from@localhost", "subject", "body",
                greenMail.getSmtp().getServerSetup());

        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertThat(emails).hasSize(1);
    }
}
