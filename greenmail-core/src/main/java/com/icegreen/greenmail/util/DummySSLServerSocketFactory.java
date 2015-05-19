/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;


/**
 * DummySSLServerSocketFactory - NOT SECURE
 *
 * Contains a preconfigured key store for convenience in testing by avoiding
 * having to manually setup, install, and generate keystore / keys.
 *
 * By default, the factory loads the resource <code>greenmail.jks</code> from classpath.
 * GreenMail provides the keystore resource. For customization, place your greenmail.jks before greenmail JAR in the classpath.
 *
 * @author Wael Chatila
 * @since Feb 2006
 */
public class DummySSLServerSocketFactory extends SSLServerSocketFactory {
    private static final Logger log = LoggerFactory.getLogger(DummySSLServerSocketFactory.class);
    private SSLServerSocketFactory factory;
    private KeyStore ks;

    public DummySSLServerSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            String defaultAlg = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory km = KeyManagerFactory.getInstance(defaultAlg);
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] pass = "changeit".toCharArray();
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("greenmail.jks");
            try {
                ks.load(is, pass);
            }
            catch (IOException ex) {
                // Try hard coded default keystore
                log.warn("Can not load greenmail keystore from 'greenmail.jks' in classpath. Falling back to hard coded keystore.", ex);
                ks.load(new ByteArrayInputStream(HARD_CODED_KEY_STORE), pass);
            }
            finally {
                is.close();
            }
            km.init(ks, pass);
            KeyManager[] kma = km.getKeyManagers();
            sslcontext.init(kma,
                            new TrustManager[]{new DummyTrustManager()},
                            null);
            factory = sslcontext.getServerSocketFactory();
