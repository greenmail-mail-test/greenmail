/*
* Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the Apache license 2.0
*
*/
package com.icegreen.greenmail.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

/**
 * DummySSLSocketFactory - NOT SECURE
 */
public class DummySSLSocketFactory extends SSLSocketFactory {
    protected static final Logger log = LoggerFactory.getLogger(DummySSLSocketFactory.class);
    private SSLSocketFactory factory;

    public DummySSLSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null,
                    new TrustManager[]{new DummyTrustManager()},
                    null);
            factory = sslcontext.getSocketFactory();
        } catch (Exception ex) {
            log.error("Can not create and initialize SSL", ex);
            throw new IllegalStateException("Can not create and initialize SSL", ex);
        }
    }

    public static SocketFactory getDefault() {
        return new DummySSLSocketFactory();
    }

    private Socket addAnonCipher(Socket socket) {
        SSLSocket ssl = (SSLSocket) socket;
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

    public Socket createSocket()
            throws IOException {
        final Socket socket = factory.createSocket();
        trySetFakeRemoteHost(socket);
        return addAnonCipher(socket);
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

    /**
     * We set the host name of the remote machine because otherwise the SSL implementation is going to try
     * to try to do a reverse lookup to find out the host name for the host which is really slow.
     * Of course we don't know the host name of the remote machine so we just set a fake host name that is unique.
     * <p/>
     * This forces the SSL stack to do key negociation every time we connect to a host but is still much faster
     * than doing the reverse hostname lookup. The negociation is caused by the fact that the SSL stack remembers
     * a trust relationship with a host. If we connect to the same host twice this relationship is reused. Since
     * we set the host name to a random value this reuse never happens.
     *
     * @param socket Socket to set host on
     */
    private static void trySetFakeRemoteHost(Socket socket) {
        try {
            final Method setHostMethod = socket.getClass().getMethod("setHost", String.class);
            String fakeHostName = "greenmailHost" + new BigInteger(130, new Random()).toString(32);
            setHostMethod.invoke(socket, fakeHostName);
        } catch (NoSuchMethodException e) {
            log.debug("Could not set fake remote host. SSL connection setup may be slow.");
        } catch (InvocationTargetException e) {
            log.debug("Could not set fake remote host. SSL connection setup may be slow.");
        } catch (IllegalAccessException e) {
            log.debug("Could not set fake remote host. SSL connection setup may be slow.");
        }
    }
}