package com.icegreen.greenmail.test.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * Tests date handling for received messages
 */
public class DateTest {
    @Rule
    public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    @Ignore
    public void testDatesCorrect() throws MessagingException, IOException {
        String to = "to@localhost";
        greenMail.setUser(to, to);

        // Create mail with specific 'sent' date
        final MimeMessage mail = GreenMailUtil.createTextEmail(to, "from@localhost", "Subject", "msg", greenMail.getSmtp().getServerSetup());
        final Date sentDate = new GregorianCalendar(2000, 1, 1, 0, 0, 0).getTime();
        mail.setSentDate(sentDate);
        GreenMailUtil.sendMimeMessage(mail);

        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheck(new Retriever(greenMail.getPop3()), to, sentDate, false);
        retrieveAndCheck(new Retriever(greenMail.getImap()), to, sentDate, true);
    }

    /**
     * Retrieve message from retriever and check content
     *
     * @param retriever         Retriever to read from
     * @param to                Account to retrieve
     * @param sentDate          Desired 'sent' date of message
     * @param checkReceivedDate True if received date should be checked. POP3 does not provide a received date
     */
    private void retrieveAndCheck(Retriever retriever, String to, Date sentDate, boolean checkReceivedDate)
            throws MessagingException, IOException {
        Message[] messages = retriever.getMessages(to);
        assertThat(messages.length, is(1));
        Message message = messages[0];
        assertThat(milliSecondDateDiff(message.getSentDate(), sentDate), lessThan(3000L));
        if (checkReceivedDate) {
            assertThat(milliSecondDateDiff(message.getReceivedDate(), new Date()), lessThan(3000L));
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
