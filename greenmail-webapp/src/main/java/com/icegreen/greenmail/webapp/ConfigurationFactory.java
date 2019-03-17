package com.icegreen.greenmail.webapp;

import java.util.Map;
import java.util.StringTokenizer;

/**
 * Creates a configuration from the servlet context.
 *
 * @author mm
 */
public class ConfigurationFactory {
    private ConfigurationFactory() {
    }

    static Configuration create(final Map<String, String> pParameters) {
        Configuration conf = new Configuration();

        for (Map.Entry<String, String> param : pParameters.entrySet()) {
            if (param.getKey().startsWith("greenmail.")) {
                configure(conf, param.getKey().substring("greenmail.".length()), param.getValue());
            }
        }
        return conf;
    }

    private static void configure(final Configuration pConf,
                                  final String pParamName, final String pValue) {
        // General configuration
        if ("defaultHostname".equals(pParamName)) {
            pConf.setDefaultHostname(pValue);
        } else if ("portOffset".equals(pParamName)) {
            pConf.setPortOffset(Integer.parseInt(pValue));
        } else if ("users".equals(pParamName)) {
            StringTokenizer tokenizer = new StringTokenizer(pValue);
            while (tokenizer.hasMoreElements()) {
                pConf.addUser(createUser(tokenizer.nextToken()));
            }
        }
        // Service configuration: PROTOCOLNAME[.host|.port]
        else {
            Protocol protocol;
            int dotIdx = pParamName.indexOf('.');
            if (dotIdx < 0) { // PROTOCOLNAME
                protocol = Protocol.valueOf(pParamName.toUpperCase());
            } else {
                protocol = Protocol.valueOf(pParamName.substring(0, dotIdx).toUpperCase());
            }
            Configuration.ServiceConfiguration serviceConf =
                    pConf.getServiceConfigurationByProtocol(protocol);
            if (null == serviceConf) {
                serviceConf = new Configuration.ServiceConfiguration();
                serviceConf.protocol = protocol;
                pConf.addServiceConfiguration(serviceConf);
            }

            if (dotIdx >= 0) {
                String hostOrPort = pParamName.substring(dotIdx + 1);
                if ("host".equals(hostOrPort)) {
                    serviceConf.hostname = pValue;
                } else if ("port".equals(hostOrPort)) {
                    serviceConf.port = Integer.parseInt(pValue);
                }
            }
        }
    }

    static Configuration.User createUser(final String pUserText) {
        Configuration.User user = new Configuration.User();

        int posColon = pUserText.indexOf(':');
        int posAt = pUserText.indexOf('@');
        user.login = pUserText.substring(0, posColon);
        user.password = pUserText.substring(posColon + 1, posAt);
        user.email = user.login + '@' + pUserText.substring(posAt + 1);

        return user;
    }
}
