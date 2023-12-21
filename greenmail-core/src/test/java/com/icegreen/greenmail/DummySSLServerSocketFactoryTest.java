package com.icegreen.greenmail;

import com.icegreen.greenmail.util.DummySSLServerSocketFactory;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
    @BeforeClass
    public static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @AfterClass
    public static void tearDown() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @After
    public void cleanup() {
        System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_FILE_PROPERTY);
        System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEYSTORE_PASSWORD_PROPERTY);
        System.clearProperty(DummySSLServerSocketFactory.GREENMAIL_KEY_PASSWORD_PROPERTY);
    }

    @Test
    public void testLoadDefaultKeyStore() throws KeyStoreException {
        DummySSLServerSocketFactory factory = new DummySSLServerSocketFactory();
        KeyStore ks = factory.getKeyStore();
        assertThat(ks.containsAlias("greenmail")).isTrue();
    }

    @Test
    public void testLoadKeyStoreViaSystemPropertyWithDefaultKeyPwd()
        throws GeneralSecurityException, IOException, OperatorCreationException {
        testLoadKeyStoreViaSystemProperty("store password", null);
    }

    @Test
    public void testLoadKeyStoreViaSystemPropertyWithProvidedKeyPwd()
        throws GeneralSecurityException, IOException, OperatorCreationException {
        testLoadKeyStoreViaSystemProperty("store password", "key password");
    }

    public void testLoadKeyStoreViaSystemProperty(String storePassword, String keyPassword)
        throws GeneralSecurityException, IOException, OperatorCreationException {
        // Prepare new keystore
        KeyStore testKs = KeyStore.getInstance(KeyStore.getDefaultType());
        testKs.load(null, null); // Initialize

        // Create key and certificate
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String dn = "CN=greenmail.test";
        final X509Certificate cert = generateCertificate(dn, keyPair, 1, "SHA256WithRSA");
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
     * Based on <a href="https://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java">https://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java</a>
     *
     * @param dn               the X.509 Distinguished Name
     * @param pair             the KeyPair
     * @param days             how many days till expiration
     * @param signingAlgorithm the signing algorithm, e.g. "SHA256WithRSA"
     */
    public X509Certificate generateCertificate(String dn, KeyPair pair, int days, String signingAlgorithm)
        throws GeneralSecurityException, IOException, OperatorCreationException {
        Instant now = Instant.now();
        X500Name issuer = new X500Name(dn);

        final BcX509ExtensionUtils bcX509ExtensionUtils = new BcX509ExtensionUtils();
        final SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded());
        final X509v3CertificateBuilder x509v3CertificateBuilder = new JcaX509v3CertificateBuilder(issuer,
            new BigInteger(64, new SecureRandom()),
            Date.from(now),
            Date.from(now.plus(Duration.ofDays(days))),
            issuer,
            pair.getPublic())
            .addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
            .addExtension(Extension.subjectKeyIdentifier, false,
                bcX509ExtensionUtils.createSubjectKeyIdentifier(subjectPublicKeyInfo))
            .addExtension(Extension.authorityKeyIdentifier, false,
                bcX509ExtensionUtils.createAuthorityKeyIdentifier(subjectPublicKeyInfo));

        final X509CertificateHolder certificateHolder = x509v3CertificateBuilder.build(
            new JcaContentSignerBuilder(signingAlgorithm).build(pair.getPrivate()));
        return new JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(certificateHolder);
    }
}
