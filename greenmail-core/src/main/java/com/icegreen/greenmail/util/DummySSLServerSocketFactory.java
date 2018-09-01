/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import javax.net.ServerSocketFactory;
import javax.net.ssl.*;


/**
 * DummySSLServerSocketFactory - NOT SECURE
 * <p/>
 * Contains a preconfigured key store for convenience in testing by avoiding
 * having to manually setup, install, and generate keystore / keys.
 * <p/>
 * By default, the factory loads the resource <code>greenmail.jks</code> from classpath.
 * GreenMail provides the keystore resource. For customization, place your greenmail.jks before greenmail JAR in the classpath.
 *
 * @author Wael Chatila
 * @since Feb 2006
 */
public class DummySSLServerSocketFactory extends SSLServerSocketFactory {
    public static final String GREENMAIL_JKS = "greenmail.jks";
    private final SSLServerSocketFactory factory;
    private final KeyStore ks;

    public DummySSLServerSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            String defaultAlg = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory km = KeyManagerFactory.getInstance(defaultAlg);
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] pass = "changeit".toCharArray();
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(GREENMAIL_JKS)) {
                ks.load(is, pass);
            } catch (IOException ex) {
                // Try hard coded default keystore
                throw new IllegalStateException("Can not load greenmail keystore from '" + GREENMAIL_JKS +
                        "' in classpath", ex);
            }
            km.init(ks, pass);
            KeyManager[] kma = km.getKeyManagers();
            sslcontext.init(kma,
                    new TrustManager[]{new DummyTrustManager()},
                    null);
            factory = sslcontext.getServerSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Can not create and initialize SSL", e);
        }
    }

    private SSLServerSocket addAnonCipher(ServerSocket socket) {
        SSLServerSocket ssl = (SSLServerSocket) socket;
        final String[] ciphers = ssl.getEnabledCipherSuites();
        final String[] anonCiphers = {"SSL_DH_anon_WITH_RC4_128_MD5"
                , "SSL_DH_anon_WITH_RC4_128_MD5"
                , "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA"
                , "SSL_DH_anon_WITH_DES_CBC_SHA"
                , "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5"
                , "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA"};
        final String[] newCiphers = new String[ciphers.length + anonCiphers.length];
        System.arraycopy(ciphers, 0, newCiphers, 0, ciphers.length);
        System.arraycopy(anonCiphers, 0, newCiphers, ciphers.length, anonCiphers.length);
        ssl.setEnabledCipherSuites(newCiphers);
        return ssl;
    }

    public static ServerSocketFactory getDefault() {
        return new DummySSLServerSocketFactory();
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
