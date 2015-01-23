/* ownCloud Android client application
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

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SyncFolder worker. Performs the pending operations in the order they were requested.
 *
 * Created with the Looper of a new thread, started in
 * {@link com.owncloud.android.services.OperationsService#onCreate()}.
 */
class SyncFolderHandler extends Handler {

    private static final String TAG = SyncFolderHandler.class.getSimpleName();


    OperationsService mService;

    private ConcurrentMap<String,SynchronizeFolderOperation> mPendingOperations =
            new ConcurrentHashMap<String,SynchronizeFolderOperation>();
    private OwnCloudClient mOwnCloudClient = null;
    private FileDataStorageManager mStorageManager;
    private SynchronizeFolderOperation mCurrentSyncOperation;


    public SyncFolderHandler(Looper looper, OperationsService service) {
        super(looper);
        if (service == null) {
            throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
        }
        mService = service;
    }


    public boolean isSynchronizing(Account account, String remotePath) {
        if (account == null || remotePath == null) return false;
        String targetKey = buildRemoteName(account, remotePath);
        synchronized (mPendingOperations) {
            // TODO - this can be slow when synchronizing a big tree - need a better data structure
            Iterator<String> it = mPendingOperations.keySet().iterator();
            boolean found = false;
            while (it.hasNext() && !found) {
                found = it.next().startsWith(targetKey);
            }
            return found;
        }
    }


    @Override
    public void handleMessage(Message msg) {
        Pair<Account, String> itemSyncKey = (Pair<Account, String>) msg.obj;
        doOperation(itemSyncKey.first, itemSyncKey.second);
        mService.stopSelf(msg.arg1);
    }


    /**
     * Performs the next operation in the queue
     */
    private void doOperation(Account account, String remotePath) {

        String syncKey = buildRemoteName(account,remotePath);

        synchronized(mPendingOperations) {
            mCurrentSyncOperation = mPendingOperations.get(syncKey);
        }

        if (mCurrentSyncOperation != null) {
            RemoteOperationResult result = null;

            try {

                OwnCloudAccount ocAccount = new OwnCloudAccount(account, mService);
                mOwnCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                        getClientFor(ocAccount, mService);
                mStorageManager = new FileDataStorageManager(
                        account,
                        mService.getContentResolver()
                );

                result = mCurrentSyncOperation.execute(mOwnCloudClient, mStorageManager);

            } catch (AccountsException e) {
                Log_OC.e(TAG, "Error while trying to get autorization", e);
            } catch (IOException e) {
                Log_OC.e(TAG, "Error while trying to get autorization", e);
            } finally {
                synchronized (mPendingOperations) {
                    mPendingOperations.remove(syncKey);
                    /*
                    SynchronizeFolderOperation checkedOp = mCurrentSyncOperation;
                    String checkedKey = syncKey;
                    while (checkedOp.getPendingChildrenCount() <= 0) {
                    // while (!checkedOp.hasChildren()) {
                        mPendingOperations.remove(checkedKey);
                        String parentKey = buildRemoteName(account, (new File(checkedOp.getFolderPath())).getParent());
                        // String parentKey = buildRemoteName(account, checkedOp.getParentPath());
                        SynchronizeFolderOperation parentOp = mPendingOperations.get(parentKey);
                        if (parentOp != null) {
                            parentOp.decreasePendingChildrenCount();
                        }
                    }
                    */
                }

                mService.dispatchResultToOperationListeners(null, mCurrentSyncOperation, result);

                sendBroadcastFinishedSyncFolder(account, remotePath, result.isSuccess());
            }
        }
    }

    public void add(Account account, String remotePath, SynchronizeFolderOperation syncFolderOperation){
        String syncKey = buildRemoteName(account,remotePath);
        mPendingOperations.putIfAbsent(syncKey,syncFolderOperation);
        sendBroadcastNewSyncFolder(account, remotePath);
    }

    /**
     * Cancels sync operations.
     * @param account       Owncloud account where the remote file is stored.
     * @param file          File OCFile
     */
    public void cancel(Account account, OCFile file){
        SynchronizeFolderOperation syncOperation = null;
        String targetKey = buildRemoteName(account, file.getRemotePath());
        ArrayList<String> keyItems = new ArrayList<String>();
        synchronized (mPendingOperations) {
            if (file.isFolder()) {
                Log_OC.d(TAG, "Canceling pending sync operations");
                Iterator<String> it = mPendingOperations.keySet().iterator();
                boolean found = false;
                while (it.hasNext()) {
                    String keySyncOperation = it.next();
                    found = keySyncOperation.startsWith(targetKey);
                    if (found) {
                        keyItems.add(keySyncOperation);
                    }
                }

            } else {
                // this is not really expected...
                Log_OC.d(TAG, "Canceling sync operation");
                keyItems.add(buildRemoteName(account, file.getRemotePath()));
            }
            for (String item: keyItems) {
                syncOperation = mPendingOperations.remove(item);
                if (syncOperation != null) {
                    syncOperation.cancel();
                }
            }
        }

        //sendBroadcastFinishedSyncFolder(account, file.getRemotePath());
    }

    /**
     * Builds a key from the account and file to download
     *
     * @param account   Account where the file to download is stored
     * @param path      File path
     */
    private String buildRemoteName(Account account, String path) {
        return account.name + path;
    }


    /**
     * TODO review this method when "folder synchronization" replaces "folder download"; this is a fast and ugly
     * patch.
     */
    private void sendBroadcastNewSyncFolder(Account account, String remotePath) {
        Intent added = new Intent(FileDownloader.getDownloadAddedMessage());
        added.putExtra(FileDownloader.ACCOUNT_NAME, account.name);
        added.putExtra(FileDownloader.EXTRA_REMOTE_PATH, remotePath);
        added.putExtra(FileDownloader.EXTRA_FILE_PATH, FileStorageUtils.getSavePath(account.name) + remotePath);
        mService.sendStickyBroadcast(added);
    }

    /**
     * TODO review this method when "folder synchronization" replaces "folder download"; this is a fast and ugly
     * patch.
     */
    private void sendBroadcastFinishedSyncFolder(Account account, String remotePath, boolean success) {
        Intent finished = new Intent(FileDownloader.getDownloadFinishMessage());
        finished.putExtra(FileDownloader.ACCOUNT_NAME, account.name);
        finished.putExtra(FileDownloader.EXTRA_REMOTE_PATH, remotePath);
        finished.putExtra(FileDownloader.EXTRA_FILE_PATH, FileStorageUtils.getSavePath(account.name) + remotePath);
        finished.putExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, success);
        mService.sendStickyBroadcast(finished);
    }


}
