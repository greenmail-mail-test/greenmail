package com.icegreen.greenmail.docker;

import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class DockerServiceIT {

    private final String bindAddress = System.getProperty("greenmail.host.address", "127.0.0.1");

    @Test
    public void testAllServices() throws MessagingException, InterruptedException {
        // Ugly workaround : GreenMail in docker starts with open TCP connections,
        //                   but TLS sockets might not be ready yet.
        TimeUnit.SECONDS.sleep(1);

        // Send messages via SMTP and secure SMTPS
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost",
                "test1", "Test GreenMail Docker service",
                ServerSetupTest.SMTP.createCopy(bindAddress));
        GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost",
                "test2", "Test GreenMail Docker service",
                ServerSetupTest.SMTPS.createCopy(bindAddress));

        // IMAP
        for (ServerSetup setup : Arrays.asList(
                ServerSetupTest.IMAP.createCopy(bindAddress),
                ServerSetupTest.IMAPS.createCopy(bindAddress),
                ServerSetupTest.POP3.createCopy(bindAddress),
                ServerSetupTest.POP3S.createCopy(bindAddress))) {
            final Store store = Session.getInstance(setup.configureJavaMailSessionProperties(null, false)).getStore();
            store.connect("foo@localhost", "foo@localhost");
            try {
                Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);
                assertEquals("Can not check mails using "+store.getURLName(), 2, folder.getMessageCount());
            } finally {
                store.close();
            }
        }
    }
}