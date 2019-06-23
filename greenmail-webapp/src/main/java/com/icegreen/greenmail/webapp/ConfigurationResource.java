package com.icegreen.greenmail.webapp;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

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
