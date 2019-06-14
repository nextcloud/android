/*
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author David A. Velasco
 *   @author Andy Scherzinger
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2018 Andy Scherzinger
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
 */

package com.owncloud.android.services;

import android.accounts.Account;
import android.accounts.AccountsException;
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
import android.text.TextUtils;
import android.util.Pair;

import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.RestoreFileVersionRemoteOperation;
import com.owncloud.android.lib.resources.files.model.FileVersion;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.operations.CheckCurrentCredentialsOperation;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.CreateShareWithShareeOperation;
import com.owncloud.android.operations.GetServerInfoOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.UnshareOperation;
import com.owncloud.android.operations.UpdateNoteForShareOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.operations.UpdateShareViaLinkOperation;
import com.owncloud.android.operations.common.SyncOperation;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

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
    public static final String EXTRA_FILE_VERSION = "FILE_VERSION";
    public static final String EXTRA_SHARE_PASSWORD = "SHARE_PASSWORD";
    public static final String EXTRA_SHARE_TYPE = "SHARE_TYPE";
    public static final String EXTRA_SHARE_WITH = "SHARE_WITH";
    public static final String EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS = "SHARE_EXPIRATION_YEAR";
    public static final String EXTRA_SHARE_PERMISSIONS = "SHARE_PERMISSIONS";
    public static final String EXTRA_SHARE_PUBLIC_UPLOAD = "SHARE_PUBLIC_UPLOAD";
    public static final String EXTRA_SHARE_HIDE_FILE_DOWNLOAD = "HIDE_FILE_DOWNLOAD";
    public static final String EXTRA_SHARE_ID = "SHARE_ID";
    public static final String EXTRA_SHARE_NOTE = "SHARE_NOTE";
    public static final String EXTRA_IN_BACKGROUND = "IN_BACKGROUND";

    public static final String EXTRA_COOKIE = "COOKIE";

    public static final String ACTION_CREATE_SHARE_VIA_LINK = "CREATE_SHARE_VIA_LINK";
    public static final String ACTION_CREATE_SHARE_WITH_SHAREE = "CREATE_SHARE_WITH_SHAREE";
    public static final String ACTION_UNSHARE = "UNSHARE";
    public static final String ACTION_UPDATE_SHARE = "UPDATE_SHARE";
    public static final String ACTION_UPDATE_SHARE_NOTE = "UPDATE_SHARE_NOTE";
    public static final String ACTION_GET_SERVER_INFO = "GET_SERVER_INFO";
    public static final String ACTION_GET_USER_NAME = "GET_USER_NAME";
    public static final String ACTION_RENAME = "RENAME";
    public static final String ACTION_REMOVE = "REMOVE";
    public static final String ACTION_CREATE_FOLDER = "CREATE_FOLDER";
    public static final String ACTION_SYNC_FILE = "SYNC_FILE";
    public static final String ACTION_SYNC_FOLDER = "SYNC_FOLDER";
    public static final String ACTION_MOVE_FILE = "MOVE_FILE";
    public static final String ACTION_COPY_FILE = "COPY_FILE";
    public static final String ACTION_CHECK_CURRENT_CREDENTIALS = "CHECK_CURRENT_CREDENTIALS";
    public static final String ACTION_RESTORE_VERSION = "RESTORE_VERSION";

    public static final String ACTION_OPERATION_ADDED = OperationsService.class.getName() + ".OPERATION_ADDED";
    public static final String ACTION_OPERATION_FINISHED = OperationsService.class.getName() + ".OPERATION_FINISHED";

    private ServiceHandler mOperationsHandler;
    private OperationsServiceBinder mOperationsBinder;

    private SyncFolderHandler mSyncFolderHandler;

    private ConcurrentMap<Integer, Pair<RemoteOperation, RemoteOperationResult>>
            mUndispatchedFinishedOperations = new ConcurrentHashMap<>();

    @Inject UserAccountManager accountManager;

    private static class Target {
        public Uri mServerUrl;
        public Account mAccount;
        public String mCookie;

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
        AndroidInjection.inject(this);
        Log_OC.d(TAG, "Creating service");

        // First worker thread for most of operations
        HandlerThread thread = new HandlerThread("Operations thread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mOperationsHandler = new ServiceHandler(thread.getLooper(), this, accountManager);
        mOperationsBinder = new OperationsServiceBinder(mOperationsHandler);

        // Separated worker thread for download of folders (WIP)
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
        if (intent != null && ACTION_SYNC_FOLDER.equals(intent.getAction())) {

            if (!intent.hasExtra(EXTRA_ACCOUNT) || !intent.hasExtra(EXTRA_REMOTE_PATH)) {
                Log_OC.e(TAG, "Not enough information provided in intent");
                return START_NOT_STICKY;
            }
            Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
            String remotePath = intent.getStringExtra(EXTRA_REMOTE_PATH);

            Pair<Account, String> itemSyncKey = new Pair<>(account, remotePath);

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
        OwnCloudClientManagerFactory.getDefaultSingleton()
            .saveAllClients(this, MainApp.getAccountType(getApplicationContext()));

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
        private final ConcurrentMap<OnRemoteOperationListener, Handler> mBoundListeners = new ConcurrentHashMap<>();

        private ServiceHandler mServiceHandler;

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
            return !mServiceHandler.mPendingOperations.isEmpty();
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
            } else {
                return !mServiceHandler.mPendingOperations.isEmpty();
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
                new ConcurrentLinkedQueue<>();
        private RemoteOperation mCurrentOperation;
        private Target mLastTarget;
        private OwnCloudClient mOwnCloudClient;
        private FileDataStorageManager mStorageManager;
        private UserAccountManager accountManager;


        public ServiceHandler(Looper looper, OperationsService service, UserAccountManager accountManager) {
            super(looper);
            if (service == null) {
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            }
            mService = service;
            this.accountManager = accountManager;
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

            Pair<Target, RemoteOperation> next;
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

                            OwnCloudVersion version = accountManager.getServerVersion(mLastTarget.mAccount);
                            mOwnCloudClient.setOwnCloudVersion(version);

                            mStorageManager = new FileDataStorageManager(
                                mLastTarget.mAccount,
                                    mService.getContentResolver()
                            );
                        } else {
                            OwnCloudCredentials credentials = null;
                            if (!TextUtils.isEmpty(mLastTarget.mCookie)) {
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
    private Pair<Target, RemoteOperation> newOperation(Intent operationIntent) {
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
                String remotePath;
                String password;
                ShareType shareType;
                String newParentPath;
                long shareId;

                switch (action) {
                    case ACTION_CREATE_SHARE_VIA_LINK:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        password = operationIntent.getStringExtra(EXTRA_SHARE_PASSWORD);
                        if (!TextUtils.isEmpty(remotePath)) {
                            operation = new CreateShareViaLinkOperation(remotePath, password);
                        }
                        break;

                    case ACTION_UPDATE_SHARE:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        shareId = operationIntent.getLongExtra(EXTRA_SHARE_ID, -1);

                        if (!TextUtils.isEmpty(remotePath)) {
                            UpdateShareViaLinkOperation updateLinkOperation = new UpdateShareViaLinkOperation(remotePath);

                            password = operationIntent.getStringExtra(EXTRA_SHARE_PASSWORD);
                            updateLinkOperation.setPassword(password);

                            long expirationDate = operationIntent.getLongExtra(EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS, 0);
                            updateLinkOperation.setExpirationDateInMillis(expirationDate);

                            boolean hideFileDownload = operationIntent.getBooleanExtra(EXTRA_SHARE_HIDE_FILE_DOWNLOAD,
                                false);
                            updateLinkOperation.setHideFileDownload(hideFileDownload);

                            if (operationIntent.hasExtra(EXTRA_SHARE_PUBLIC_UPLOAD)) {
                                if (remotePath.endsWith("/")) {
                                    updateLinkOperation.setPublicUploadOnFolder(
                                        operationIntent.getBooleanExtra(EXTRA_SHARE_PUBLIC_UPLOAD, false));
                                } else {
                                    updateLinkOperation.setPublicUploadOnFile(
                                        operationIntent.getBooleanExtra(EXTRA_SHARE_PUBLIC_UPLOAD, false));
                                }
                            }
                            operation = updateLinkOperation;
                        } else if (shareId > 0) {
                            UpdateSharePermissionsOperation updateShare = new UpdateSharePermissionsOperation(shareId);

                            int permissions = operationIntent.getIntExtra(EXTRA_SHARE_PERMISSIONS, -1);
                            updateShare.setPermissions(permissions);

                            long expirationDateInMillis = operationIntent
                                    .getLongExtra(EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS, 0L);
                            updateShare.setExpirationDateInMillis(expirationDateInMillis);

                            password = operationIntent.getStringExtra(EXTRA_SHARE_PASSWORD);
                            updateShare.setPassword(password);

                            operation = updateShare;
                        }
                        break;

                    case ACTION_UPDATE_SHARE_NOTE:
                        shareId = operationIntent.getLongExtra(EXTRA_SHARE_ID, -1);
                        String note = operationIntent.getStringExtra(EXTRA_SHARE_NOTE);

                        if (shareId > 0) {
                            operation = new UpdateNoteForShareOperation(shareId, note);
                        }
                        break;

                    case ACTION_CREATE_SHARE_WITH_SHAREE:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        String shareeName = operationIntent.getStringExtra(EXTRA_SHARE_WITH);
                        shareType = (ShareType) operationIntent.getSerializableExtra(EXTRA_SHARE_TYPE);
                        int permissions = operationIntent.getIntExtra(EXTRA_SHARE_PERMISSIONS, -1);
                        if (!TextUtils.isEmpty(remotePath)) {
                            operation = new CreateShareWithShareeOperation(remotePath, shareeName, shareType,
                                    permissions);
                        }
                        break;

                    case ACTION_UNSHARE:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        shareType = (ShareType) operationIntent.getSerializableExtra(EXTRA_SHARE_TYPE);
                        String shareWith = operationIntent.getStringExtra(EXTRA_SHARE_WITH);

                        if (!TextUtils.isEmpty(remotePath)) {
                            operation = new UnshareOperation(remotePath, shareType, shareWith, this);
                        }
                        break;

                    case ACTION_GET_SERVER_INFO:
                        operation = new GetServerInfoOperation(serverUrl, this);
                        break;

                    case ACTION_GET_USER_NAME:
                        operation = new GetUserInfoRemoteOperation();
                        break;

                    case ACTION_RENAME:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        String newName = operationIntent.getStringExtra(EXTRA_NEWNAME);
                        operation = new RenameFileOperation(remotePath, newName);
                        break;

                    case ACTION_REMOVE:
                        // Remove file or folder
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        boolean onlyLocalCopy = operationIntent.getBooleanExtra(EXTRA_REMOVE_ONLY_LOCAL, false);
                        boolean inBackground = operationIntent.getBooleanExtra(EXTRA_IN_BACKGROUND, false);
                        operation = new RemoveFileOperation(remotePath, onlyLocalCopy, account, inBackground,
                                getApplicationContext());
                        break;

                    case ACTION_CREATE_FOLDER:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        boolean createFullPath = operationIntent.getBooleanExtra(EXTRA_CREATE_FULL_PATH, true);
                        operation = new CreateFolderOperation(remotePath, createFullPath);
                        break;

                    case ACTION_SYNC_FILE:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        boolean syncFileContents = operationIntent.getBooleanExtra(EXTRA_SYNC_FILE_CONTENTS, true);
                        operation = new SynchronizeFileOperation(remotePath, account, syncFileContents,
                                getApplicationContext());
                        break;

                    case ACTION_SYNC_FOLDER:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        operation = new SynchronizeFolderOperation(
                                this,                       // TODO remove this dependency from construction time
                                remotePath,
                                account,
                                System.currentTimeMillis()  // TODO remove this dependency from construction time
                        );
                        break;

                    case ACTION_MOVE_FILE:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        newParentPath = operationIntent.getStringExtra(EXTRA_NEW_PARENT_PATH);
                        operation = new MoveFileOperation(remotePath, newParentPath);
                        break;

                    case ACTION_COPY_FILE:
                        remotePath = operationIntent.getStringExtra(EXTRA_REMOTE_PATH);
                        newParentPath = operationIntent.getStringExtra(EXTRA_NEW_PARENT_PATH);
                        operation = new CopyFileOperation(remotePath, newParentPath);
                        break;

                    case ACTION_CHECK_CURRENT_CREDENTIALS:
                    operation = new CheckCurrentCredentialsOperation(account);
                        break;

                    case ACTION_RESTORE_VERSION:
                        FileVersion fileVersion = operationIntent.getParcelableExtra(EXTRA_FILE_VERSION);
                        operation = new RestoreFileVersionRemoteOperation(fileVersion.getRemoteId(),
                                                                          fileVersion.getFileName());
                        break;

                    default:
                        // do nothing
                        break;
                }
            }

        } catch (IllegalArgumentException e) {
            Log_OC.e(TAG, "Bad information provided in intent: " + e.getMessage());
            operation = null;
        }

        if (operation != null) {
            return new Pair<Target, RemoteOperation>(target, operation);
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
        Iterator<OnRemoteOperationListener> listeners = mOperationsBinder.mBoundListeners.keySet().iterator();
        while (listeners.hasNext()) {
            final OnRemoteOperationListener listener = listeners.next();
            final Handler handler = mOperationsBinder.mBoundListeners.get(listener);
            if (handler != null) {
                handler.post(() -> listener.onRemoteOperationFinish(operation, result));
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
