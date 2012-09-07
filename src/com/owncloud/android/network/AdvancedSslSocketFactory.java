/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509TrustManager;

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
            IOException failInHandshake = null;
            /// 1. VERIFY THE SERVER CERTIFICATE through the registered TrustManager (that should be an instance of AdvancedX509TrustManager) 
            try {
                SSLSocket sock = (SSLSocket) socket;    // a new SSLSession instance is created as a "side effect" 
                sock.startHandshake();
            } catch (IOException e) {
                failInHandshake = e;
                if (!(e.getCause() instanceof CertificateCombinedException)) {
                    throw e;
                }
            }
            
            /// 2. VERIFY HOSTNAME
            SSLSession newSession = null;
            boolean verifiedHostname = true;
            if (mHostnameVerifier != null) {
                if (failInHandshake != null) {
                    /// 2.1 : a new SSLSession instance was NOT created in the handshake
                    X509Certificate serverCert = ((CertificateCombinedException)failInHandshake.getCause()).getServerCertificate();
                    try {
                        mHostnameVerifier.verify(host, serverCert);
                    } catch (SSLException e) {
                        verifiedHostname = false;
                    }
                
                } else {
                    /// 2.2 : a new SSLSession instance was created in the handshake
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                        /// this is sure ONLY for Android >= 3.0 ; the same is true for mHostnameVerifier.verify(host, (SSLSocket)socket)
                        newSession = ((SSLSocket)socket).getSession();
                        if (!mTrustManager.isKnownServer((X509Certificate)(newSession.getPeerCertificates()[0])))
                            verifiedHostname = mHostnameVerifier.verify(host, newSession); 
                    
                    } else {
                        //// performing the previous verification in Android versions under 2.3.x  (and we don't know the exact value of x) WILL BREAK THE SSL CONTEXT, and any HTTP operation executed later through the socket WILL FAIL ; 
                        //// it is related with A BUG IN THE OpenSSLSOcketImpl.java IN THE ANDROID CORE SYSTEM; it was fixed here:
                        ////        http://gitorious.org/ginger/libcore/blobs/df349b3eaf4d1fa0643ab722173bc3bf20a266f5/luni/src/main/java/org/apache/harmony/xnet/provider/jsse/OpenSSLSocketImpl.java
                        ///  but we could not find out in what Android version was released the bug fix;
                        ///
                        /// besides, due to the bug, calling ((SSLSocket)socket).getSession() IS NOT SAFE ; the next workaround is an UGLY BUT SAFE solution to get it
                        SSLSessionContext sessionContext = mSslContext.getClientSessionContext();
                        if (sessionContext  != null) {
                            SSLSession session = null;
                            synchronized(sessionContext) {  // a SSLSession in the SSLSessionContext can be closed while we are searching for the new one; it happens; really
                                Enumeration<byte[]> ids = sessionContext.getIds();
                                while (ids.hasMoreElements()) {
                                    session = sessionContext.getSession(ids.nextElement());
                                    if (    session.getPeerHost().equals(host) && 
                                            session.getPeerPort() == port && 
                                            (newSession == null || newSession.getCreationTime() < session.getCreationTime())) {
                                        newSession = session;
                                    }
                               }
                            }
                            if (newSession != null) {
                                if (!mTrustManager.isKnownServer((X509Certificate)(newSession.getPeerCertificates()[0]))) {
                                    verifiedHostname = mHostnameVerifier.verify(host, newSession);
                                }
                            } else {
                                Log.d(TAG, "Hostname verification could not be performed because the new SSLSession was not found");
                            }
                        }
                    }
                }
            }

            /// 3. Combine the exceptions to throw, if any
            if (failInHandshake != null) {
                if (!verifiedHostname) {
                    ((CertificateCombinedException)failInHandshake.getCause()).setSslPeerUnverifiedException(new SSLPeerUnverifiedException(host));
                }
                throw failInHandshake;
            } else if (!verifiedHostname) {
                CertificateCombinedException ce = new CertificateCombinedException((X509Certificate) newSession.getPeerCertificates()[0]);
                SSLPeerUnverifiedException pue = new SSLPeerUnverifiedException(host);
                ce.setSslPeerUnverifiedException(pue);
                pue.initCause(ce);
                throw pue;
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
