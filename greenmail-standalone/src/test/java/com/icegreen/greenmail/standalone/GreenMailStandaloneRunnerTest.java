package com.icegreen.greenmail.standalone;

import com.icegreen.greenmail.configuration.PropertiesBasedGreenMailConfigurationBuilder;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.PropertiesBasedServerSetupBuilder;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import javax.mail.*;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GreenMailStandaloneRunnerTest {

    @Test
    public void testDoRun() throws MessagingException {
        GreenMailStandaloneRunner runner = new GreenMailStandaloneRunner();
        final Properties properties = new Properties();
        properties.setProperty(PropertiesBasedServerSetupBuilder.GREENMAIL_VERBOSE, "");
        properties.setProperty("greenmail.setup.test.smtp", "");
        properties.setProperty("greenmail.setup.test.imap", "");
        properties.setProperty(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS,
                "test1:pwd1,test2:pwd2@localhost");
        runner.doRun(properties);

        GreenMailUtil.sendTextEmail("test2@localhost", "test1@localhost", "Standalone test", "It worked",
                ServerSetupTest.SMTP);

        final Session session = runner.getGreenMail().getImap().createSession();
        assertTrue(session.getDebug());
        Store store = session.getStore("imap");
        try {
            store.connect("test2", "pwd2");
            final Folder folder = store.getFolder("INBOX");
            try {
                folder.open(Folder.READ_ONLY);
                Message msg = folder.getMessages()[0];
                assertEquals("test1@localhost", msg.getFrom()[0].toString());
                assertEquals("Standalone test", msg.getSubject());
            } finally {
                folder.close(true);
            }
        } finally {
            store.close();
        }
    }
}
