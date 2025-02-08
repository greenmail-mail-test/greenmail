package com.icegreen.greenmail.specificmessages;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Tests date handling for received messages
 */
class DateTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_POP3_IMAP);

    @Test
    void testDatesCorrect() throws MessagingException {
        String to = "to@localhost";
        greenMail.setUser(to, to);

        // Create mail with specific 'sent' date
        final MimeMessage mail = GreenMailUtil.createTextEmail(to, "from@localhost", "Subject", "msg", greenMail.getSmtp().getServerSetup());
        final Date sentDate = new GregorianCalendar(2000, Calendar.FEBRUARY, 1, 0, 0, 0).getTime();
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
            throws MessagingException {
        try (Retriever retriever = new Retriever(server)) {
            Message[] messages = retriever.getMessages(to);
            assertThat(messages).hasSize(1);
            Message message = messages[0];
            assertThat(milliSecondDateDiff(message.getSentDate(), sentDate)).isLessThan(3000L);
            if (checkReceivedDate) {
                assertThat(milliSecondDateDiff(message.getReceivedDate(), new Date())).isLessThan(3000L);
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
