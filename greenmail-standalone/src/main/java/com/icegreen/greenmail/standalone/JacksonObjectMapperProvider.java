package com.icegreen.greenmail.standalone;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetup;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

/**
 *
 */
@Provider
public class JacksonObjectMapperProvider implements ContextResolver<ObjectMapper> {
    final ObjectMapper defaultObjectMapper;

    public JacksonObjectMapperProvider() {
        defaultObjectMapper = createDefaultMapper();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return defaultObjectMapper;
    }


    static class GreenMailUserSerializer extends StdSerializer<GreenMailUser> {

        public GreenMailUserSerializer() {
            super(GreenMailUser.class);
        }

        @Override
        public void serialize(GreenMailUser value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField("login", value.getLogin());
            generator.writeStringField("email", value.getEmail());
            generator.writeEndObject();
        }
    }

    static class GreenMailServerSetupSerializer extends StdSerializer<ServerSetup> {

        public GreenMailServerSetupSerializer() {
            super(ServerSetup.class);
        }

        @Override
        public void serialize(ServerSetup value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeStartObject();
            generator.writeNumberField("port", value.getPort());
            generator.writeStringField("address", value.getBindAddress());
            generator.writeStringField("protocol", value.getProtocol());
            generator.writeBooleanField("isSecure", value.isSecure());
            generator.writeNumberField("readTimeout", value.getReadTimeout());
            generator.writeNumberField("writeTimeout", value.getWriteTimeout());
            generator.writeNumberField("connectionTimeout", value.getConnectionTimeout());
            generator.writeNumberField("serverStartupTimeout", value.getServerStartupTimeout());
            generator.writeBooleanField("isDynamicPort", value.isDynamicPort());
            generator.writeEndObject();
        }
    }

    private static ObjectMapper createDefaultMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(FAIL_ON_EMPTY_BEANS, false);

        SimpleModule module = new SimpleModule();
        module.addSerializer(GreenMailUser.class, new GreenMailUserSerializer());
        module.addSerializer(ServerSetup.class, new GreenMailServerSetupSerializer());
        mapper.registerModule(module);

        return mapper;
    }
}

