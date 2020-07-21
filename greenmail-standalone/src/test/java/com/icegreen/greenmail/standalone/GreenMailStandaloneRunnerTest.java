package com.icegreen.greenmail.standalone;

import com.icegreen.greenmail.configuration.PropertiesBasedGreenMailConfigurationBuilder;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.PropertiesBasedServerSetupBuilder;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Test;

import javax.mail.*;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(session.getDebug()).isTrue();
        Store store = session.getStore("imap");
        try {
            store.connect("test2", "pwd2");
            final Folder folder = store.getFolder("INBOX");
            try {
                folder.open(Folder.READ_ONLY);
                Message msg = folder.getMessages()[0];
                assertThat(msg.getFrom()[0].toString()).isEqualTo("test1@localhost");
                assertThat(msg.getSubject()).isEqualTo("Standalone test");
            } finally {
                folder.close(true);
            }
        } finally {
            store.close();
        }
    }
}
