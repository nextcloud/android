/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.services;

import android.accounts.Account;
import android.accounts.AccountsException;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Pair;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.operations.CheckCurrentCredentialsOperation;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.CreateShareWithShareeOperation;
import com.owncloud.android.operations.GetServerInfoOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.OAuth2GetAccessToken;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UnshareOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.operations.common.SyncOperation;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class OperationsService extends Service {

    private static final String TAG = OperationsService.class.getSimpleName();

    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_SERVER_URL = "SERVER_URL";
    public static final String EXTRA_OAUTH2_QUERY_PARAMETERS = "OAUTH2_QUERY_PARAMETERS";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_NEWNAME = "NEWNAME";
    public static final String EXTRA_REMOVE_ONLY_LOCAL = "REMOVE_LOCAL_COPY";
    public static final String EXTRA_CREATE_FULL_PATH = "CREATE_FULL_PATH";
    public static final String EXTRA_SYNC_FILE_CONTENTS = "SYNC_FILE_CONTENTS";
    public static final String EXTRA_RESULT = "RESULT";
    public static final String EXTRA_NEW_PARENT_PATH = "NEW_PARENT_PATH";
    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_SHARE_PASSWORD = "SHARE_PASSWORD";
    public static final String EXTRA_SHARE_TYPE = "SHARE_TYPE";
    public static final String EXTRA_SHARE_WITH = "SHARE_WITH";
    public static final String EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS = "SHARE_EXPIRATION_YEAR";
    public static final String EXTRA_SHARE_PERMISSIONS = "SHARE_PERMISSIONS";
    public static final String EXTRA_SHARE_PUBLIC_UPLOAD = "SHARE_PUBLIC_UPLOAD";
    public static final String EXTRA_SHARE_ID = "SHARE_ID";

    public static final String EXTRA_COOKIE = "COOKIE";

    public static final String ACTION_CREATE_SHARE_VIA_LINK = "CREATE_SHARE_VIA_LINK";
    public static final String ACTION_CREATE_SHARE_WITH_SHAREE = "CREATE_SHARE_WITH_SHAREE";
    public static final String ACTION_UNSHARE = "UNSHARE";
    public static final String ACTION_UPDATE_SHARE = "UPDATE_SHARE";
    public static final String ACTION_GET_SERVER_INFO = "GET_SERVER_INFO";
    public static final String ACTION_OAUTH2_GET_ACCESS_TOKEN = "OAUTH2_GET_ACCESS_TOKEN";
    public static final String ACTION_GET_USER_NAME = "GET_USER_NAME";
    public static final String ACTION_GET_USER_AVATAR = "GET_USER_AVATAR";
    public static final String ACTION_RENAME = "RENAME";
    public static final String ACTION_REMOVE = "REMOVE";
    public static final String ACTION_CREATE_FOLDER = "CREATE_FOLDER";
    public static final String ACTION_SYNC_FILE = "SYNC_FILE";
    public static final String ACTION_SYNC_FOLDER = "SYNC_FOLDER";
    public static final String ACTION_MOVE_FILE = "MOVE_FILE";
    public static final String ACTION_COPY_FILE = "COPY_FILE";
    public static final String ACTION_CHECK_CURRENT_CREDENTIALS = "CHECK_CURRENT_CREDENTIALS";

    public static final String ACTION_OPERATION_ADDED = OperationsService.class.getName() + ".OPERATION_ADDED";
    public static final String ACTION_OPERATION_FINISHED = OperationsService.class.getName() + ".OPERATION_FINISHED";

    private ServiceHandler mOperationsHandler;
    private OperationsServiceBinder mOperationsBinder;

    private SyncFolderHandler mSyncFolderHandler;

    private ConcurrentMap<Integer, Pair<RemoteOperation, RemoteOperationResult>>
            mUndispatchedFinishedOperations = new ConcurrentHashMap<>();

    private static class Target {
        public Uri mServerUrl = null;
        public Account mAccount = null;
        public String mCookie = null;

        public Target(Account account, Uri serverUrl, String cookie) {
            mAccount = account;
            mServerUrl = serverUrl;
            mCookie = cookie;
        }
    }
    
    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log_OC.d(TAG, "Creating service");

        /// First worker thread for most of operations 
        HandlerThread thread = new HandlerThread("Operations thread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mOperationsHandler = new ServiceHandler(thread.getLooper(), this);
        mOperationsBinder = new OperationsServiceBinder(mOperationsHandler);
        
        /// Separated worker thread for download of folders (WIP)
        thread = new HandlerThread("Syncfolder thread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mSyncFolderHandler = new SyncFolderHandler(thread.getLooper(), this);
    }

    /**
     * Entry point to add a new operation to the queue of operations.
     * <p/>
     * New operations are added calling to startService(), resulting in a call to this method.
     * This ensures the service will keep on working although the caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command with id " + startId);

        // WIP: for the moment, only SYNC_FOLDER is expected here;
        // the rest of the operations are requested through the Binder
        if (ACTION_SYNC_FOLDER.equals(intent.getAction())) {

            if (!intent.hasExtra(EXTRA_ACCOUNT) || !intent.hasExtra(EXTRA_REMOTE_PATH)) {
                Log_OC.e(TAG, "Not enough information provided in intent");
                return START_NOT_STICKY;
            }
            Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
            String remotePath = intent.getStringExtra(EXTRA_REMOTE_PATH);

            Pair<Account, String> itemSyncKey =  new Pair<Account , String>(account, remotePath);

            Pair<Target, RemoteOperation> itemToQueue = newOperation(intent);
            if (itemToQueue != null) {
                mSyncFolderHandler.add(account, remotePath,
                        (SynchronizeFolderOperation)itemToQueue.second);
                Message msg = mSyncFolderHandler.obtainMessage();
                msg.arg1 = startId;
                msg.obj = itemSyncKey;
                mSyncFolderHandler.sendMessage(msg);
            }

        } else {
            Message msg = mOperationsHandler.obtainMessage();
            msg.arg1 = startId;
            mOperationsHandler.sendMessage(msg);
        }
        
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "Destroying service" );
        // Saving cookies
        try {
            OwnCloudClientManagerFactory.getDefaultSingleton().
                    saveAllClients(this, MainApp.getAccountType());

            // TODO - get rid of these exceptions
        } catch (AccountNotFoundException | IOException | OperationCanceledException | AuthenticatorException e) {
            Log_OC.d(TAG, e.getMessage(), e);
        }

        mUndispatchedFinishedOperations.clear();

        mOperationsBinder = null;

        mOperationsHandler.getLooper().quit();
        mOperationsHandler = null;

        mSyncFolderHandler.getLooper().quit();
        mSyncFolderHandler = null;

        super.onDestroy();
    }

    /**
     * Provides a binder object that clients can use to perform actions on the queue of operations,
     * except the addition of new operations.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mOperationsBinder;
    }


    /**
     * Called when ALL the bound clients were unbound.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        mOperationsBinder.clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }


    /**
     * Binder to let client components to perform actions on the queue of operations.
     * <p/>
     * It provides by itself the available operations.
     */
    public class OperationsServiceBinder extends Binder /* implements OnRemoteOperationListener */ {

        /**
         * Map of listeners that will be reported about the end of operations from a
         * {@link OperationsServiceBinder} instance
         */
        private final ConcurrentMap<OnRemoteOperationListener, Handler> mBoundListeners =
                new ConcurrentHashMap<OnRemoteOperationListener, Handler>();

        private ServiceHandler mServiceHandler = null;

        public OperationsServiceBinder(ServiceHandler serviceHandler) {
            mServiceHandler = serviceHandler;
        }


        /**
         * Cancels a pending or current synchronization.
         *
         * @param account       ownCloud account where the remote folder is stored.
         * @param file          A folder in the queue of pending synchronizations
         */
        public void cancel(Account account, OCFile file) {
            mSyncFolderHandler.cancel(account, file);
        }


        public void clearListeners() {

            mBoundListeners.clear();
        }


        /**
         * Adds a listener interested in being reported about the end of operations.
         *
         * @param listener          Object to notify about the end of operations.
         * @param callbackHandler   {@link Handler} to access the listener without
         *                                         breaking Android threading protection.
         */
        public void addOperationListener (OnRemoteOperationListener listener,
                                          Handler callbackHandler) {
            synchronized (mBoundListeners) {
                mBoundListeners.put(listener, callbackHandler);
            }
        }


        /**
         * Removes a listener from the list of objects interested in the being reported about
         * the end of operations.
         * 
         * @param listener      Object to notify about progress of transfer.    
         */
        public void removeOperationListener(OnRemoteOperationListener listener) {
            synchronized (mBoundListeners) {
                mBoundListeners.remove(listener);
            }
        }


        /**
         * TODO - IMPORTANT: update implementation when more operations are moved into the service
         *
         * @return  'True' when an operation that enforces the user to wait for completion is
         *          in process.
         */
        public boolean isPerformingBlockingOperation() {
            return (!mServiceHandler.mPendingOperations.isEmpty());
        }


        /**
         * Creates and adds to the queue a new operation, as described by operationIntent.
         * 
         * Calls startService to make the operation is processed by the ServiceHandler.
         * 
         * @param operationIntent       Intent describing a new operation to queue and execute.
         * @return                      Identifier of the operation created, or null if failed.
         */
        public long queueNewOperation(Intent operationIntent) {
            Pair<Target, RemoteOperation> itemToQueue = newOperation(operationIntent);
            if (itemToQueue != null) {
                mServiceHandler.mPendingOperations.add(itemToQueue);
                startService(new Intent(OperationsService.this, OperationsService.class));
                return itemToQueue.second.hashCode();
                
            } else {
                return Long.MAX_VALUE;
            }
        }


        public boolean dispatchResultIfFinished(int operationId,
                                                OnRemoteOperationListener listener) {
            Pair<RemoteOperation, RemoteOperationResult> undispatched = 
                    mUndispatchedFinishedOperations.remove(operationId);
            if (undispatched != null) {
                listener.onRemoteOperationFinish(undispatched.first, undispatched.second);
                return true;
                //Log_OC.e(TAG, "Sending callback later");
            } else {
                return (!mServiceHandler.mPendingOperations.isEmpty());
            }
        }
        
        
        /**
         * Returns True when the file described by 'file' in the ownCloud account 'account' is
         * downloading or waiting to download.
         * 
         * If 'file' is a directory, returns 'true' if some of its descendant files is downloading
         * or waiting to download.
         * 
         * @param account       ownCloud account where the remote file is stored.
         * @param file          File to check if something is synchronizing
         *                      / downloading / uploading inside.
         */
        public boolean isSynchronizing(Account account, OCFile file) {
            return mSyncFolderHandler.isSynchronizing(account, file.getRemotePath());
        }

    }


    /**
     * Operations worker. Performs the pending operations in the order they were requested.
     *
     * Created with the Looper of a new thread, started in {@link OperationsService#onCreate()}. 
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will warn about a p
        // ossible memory leak
        
        
        OperationsService mService;


        private ConcurrentLinkedQueue<Pair<Target, RemoteOperation>> mPendingOperations =
                new ConcurrentLinkedQueue<Pair<Target, RemoteOperation>>();
        private RemoteOperation mCurrentOperation = null;
        private Target mLastTarget = null;
        private OwnCloudClient mOwnCloudClient = null;
        private FileDataStorageManager mStorageManager;
        
        
        public ServiceHandler(Looper looper, OperationsService service) {
            super(looper);
            if (service == null) {
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            }
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            nextOperation();
            Log_OC.d(TAG, "Stopping after command with id " + msg.arg1);
            mService.stopSelf(msg.arg1);
        }

        
        /**
         * Performs the next operation in the queue
         */
        private void nextOperation() {
            
            //Log_OC.e(TAG, "nextOperation init" );
            
            Pair<Target, RemoteOperation> next = null;
            synchronized(mPendingOperations) {
                next = mPendingOperations.peek();
            }

            if (next != null) {
                
                mCurrentOperation = next.second;
                RemoteOperationResult result = null;
                try {
                    /// prepare client object to send the request to the ownCloud server
                    if (mLastTarget == null || !mLastTarget.equals(next.first)) {
                        mLastTarget = next.first;
                        if (mLastTarget.mAccount != null) {
                            OwnCloudAccount ocAccount = new OwnCloudAccount(mLastTarget.mAccount, mService);
                            mOwnCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                                    getClientFor(ocAccount, mService);

                            OwnCloudVersion version = com.owncloud.android.authentication.AccountUtils.getServerVersion(
                                    mLastTarget.mAccount);
                            mOwnCloudClient.setOwnCloudVersion(version);

                            mStorageManager = new FileDataStorageManager(
                                    mLastTarget.mAccount, 
                                    mService.getContentResolver()
                            );
                        } else {
                            OwnCloudCredentials credentials = null;
                            if (mLastTarget.mCookie != null &&
                                    mLastTarget.mCookie.length() > 0) {
                                // just used for GetUserName
                                // TODO refactor to run GetUserName as AsyncTask in the context of
                                // AuthenticatorActivity
                                credentials = OwnCloudCredentialsFactory.newSamlSsoCredentials(
                                        null,                   // unknown
                                        mLastTarget.mCookie);   // SAML SSO
                            }
                            OwnCloudAccount ocAccount = new OwnCloudAccount(
                                    mLastTarget.mServerUrl, credentials);
                            mOwnCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                                    getClientFor(ocAccount, mService);
                            mStorageManager = null;
                        }
                    }

                    /// perform the operation
                    if (mCurrentOperation instanceof SyncOperation) {
                        result = ((SyncOperation)mCurrentOperation).execute(mOwnCloudClient,
                                mStorageManager);
                    } else {
                        result = mCurrentOperation.execute(mOwnCloudClient);
                    }

                } catch (AccountsException e) {
                    if (mLastTarget.mAccount == null) {
                        Log_OC.e(TAG, "Error while trying to get authorization for a NULL account",
                                e);
                    } else {
                        Log_OC.e(TAG, "Error while trying to get authorization for " +
                                mLastTarget.mAccount.name, e);
                    }
                    result = new RemoteOperationResult(e);
                    
                } catch (IOException e) {
                    if (mLastTarget.mAccount == null) {
                        Log_OC.e(TAG, "Error while trying to get authorization for a NULL account",
                                e);
                    } else {
                        Log_OC.e(TAG, "Error while trying to get authorization for " +
                                mLastTarget.mAccount.name, e);
                    }
                    result = new RemoteOperationResult(e);
                } catch (Exception e) {
                    if (mLastTarget.mAccount == null) {
                        Log_OC.e(TAG, "Unexpected error for a NULL account", e);
                    } else {
                        Log_OC.e(TAG, "Unexpected error for " + mLastTarget.mAccount.name, e);
                    }
                    result = new RemoteOperationResult(e);
                
                } finally {
                    synchronized(mPendingOperations) {
                        mPendingOperations.poll();
                    }
                }
                
                //sendBroadcastOperationFinished(mLastTarget, mCurrentOperation, result);
                mService.dispatchResultToOperationListeners(mCurrentOperation, result);
            }
        }



    }


    /**
     * Creates a new operation, as described by operationIntent.
     * 
     * TODO - move to ServiceHandler (probably)
     * 
     * @param operationIntent       Intent describing a new operation to queue and execute.
     * @return                      Pair with the new operation object and the information about its
     *                              target server.
     */
    private Pair<Target , RemoteOperation> newOperation(Intent operationIntent) {
        RemoteOperation operation = null;
        Target target = null;
        try {
            if (!operationIntent.hasExtra(EXTRA_ACCOUNT) && 
                    !operationIntent.hasExtra(EXTRA_SERVER_URL)) {
                Log_OC.e(TAG, "Not enough information provided in intent");

            } else {
                Account account = operationIntent.getParcelableExtra(EXTRA_ACCOUNT);
                String serverUrl = operationIntent.getStringExtra(EXTRA_SERVER_URL);
                String cookie = operationIntent.getStringExtra(EXTRA_COOKIE);
                target = new Target(
                        account, 
                        (serverUrl == null) ? null : Uri.parse(serverUrl),
                        cookie
                );
                
                String action = operationIntent.getAction();

                if (action.equals(ACTION_CREATE_SHARE_VIA_LINK)) {  // Create public share via link
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    String password = operationIntent.getStringExtra(EXTRA_SHARE_PASSWORD);
                    if (remotePath.length() > 0) {
                        operation = new CreateShareViaLinkOperation(
                                remotePath,
                                password
                        );
                    }

                } else if (ACTION_UPDATE_SHARE.equals(action)) {
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    long shareId = operationIntent.getLongExtra(EXTRA_SHARE_ID, -1);
                    if (remotePath != null && remotePath.length() > 0) {
                        operation = new UpdateShareViaLinkOperation(remotePath);

                        String password = operationIntent.getStringExtra(EXTRA_SHARE_PASSWORD);
                        ((UpdateShareViaLinkOperation) operation).setPassword(password);

                        long expirationDate = operationIntent.getLongExtra(
                                EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS,
                                0
                        );
                        ((UpdateShareViaLinkOperation)operation).setExpirationDate(
                                expirationDate
                        );

                        if (operationIntent.hasExtra(EXTRA_SHARE_PUBLIC_UPLOAD)) {
                            ((UpdateShareViaLinkOperation) operation).setPublicUpload(
                                operationIntent.getBooleanExtra(EXTRA_SHARE_PUBLIC_UPLOAD, false)
                            );
                        }

                    } else if (shareId > 0) {
                        operation = new UpdateSharePermissionsOperation(shareId);
                        int permissions = operationIntent.getIntExtra(EXTRA_SHARE_PERMISSIONS, 1);
                        ((UpdateSharePermissionsOperation)operation).setPermissions(permissions);
                    }

                } else if (action.equals(ACTION_CREATE_SHARE_WITH_SHAREE)) {
                    // Create private share with user or group
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    String shareeName = operationIntent.getStringExtra(EXTRA_SHARE_WITH);
                    ShareType shareType = (ShareType) operationIntent.getSerializableExtra(EXTRA_SHARE_TYPE);
                    int permissions = operationIntent.getIntExtra(EXTRA_SHARE_PERMISSIONS, -1);
                    if (remotePath.length() > 0) {
                        operation = new CreateShareWithShareeOperation(
                                remotePath,
                                shareeName,
                                shareType,
                                permissions
                        );
                    }

                } else if (action.equals(ACTION_UNSHARE)) {  // Unshare file
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    ShareType shareType = (ShareType) operationIntent.
                            getSerializableExtra(EXTRA_SHARE_TYPE);
                    String shareWith = operationIntent.getStringExtra(EXTRA_SHARE_WITH);
                    if (remotePath.length() > 0) {
                        operation = new UnshareOperation(
                                remotePath,
                                shareType,
                                shareWith,
                                OperationsService.this
                        );
                    }
                    
                } else if (action.equals(ACTION_GET_SERVER_INFO)) { 
                    // check OC server and get basic information from it
                    operation = new GetServerInfoOperation(serverUrl, OperationsService.this);

                } else if (action.equals(ACTION_OAUTH2_GET_ACCESS_TOKEN)) {
                    /// GET ACCESS TOKEN to the OAuth server
                    String oauth2QueryParameters =
                            operationIntent.getStringExtra(EXTRA_OAUTH2_QUERY_PARAMETERS);
                    operation = new OAuth2GetAccessToken(
                            getString(R.string.oauth2_client_id), 
                            getString(R.string.oauth2_redirect_uri),       
                            getString(R.string.oauth2_grant_type),
                            oauth2QueryParameters);

                } else if (action.equals(ACTION_GET_USER_NAME)) {
                    // Get User Name
                    operation = new GetRemoteUserInfoOperation();
                    
                } else if (action.equals(ACTION_RENAME)) {
                    // Rename file or folder
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    String newName = operationIntent.getStringExtra(EXTRA_NEWNAME);
                    operation = new RenameFileOperation(remotePath, newName);
                    
                } else if (action.equals(ACTION_REMOVE)) {
                    // Remove file or folder
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    boolean onlyLocalCopy = operationIntent.getBooleanExtra(EXTRA_REMOVE_ONLY_LOCAL, false);
                    operation = new RemoveFileOperation(remotePath, onlyLocalCopy, account, getApplicationContext());
                    
                } else if (action.equals(ACTION_CREATE_FOLDER)) {
                    // Create Folder
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    boolean createFullPath = operationIntent.getBooleanExtra(EXTRA_CREATE_FULL_PATH, true);
                    operation = new CreateFolderOperation(remotePath, createFullPath);

                } else if (action.equals(ACTION_SYNC_FILE)) {
                    // Sync file
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    boolean syncFileContents = operationIntent.getBooleanExtra(EXTRA_SYNC_FILE_CONTENTS, true);
                    operation = new SynchronizeFileOperation(remotePath, account, syncFileContents,
                            getApplicationContext());
                    
                } else if (action.equals(ACTION_SYNC_FOLDER)) {
                    // Sync folder (all its descendant files are sync'ed)
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    operation = new SynchronizeFolderOperation(
                            this,                       // TODO remove this dependency from construction time
                            remotePath,
                            account, 
                            System.currentTimeMillis()  // TODO remove this dependency from construction time
                    );

                } else if (action.equals(ACTION_MOVE_FILE)) {
                    // Move file/folder
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    String newParentPath = operationIntent.getStringExtra(EXTRA_NEW_PARENT_PATH);
                    operation = new MoveFileOperation(remotePath, newParentPath);

                } else if (action.equals(ACTION_COPY_FILE)) {
                    // Copy file/folder
                    String remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                    String newParentPath = operationIntent.getStringExtra(EXTRA_NEW_PARENT_PATH);
                    operation = new CopyFileOperation(remotePath, newParentPath);

                } else if (action.equals(ACTION_CHECK_CURRENT_CREDENTIALS)) {
                    // Check validity of currently stored credentials for a given account
                    operation = new CheckCurrentCredentialsOperation(account);
                }
            }
                
        } catch (IllegalArgumentException e) {
            Log_OC.e(TAG, "Bad information provided in intent: " + e.getMessage());
            operation = null;
        }

        if (operation != null) {
            return new Pair<Target , RemoteOperation>(target, operation);  
        } else {
            return null;
        }
    }

    /**
     * Notifies the currently subscribed listeners about the end of an operation.
     *
     * @param operation         Finished operation.
     * @param result            Result of the operation.
     */
    protected void dispatchResultToOperationListeners(
            final RemoteOperation operation, final RemoteOperationResult result
    ) {
        int count = 0;
        Iterator<OnRemoteOperationListener> listeners =
                mOperationsBinder.mBoundListeners.keySet().iterator();
        while (listeners.hasNext()) {
            final OnRemoteOperationListener listener = listeners.next();
            final Handler handler = mOperationsBinder.mBoundListeners.get(listener);
            if (handler != null) { 
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRemoteOperationFinish(operation, result);
                    }
                });
                count += 1;
            }
        }
        if (count == 0) {
            Pair<RemoteOperation, RemoteOperationResult> undispatched = new Pair<>(operation, result);
            mUndispatchedFinishedOperations.put(operation.hashCode(), undispatched);
        }
        Log_OC.d(TAG, "Called " + count + " listeners");
    }
}
