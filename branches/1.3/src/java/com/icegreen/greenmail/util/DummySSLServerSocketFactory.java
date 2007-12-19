/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
*
*/
package com.icegreen.greenmail.util;

import javax.net.ServerSocketFactory;
import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;


/**
 * DummySSLServerSocketFactory - NOT SECURE
 * Contains a hardcoded memory store for convenience in testing by avoiding having to manually setup, install, and generate keystore / keys
 *
 * @author Wael Chatila
 * @since Feb 2006
 */
public class DummySSLServerSocketFactory extends SSLServerSocketFactory {
    private SSLServerSocketFactory factory;

    public DummySSLServerSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            String defaultAlg = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory km = KeyManagerFactory.getInstance(defaultAlg);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] pass = "changeit".toString().toCharArray();
            ks.load(new ByteArrayInputStream(hardCodedKeystore), pass);
            km.init(ks, pass);
            KeyManager[] kma = km.getKeyManagers();
            sslcontext.init(kma,
                    new TrustManager[]{new DummyTrustManager()},
                    null);
            factory = (SSLServerSocketFactory) sslcontext.getServerSocketFactory();
//            factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private SSLServerSocket addAnonCipher(ServerSocket socket) {
        SSLServerSocket ssl = (SSLServerSocket) socket;
        final String[] ciphers = ssl.getEnabledCipherSuites();
        final String[] anonCiphers = { "SSL_DH_anon_WITH_RC4_128_MD5"
                                       , "SSL_DH_anon_WITH_RC4_128_MD5"
                                       , "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA"
                                       , "SSL_DH_anon_WITH_DES_CBC_SHA"
                                       , "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5"
                                       , "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA" };
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

    private static byte[] hardCodedKeystore = new byte[]{-2, -19, -2, -19, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 1, 0, 8, 105, 99, 101, 103, 114, 101, 101, 110, 0, 0, 1, 9, 77, 96, -11, -114, 0, 0, 2, -71, 48, -126, 2, -75, 48, 14, 6, 10, 43, 6, 1, 4, 1, 42, 2, 17, 1, 1, 5, 0, 4, -126, 2, -95, -32, -63, -7, 87, -14, 62, 94, 25, -18, 67, -101, -103, -88, -31, 72, -121, -6, 97, 44, 33, -48, 24, -98, -19, -101, 117, 7, -116, -117, 70, -116, -51, 83, -124, 37, -24, -67, 75, -98, 115, 52, -45, -88, 49, 82, 22, -48, -110, -45, -78, 61, 14, 63, 77, -92, -65, -108, 72, -42, -92, -44, 81, -43, 112, 73, 75, -25, 13, 46, 14, 19, 16, 33, 91, -122, 4, 112, -79, -23, 60, 39, 54, -6, 34, -44, -115, 55, 57, -28, 127, -62, -66, 19, -87, 60, 23, -22, 44, -91, 54, 118, -58, 18, 83, 12, -43, -39, 113, 44, -36, 95, -54, 69, 18, 115, 6, 28, 75, 120, 120, 30, 75, 92, 119, -84, 119, 26, 44, -52, 0, -3, -46, -85, 88, -117, -32, 30, -16, -102, 125, -75, -85, 56, -67, 26, -48, -72, 35, 83, 87, 100, -127, 10, 76, 43, 87, -71, -101, 14, 22, -97, 124, 93, 54, -32, -57, 18, 73, 60, 98, -61, 20, 37, 10, -74, -5, 2, 26, 60, -61, 69, 94, -72, -23, -25, -16, -114, 110, -2, 34, 54, 59, 103, 41, -127, 93, -82, -113, 118, -58, -32, 0, 15, 95, 95, 52, -102, -111, 35, -17, -101, 49, 123, 3, -30, 85, -82, -53, -30, -65, -91, 101, 68, 101, -110, -11, 73, -51, -23, 90, 40, 4, -11, -111, 93, 87, -9, -3, 48, 121, -96, -80, -121, -127, 109, 113, 104, -26, 68, 92, -18, -109, 42, -6, 53, -62, 54, 127, 100, -77, 43, -122, 6, 24, 106, -29, 109, -33, 101, 1, -87, 9, -50, 68, 54, -100, -128, 15, -49, 45, -57, -7, -16, 2, -24, 1, 85, -17, -16, -77, 39, 95, -14, 0, 83, 126, -42, 90, -75, 88, 56, 32, 38, 98, 67, 74, -9, 49, 0, 113, -95, 63, -68, -21, -97, -117, 21, -108, 112, -1, 11, -2, -69, -109, -55, -106, 35, -126, 34, 73, 18, 15, -39, -81, 114, 38, -34, 108, -120, -45, -108, 109, 83, -76, 61, -86, 52, 21, 51, 86, -74, 18, 89, -25, -101, -16, -26, 75, -45, -19, -54, -118, 51, 116, -104, 59, 43, -2, 6, -82, 107, -84, 72, -7, -67, 67, -36, -118, 93, -34, 101, -75, 61, -59, 77, -128, 87, -3, 13, 19, -109, 110, 115, 10, 117, -77, -71, 53, 37, -107, 29, 38, -97, 106, 123, 34, -48, 76, -57, 63, 71, 44, -111, 39, -81, 22, -93, -96, 97, -83, -100, -8, 72, -9, 99, -124, -125, 111, -90, 4, -91, 104, 108, -7, -4, 35, 60, 90, 72, -96, -76, 78, 40, 69, 121, -28, -107, -84, -13, -17, -12, 44, 50, -16, -16, 69, -111, 61, 3, 50, -65, -126, 3, -108, -110, 29, 38, -41, 16, -70, -11, -47, -31, -62, -99, 64, -95, 32, -10, 39, 12, 74, 110, -107, -60, 16, -12, -67, 43, -106, -29, 67, -20, 73, -117, -4, 11, -81, 67, -110, 6, -56, -60, -15, -51, -41, 121, -2, -125, 13, 15, 64, -66, -58, -99, -14, 118, -69, -20, -53, 9, 27, 15, 89, 62, -46, 34, 98, 103, 41, 10, 89, 19, -4, -87, 107, -75, -80, -65, -82, -114, 60, 49, -69, 57, -75, 24, 126, 120, -35, -78, 40, -107, -98, 122, 16, -1, 93, -60, 36, 99, -104, 27, -42, -53, -65, 36, 24, -87, -126, -46, -99, -94, -67, -78, -112, -42, 46, -8, -70, 103, -31, 107, -37, 94, 61, 78, 76, -7, 75, 92, -110, 104, -42, -7, -3, 51, -82, 34, 55, 29, 30, -118, 100, -34, 86, -6, 81, 65, 7, -30, 41, -42, -99, -103, 11, -37, 88, 104, 38, -12, -114, 86, 88, 16, 23, 46, 74, 14, -18, 72, -93, 80, -20, 36, 44, -67, -84, -86, -92, 37, -12, -35, 65, -121, -46, -1, -6, -126, 12, -40, -74, -59, 92, 74, 71, -14, 21, 105, -18, 4, -119, -128, 61, 61, -114, -24, -106, -63, -65, 89, -40, 99, 0, 0, 0, 1, 0, 5, 88, 46, 53, 48, 57, 0, 0, 2, 78, 48, -126, 2, 74, 48, -126, 1, -77, 2, 4, 67, -22, -39, 83, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 4, 5, 0, 48, 108, 49, 16, 48, 14, 6, 3, 85, 4, 6, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 8, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 7, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 10, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 11, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 3, 19, 7, 85, 110, 107, 110, 111, 119, 110, 48, 30, 23, 13, 48, 54, 48, 50, 48, 57, 48, 53, 53, 53, 51, 49, 90, 23, 13, 48, 54, 48, 53, 49, 48, 48, 53, 53, 53, 51, 49, 90, 48, 108, 49, 16, 48, 14, 6, 3, 85, 4, 6, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 8, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 7, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 10, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 11, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 3, 19, 7, 85, 110, 107, 110, 111, 119, 110, 48, -127, -97, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -127, -115, 0, 48, -127, -119, 2, -127, -127, 0, -81, 83, -88, -35, -61, -91, 16, 53, 107, -45, -117, -18, 22, -6, -119, -117, -85, -51, 65, -65, 0, -112, -10, -22, 110, -57, -13, -106, -85, -32, -110, 32, 107, 91, 57, 2, -51, -49, -113, 102, -109, -90, 87, -112, 11, 84, -10, 114, 60, -52, 33, 70, 58, 56, -106, 76, -44, 0, -32, 77, -45, -27, -120, -53, 8, 118, -108, 85, 74, 82, 60, 68, -57, 61, 52, -107, 16, 6, 44, 71, -60, -22, -14, -113, 105, -42, 76, 41, -11, 2, 98, 33, -45, 78, 124, 10, 6, 30, 82, 113, -105, -115, 29, 24, 89, -20, 37, 127, -102, -118, -74, -114, -105, 111, 43, 112, 124, 60, -67, 56, 106, -96, -11, -99, -5, 72, -52, -45, 2, 3, 1, 0, 1, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 4, 5, 0, 3, -127, -127, 0, 40, -92, -6, -125, 61, -112, -107, 48, 57, -120, -95, -34, -127, 58, -117, -117, 90, -64, -68, 33, 102, 112, 59, 81, -104, 6, 76, 91, -17, -119, -72, -40, -16, 27, 22, 68, -124, 109, -67, 35, 114, 105, -37, 71, -48, -24, -45, 23, -90, -90, 3, -38, 82, -116, -45, 57, -22, 56, 66, -40, 62, -117, -100, -4, -71, -112, 58, 48, -45, -120, -18, 39, 94, -123, 82, -125, 0, -97, 48, 117, -26, 76, -95, -104, 33, -14, -4, 39, 93, 50, 95, -104, -128, 65, -25, -11, 48, -90, -30, -7, 127, 25, -96, 94, -111, -89, 27, -21, -50, -119, 105, -22, 72, -95, 37, 70, 22, -87, 15, 17, -109, 29, -2, 50, 84, -22, -96, -121, 34, 26, -65, -56, -17, -127, 90, -10, 18, 124, 55, -54, 98, 88, -110, 94, -43, -117, 59, 98};
}