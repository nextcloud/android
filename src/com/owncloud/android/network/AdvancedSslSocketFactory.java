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

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
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
    private X509HostnameVerifier mHostnameVerifier;

    /**
     * Constructor for AdvancedSSLProtocolSocketFactory.
     */
    public AdvancedSslSocketFactory(SSLContext sslContext, X509HostnameVerifier hostnameVerifier) {
        if (sslContext == null)
            throw new IllegalArgumentException("AdvancedSslSocketFactory can not be created with a null SSLContext");
        mSslContext = sslContext;
        mHostnameVerifier = hostnameVerifier;
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException, UnknownHostException {
        Socket socket = mSslContext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
        verifyHostname(host, socket);
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
        verifyHostname(host, socket);
        return socket;
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port) throws IOException,
            UnknownHostException {
        Log.d(TAG, "Creating SSL Socket with remote " + host + ":" + port);
        Socket socket = mSslContext.getSocketFactory().createSocket(host, port);
        verifyHostname(host, socket);
        return socket; 
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
     */
    /*public Socket createSocket(Socket socket, String host, int port,
            boolean autoClose) throws IOException, UnknownHostException {
        Log.d(TAG, "Creating SSL Socket from other shocket " + socket + " to remote " + host + ":" + port);
        return getSSLContext().getSocketFactory().createSocket(socket, host,
                port, autoClose);
    }*/

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
     * Verifies the host name with the content of the server certificate using the current host name verifier, if some
     * @param socket
     */
    private void verifyHostname(String host, Socket socket) throws IOException {
        if (mHostnameVerifier != null) {
            try {
                mHostnameVerifier.verify(host, (SSLSocket) socket);
            } catch (IOException iox) {
                try {
                    socket.close();
                } catch (Exception x) {}
                throw iox;
            }
        }
    }
    
}