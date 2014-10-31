package com.icegreen.greenmail.test.specificmessages;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Date;

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
    public void testDatesCorrect() throws MessagingException, IOException {
        String to = "to@localhost";
        greenMail.setUser(to, to);
        GreenMailUtil.sendTextEmail(to, "from@localhost", "Subject", "msg", greenMail.getSmtp().getServerSetup());
        greenMail.waitForIncomingEmail(5000, 1);

        retrieveAndCheck(new Retriever(greenMail.getPop3()), to, false);
        retrieveAndCheck(new Retriever(greenMail.getImap()), to, true);
    }

    /**
     * Retrieve message from retriever and check content
     *
     * @param retriever         Retriever to read from
     * @param to                Account to retrieve
     * @param checkReceivedDate True if received date should be checked. POP3 does not provide a received date
     */
    private void retrieveAndCheck(Retriever retriever, String to, boolean checkReceivedDate)
            throws MessagingException, IOException {
        Message[] messages = retriever.getMessages(to);
        assertThat(messages.length, is(1));
        Message message = messages[0];
        assertThat(milliSecondDateDiffToNow(message.getSentDate()), lessThan(3000L));
        if(checkReceivedDate) {
            assertThat(milliSecondDateDiffToNow(message.getReceivedDate()), lessThan(3000L));
        }
    }

    /**
     * Difference in milliseconds between the given date and now
     *
     * @param compareDate Date to compare with
     * @return Difference, always positive
     */
    private long milliSecondDateDiffToNow(Date compareDate) {
        long diff = new Date().getTime() - compareDate.getTime();
        return Math.abs(diff);
    }
}
