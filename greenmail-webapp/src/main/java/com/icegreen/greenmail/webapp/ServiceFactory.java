/**
 * DESC
 *
 * @author mm
 */
package com.icegreen.greenmail.webapp;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Service;

import java.util.ArrayList;
import java.util.List;

public class ServiceFactory {
    private ServiceFactory() {
    }

    public static List<Service> create(final Configuration pConf,
                                       final Managers pManagers) {
        List<Configuration.ServiceConfiguration> seviceConfigs = pConf.getServiceConfigurations();
        List<Service> services = new ArrayList<>(seviceConfigs.size());
        for (Configuration.ServiceConfiguration serviceConf : seviceConfigs) {
            services.add(create(pConf, serviceConf, pManagers));
        }
        return services;
    }

    public static Service create(final Configuration pConf,
                                 final Configuration.ServiceConfiguration pServiceConfiguration,
                                 final Managers pManagers) {
        Service service;
        if (Protocol.SMTP.equals(pServiceConfiguration.protocol)) {
            service = new SmtpServer(new ServerSetup(getPort(pConf, pServiceConfiguration),
                getHost(pConf, pServiceConfiguration),
                ServerSetup.PROTOCOL_SMTP),
                pManagers
            );
        } else if (Protocol.SMTPS.equals(pServiceConfiguration.protocol)) {
            service = new SmtpServer(new ServerSetup(getPort(pConf, pServiceConfiguration),
                getHost(pConf, pServiceConfiguration),
                ServerSetup.PROTOCOL_SMTPS),
                pManagers
            );
        } else if (Protocol.POP3.equals(pServiceConfiguration.protocol)) {
            service = new Pop3Server(new ServerSetup(getPort(pConf, pServiceConfiguration),
                getHost(pConf, pServiceConfiguration),
                ServerSetup.PROTOCOL_POP3),
                pManagers
            );
        } else if (Protocol.POP3S.equals(pServiceConfiguration.protocol)) {
            service = new Pop3Server(new ServerSetup(getPort(pConf, pServiceConfiguration),
                getHost(pConf, pServiceConfiguration),
                ServerSetup.PROTOCOL_POP3S),
                pManagers
            );
        } else if (Protocol.IMAP.equals(pServiceConfiguration.protocol)) {
            service = new ImapServer(new ServerSetup(getPort(pConf, pServiceConfiguration),
                getHost(pConf, pServiceConfiguration),
                ServerSetup.PROTOCOL_IMAP),
                pManagers
            );
        } else if (Protocol.IMAPS.equals(pServiceConfiguration.protocol)) {
            service = new ImapServer(new ServerSetup(getPort(pConf, pServiceConfiguration),
                getHost(pConf, pServiceConfiguration),
                ServerSetup.PROTOCOL_IMAPS),
                pManagers
            );
        } else {
            throw new IllegalArgumentException(
                "Can not handle protocol " + pServiceConfiguration.protocol.toString());
        }
        return service;
    }

    private static String getHost(final Configuration pConf,
                                  final Configuration.ServiceConfiguration pServiceConfiguration) {
        String host = pServiceConfiguration.hostname;
        if (null == host || host.length() == 0) {
            host = pConf.getDefaultHostname();
        }
        return host;
    }

    private static int getPort(final Configuration pConf,
                               final Configuration.ServiceConfiguration pServiceConfiguration) {
        int port = pServiceConfiguration.port;
        if (port == 0) {
            port = pServiceConfiguration.protocol.port + pConf.getPortOffset();
        }
        return port;
    }
}
