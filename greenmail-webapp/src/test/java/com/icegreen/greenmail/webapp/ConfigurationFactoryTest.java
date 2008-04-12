package com.icegreen.greenmail.webapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * Test for ConfigurationFactory.
 *
 * @author mm
 */
@Test
public class ConfigurationFactoryTest {

    public void testCreate() {
        Map<String, String> paramValues = new HashMap<String, String>();
        paramValues.put("greenmail.defaultHostname", "127.0.0.1");
        paramValues.put("greenmail.portOffset", "20000");
        paramValues.put("greenmail.smtp", "");
        paramValues.put("greenmail.pop3.host", "127.0.0.2");
        paramValues.put("greenmail.pop3.port", "1110");
        paramValues.put("greenmail.imaps", "");
        paramValues.put("greenmail.users", "user1:pwd1@localhost, user2:pwd2@localhost\nuser3:pwd3@localhost");

        Configuration conf = ConfigurationFactory.create(paramValues);

        assert paramValues.get("greenmail.defaultHostname").equals(conf.getDefaultHostname());
        assert 20000 == conf.getPortOffset();

        Configuration.ServiceConfiguration serviceConfSmtp =
                conf.getServiceConfigurationByProtocol(Protocol.SMTP);
        assert null != serviceConfSmtp;

        Configuration.ServiceConfiguration serviceConfPop3 =
                conf.getServiceConfigurationByProtocol(Protocol.POP3);
        assert null != serviceConfPop3;
        assert "127.0.0.2".equals(serviceConfPop3.hostname);
        assert 1110 == serviceConfPop3.port;

        Configuration.ServiceConfiguration serviceConfImaps =
                conf.getServiceConfigurationByProtocol(Protocol.IMAPS);
        assert null != serviceConfImaps;

        List<Configuration.User> users = conf.getUsers();
        assert null != users;
        assert users.size() == 3;
    }
}
