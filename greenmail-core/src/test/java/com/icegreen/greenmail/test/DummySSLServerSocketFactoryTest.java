package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import org.junit.Test;

import java.security.KeyStore;
import java.security.KeyStoreException;

import static org.assertj.core.api.Assertions.assertThat;

public class DummySSLServerSocketFactoryTest {
    @Test
    public void testKeyStore() throws KeyStoreException {
        DummySSLServerSocketFactory factory = new DummySSLServerSocketFactory();
        KeyStore ks = factory.getKeyStore();
        assertThat(ks.containsAlias("greenmail")).isTrue();
    }
}
