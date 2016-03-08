package com.icegreen.greenmail.spring;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parses the GreenMail bean definition.
 *
 * @author Marcel May (mm)
 */
public class GreenMailBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    /** The default hostname ({@value}). */
    public static final String DEFAULT_HOSTNAME = "localhost";
    /** The default port offset ({@value}). */
    private static final Integer DEFAULT_PORT_OFFSET = Integer.valueOf(3000);

    /** {@inheritDoc} */
    @Override
    protected Class<?> getBeanClass(final Element element) {
        return GreenMailBean.class;
    }

    /** {@inheritDoc} */
    @Override
    protected void doParse(final Element element, final BeanDefinitionBuilder builder) {
        builder.addPropertyValue("hostname", extractHostname(element));
        builder.addPropertyValue("portOffset", extractPortOffset(element));
    }

    private Object extractPortOffset(final Element pElement) {
        Element portOffsetElement = DomUtils.getChildElementByTagName(pElement, "portOffset");
        if(null!=portOffsetElement) {
            return portOffsetElement.getTextContent();
        }
        return DEFAULT_PORT_OFFSET;
    }

    private Object extractHostname(final Element pElement) {
        Element hostnameElement = DomUtils.getChildElementByTagName(pElement, "hostname");
        if(null!=hostnameElement) {
            return hostnameElement.getTextContent();
        }
        return DEFAULT_HOSTNAME;
    }
}
