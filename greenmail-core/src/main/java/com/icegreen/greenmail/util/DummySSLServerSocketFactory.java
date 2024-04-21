/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * DummySSLServerSocketFactory - NOT SECURE
 * <p>
 * Contains a preconfigured key store for convenience in testing by avoiding
 * having to manually set up, install, and generate keystore / keys.
 * <p>
 * By default, the factory loads the resource <code>{@value #GREENMAIL_KEYSTORE_P12}</code> from classpath.
 * A fallback to old <code>>{@value #GREENMAIL_KEYSTORE_JKS}</code> exists.
 * <p>
 * The system property {@value #GREENMAIL_KEYSTORE_FILE_PROPERTY} can override the default keystore location.
 * <p>
 * The system property {@value #GREENMAIL_KEYSTORE_PASSWORD_PROPERTY} can override the default keystore password.
 * <p>
 * The system property {@value #GREENMAIL_KEY_PASSWORD_PROPERTY} can override the default key password
 * (defaults to keystore password).
 * <p>
 * GreenMail provides the keystore resource. For customization, place your greenmail.p12 before
 * greenmail JAR in the classpath.
 *
 * @author Wael Chatila
 * @since Feb 2006
 */
public class DummySSLServerSocketFactory extends SSLServerSocketFactory {
    protected final Logger log = LoggerFactory.getLogger(DummySSLServerSocketFactory.class);
    public static final String GREENMAIL_KEYSTORE_FILE_PROPERTY = "greenmail.tls.keystore.file";
    public static final String GREENMAIL_KEYSTORE_PASSWORD_PROPERTY = "greenmail.tls.keystore.password";
    public static final String GREENMAIL_KEY_PASSWORD_PROPERTY = "greenmail.tls.key.password";
    public static final String GREENMAIL_KEYSTORE_P12 = "greenmail.p12";
    public static final String GREENMAIL_KEYSTORE_JKS = "greenmail.jks";
    private final SSLServerSocketFactory factory;
    private final KeyStore ks;

    // From https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html#SupportedCipherSuites
    static final String[] ANONYMOUS_CIPHERS = {
        "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
        "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
        "SSL_DH_anon_WITH_DES_CBC_SHA",
        "SSL_DH_anon_WITH_RC4_128_MD5",
        "TLS_DH_anon_WITH_AES_128_CBC_SHA",
        "TLS_DH_anon_WITH_AES_128_CBC_SHA256",
        "TLS_DH_anon_WITH_AES_256_CBC_SHA",
        "TLS_DH_anon_WITH_AES_256_CBC_SHA256",
        "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_anon_WITH_AES_256_CBC_SHA",
        "TLS_ECDH_anon_WITH_NULL_SHA",
        "TLS_ECDH_anon_WITH_RC4_128_SHA"};

    public DummySSLServerSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            String defaultAlg = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory km = KeyManagerFactory.getInstance(defaultAlg);
            ks = KeyStore.getInstance(KeyStore.getDefaultType());

            char[] pass = System.getProperty(GREENMAIL_KEYSTORE_PASSWORD_PROPERTY, "changeit").toCharArray();
            loadKeyStore(pass);

            String keyPassStr = System.getProperty(GREENMAIL_KEY_PASSWORD_PROPERTY);
            char[] keyPass = keyPassStr != null ? keyPassStr.toCharArray() : pass;
            km.init(ks, keyPass);

            KeyManager[] kma = km.getKeyManagers();
            sslcontext.init(kma, new TrustManager[]{new DummyTrustManager()}, null);
            factory = sslcontext.getServerSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Can not create and initialize SSL", e);
        }
    }

    private void loadKeyStore(char[] pass) throws NoSuchAlgorithmException, CertificateException {
        String keystore = System.getProperty(GREENMAIL_KEYSTORE_FILE_PROPERTY);
        if (null != keystore) {
            loadKeyStore(ks, pass, new File(keystore));
        } else {
            try {
                loadKeyStore(ks, pass, GREENMAIL_KEYSTORE_P12);
            } catch (IllegalStateException ex) {
                // Fallback to legacy JKS keystore
                loadKeyStore(ks, pass, GREENMAIL_KEYSTORE_JKS);
            }
        }
    }

    private void loadKeyStore(KeyStore keyStore, char[] pass, String keystoreResource)
        throws NoSuchAlgorithmException, CertificateException {
        log.debug("Loading keystore from resource {} ...", keystoreResource);
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(keystoreResource)) {
            keyStore.load(is, pass);
        } catch (IOException ex) {
            // Try hard coded default keystore
            throw new IllegalStateException(
                "Can not load greenmail keystore from '" + keystoreResource + "' in classpath", ex);
        }
    }

    private void loadKeyStore(KeyStore keyStore, char[] pass, File keystoreResource)
        throws NoSuchAlgorithmException, CertificateException {
        log.debug("Loading keystore from file {} ...", keystoreResource);
        try (InputStream is = Files.newInputStream(keystoreResource.toPath())) {
            keyStore.load(is, pass);
        } catch (IOException ex) {
            // Try hard coded default keystore
            throw new IllegalStateException("Can not load greenmail keystore from file '" + keystoreResource + "'", ex);
        }
    }

    private SSLServerSocket addAnonCipher(ServerSocket socket) {
        SSLServerSocket ssl = (SSLServerSocket) socket;
        ssl.setEnabledCipherSuites(addAnonCiphers(ssl.getEnabledCipherSuites()));
        return ssl;
    }

    static String[] addAnonCiphers(String[] ciphers) {
        final String[] newCiphers = new String[ciphers.length + ANONYMOUS_CIPHERS.length];
        System.arraycopy(ciphers, 0, newCiphers, 0, ciphers.length);
        System.arraycopy(ANONYMOUS_CIPHERS, 0, newCiphers, ciphers.length, ANONYMOUS_CIPHERS.length);
        return newCiphers;
    }

    private enum Holder {
        INSTANCE;
        final DummySSLServerSocketFactory value = new DummySSLServerSocketFactory();
    }

    public static ServerSocketFactory getDefault() {
        return Holder.INSTANCE.value;
    }

    @Override
    public ServerSocket createServerSocket() throws IOException {
        return addAnonCipher(factory.createServerSocket());
    }

    @Override
    public ServerSocket createServerSocket(int i) throws IOException {
        return addAnonCipher(factory.createServerSocket(i));
    }

    @Override
    public ServerSocket createServerSocket(int i, int i1) throws IOException {
        return addAnonCipher(factory.createServerSocket(i, i1));
    }

    @Override
    public ServerSocket createServerSocket(int i, int i1, InetAddress inetAddress) throws IOException {
        return addAnonCipher(factory.createServerSocket(i, i1, inetAddress));
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    public KeyStore getKeyStore() {
        return ks;
    }
}
