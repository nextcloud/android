/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import android.util.Log;

/**
 * AdvancedSSLProtocolSocketFactory allows to create SSL {@link Socket}s with 
 * a custom SSLContext and an optional Hostname Verifier.
 * 
 * @author David A. Velasco
 */

public class AdvancedSslSocketFactory implements ProtocolSocketFactory {

    private static final String TAG = AdvancedSslSocketFactory.class.getSimpleName();
    
    private SSLContext mSslContext = null;
    private AdvancedX509TrustManager mTrustManager = null;
    private X509HostnameVerifier mHostnameVerifier = null;

    public SSLContext getSslContext() {
        return mSslContext;
    }
    
    /**
     * Constructor for AdvancedSSLProtocolSocketFactory.
     */
    public AdvancedSslSocketFactory(SSLContext sslContext, AdvancedX509TrustManager trustManager, X509HostnameVerifier hostnameVerifier) {
        if (sslContext == null)
            throw new IllegalArgumentException("AdvancedSslSocketFactory can not be created with a null SSLContext");
        if (trustManager == null)
            throw new IllegalArgumentException("AdvancedSslSocketFactory can not be created with a null Trust Manager");
        mSslContext = sslContext;
        mTrustManager = trustManager;
        mHostnameVerifier = hostnameVerifier;
    }

    /**
     * @see ProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException, UnknownHostException {
        Socket socket = mSslContext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
        verifyPeerIdentity(host, port, socket);
        return socket;
    }

    
    /**
     * Attempts to get a new socket connection to the given host within the
     * given time limit.
     * 
     * @param host the host name/IP
     * @param port the port on the host
     * @param clientHost the local host name/IP to bind the socket to
     * @param clientPort the port on the local machine
     * @param params {@link HttpConnectionParams Http connection parameters}
     * 
     * @return Socket a new socket
     * 
     * @throws IOException if an I/O error occurs while creating the socket
     * @throws UnknownHostException if the IP address of the host cannot be
     *             determined
     */
    public Socket createSocket(final String host, final int port,
            final InetAddress localAddress, final int localPort,
            final HttpConnectionParams params) throws IOException,
            UnknownHostException, ConnectTimeoutException {
        Log.d(TAG, "Creating SSL Socket with remote " + host + ":" + port + ", local " + localAddress + ":" + localPort + ", params: " + params);
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        } 
        int timeout = params.getConnectionTimeout();
        SocketFactory socketfactory = mSslContext.getSocketFactory();
        Log.d(TAG, " ... with connection timeout " + timeout + " and socket timeout " + params.getSoTimeout());
        Socket socket = socketfactory.createSocket();
        SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
        SocketAddress remoteaddr = new InetSocketAddress(host, port);
        socket.setSoTimeout(params.getSoTimeout());
        socket.bind(localaddr);
        socket.connect(remoteaddr, timeout);
        verifyPeerIdentity(host, port, socket);
        return socket;
    }

    /**
     * @see ProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port) throws IOException,
            UnknownHostException {
        Log.d(TAG, "Creating SSL Socket with remote " + host + ":" + port);
        Socket socket = mSslContext.getSocketFactory().createSocket(host, port);
        verifyPeerIdentity(host, port, socket);
        return socket; 
    }

    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(
                AdvancedSslSocketFactory.class));
    }

    public int hashCode() {
        return AdvancedSslSocketFactory.class.hashCode();
    }


    public X509HostnameVerifier getHostNameVerifier() {
        return mHostnameVerifier;
    }
    
    
    public void setHostNameVerifier(X509HostnameVerifier hostnameVerifier) {
        mHostnameVerifier = hostnameVerifier;
    }
    
    /**
     * Verifies the identity of the server. 
     * 
     * The server certificate is verified first.
     * 
     * Then, the host name is compared with the content of the server certificate using the current host name verifier, if any.
     * @param socket
     */
    private void verifyPeerIdentity(String host, int port, Socket socket) throws IOException {
        try {
            CertificateCombinedException failInHandshake = null;
            /// 1. VERIFY THE SERVER CERTIFICATE through the registered TrustManager (that should be an instance of AdvancedX509TrustManager) 
            try {
                SSLSocket sock = (SSLSocket) socket;    // a new SSLSession instance is created as a "side effect" 
                sock.startHandshake();
                
            } catch (RuntimeException e) {
                
                if (e instanceof CertificateCombinedException) {
                    failInHandshake = (CertificateCombinedException) e;
                } else {
                    Throwable cause = e.getCause();
                    Throwable previousCause = null;
                    while (cause != null && cause != previousCause && !(cause instanceof CertificateCombinedException)) {
                        previousCause = cause;
                        cause = cause.getCause();
                    }
                    if (cause != null && cause instanceof CertificateCombinedException) {
                        failInHandshake = (CertificateCombinedException)cause;
                    }
                }
                if (failInHandshake == null) {
                    throw e;
                }
                failInHandshake.setHostInUrl(host);
                
            }
            
            /// 2. VERIFY HOSTNAME
            SSLSession newSession = null;
            boolean verifiedHostname = true;
            if (mHostnameVerifier != null) {
                if (failInHandshake != null) {
                    /// 2.1 : a new SSLSession instance was NOT created in the handshake
                    X509Certificate serverCert = failInHandshake.getServerCertificate();
                    try {
                        mHostnameVerifier.verify(host, serverCert);
                    } catch (SSLException e) {
                        verifiedHostname = false;
                    }
                
                } else {
                    /// 2.2 : a new SSLSession instance was created in the handshake
                    newSession = ((SSLSocket)socket).getSession();
                    if (!mTrustManager.isKnownServer((X509Certificate)(newSession.getPeerCertificates()[0]))) {
                        verifiedHostname = mHostnameVerifier.verify(host, newSession); 
                    }
                }
            }

            /// 3. Combine the exceptions to throw, if any
            if (!verifiedHostname) {
                SSLPeerUnverifiedException pue = new SSLPeerUnverifiedException("Names in the server certificate do not match to " + host + " in the URL");
                if (failInHandshake == null) {
                    failInHandshake = new CertificateCombinedException((X509Certificate) newSession.getPeerCertificates()[0]);
                    failInHandshake.setHostInUrl(host);
                }
                failInHandshake.setSslPeerUnverifiedException(pue);
                pue.initCause(failInHandshake);
                throw pue;
                
            } else if (failInHandshake != null) {
                SSLHandshakeException hse = new SSLHandshakeException("Server certificate could not be verified");
                hse.initCause(failInHandshake);
                throw hse;
            }
            
        } catch (IOException io) {        
            try {
                socket.close();
            } catch (Exception x) {
                // NOTHING - irrelevant exception for the caller 
            }
            throw io;
        }
    }

}
