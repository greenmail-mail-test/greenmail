package com.icegreen.greenmail;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import org.junit.After;
import org.junit.Test;
import sun.security.x509.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class DummySSLServerSocketFactoryTest {
    @Test
    public void testLoadDefaultKeyStore() throws KeyStoreException {
        DummySSLServerSocketFactory factory = new DummySSLServerSocketFactory();
        KeyStore ks = factory.getKeyStore();
        assertThat(ks.containsAlias("greenmail")).isTrue();
    }

    @Test
    public void testLoadKeyStoreViaSystemPropertyWithDefaultKeyPwd()
        throws GeneralSecurityException, IOException {
        testLoadKeyStoreViaSystemProperty("store password", null);
    }

    @Test
    public void testLoadKeyStoreViaSystemPropertyWithProvidedKeyPwd()
        throws GeneralSecurityException, IOException {
        testLoadKeyStoreViaSystemProperty("store password", "key password");
    }

    @After
    public void cleanup() {
        System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_FILE_PROPERTY);
        System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_PASSWORD_PROPERTY);
        System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEY_PASSWORD_PROPERTY);
    }

    public void testLoadKeyStoreViaSystemProperty(String storePassword, String keyPassword)
        throws GeneralSecurityException, IOException {
        // Prepare new keystore
        KeyStore testKs = KeyStore.getInstance(KeyStore.getDefaultType());
        testKs.load(null, null); // Initialize

        // Create key and certificate
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String dn = "CN=greenmail.test";
        final X509Certificate cert = generateCertificate(dn, keyPair, 1, AlgorithmId.get("SHA256WithRSA"));
        final String alias = "test-key";
        testKs.setKeyEntry(alias, keyPair.getPrivate(),
            (null != keyPassword ? keyPassword : storePassword).toCharArray(),
            new Certificate[]{cert});

        // Save to file
        final String filename = "testLoadKeyStoreViaSystemProperty." + testKs.getType();
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            testKs.store(fos, storePassword.toCharArray());
        }

        try {
            System.setProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_FILE_PROPERTY, filename);
            System.setProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_PASSWORD_PROPERTY, storePassword);
            if (null != keyPassword) {
                System.setProperty(DummySSLServerSocketFactory.GREENMAIL_KEY_PASSWORD_PROPERTY, keyPassword);
            }

            // Check if loaded
            DummySSLServerSocketFactory factory = new DummySSLServerSocketFactory();
            KeyStore ks = factory.getKeyStore();
            assertThat(ks.containsAlias(alias)).isTrue();
        } finally {
            System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_FILE_PROPERTY);
            System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_PASSWORD_PROPERTY);
        }
    }

    /**
     * Create a self-signed X.509 certificate
     * <p>
     * Based on <a href="https://stackoverflow.com/questions/1615871">https://stackoverflow.com/questions/1615871</a>
     *
     * @param dn   the X.509 Distinguished Name
     * @param pair the KeyPair
     * @param days how many days till expiration
     * @param algo the signing algorithm, eg "SHA256WithRSA"
     */
    public X509Certificate generateCertificate(String dn, KeyPair pair, int days, AlgorithmId algo)
        throws GeneralSecurityException, IOException {
        PrivateKey privateKey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();

        Instant now = Instant.now();
        CertificateValidity interval = new CertificateValidity(
            Date.from(now),
            Date.from(now.plus(Duration.ofDays(days)))
        );

        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER,
            new CertificateSerialNumber(new BigInteger(64, new SecureRandom())));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privateKey, algo.getName());

        // Update the algorithm, and resign.
        algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privateKey, algo.getName());
        return cert;
    }
}
