package com.icegreen.greenmail.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Handles the GreenMail namespace.
 *
 * @author Marcel May (mm)
 */
public class GreenMailNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        registerBeanDefinitionParser("greenmail", new GreenMailBeanDefinitionParser());
    }
}
