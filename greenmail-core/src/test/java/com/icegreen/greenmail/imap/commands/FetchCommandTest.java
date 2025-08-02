package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import org.eclipse.angus.mail.iap.Response;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.eclipse.angus.mail.imap.protocol.BODY;
import org.eclipse.angus.mail.imap.protocol.FetchResponse;
import org.eclipse.angus.mail.imap.protocol.IMAPResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class FetchCommandTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);
    private GreenMailUser user;
    private IMAPStore store;

    @Before
    public void beforeEachTest() throws MessagingException {
        user = greenMail.setUser("foo@localhost", "pwd");
        store = greenMail.getImap().createStore();

        MimeMultipart alternative = new MimeMultipart("alternative");
        MimeBodyPart alt1 = new MimeBodyPart();
        alternative.addBodyPart(alt1);
        MimeMultipart mixed = new MimeMultipart("mixed");
        alt1.setContent(mixed);

        for (int i = 1; i <= 20; i++) {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent("Part " + i, "text/plain");
            mixed.addBodyPart(textPart);
        }
        GreenMailUtil.sendMessageBody("foo@localhost", "from@localhost",
            "Muxed multiparts", alternative, null, ServerSetupTest.SMTP);

        greenMail.waitForIncomingEmail(1);
    }

    @Test
    public void testFetchBodyPeekSection() throws Exception {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            String cmd = "UID FETCH 1 (BODY.PEEK[1.1] BODY.PEEK[1.9] BODY.PEEK[1.15] BODY.PEEK[1.20])";
            Response[] ret = (Response[]) folder.doCommand(protocol -> protocol.command(cmd, null));

            IMAPResponse okResponse = (IMAPResponse) ret[1];
            assertThat(okResponse.isOK()).isTrue();

            List<String> received = FetchResponse
                .getItems(ret, 1, BODY.class).stream()
                .map(item -> new String(item.getByteArray().getNewBytes()))
                .collect(Collectors.toList());

            List<String> expected = new ArrayList<>();
            expected.add("Part 1");
            expected.add("Part 9");
            expected.add("Part 15");
            expected.add("Part 20");
            assertThat(received).containsAll(expected);

        } finally {
            store.close();
        }
    }
}
