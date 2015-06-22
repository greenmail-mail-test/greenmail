package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.security.KeyStore;
import java.security.KeyStoreException;

import static org.junit.Assert.assertTrue;

public class DummySSLServerSocketFactoryTest {
    @Test
    @Ignore
    public void testKeyStore() throws KeyStoreException {
        DummySSLServerSocketFactory factory = new DummySSLServerSocketFactory();
        KeyStore ks = factory.getKeyStore();
        assertTrue(ks.containsAlias("greenmail"));
    }
}
