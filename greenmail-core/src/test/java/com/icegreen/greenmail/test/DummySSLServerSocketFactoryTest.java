package com.icegreen.greenmail.test;

import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.mail.Message;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import junit.framework.TestCase;

public class DummySSLServerSocketFactoryTest extends TestCase {
    public void testKeyStore() throws KeyStoreException {
        DummySSLServerSocketFactory factory = new DummySSLServerSocketFactory();
        KeyStore ks = factory.getKeyStore();
        assertTrue(ks.containsAlias("greenmail"));
    }
}
