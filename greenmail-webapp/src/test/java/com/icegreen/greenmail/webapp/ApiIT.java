package com.icegreen.greenmail.webapp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

class ApiIT {
  private static Server server;
  private static Client client;
  private static WebTarget root;

  @BeforeAll
  static void setUp() throws Exception {
    // Check if executed inside target directory or module directory
    String pathPrefix = new File(".").getCanonicalFile().getName().equals("target") ? "../" : "./";

    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(18080);
    connector.setHost("localhost");
    server.setConnectors(new Connector[] { connector });

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
  void testGetConfiguration() {
    Response response = root.path("api").path("configuration")
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get(Response.class);
    assertEquals(200, response.getStatus());
    assertEquals("{'users':[" +
                 "{'login':'user1','email':'user1@localhost'}," +
                 "{'login':'user2','email':'user2@localhost'}" +
                 "]," +
                 "'defaultHostname':'localhost'," +
                 "'portOffset':10000," +
                 "'serviceConfigurations':[" +
                 "{'protocol':'POP3','hostname':'127.0.0.1','port':10110}," +
                 "{'protocol':'SMTP','hostname':'127.0.0.1','port':10025}]}", response.readEntity(String.class).replaceAll("\"", "'"));
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (null != client) {
      client.close();
    }
    if (null != server) {
      server.stop();
    }
  }
}
