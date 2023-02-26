package com.icegreen.greenmail.standalone;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Properties;

import static com.icegreen.greenmail.util.PropertiesBasedServerSetupBuilder.*;


/**
 * Builds an API Server using JDK internal HTTP Server as default.
 */
public class GreenMailApiServerBuilder {
    public static final String GREENMAIL_API_HOSTNAME = "greenmail.api.hostname";
    public static final String GREENMAIL_API_PORT = "greenmail.api.port";
    public static final int GREENMAIL_API_PORT_DEFAULT = 8080;
    private String address;
    private int port = GREENMAIL_API_PORT_DEFAULT;

    private GreenMail greenMail;
    private ServerSetup[] serverSetups;
    private GreenMailConfiguration configuration;

    interface ApiServer {
        /**
         * Gets the server URI.
         *
         * @return the serve URI address
         */
        URI getUri();

        /**
         * Starts the server.
         */
        void start();

        /**
         * Stops the server.
         */
        void stop();
    }

    public static class GreenMailApiJdkServer implements ApiServer {
        private final URI baseUri;
        private final ResourceConfig resourceConfig;
        private HttpServer httpServer;

        public GreenMailApiJdkServer(URI baseUri, GreenMailApiResource greenMailApiResource) {
            this.baseUri = baseUri;
            resourceConfig = new ResourceConfig();
            resourceConfig.registerInstances(greenMailApiResource);
            resourceConfig.register(JacksonFeature.class);
            resourceConfig.register(JacksonObjectMapperProvider.class);
        }

        @Override
        public URI getUri() {
            return baseUri;
        }

        @Override
        public void start() {
            httpServer = JdkHttpServerFactory.createHttpServer(baseUri, resourceConfig);
        }

        @Override
        public void stop() {
            httpServer.stop(0);
        }
    }

    public GreenMailApiServerBuilder configure(Properties properties) {
        if (properties.containsKey(GREENMAIL_API_HOSTNAME)
            || properties.containsKey(GREENMAIL_SETUP_ALL)
            || properties.containsKey(GREENMAIL_SETUP_TEST_ALL)
            || properties.containsKey(GREENMAIL_HOSTNAME)) {
            if (properties.containsKey(GREENMAIL_HOSTNAME)) {
                address = properties.getProperty(GREENMAIL_HOSTNAME);
            } else {
                address = ServerSetup.getLocalHostAddress();
            }
            address = properties.getProperty(GREENMAIL_API_HOSTNAME, address);
        }
        if (properties.containsKey(GREENMAIL_API_PORT)) {
            try {
                port = Integer.parseInt(properties.getProperty(GREENMAIL_API_PORT));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Can not parse port address value "
                    + properties.getProperty(GREENMAIL_API_PORT) + " of system property " + GREENMAIL_API_PORT, ex);
            }
        }
        return this;
    }

    public boolean isEnabled() {
        return null != address && 0 != port;
    }

    public GreenMailApiServerBuilder withGreenMail(GreenMail greenMail, ServerSetup[] serverSetups,
                                                   GreenMailConfiguration configuration) {
        this.greenMail = greenMail;
        this.serverSetups = serverSetups;
        this.configuration = configuration;
        return this;
    }

    public ApiServer build() {
        if (!isEnabled()) {
            throw new IllegalStateException("GreenMail API service is not enabled, please configure "
                + GREENMAIL_API_HOSTNAME + " and " + GREENMAIL_API_PORT);
        }
        URI baseUri = UriBuilder.fromUri("http://" + address + "/").port(port).build();
        final GreenMailApiResource greenMailApiResource = new GreenMailApiResource(greenMail, serverSetups, configuration);
        return new GreenMailApiJdkServer(baseUri, greenMailApiResource);
    }
}
