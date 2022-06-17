package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static org.assertj.core.api.Assertions.assertThat;

public class DummySSLServerSocketFactoryTest {
    @Test
    public void testLoadDefaultKeyStore() throws KeyStoreException {
        DummySSLServerSocketFactory factory = new DummySSLServerSocketFactory();
        KeyStore ks = factory.getKeyStore();
        assertThat(ks.containsAlias("greenmail")).isTrue();
    }

    @Test
    public void testLoadKeyStoreViaSystemProperty() throws KeyStoreException, CertificateException,
        IOException, NoSuchAlgorithmException {
        // Load default KS, as re-using an existing cert is easier than creating one for testing
        KeyStore systemKs = KeyStore.getInstance(KeyStore.getDefaultType());
        try (final FileInputStream stream = new FileInputStream(
            System.getProperty("java.home") + "/lib/security/cacerts")) {
            systemKs.load(stream, "changeit".toCharArray());
        }

        // Prepare new keystore
        KeyStore testKs = KeyStore.getInstance(KeyStore.getDefaultType());
        testKs.load(null, null); // Initialize

        // Create dummy entry
        String testAlias = "greenmail-testLoadKeyStoreViaSystemProperty-alias";
        String baseAlias = systemKs.aliases().nextElement(); // Any alias is fine
        final Certificate testCert = systemKs.getCertificate(baseAlias);
        assertThat(testCert).isNotNull();
        testKs.setCertificateEntry(testAlias, testCert);

        // Save to file
        String password = "some password";
        final String filename = "testLoadKeyStoreViaSystemProperty." + testKs.getType();
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            testKs.store(fos, password.toCharArray());
        }

        try {
            System.setProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_FILE_PROPERTY, filename);
            System.setProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_PASSWORD_PROPERTY, password);

            // Check if loaded
            DummySSLServerSocketFactory factory = new DummySSLServerSocketFactory();
            KeyStore ks = factory.getKeyStore();
            assertThat(ks.containsAlias(testAlias)).isTrue();
        } finally {
            System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_FILE_PROPERTY);
            System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_PASSWORD_PROPERTY);
        }
    }
}
