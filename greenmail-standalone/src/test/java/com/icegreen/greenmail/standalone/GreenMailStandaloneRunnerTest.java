package com.icegreen.greenmail.standalone;

import com.icegreen.greenmail.configuration.PropertiesBasedGreenMailConfigurationBuilder;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.PropertiesBasedServerSetupBuilder;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.After;
import org.junit.Test;

import javax.mail.*;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GreenMailStandaloneRunnerTest {
    private GreenMailStandaloneRunner runner;

    @After
    public void tearDown() {
        if (null != runner) {
            runner.stop();
        }
    }

    @Test
    public void testDoRun() throws MessagingException {
        runner = createAndConfigureRunner(new Properties());

        GreenMailUtil.sendTextEmail("test2@localhost", "test1@localhost", "Standalone test", "It worked",
            ServerSetupTest.SMTP);

        final Session session = runner.getGreenMail().getImap().createSession();
        assertThat(session.getDebug()).isTrue();
        try (Store store = session.getStore("imap")) {
            store.connect("test2", "pwd2");
            try (Folder folder = store.getFolder("INBOX")) {
                folder.open(Folder.READ_ONLY);
                Message msg = folder.getMessages()[0];
                assertThat(msg.getFrom()[0].toString()).isEqualTo("test1@localhost");
                assertThat(msg.getSubject()).isEqualTo("Standalone test");
            }
        }

        // Test if default API is disabled
        Client client = ClientBuilder.newClient();
        WebTarget api = client.target("http://localhost:8080");
        assertThatThrownBy(() -> api.path("/").request(MediaType.TEXT_HTML).get(Response.class))
            .isInstanceOf(ProcessingException.class);
    }

    @Test
    public void testApi() {
        final Properties properties = new Properties();
        properties.put(GreenMailApiServerBuilder.GREENMAIL_API_HOSTNAME, "localhost");
        runner = createAndConfigureRunner(properties);

        Client client = ClientBuilder.newClient();
        WebTarget api = client.target("http://localhost:8080");

        // Check indexResponse page
        final Response indexResponse = api.path("/")
            .request(MediaType.TEXT_HTML).get(Response.class);
        assertThat(indexResponse.getStatus()).isEqualTo(200);
        assertThat(indexResponse.readEntity(String.class)).contains("GreenMail API");

        // Check API
        final Response configResponse = api.path("/api/configuration")
            .request(MediaType.APPLICATION_JSON).get(Response.class);
        assertThat(configResponse.getStatus()).isEqualTo(200);
        assertThat(configResponse.readEntity(String.class)).isEqualTo("{\"serverSetups\":[" +
            "{\"port\":3025,\"address\":\"127.0.0.1\",\"protocol\":\"smtp\",\"isSecure\":false,\"readTimeout\":-1," +
            "\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}," +
            "{\"port\":3143,\"address\":\"127.0.0.1\",\"protocol\":\"imap\",\"isSecure\":false,\"readTimeout\":-1," +
            "\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}],\"authenticationDisabled\":false" +
            "}");

        final Response userListResponse = api.path("/api/user")
            .request(MediaType.APPLICATION_JSON).get(Response.class);
        assertThat(userListResponse.getStatus()).isEqualTo(200);
        assertThat(userListResponse.readEntity(String.class)).isEqualTo("[" +
            "{\"login\":\"test2\",\"email\":\"test2@localhost\"}," +
            "{\"login\":\"test1\",\"email\":\"test1\"}" +
            "]");

        String userId = "foo.bar";
        final Response userCreateResponse = api.path("/api/user")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity("{\"email\":\"foo.bar@localhost\", \"login\":\""+userId+"\", \"password\":\"xxx\"}",
                MediaType.APPLICATION_JSON));
        assertThat(userCreateResponse.getStatus()).isEqualTo(200);
        assertThat(userCreateResponse.readEntity(String.class)).isEqualTo("{\"login\":\""+userId+"\",\"email\":\"foo.bar@localhost\"}");

        final Invocation.Builder deleteRequest = api.path("/api/user/" + userId).request();
        final Response userDeleteResponse = deleteRequest.delete();
        assertThat(userDeleteResponse.getStatus()).isEqualTo(200);
        assertThat(deleteRequest.delete().getStatus()).isEqualTo(400);

        final Response readinessResponse = api.path("/api/service/readiness")
            .request(MediaType.APPLICATION_JSON).get(Response.class);
        assertThat(readinessResponse.getStatus()).isEqualTo(200);
        assertThat(readinessResponse.readEntity(String.class)).isEqualTo("{\"message\":\"Service running\"}");
    }

    private GreenMailStandaloneRunner createAndConfigureRunner(Properties properties) {
        GreenMailStandaloneRunner runner = new GreenMailStandaloneRunner();
        properties.setProperty(PropertiesBasedServerSetupBuilder.GREENMAIL_VERBOSE, "");
        properties.setProperty("greenmail.setup.test.smtp", "");
        properties.setProperty("greenmail.setup.test.imap", "");
        properties.setProperty(PropertiesBasedGreenMailConfigurationBuilder.GREENMAIL_USERS,
            "test1:pwd1,test2:pwd2@localhost");
        runner.doRun(properties);
        return runner;
    }

}