//            factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        } catch (Exception e) {
            log.error("Can not create and initialize SSL", e);
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
//        return SSLServerSocketFactory.getDefault();
        return new DummySSLServerSocketFactory();
    }

    public ServerSocket createServerSocket() throws IOException {
        return addAnonCipher(factory.createServerSocket());
    }

    public ServerSocket createServerSocket(int i) throws IOException {
        return addAnonCipher(factory.createServerSocket(i));
    }

    public ServerSocket createServerSocket(int i, int i1) throws IOException {
        return addAnonCipher(factory.createServerSocket(i, i1));
    }

    public ServerSocket createServerSocket(int i, int i1, InetAddress inetAddress) throws IOException {
        return addAnonCipher(factory.createServerSocket(i, i1, inetAddress));
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    public KeyStore getKeyStore() {
        return ks;
    }

    private static final byte[] HARD_CODED_KEY_STORE = new byte[]{
            -2, -19, -2, -19, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 1, 0, 9, 103, 114, 101, 101, 110, 109, 97, 105, 108, 0, 0, 1, 38, 28, 67, 53, -102, 0, 0, 1, -113, 48, -126, 1, -117, 48, 14, 6, 10, 43, 6, 1, 4, 1, 42, 2, 17, 1, 1, 5, 0, 4, -126, 1, 119, -65, -113, 44, 108, 62, -29, 29, -112, 29, -59, -27, 74, 86, -1, -59, -38, 34, -55, -122, 7, 92, -124, -9, -82, -14, -17, -14, -86, -75, 51, 31, -10, 25, -106, 49, 48, 120, -10, 102, -128, -68, 80, -96, 64, 17, -43, 20, -90, 20, 95, 103, -30, -26, -115, -81, -37, -101, 7, 24, 60, 114, 116, -55, -8, -68, -77, -24, 63, 3, -38, 58, -66, -17, 59, 68, 104, -44, 96, 102, -48, -20, 13, 85, -20, -20, 69, -97, -39, 116, -103, 82, 67, 91, 64, -101, 117, -77, -83, -24, -111, 119, 62, 71, 1, -67, -27, 53, 30, 39, -36, -58, -76, -8, 101, 65, 46, 116, 82, -14, -67, -45, 87, -37, -4, -102, -41, 37, -128, -8, -17, -15, -67, -32, 127, -45, 39, 111, 104, -64, 89, -45, -120, -93, -24, 50, 110, 13, -91, 14, -84, 127, -122, 30, -88, -101, -53, -23, 29, -39, 54, -30, 24, 76, 82, -84, -41, -3, 60, 103, 48, -77, 114, 12, -43, 57, -78, 70, -63, 20, -46, 44, -60, -4, -100, -51, -33, 101, -105, 109, -40, 124, -111, -117, -99, 36, -103, -90, 45, -86, 26, 5, -68, -67, 2, -58, -32, -106, 64, -102, -74, -37, 114, 112, -4, -68, -16, -98, -39, 123, 92, 15, -57, -90, -49, -84, 50, -85, -10, -40, 104, 79, 92, 40, 44, 38, -96, -75, 50, 23, 41, -106, -104, -77, 123, 117, -87, 13, 83, 64, 71, -55, -40, -84, -103, -127, -60, 119, -90, 17, 40, -91, 115, -73, 107, 94, -88, -11, 33, 80, 121, -47, -29, 72, 16, 28, -58, -81, 23, 17, 92, 99, 61, -29, 99, -94, -127, -21, 19, -45, 5, -48, -80, -51, -1, -81, -6, -15, 84, 46, 102, 56, 102, -31, -37, -4, -114, -102, 64, 72, -24, -30, -89, 38, -94, 121, 15, -22, 11, 89, 36, -37, -100, 100, 43, -91, -95, 18, 17, -107, 86, -101, 8, -11, 12, -112, -81, 113, 10, -15, -74, -81, -38, -28, -28, 15, 114, 51, -49, 29, 19, -2, 103, -48, 92, -56, 35, 112, -50, 28, 34, -110, 65, 13, -9, -29, -108, 106, 46, 118, -80, -108, -55, 31, 45, -72, 0, 0, 0, 1, 0, 5, 88, 46, 53, 48, 57, 0, 0, 3, 33, 48, -126, 3, 29, 48, -126, 2, -38, -96, 3, 2, 1, 2, 2, 4, 75, 74, -47, -77, 48, 11, 6, 7, 42, -122, 72, -50, 56, 4, 3, 5, 0, 48, 113, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 85, 83, 49, 30, 48, 28, 6, 3, 85, 4, 10, 19, 21, 73, 99, 101, 103, 114, 101, 101, 110, 32, 84, 101, 99, 104, 110, 111, 108, 111, 103, 105, 101, 115, 49, 18, 48, 16, 6, 3, 85, 4, 11, 19, 9, 71, 114, 101, 101, 110, 77, 97, 105, 108, 49, 46, 48, 44, 6, 3, 85, 4, 3, 19, 37, 71, 114, 101, 101, 110, 77, 97, 105, 108, 32, 115, 101, 108, 102, 115, 105, 103, 110, 101, 100, 32, 84, 101, 115, 116, 32, 67, 101, 114, 116, 105, 102, 105, 99, 97, 116, 101, 48, 30, 23, 13, 49, 48, 48, 49, 49, 49, 48, 55, 50, 50, 50, 55, 90, 23, 13, 49, 51, 48, 49, 49, 48, 48, 55, 50, 50, 50, 55, 90, 48, 113, 49, 11, 48, 9, 6, 3, 85, 4, 6, 19, 2, 85, 83, 49, 30, 48, 28, 6, 3, 85, 4, 10, 19, 21, 73, 99, 101, 103, 114, 101, 101, 110, 32, 84, 101, 99, 104, 110, 111, 108, 111, 103, 105, 101, 115, 49, 18, 48, 16, 6, 3, 85, 4, 11, 19, 9, 71, 114, 101, 101, 110, 77, 97, 105, 108, 49, 46, 48, 44, 6, 3, 85, 4, 3, 19, 37, 71, 114, 101, 101, 110, 77, 97, 105, 108, 32, 115, 101, 108, 102, 115, 105, 103, 110, 101, 100, 32, 84, 101, 115, 116, 32, 67, 101, 114, 116, 105, 102, 105, 99, 97, 116, 101, 48, -126, 1, -72, 48, -126, 1, 44, 6, 7, 42, -122, 72, -50, 56, 4, 1, 48, -126, 1, 31, 2, -127, -127, 0, -3, 127, 83, -127, 29, 117, 18, 41, 82, -33, 74, -100, 46, -20, -28, -25, -10, 17, -73, 82, 60, -17, 68, 0, -61, 30, 63, -128, -74, 81, 38, 105, 69, 93, 64, 34, 81, -5, 89, 61, -115, 88, -6, -65, -59, -11, -70, 48, -10, -53, -101, 85, 108, -41, -127, 59, -128, 29, 52, 111, -14, 102, 96, -73, 107, -103, 80, -91, -92, -97, -97, -24, 4, 123, 16, 34, -62, 79, -69, -87, -41, -2, -73, -58, 27, -8, 59, 87, -25, -58, -88, -90, 21, 15, 4, -5, -125, -10, -45, -59, 30, -61, 2, 53, 84, 19, 90, 22, -111, 50, -10, 117, -13, -82, 43, 97, -41, 42, -17, -14, 34, 3, 25, -99, -47, 72, 1, -57, 2, 21, 0, -105, 96, 80, -113, 21, 35, 11, -52, -78, -110, -71, -126, -94, -21, -124, 11, -16, 88, 28, -11, 2, -127, -127, 0, -9, -31, -96, -123, -42, -101, 61, -34, -53, -68, -85, 92, 54, -72, 87, -71, 121, -108, -81, -69, -6, 58, -22, -126, -7, 87, 76, 11, 61, 7, -126, 103, 81, 89, 87, -114, -70, -44, 89, 79, -26, 113, 7, 16, -127, -128, -76, 73, 22, 113, 35, -24, 76, 40, 22, 19, -73, -49, 9, 50, -116, -56, -90, -31, 60, 22, 122, -117, 84, 124, -115, 40, -32, -93, -82, 30, 43, -77, -90, 117, -111, 110, -93, 127, 11, -6, 33, 53, 98, -15, -5, 98, 122, 1, 36, 59, -52, -92, -15, -66, -88, 81, -112, -119, -88, -125, -33, -31, 90, -27, -97, 6, -110, -117, 102, 94, -128, 123, 85, 37, 100, 1, 76, 59, -2, -49, 73, 42, 3, -127, -123, 0, 2, -127, -127, 0, -62, 126, 81, 35, 24, -63, -86, 79, 108, 123, 117, 44, 113, 29, -109, -104, -9, 101, 106, 119, -31, 50, 55, -121, 16, 67, -30, -18, -85, 100, 52, -36, 40, -28, 109, -66, 0, 125, -90, -39, 74, 56, -84, -22, 23, 37, 41, 94, 15, 32, -95, 89, -124, 7, 32, -41, 21, 3, -16, -48, -78, -85, -51, -35, 68, 6, 42, 117, 41, 32, 99, -60, -51, -99, -116, -124, -66, 107, 73, -47, -50, -103, 20, -94, -44, 89, -8, -51, 77, -46, -61, 72, -82, -126, 97, 92, 103, 24, -50, -44, 124, -25, -11, 36, -76, -12, -30, 111, 25, -82, 119, 0, 82, -23, -14, 95, -29, -88, -112, -47, -56, -118, 71, -127, -106, 109, 121, 41, 48, 11, 6, 7, 42, -122, 72, -50, 56, 4, 3, 5, 0, 3, 48, 0, 48, 45, 2, 20, 103, -112, 78, -53, 120, 28, -100, -2, 52, 105, 27, -11, 22, 66, -121, 60, 68, -109, -49, -33, 2, 21, 0, -114, -60, 97, 40, -86, 127, -123, -41, -55, 56, -119, -81, -44, 89, 40, -80, 75, -115, -80, 3, -88, 63, -13, 6, 117, 19, -94, 3, 50, -23, -80, 1, -40, 43, 98, -126, -91, 97, 46, -43
    };
}
