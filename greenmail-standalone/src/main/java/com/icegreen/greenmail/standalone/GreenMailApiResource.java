package com.icegreen.greenmail.standalone;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Scanner;

/**
 * Exposes GreenMail API and openapi.
 */
@Path("/")
public class GreenMailApiResource {
    private static final Logger LOG = LoggerFactory.getLogger(GreenMailApiResource.class);
    private final GreenMail greenMail;
    private final ServerSetup[] serverSetups;
    private final GreenMailConfiguration configuration;

    public GreenMailApiResource(GreenMail greenMail, ServerSetup[] serverSetups, GreenMailConfiguration configuration) {
        this.greenMail = greenMail;
        this.serverSetups = serverSetups;
        this.configuration = configuration;
    }

    // UI
    private static final String INDEX_CONTENT = loadResource("index.html");
    private static final String OPENAPI_CONTENT = loadResource("greenmail-openapi.yml");

    private static String loadResource(String name) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            return new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        } catch (IOException | NullPointerException e) {
            throw new IllegalArgumentException("Can not load resource " + name + " from classpath", e);
        }
    }

    @GET
    @Produces("text/html")
    public String index() {
        return INDEX_CONTENT;
    }

    @Path("/greenmail-openapi.yml")
    @GET
    @Produces("application/yaml")
    public String openapi() {
        return OPENAPI_CONTENT;
    }

    // General
    abstract static class AbstractMessage {
        private final String message;

        protected AbstractMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    static class SuccessMessage extends AbstractMessage {
        protected SuccessMessage(String message) {
            super(message);
        }
    }

    static class ErrorMessage extends AbstractMessage {
        protected ErrorMessage(String message) {
            super(message);
        }
    }

    // Configuration
    static class Configuration {
        public ServerSetup[] serverSetups;
        public boolean authenticationDisabled;
    }

    @GET
    @Path("/api/configuration")
    @Produces("application/json")
    public Response configuration() {
        final Configuration config = new Configuration();
        config.serverSetups = serverSetups;
        config.authenticationDisabled = configuration.isAuthenticationDisabled();
        return Response.status(Response.Status.OK)
            .entity(config)
            .build();
    }

    // User
    public static class User {
        public String email;
        public String login;
        public String password;
    }

    /**
     * Custom mapped, see {@link JacksonObjectMapperProvider.GreenMailUserSerializer}
     */
    @GET
    @Path("/api/user")
    @Produces("application/json")
    public Collection<GreenMailUser> listUsers() {
        return greenMail.getManagers().getUserManager().listUser();
    }

    @POST
    @Path("/api/user")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces("application/json")
    public Response createUsers(User newUser) {
        try {
            final GreenMailUser user = greenMail.getManagers().getUserManager().createUser(newUser.email, newUser.login, newUser.password);
            LOG.debug("Created user {}", user);
            return Response.status(Response.Status.OK)
                .entity(user)
                .build();
        } catch (UserException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorMessage("Can not create user : " + e.getMessage()))
                .build();
        }
    }

    @DELETE
    @Path("/api/user/{emailOrLogin}")
    @Produces("application/json")
    public Response deleteUserById(@PathParam("emailOrLogin") String id) {
        final UserManager userManager = greenMail.getManagers().getUserManager();
        LOG.debug("Searching user using '{}'", id);
        GreenMailUser user = userManager.getUser(id);
        if (null == user) {
            user = userManager.getUserByEmail(id);
        }
        if (null == user) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorMessage("User '" + id + "' not found")).build();
        }
        LOG.debug("Deleting user {}", user);
        userManager.deleteUser(user);
        return Response.status(Response.Status.OK)
            .entity(new SuccessMessage("User '" + id + "' deleted")).build();

    }

    // Operations
    @POST
    @Path("/api/mail/purge")
    @Produces("application/json")
    public AbstractMessage purge() {
        try {
            greenMail.purgeEmailFromAllMailboxes();
            return new SuccessMessage("Purged mails");
        } catch (FolderException e) {
            return new ErrorMessage("Can not purge mails : " + e.getMessage());
        }
    }

    @POST
    @Path("/api/service/reset")
    @Produces("application/json")
    public AbstractMessage reset() {
        greenMail.reset();
        return new SuccessMessage("Performed reset");
    }


    @GET
    @Path("/api/service/readiness")
    @Produces("application/json")
    public Response ready() {
        if (greenMail.isRunning()) {
            return Response.status(Response.Status.OK)
                .entity(new SuccessMessage("Service running")).build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new SuccessMessage("Service not running")).build();
        }
    }
}
