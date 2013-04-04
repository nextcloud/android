/* ownCloud Android client application
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
package com.owncloud.android.operations;

import android.os.Handler;

import eu.alefzero.webdav.WebdavClient;

/**
 * Operation which execution involves one or several interactions with an ownCloud server.
 * 
 * Provides methods to execute the operation both synchronously or asynchronously.
 * 
 * @author David A. Velasco 
 */
public abstract class RemoteOperation implements Runnable {
	
	/** Object to interact with the ownCloud server */
	private WebdavClient mClient = null;
	
	/** Callback object to notify about the execution of the remote operation */
	private OnRemoteOperationListener mListener = null;
	
	/** Handler to the thread where mListener methods will be called */
	private Handler mListenerHandler = null;

	
	/**
	 *  Abstract method to implement the operation in derived classes.
	 */
	protected abstract RemoteOperationResult run(WebdavClient client); 
	
	
	/**
	 * Synchronously executes the remote operation
	 * 
	 * @param client	Client object to reach an ownCloud server during the execution of the operation.
	 * @return			Result of the operation.
	 */
	public final RemoteOperationResult execute(WebdavClient client) {
		if (client == null)
			throw new IllegalArgumentException("Trying to execute a remote operation with a NULL WebdavClient");
		mClient = client;
		return run(client);
	}

	
	/**
	 * Asynchronously executes the remote operation
	 * 
	 * @param client			Client object to reach an ownCloud server during the execution of the operation.
	 * @param listener			Listener to be notified about the execution of the operation.
	 * @param listenerHandler	Handler associated to the thread where the methods of the listener objects must be called.
	 * @return					Thread were the remote operation is executed.
	 */
	public final Thread execute(WebdavClient client, OnRemoteOperationListener listener, Handler listenerHandler) {
		if (client == null) {
			throw new IllegalArgumentException("Trying to execute a remote operation with a NULL WebdavClient");
		}
		mClient = client;
		
		if (listener == null) {
			throw new IllegalArgumentException("Trying to execute a remote operation asynchronously without a listener to notiy the result");
		}
		mListener = listener;
		
		if (listenerHandler == null) {
			throw new IllegalArgumentException("Trying to execute a remote operation asynchronously without a handler to the listener's thread");
		}
		mListenerHandler = listenerHandler;
		
		Thread runnerThread = new Thread(this);
		runnerThread.start();
		return runnerThread;
	}
	
    /**
     * Synchronously retries the remote operation using the same WebdavClient in the last call to {@link RemoteOperation#execute(WebdavClient)}
     * 
     * @param listener          Listener to be notified about the execution of the operation.
     * @param listenerHandler   Handler associated to the thread where the methods of the listener objects must be called.
     * @return                  Thread were the remote operation is executed.
     */
    public final RemoteOperationResult retry() {
        return execute(mClient);
    }
    
    /**
     * Asynchronously retries the remote operation using the same WebdavClient in the last call to {@link RemoteOperation#execute(WebdavClient, OnRemoteOperationListener, Handler)}
     * 
     * @param listener          Listener to be notified about the execution of the operation.
     * @param listenerHandler   Handler associated to the thread where the methods of the listener objects must be called.
     * @return                  Thread were the remote operation is executed.
     */
    public final Thread retry(OnRemoteOperationListener listener, Handler listenerHandler) {
        return execute(mClient, listener, listenerHandler);
    }
	
	
	/**
	 * Asynchronous execution of the operation 
	 * started by {@link RemoteOperation#execute(WebdavClient, OnRemoteOperationListener, Handler)}, 
	 * and result posting.
	 */
    @Override
    public final void run() {
    	final RemoteOperationResult result = execute(mClient);
    	
        if (mListenerHandler != null && mListener != null) {
        	mListenerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onRemoteOperationFinish(RemoteOperation.this, result);
                }
            });
        }
    }
	
	
}
