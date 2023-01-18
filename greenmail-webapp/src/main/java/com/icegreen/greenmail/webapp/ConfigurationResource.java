package com.icegreen.greenmail.webapp;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;

/**
 * JAX-RS 2.0
 */
@Path("/configuration")
public class ConfigurationResource {
    private @Context
    ServletContext context;

    @GET
    @Produces("application/json")
    public Configuration configuration() {
        return ContextHelper.getConfiguration(context);
    }
}
