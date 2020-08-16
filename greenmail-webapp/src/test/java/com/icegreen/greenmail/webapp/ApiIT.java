package com.icegreen.greenmail.webapp;

import java.io.File;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ApiIT {
    private static Server server;
    private static Client client;
    private static WebTarget root;

    @BeforeClass
    public static void setUp() throws Exception {
        // Check if executed inside target directory or module directory
        String pathPrefix =new File(".").getCanonicalFile().getName().equals("target") ? "../" : "./";

        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(18080);
        connector.setHost("localhost");
        server.setConnectors(new Connector[]{connector});

        WebAppContext context = new WebAppContext();
        context.setDescriptor(pathPrefix + "src/main/webapp/WEB-INF/web.xml");
        context.setResourceBase(pathPrefix + "src/main/webapp");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);

        server.setHandler(context);

        server.start();

        client = ClientBuilder.newClient();
        root = client.target("http://" + connector.getHost() + ':' + connector.getPort() + '/');
    }

    @Test
    public void testGetConfiguration() {
        Response response = root.path("api").path("configuration").request(MediaType.APPLICATION_JSON_TYPE).get(Response.class);
        assertEquals(200, response.getStatus());
        assertEquals("{" +
                "'defaultHostname':'localhost'," +
                "'portOffset':10000," +
                "'users':[" +
                "{'login':'user1','email':'user1@localhost'}," +
                "{'login':'user2','email':'user2@localhost'}" +
                "]," +
                "'serviceConfigurations':[" +
                "{'protocol':'POP3','hostname':'127.0.0.1','port':10110}," +
                "{'protocol':'SMTP','hostname':'127.0.0.1','port':10025}" +
                "]" +
                "}", response.readEntity(String.class).replaceAll("\"", "'"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (null != client) {
            client.close();
        }
        if (null != server) {
            server.stop();
        }
    }
}
