package com.icegreen.greenmail.test;

import static org.junit.Assert.assertEquals;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

public class ConcurrentCloseIT {
    @Test
    public void concurrentCloseTest() throws Exception {
        for (int i = 0; i < 10000; i++) {
            testThis();
        }
    }

    private volatile RuntimeException exc;
    private void testThis() throws InterruptedException {
        exc = null;
        final GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
        greenMail.setUser("test@localhost.com","test@localhost.com");
        final Thread sendThread = new Thread() {
            public void run() {
                try {
                    sendMail("test@localhost.com", "from@localhost.com", "abc", "def", ServerSetupTest.SMTP);
                } catch (final Throwable e) {
                    exc = new RuntimeException(e);
                }
            }
        };
        sendThread.start();
        greenMail.waitForIncomingEmail(3000, 1);
        final MimeMessage[] emails = greenMail.getReceivedMessages();
        assertEquals(1, emails.length);
        greenMail.stop();
        sendThread.join(10000);
        if (exc != null) {
            throw exc;
        }
    }

    private void sendMail(final String to,
                          final String from,
                          final String subject,
                          final String msg,
                          final ServerSetup serverSetup) throws MessagingException {
        final Session session = GreenMailUtil.getSession(serverSetup);
        final MimeMessage textEmail = GreenMailUtil.createTextEmail(to, from, subject, msg, serverSetup);
        final Transport transport = session.getTransport(serverSetup.getProtocol());
        transport.connect();
        transport.sendMessage(textEmail, new Address[] {new InternetAddress(to)});
        try {
            transport.close();
        } catch (final MessagingException e) {
            //ignore
        }
    }
}
