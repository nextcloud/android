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
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.IndexedForest;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.IOException;

/**
 * SyncFolder worker. Performs the pending operations in the order they were requested.
 *
 * Created with the Looper of a new thread, started in
 * {@link com.owncloud.android.services.OperationsService#onCreate()}.
 */
class SyncFolderHandler extends Handler {

    private static final String TAG = SyncFolderHandler.class.getSimpleName();


    OperationsService mService;

    private IndexedForest<SynchronizeFolderOperation> mPendingOperations =
            new IndexedForest<SynchronizeFolderOperation>();

    private OwnCloudClient mOwnCloudClient = null;
    private Account mCurrentAccount = null;
    private FileDataStorageManager mStorageManager;
    private SynchronizeFolderOperation mCurrentSyncOperation;


    public SyncFolderHandler(Looper looper, OperationsService service) {
        super(looper);
        if (service == null) {
            throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
        }
        mService = service;
    }


    /**
     * Returns True when the folder located in 'remotePath' in the ownCloud account 'account', or any of its
     * descendants, is being synchronized (or waiting for it).
     *
     * @param account       ownCloud account where the remote folder is stored.
     * @param remotePath    The path to a folder that could be in the queue of synchronizations.
     */
    public boolean isSynchronizing(Account account, String remotePath) {
        if (account == null || remotePath == null) return false;
        return (mPendingOperations.contains(account, remotePath));
    }


    @Override
    public void handleMessage(Message msg) {
        Pair<Account, String> itemSyncKey = (Pair<Account, String>) msg.obj;
        doOperation(itemSyncKey.first, itemSyncKey.second);
        Log_OC.d(TAG, "Stopping after command with id " + msg.arg1);
        mService.stopSelf(msg.arg1);
    }


    /**
     * Performs the next operation in the queue
     */
    private void doOperation(Account account, String remotePath) {

        mCurrentSyncOperation = mPendingOperations.get(account, remotePath);

        if (mCurrentSyncOperation != null) {
            RemoteOperationResult result = null;

            try {

                if (mCurrentAccount == null || !mCurrentAccount.equals(account)) {
                    mCurrentAccount = account;
                    mStorageManager = new FileDataStorageManager(
                            account,
                            mService.getContentResolver()
                    );
                }   // else, reuse storage manager from previous operation

                // always get client from client manager, to get fresh credentials in case of update
                OwnCloudAccount ocAccount = new OwnCloudAccount(account, mService);
                mOwnCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                        getClientFor(ocAccount, mService);

                result = mCurrentSyncOperation.execute(mOwnCloudClient, mStorageManager);

            } catch (AccountsException e) {
                Log_OC.e(TAG, "Error while trying to get authorization", e);
            } catch (IOException e) {
                Log_OC.e(TAG, "Error while trying to get authorization", e);
            } finally {
                mPendingOperations.removePayload(account, remotePath);

                mService.dispatchResultToOperationListeners(mCurrentSyncOperation, result);

                sendBroadcastFinishedSyncFolder(account, remotePath, result.isSuccess());
            }
        }
    }

    public void add(Account account, String remotePath,
                    SynchronizeFolderOperation syncFolderOperation){
        Pair<String, String> putResult =
                mPendingOperations.putIfAbsent(account, remotePath, syncFolderOperation);
        if (putResult != null) {
            sendBroadcastNewSyncFolder(account, remotePath);    // TODO upgrade!
        }
    }


    /**
     * Cancels a pending or current sync' operation.
     *
     * @param account       ownCloud account where the remote file is stored.
     * @param file          A file in the queue of pending synchronizations
     */
    public void cancel(Account account, OCFile file){
        if (account == null || file == null) {
            Log_OC.e(TAG, "Cannot cancel with NULL parameters");
            return;
        }
        Pair<SynchronizeFolderOperation, String> removeResult =
                mPendingOperations.remove(account, file.getRemotePath());
        SynchronizeFolderOperation synchronization = removeResult.first;
        if (synchronization != null) {
            synchronization.cancel();
        } else {
            // TODO synchronize?
            if (mCurrentSyncOperation != null && mCurrentAccount != null &&
                    mCurrentSyncOperation.getRemotePath().startsWith(file.getRemotePath()) &&
                    account.name.equals(mCurrentAccount.name)) {
                mCurrentSyncOperation.cancel();
            }
        }

        //sendBroadcastFinishedSyncFolder(account, file.getRemotePath());
    }

    /**
     * TODO review this method when "folder synchronization" replaces "folder download";
     * this is a fast and ugly patch.
     */
    private void sendBroadcastNewSyncFolder(Account account, String remotePath) {
        Intent added = new Intent(FileDownloader.getDownloadAddedMessage());
        added.putExtra(FileDownloader.ACCOUNT_NAME, account.name);
        added.putExtra(FileDownloader.EXTRA_REMOTE_PATH, remotePath);
        added.putExtra(FileDownloader.EXTRA_FILE_PATH, FileStorageUtils.getSavePath(account.name)
                + remotePath);
        mService.sendStickyBroadcast(added);
    }

    /**
     * TODO review this method when "folder synchronization" replaces "folder download";
     * this is a fast and ugly patch.
     */
    private void sendBroadcastFinishedSyncFolder(Account account, String remotePath,
                                                 boolean success) {
        Intent finished = new Intent(FileDownloader.getDownloadFinishMessage());
        finished.putExtra(FileDownloader.ACCOUNT_NAME, account.name);
        finished.putExtra(FileDownloader.EXTRA_REMOTE_PATH, remotePath);
        finished.putExtra(FileDownloader.EXTRA_FILE_PATH,
                FileStorageUtils.getSavePath(account.name) + remotePath);
        finished.putExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, success);
        mService.sendStickyBroadcast(finished);
    }


}
