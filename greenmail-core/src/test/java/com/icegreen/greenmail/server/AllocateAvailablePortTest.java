package com.icegreen.greenmail.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.Rule;
import org.junit.Test;

public class AllocateAvailablePortTest {

    private static final int AnyFreePort = 0;

    private final ServerSetup allocateAnyFreePortForAnSmtpServer = smtpServerAtPort(AnyFreePort);

    @Rule
    public final GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(allocateAnyFreePortForAnSmtpServer);

    @Test
    @StoreChooser(store="file,memory")
    public void returnTheActuallyAllocatedPort() throws Exception {
        assertThat(greenMail.getSmtp().getPort(), not(0));
    }

    @Test
    @StoreChooser(store="file,memory")
    public void ensureThatMailCanActuallyBeSentToTheAllocatedPort() throws Exception {
        GreenMailUtil.sendTextEmail("to@localhost.com", "from@localhost.com", "subject", "body", smtpServerAtPort(greenMail.getSmtp().getPort()));

        MimeMessage[] emails = greenMail.getReceivedMessages();
        assertThat(emails.length, is(1));
    }

    private ServerSetup smtpServerAtPort(int port) {
        return new ServerSetup(port, null, ServerSetup.PROTOCOL_SMTP);
    }
}