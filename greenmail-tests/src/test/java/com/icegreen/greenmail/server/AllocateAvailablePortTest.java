package com.icegreen.greenmail.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;

class AllocateAvailablePortTest {

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetup.SMTP.dynamicPort());

    @Test
    void returnTheActuallyAllocatedPort() {
        assertThat(greenMail.getSmtp().getPort()).isNotZero();
    }

    @Test
    void ensureThatMailCanActuallyBeSentToTheAllocatedPort() {
        GreenMailUtil.sendTextEmail("to@localhost", "from@localhost", "subject", "body",
                greenMail.getSmtp().getServerSetup());

        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertThat(emails).hasSize(1);
    }
}
