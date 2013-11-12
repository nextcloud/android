/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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
package com.owncloud.android.oc_framework.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import android.content.Context;
import android.util.Log;

public class NetworkUtils {
    
    final private static String TAG = NetworkUtils.class.getSimpleName();
    
    /** Default timeout for waiting data from the server */
    public static final int DEFAULT_DATA_TIMEOUT = 60000;
    
    /** Default timeout for establishing a connection */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;

    /** Connection manager for all the WebdavClients */
    private static MultiThreadedHttpConnectionManager mConnManager = null;
    
    private static Protocol mDefaultHttpsProtocol = null;

    private static AdvancedSslSocketFactory mAdvancedSslSocketFactory = null;

    private static X509HostnameVerifier mHostnameVerifier = null;
    
    
    /**
     * Registers or unregisters the proper components for advanced SSL handling.
     * @throws IOException 
     */
    public static void registerAdvancedSslContext(boolean register, Context context) throws GeneralSecurityException, IOException {
        Protocol pr = null;
        try {
            pr = Protocol.getProtocol("https");
            if (pr != null && mDefaultHttpsProtocol == null) {
                mDefaultHttpsProtocol = pr;
            }
        } catch (IllegalStateException e) {
            // nothing to do here; really
        }
        boolean isRegistered = (pr != null && pr.getSocketFactory() instanceof AdvancedSslSocketFactory);
        if (register && !isRegistered) {
            Protocol.registerProtocol("https", new Protocol("https", getAdvancedSslSocketFactory(context), 443));
            
        } else if (!register && isRegistered) {
            if (mDefaultHttpsProtocol != null) {
                Protocol.registerProtocol("https", mDefaultHttpsProtocol);
            }
        }
    }
    
    public static AdvancedSslSocketFactory getAdvancedSslSocketFactory(Context context) throws GeneralSecurityException, IOException {
        if (mAdvancedSslSocketFactory  == null) {
            KeyStore trustStore = getKnownServersStore(context);
            AdvancedX509TrustManager trustMgr = new AdvancedX509TrustManager(trustStore);
            TrustManager[] tms = new TrustManager[] { trustMgr };
                
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tms, null);
                    
            mHostnameVerifier = new BrowserCompatHostnameVerifier();
            mAdvancedSslSocketFactory = new AdvancedSslSocketFactory(sslContext, trustMgr, mHostnameVerifier);
        }
        return mAdvancedSslSocketFactory;
    }


    private static String LOCAL_TRUSTSTORE_FILENAME = "knownServers.bks";
    
    private static String LOCAL_TRUSTSTORE_PASSWORD = "password";

    private static KeyStore mKnownServersStore = null;
    
    /**
     * Returns the local store of reliable server certificates, explicitly accepted by the user.
     * 
     * Returns a KeyStore instance with empty content if the local store was never created.
     * 
     * Loads the store from the storage environment if needed.
     * 
     * @param context                       Android context where the operation is being performed.
     * @return                              KeyStore instance with explicitly-accepted server certificates. 
     * @throws KeyStoreException            When the KeyStore instance could not be created.
     * @throws IOException                  When an existing local trust store could not be loaded.
     * @throws NoSuchAlgorithmException     When the existing local trust store was saved with an unsupported algorithm.
     * @throws CertificateException         When an exception occurred while loading the certificates from the local trust store.
     */
    private static KeyStore getKnownServersStore(Context context) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (mKnownServersStore == null) {
            //mKnownServersStore = KeyStore.getInstance("BKS");
            mKnownServersStore = KeyStore.getInstance(KeyStore.getDefaultType());
            File localTrustStoreFile = new File(context.getFilesDir(), LOCAL_TRUSTSTORE_FILENAME);
            Log.d(TAG, "Searching known-servers store at " + localTrustStoreFile.getAbsolutePath());
            if (localTrustStoreFile.exists()) {
                InputStream in = new FileInputStream(localTrustStoreFile);
                try {
                    mKnownServersStore.load(in, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
                } finally {
                    in.close();
                }
            } else {
                mKnownServersStore.load(null, LOCAL_TRUSTSTORE_PASSWORD.toCharArray()); // necessary to initialize an empty KeyStore instance
            }
        }
        return mKnownServersStore;
    }
    
    
    public static void addCertToKnownServersStore(Certificate cert, Context context) throws  KeyStoreException, NoSuchAlgorithmException, 
                                                                                            CertificateException, IOException {
        KeyStore knownServers = getKnownServersStore(context);
        knownServers.setCertificateEntry(Integer.toString(cert.hashCode()), cert);
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(LOCAL_TRUSTSTORE_FILENAME, Context.MODE_PRIVATE);
            knownServers.store(fos, LOCAL_TRUSTSTORE_PASSWORD.toCharArray());
        } finally {
            fos.close();
        }
    }
    
    
    static public MultiThreadedHttpConnectionManager getMultiThreadedConnManager() {
        if (mConnManager == null) {
            mConnManager = new MultiThreadedHttpConnectionManager();
            mConnManager.getParams().setDefaultMaxConnectionsPerHost(5);
            mConnManager.getParams().setMaxTotalConnections(5);
        }
        return mConnManager;
    }


}
