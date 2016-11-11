package com.icegreen.greenmail.test.specificmessages;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.internal.GreenMailRuleWithStoreChooser;
import com.icegreen.greenmail.internal.StoreChooser;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests date handling for received messages
 */
public class DateTest {
    @Rule
    public GreenMailRuleWithStoreChooser greenMail = new GreenMailRuleWithStoreChooser(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    @StoreChooser(store="file,memory")
    public void testDatesCorrect() throws MessagingException, IOException {
        String to = "to@localhost";
        greenMail.setUser(to, to);

        // Create mail with specific 'sent' date
        final MimeMessage mail = GreenMailUtil.createTextEmail(to, "from@localhost", "Subject", "msg", greenMail.getSmtp().getServerSetup());
        final Date sentDate = new GregorianCalendar(2000, 1, 1, 0, 0, 0).getTime();
        mail.setSentDate(sentDate);
        GreenMailUtil.sendMimeMessage(mail);

        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheck(greenMail.getPop3(), to, sentDate, false);
        retrieveAndCheck(greenMail.getImap(), to, sentDate, true);
    }

    /**
     * Retrieve message from retriever and check content
     *
     * @param server            Server to read from
     * @param to                Account to retrieve
     * @param sentDate          Desired 'sent' date of message
     * @param checkReceivedDate True if received date should be checked. POP3 does not provide a received date
     */
    private void retrieveAndCheck(AbstractServer server, String to, Date sentDate, boolean checkReceivedDate)
            throws MessagingException, IOException {
        try (Retriever retriever = new Retriever(server)) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages.length, is(1));
            Message message = messages[0];
            assertThat(milliSecondDateDiff(message.getSentDate(), sentDate), lessThan(3000L));
            if (checkReceivedDate) {
                assertThat(milliSecondDateDiff(message.getReceivedDate(), new Date()), lessThan(3000L));
            }
        }
    }

    /**
     * Difference in milliseconds between the two given dates
     *
     * @param date1 First date
     * @param date2 Second date
     * @return Difference, always positive
     */

    private long milliSecondDateDiff(Date date1, Date date2) {
        long diff = date1.getTime() - date2.getTime();
        return Math.abs(diff);
    }
}
