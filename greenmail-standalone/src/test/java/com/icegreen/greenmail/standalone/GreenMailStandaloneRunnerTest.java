package com.icegreen.greenmail.standalone;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.icegreen.greenmail.configuration.PropertiesBasedGreenMailConfigurationBuilder;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.PropertiesBasedServerSetupBuilder;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

class GreenMailStandaloneRunnerTest {
    private GreenMailStandaloneRunner runner;

    @AfterEach
    void tearDown() {
        if (null != runner) {
            runner.stop();
        }
    }

    @Test
    void testDoRun() throws MessagingException {
        runner = createAndConfigureRunner(new Properties());

        GreenMailUtil.sendTextEmail("test2@localhost", "test1@localhost",
            "Standalone test", "It worked", ServerSetupTest.SMTP);

        final Session session = runner.getGreenMail().getImap().createSession();
        assertThat(session.getDebug()).isTrue();
        try (Store store = session.getStore("imap")) {
            store.connect("test2", "pwd2");
            try (Folder folder = store.getFolder("INBOX")) {
                folder.open(Folder.READ_ONLY);
                Message msg = folder.getMessages()[0];
                assertThat(msg.getFrom()[0]).hasToString("test1@localhost");
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
    void testApi() {
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
            "\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}]," +
            "\"authenticationDisabled\":false," +
            "\"sieveIgnoreDetail\":false," +
            "\"preloadDirectory\":null" +
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
            .post(Entity.entity(
                "{\"email\":\"foo.bar@localhost\", \"login\":\"" + userId + "\", \"password\":\"xxx\"}",
                MediaType.APPLICATION_JSON));
        assertThat(userCreateResponse.getStatus()).isEqualTo(200);
        assertThat(userCreateResponse.readEntity(String.class))
            .isEqualTo("{\"login\":\"" + userId + "\",\"email\":\"foo.bar@localhost\"}");

        // Getting messages
        final Response userMessagesEmptyResponse = api.path("/api/user/"+userId+"/messages")
            .request(MediaType.APPLICATION_JSON)
            .get(Response.class);
        assertThat(userMessagesEmptyResponse.getStatus()).isEqualTo(200);
        assertThat(userMessagesEmptyResponse.readEntity(String.class))
            .isEqualTo("[]"); // EMPTY

        GreenMailUtil.sendTextEmail(userId+"@localhost", "test1@localhost",
            "testApi", "A test text message", ServerSetupTest.SMTP);
        final Response userMessagesResponse = api.path("/api/user/"+userId+"/messages")
            .request(MediaType.APPLICATION_JSON)
            .get(Response.class);
        assertThat(userMessagesResponse.getStatus()).isEqualTo(200);
        GenericType<List<Map<String,String>>> userMessagesResponseType = new GenericType<List<Map<String,String>>>(){};
        Map<String,String> value = userMessagesResponse.readEntity(userMessagesResponseType).get(0);
        assertThat(value)
            .containsEntry("uid", "1")
            .containsEntry("subject", "testApi")
            .containsEntry("contentType", "text/plain; charset=us-ascii");
        assertThat(value.get("Message-ID")).matches("^<.*>$");
        assertThat(value.get("mimeMessage"))
            .contains("testApi")
            .contains("A test text message");

        final Invocation.Builder deleteRequest = api.path("/api/user/" + userId).request();
        final Response userDeleteResponse = deleteRequest.delete();
        assertThat(userDeleteResponse.getStatus()).isEqualTo(200);
        assertThat(deleteRequest.delete().getStatus()).isEqualTo(400);

        final Response readinessResponse = api.path("/api/service/readiness")
            .request(MediaType.APPLICATION_JSON).get(Response.class);
        assertThat(readinessResponse.getStatus()).isEqualTo(200);
        assertThat(readinessResponse.readEntity(String.class))
            .isEqualTo("{\"message\":\"Service running\"}");
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
