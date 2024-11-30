package com.icegreen.greenmail.docker;

import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

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
                assertThat(folder.getMessageCount())
                    .isEqualTo(2)
                    .withFailMessage("Can not check mails using " + store.getURLName());
            } finally {
                store.close();
            }
        }

        // API
        Client client = ClientBuilder.newClient();
        WebTarget api = client.target("http://" + bindAddress + ":8080");

        // Check indexResponse page
        final Response indexResponse = api.path("/")
            .request(MediaType.TEXT_HTML).get(Response.class);
        assertThat(indexResponse.getStatus()).isEqualTo(200);
        assertThat(indexResponse.readEntity(String.class)).contains("GreenMail API");

        // Check API
        final Response configResponse = api.path("/api/configuration")
            .request(MediaType.APPLICATION_JSON).get(Response.class);
        assertThat(configResponse.getStatus()).isEqualTo(200);
        assertThat(configResponse.readEntity(String.class)).isEqualTo("{" +
            "\"serverSetups\":[" +
            "{\"port\":3025,\"address\":\"0.0.0.0\",\"protocol\":\"smtp\",\"isSecure\":false,\"readTimeout\":-1,\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}," +
            "{\"port\":3465,\"address\":\"0.0.0.0\",\"protocol\":\"smtps\",\"isSecure\":true,\"readTimeout\":-1,\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}," +
            "{\"port\":3110,\"address\":\"0.0.0.0\",\"protocol\":\"pop3\",\"isSecure\":false,\"readTimeout\":-1,\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}," +
            "{\"port\":3995,\"address\":\"0.0.0.0\",\"protocol\":\"pop3s\",\"isSecure\":true,\"readTimeout\":-1,\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}," +
            "{\"port\":3143,\"address\":\"0.0.0.0\",\"protocol\":\"imap\",\"isSecure\":false,\"readTimeout\":-1,\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}," +
            "{\"port\":3993,\"address\":\"0.0.0.0\",\"protocol\":\"imaps\",\"isSecure\":true,\"readTimeout\":-1,\"writeTimeout\":-1,\"connectionTimeout\":-1,\"serverStartupTimeout\":2000,\"isDynamicPort\":false}" +
            "]," +
            "\"authenticationDisabled\":true," +
            "\"sieveIgnoreDetail\":false,\"preloadDirectory\":null" +
            "}"
        );
    }
}
