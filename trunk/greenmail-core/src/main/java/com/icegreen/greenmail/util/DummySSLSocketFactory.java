/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
*
*/
package com.icegreen.greenmail.util;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


/**
 * DummySSLSocketFactory - NOT SECURE
 */
public class DummySSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory;

    public DummySSLSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null,
                    new TrustManager[]{new DummyTrustManager()},
                    null);
            factory = (SSLSocketFactory) sslcontext.getSocketFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public static SocketFactory getDefault() {
        return new DummySSLSocketFactory();
    }

    private Socket addAnonCipher(Socket socket) {
        SSLSocket ssl = (SSLSocket) socket;
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

    public Socket createSocket()
            throws IOException {
        return addAnonCipher(factory.createSocket());
    }

    public Socket createSocket(Socket socket, String s, int i, boolean flag)
            throws IOException {
        return addAnonCipher(factory.createSocket(socket, s, i, flag));
    }


    public Socket createSocket(InetAddress inaddr, int i,
                               InetAddress inaddr1, int j) throws IOException {
        return addAnonCipher(factory.createSocket(inaddr, i, inaddr1, j));
    }

    public Socket createSocket(InetAddress inaddr, int i)
            throws IOException {
        return addAnonCipher(factory.createSocket(inaddr, i));
    }

    public Socket createSocket(String s, int i, InetAddress inaddr, int j)
            throws IOException {
        return addAnonCipher(factory.createSocket(s, i, inaddr, j));
    }

    public Socket createSocket(String s, int i) throws IOException {
        return addAnonCipher(factory.createSocket(s, i));
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
}