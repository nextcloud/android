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
package com.owncloud.android.operations;

import java.io.IOException;

import org.apache.commons.httpclient.Credentials;

import com.owncloud.android.Log_OC;
import com.owncloud.android.authentication.AccountAuthenticator;
import com.owncloud.android.network.BearerCredentials;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.app.Activity;
import android.content.Context;
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
	
    private static final String TAG = RemoteOperation.class.getSimpleName();

    /** ownCloud account in the remote ownCloud server to operate */
    private Account mAccount = null;
    
    /** Android Application context */
    private Context mContext = null;
    
	/** Object to interact with the remote server */
	private WebdavClient mClient = null;
	
	/** Callback object to notify about the execution of the remote operation */
	private OnRemoteOperationListener mListener = null;
	
	/** Handler to the thread where mListener methods will be called */
	private Handler mListenerHandler = null;

	/** Activity */
    private Activity mCallerActivity;

	
	/**
	 *  Abstract method to implement the operation in derived classes.
	 */
	protected abstract RemoteOperationResult run(WebdavClient client); 
	

    /**
     * Synchronously executes the remote operation on the received ownCloud account.
     * 
     * Do not call this method from the main thread.
     * 
     * This method should be used whenever an ownCloud account is available, instead of {@link #execute(WebdavClient)}. 
     * 
     * @param account   ownCloud account in remote ownCloud server to reach during the execution of the operation.
     * @param context   Android context for the component calling the method.
     * @return          Result of the operation.
     */
    public final RemoteOperationResult execute(Account account, Context context) {
        if (account == null)
            throw new IllegalArgumentException("Trying to execute a remote operation with a NULL Account");
        if (context == null)
            throw new IllegalArgumentException("Trying to execute a remote operation with a NULL Context");
        mAccount = account;
        mContext = context.getApplicationContext();
        try {
            mClient = OwnCloudClientUtils.createOwnCloudClient(mAccount, mContext);
        } catch (Exception e) {
            Log_OC.e(TAG, "Error while trying to access to " + mAccount.name, e);
            return new RemoteOperationResult(e);
        }
        return run(mClient);
    }
    
	
	/**
	 * Synchronously executes the remote operation
	 * 
     * Do not call this method from the main thread.
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
     * This method should be used whenever an ownCloud account is available, instead of {@link #execute(WebdavClient)}. 
     * 
     * @param account           ownCloud account in remote ownCloud server to reach during the execution of the operation.
     * @param context           Android context for the component calling the method.
     * @param listener          Listener to be notified about the execution of the operation.
     * @param listenerHandler   Handler associated to the thread where the methods of the listener objects must be called.
     * @return                  Thread were the remote operation is executed.
     */
    public final Thread execute(Account account, Context context, OnRemoteOperationListener listener, Handler listenerHandler, Activity callerActivity) {
        if (account == null)
            throw new IllegalArgumentException("Trying to execute a remote operation with a NULL Account");
        if (context == null)
            throw new IllegalArgumentException("Trying to execute a remote operation with a NULL Context");
        mAccount = account;
        mContext = context.getApplicationContext();
        mCallerActivity = callerActivity;
        mClient = null;     // the client instance will be created from mAccount and mContext in the runnerThread to create below
        
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
	 * 
	 * TODO refactor && clean the code; now it's a mess
	 */
    @Override
    public final void run() {
        RemoteOperationResult result = null;
        boolean repeat = false;
        do {
            try{
                if (mClient == null) {
                    if (mAccount != null && mContext != null) {
                        if (mCallerActivity != null) {
                            mClient = OwnCloudClientUtils.createOwnCloudClient(mAccount, mContext, mCallerActivity);
                        } else {
                            mClient = OwnCloudClientUtils.createOwnCloudClient(mAccount, mContext);
                        }
                    } else {
                        throw new IllegalStateException("Trying to run a remote operation asynchronously with no client instance or account");
                    }
                }
            
            } catch (IOException e) {
                Log_OC.e(TAG, "Error while trying to access to " + mAccount.name, new AccountsException("I/O exception while trying to authorize the account", e));
                result = new RemoteOperationResult(e);
            
            } catch (AccountsException e) {
                Log_OC.e(TAG, "Error while trying to access to " + mAccount.name, e);
                result = new RemoteOperationResult(e);
            }
    	
            if (result == null)
                result = run(mClient);
        
            repeat = false;
            if (mCallerActivity != null && mAccount != null && mContext != null && !result.isSuccess() &&
//                    (result.getCode() == ResultCode.UNAUTHORIZED || (result.isTemporalRedirection() && result.isIdPRedirection()))) {
                    (result.getCode() == ResultCode.UNAUTHORIZED || result.isIdPRedirection())) {
                /// possible fail due to lack of authorization in an operation performed in foreground
                Credentials cred = mClient.getCredentials();
                String ssoSessionCookie = mClient.getSsoSessionCookie();
                if (cred != null || ssoSessionCookie != null) {
                    /// confirmed : unauthorized operation
                    AccountManager am = AccountManager.get(mContext);
                    boolean bearerAuthorization = (cred != null && cred instanceof BearerCredentials);
                    boolean samlBasedSsoAuthorization = (cred == null && ssoSessionCookie != null);
                    if (bearerAuthorization) {
                        am.invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE, ((BearerCredentials)cred).getAccessToken());
                    } else if (samlBasedSsoAuthorization ) {
                        am.invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE, ssoSessionCookie);
                    } else {
                        am.clearPassword(mAccount);
                    }
                    mClient = null;
                    repeat = true;  // when repeated, the creation of a new OwnCloudClient after erasing the saved credentials will trigger the login activity
                    result = null;
                }
            }
        } while (repeat);
        
        final RemoteOperationResult resultToSend = result;
        if (mListenerHandler != null && mListener != null) {
        	mListenerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onRemoteOperationFinish(RemoteOperation.this, resultToSend);
                }
            });
        }
    }


    /**
     * Returns the current client instance to access the remote server.
     * 
     * @return      Current client instance to access the remote server.
     */
    public final WebdavClient getClient() {
        return mClient;
    }


}
