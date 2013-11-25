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

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSocket;

import android.util.Log;


/**
 * Enables the support of Server Name Indication if existing 
 * in the underlying network implementation.
 * 
 * Build as a singleton.
 * 
 * @author David A. Velasco
 */
public class ServerNameIndicator {
	
	private static final String TAG = ServerNameIndicator.class.getSimpleName();
	
	private static final AtomicReference<ServerNameIndicator> mSingleInstance = new AtomicReference<ServerNameIndicator>();
	
	private static final String METHOD_NAME = "setHostname";
	
	private final WeakReference<Class<?>> mSSLSocketClassRef;
	private final WeakReference<Method> mSetHostnameMethodRef;
	
	
	/**
	 * Private constructor, class is a singleton.
	 * 
	 * @param sslSocketClass		Underlying implementation class of {@link SSLSocket} used to connect with the server. 
	 * @param setHostnameMethod		Name of the method to call to enable the SNI support.
	 */
	private ServerNameIndicator(Class<?> sslSocketClass, Method setHostnameMethod) {
		mSSLSocketClassRef = new WeakReference<Class<?>>(sslSocketClass);
		mSetHostnameMethodRef = (setHostnameMethod == null) ? null : new WeakReference<Method>(setHostnameMethod);
	}
	
	
	/**
	 * Calls the {@code #setHostname(String)} method of the underlying implementation 
	 * of {@link SSLSocket} if exists.
	 * 
	 * Creates and initializes the single instance of the class when needed
	 *
	 * @param hostname 		The name of the server host of interest.
	 * @param sslSocket 	Client socket to connect with the server.
	 */
	public static void setServerNameIndication(String hostname, SSLSocket sslSocket) {
		final Method setHostnameMethod = getMethod(sslSocket);
		if (setHostnameMethod != null) {
			try {
				setHostnameMethod.invoke(sslSocket, hostname);
				Log.i(TAG, "SNI done, hostname: " + hostname);
				
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "Call to SSLSocket#setHost(String) failed ", e);
				
			} catch (IllegalAccessException e) {
				Log.e(TAG, "Call to SSLSocket#setHost(String) failed ", e);
				
			} catch (InvocationTargetException e) {
				Log.e(TAG, "Call to SSLSocket#setHost(String) failed ", e);
			}
		} else {
			Log.i(TAG, "SNI not supported");
		}
	}

	
	/**
	 * Gets the method to invoke trying to minimize the effective 
	 * application of reflection.
	 * 
	 * @param 	sslSocket		Instance of the SSL socket to use in connection with server.
	 * @return					Method to call to indicate the server name of interest to the server.
	 */
	private static Method getMethod(SSLSocket sslSocket) {
		final Class<?> sslSocketClass = sslSocket.getClass();
		final ServerNameIndicator instance = mSingleInstance.get();
		if (instance == null) {
			return initFrom(sslSocketClass);
			
		} else if (instance.mSSLSocketClassRef.get() != sslSocketClass) {
			// the underlying class changed
			return initFrom(sslSocketClass);
				
		} else if (instance.mSetHostnameMethodRef == null) {
			// SNI not supported
			return null;
				
		} else {
			final Method cachedSetHostnameMethod = instance.mSetHostnameMethodRef.get();
			return (cachedSetHostnameMethod == null) ? initFrom(sslSocketClass) : cachedSetHostnameMethod;
		}
	}


	/**
	 * Singleton initializer.
	 * 
	 * Uses reflection to extract and 'cache' the method to invoke to indicate the desited host name to the server side.
	 *  
	 * @param 	sslSocketClass		Underlying class providing the implementation of {@link SSLSocket}.
	 * @return						Method to call to indicate the server name of interest to the server.
	 */
	private static Method initFrom(Class<?> sslSocketClass) {
        Log.i(TAG, "SSLSocket implementation: " + sslSocketClass.getCanonicalName());
		Method setHostnameMethod = null;
		try {
			setHostnameMethod = sslSocketClass.getMethod(METHOD_NAME, String.class);
		} catch (SecurityException e) {
			Log.e(TAG, "Could not access to SSLSocket#setHostname(String) method ", e);
			
		} catch (NoSuchMethodException e) {
			Log.i(TAG, "Could not find SSLSocket#setHostname(String) method - SNI not supported");
		}
		mSingleInstance.set(new ServerNameIndicator(sslSocketClass, setHostnameMethod));
		return setHostnameMethod;
	}

}
