package com.icegreen.greenmail.webapp;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatically starts and stops GreenMail server upon deployment/undeployment.
 *
 * @author mm
 */
public class GreenMailListener implements ServletContextListener {
    private Logger log = LoggerFactory.getLogger(GreenMailListener.class);
    private Managers managers;
    private List<Service> services;
    private Configuration configuration;
    

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        log.info("Initializing GreenMail");

        managers = new Managers();
        ServletContext ctx = sce.getServletContext();
        configuration = ConfigurationFactory.create(extractParameters(ctx));
        services = ServiceFactory.create(configuration, managers);

        for (Configuration.User user : configuration.getUsers()) {
            GreenMailUser greenMailUser = managers.getUserManager().getUser(user.email);
            if (null == greenMailUser) {
                try {
                    greenMailUser = managers.getUserManager().createUser(
                            user.email, user.login, user.password);
                    greenMailUser.setPassword(user.password);
                } catch (UserException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        for (Service s : services) {
            log.info("Starting GreenMail service: {}", s);
            s.startService();
        }
        // Initialize context with variables to be retrieved by jsps.
        sce.getServletContext().setAttribute(Constants.GM_MANAGERS_ATTRIBUTE_NAME, managers );
        sce.getServletContext().setAttribute(Constants.GM_SERVICES_ATTRIBUTE_NAME, services );
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        log.info("Destroying GreenMail WebApp");
        for (Service s : services) {
            log.info("Stopping GreenMail service: {}", s);
            s.stopService();
        }
        
        sce.getServletContext().removeAttribute(Constants.GM_MANAGERS_ATTRIBUTE_NAME );
        sce.getServletContext().removeAttribute(Constants.GM_SERVICES_ATTRIBUTE_NAME);
    }

    private Map<String, String> extractParameters(ServletContext pServletContext) {
        Enumeration<?> names = pServletContext.getInitParameterNames();
        Map<String, String> parameterMap = new HashMap<>();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            parameterMap.put(name, pServletContext.getInitParameter(name));
        }
        return parameterMap;
    }

    public Managers getManagers() {
        return managers;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public List<Service> getServices() {
        return services;
    }
}
