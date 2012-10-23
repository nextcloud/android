/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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

package com.owncloud.android.syncadapter;

import java.io.IOException;
import java.util.List;

import org.apache.jackrabbit.webdav.DavException;
import org.json.JSONObject;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.utils.OwnCloudVersion;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import eu.alefzero.webdav.WebdavClient;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 * 
 * @author Bartek Przybylski
 */
public class FileSyncAdapter extends AbstractOwnCloudSyncAdapter {

    private final static String TAG = "FileSyncAdapter";

    /** 
     * Maximum number of failed folder synchronizations that are supported before finishing the synchronization operation
     */
    private static final int MAX_FAILED_RESULTS = 3; 
    
    private long mCurrentSyncTime;
    private boolean mCancellation;
    private boolean mIsManualSync;
    private int mFailedResultsCounter;    
    private RemoteOperationResult mLastFailedResult;
    
    public FileSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public synchronized void onPerformSync(Account account, Bundle extras,
            String authority, ContentProviderClient provider,
            SyncResult syncResult) {

        mCancellation = false;
        mIsManualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        mFailedResultsCounter = 0;
        mLastFailedResult = null;
        
        this.setAccount(account);
        this.setContentProvider(provider);
        this.setStorageManager(new FileDataStorageManager(account, getContentProvider()));
        
        Log.d(TAG, "syncing owncloud account " + account.name);

        sendStickyBroadcast(true, null, null);  // message to signal the start of the synchronization to the UI
        
        try {
            updateOCVersion();
            mCurrentSyncTime = System.currentTimeMillis();
            if (!mCancellation) {
                fetchData(OCFile.PATH_SEPARATOR, syncResult, DataStorageManager.ROOT_PARENT_ID);
                
            } else {
                Log.d(TAG, "Leaving synchronization before any remote request due to cancellation was requested");
            }
            
        } finally {
            // it's important making this although very unexpected errors occur; that's the reason for the finally
            
            if (mFailedResultsCounter > 0 && mIsManualSync) {
                /// don't let the system synchronization manager retries MANUAL synchronizations
                //      (be careful: "MANUAL" currently includes the synchronization requested when a new account is created and when the user changes the current account)
                syncResult.tooManyRetries = true;
                
                /// notify the user about the failure of MANUAL synchronization
                notifyFailedSynchronization();
            }
            sendStickyBroadcast(false, null, mLastFailedResult);        // message to signal the end to the UI
        }
        
    }
    
    
    
    /**
     * Called by system SyncManager when a synchronization is required to be cancelled.
     * 
     * Sets the mCancellation flag to 'true'. THe synchronization will be stopped when before a new folder is fetched. Data of the last folder
     * fetched will be still saved in the database. See onPerformSync implementation.
     */
    @Override
    public void onSyncCanceled() {
        Log.d(TAG, "Synchronization of " + getAccount().name + " has been requested to cancel");
        mCancellation = true;
        super.onSyncCanceled();
    }
    
    
    /**
     * Updates the locally stored version value of the ownCloud server
     */
    private void updateOCVersion() {
        String statUrl = getAccountManager().getUserData(getAccount(), AccountAuthenticator.KEY_OC_BASE_URL);
        statUrl += AccountUtils.STATUS_PATH;
        
        try {
            String result = getClient().getResultAsString(statUrl);
            if (result != null) {
                try {
                    JSONObject json = new JSONObject(result);
                    if (json != null && json.getString("version") != null) {
                        OwnCloudVersion ocver = new OwnCloudVersion(json.getString("version"));
                        if (ocver.isVersionValid()) {
                            getAccountManager().setUserData(getAccount(), AccountAuthenticator.KEY_OC_VERSION, ocver.toString());
                            Log.d(TAG, "Got new OC version " + ocver.toString());
                        } else {
                            Log.w(TAG, "Invalid version number received from server: " + json.getString("version"));
                        }
                    }
                } catch (Throwable e) {
                    Log.w(TAG, "Couldn't parse version response", e);
                }
            } else {
                Log.w(TAG, "Problem while getting ocversion from server");
            }
        } catch (Exception e) {
            Log.e(TAG, "Problem getting response from server", e);
        }
    }

    
    
    /**
     * Synchronize the properties of files and folders contained in a remote folder given by remotePath.
     * 
     * @param remotePath        Remote path to the folder to synchronize.
     * @param parentId          Database Id of the folder to synchronize.
     * @param syncResult        Object to update for communicate results to the system's synchronization manager.        
     */
    private void fetchData(String remotePath, SyncResult syncResult, long parentId) {
        
        if (mFailedResultsCounter > MAX_FAILED_RESULTS && isFinisher(mLastFailedResult))
            return;
        
        // get client object to connect to the remote ownCloud server
        WebdavClient client = null;
        try {
            client = getClient();
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            Log.d(TAG, "Could not get client object while trying to synchronize - impossible to continue");
            return;
        }
        
        // perform folder synchronization
        SynchronizeFolderOperation synchFolderOp = new SynchronizeFolderOperation(  remotePath, 
                                                                                    mCurrentSyncTime, 
                                                                                    parentId, 
                                                                                    getStorageManager(), 
                                                                                    getAccount(), 
                                                                                    getContext()
                                                                                  );
        RemoteOperationResult result = synchFolderOp.execute(client);
        
        
        // synchronized folder -> notice to UI - ALWAYS, although !result.isSuccess
        sendStickyBroadcast(true, remotePath, null);
        
        if (result.isSuccess()) {
            // synchronize children folders 
            List<OCFile> children = synchFolderOp.getChildren();
            fetchChildren(children, syncResult);    // beware of the 'hidden' recursion here!
            
        } else {
            if (result.getCode() == RemoteOperationResult.ResultCode.UNAUTHORIZED) {
                syncResult.stats.numAuthExceptions++;
                
            } else if (result.getException() instanceof DavException) {
                syncResult.stats.numParseExceptions++;
                
            } else if (result.getException() instanceof IOException) { 
                syncResult.stats.numIoExceptions++;
                
            }
            mFailedResultsCounter++;
            mLastFailedResult = result;
        }
            
    }

    /**
     * Checks if a failed result should terminate the synchronization process immediately, according to
     * OUR OWN POLICY
     * 
     * @param   failedResult        Remote operation result to check.
     * @return                      'True' if the result should immediately finish the synchronization
     */
    private boolean isFinisher(RemoteOperationResult failedResult) {
        if  (failedResult != null) {
            RemoteOperationResult.ResultCode code = failedResult.getCode();
            return (code.equals(RemoteOperationResult.ResultCode.SSL_ERROR) ||
                    code.equals(RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED) ||
                    code.equals(RemoteOperationResult.ResultCode.BAD_OC_VERSION) ||
                    code.equals(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED));
        }
        return false;
    }

    /**
     * Synchronize data of folders in the list of received files
     * 
     * @param files         Files to recursively fetch 
     * @param syncResult    Updated object to provide results to the Synchronization Manager
     */
    private void fetchChildren(List<OCFile> files, SyncResult syncResult) {
        int i;
        for (i=0; i < files.size() && !mCancellation; i++) {
            OCFile newFile = files.get(i);
            if (newFile.isDirectory()) {
                fetchData(newFile.getRemotePath(), syncResult, newFile.getFileId());
            }
        }
        if (mCancellation && i <files.size()) Log.d(TAG, "Leaving synchronization before synchronizing " + files.get(i).getRemotePath() + " because cancelation request");
    }

    
    /**
     * Sends a message to any application component interested in the progress of the synchronization.
     * 
     * @param inProgress        'True' when the synchronization progress is not finished.
     * @param dirRemotePath     Remote path of a folder that was just synchronized (with or without success)
     */
    private void sendStickyBroadcast(boolean inProgress, String dirRemotePath, RemoteOperationResult result) {
        Intent i = new Intent(FileSyncService.SYNC_MESSAGE);
        i.putExtra(FileSyncService.IN_PROGRESS, inProgress);
        i.putExtra(FileSyncService.ACCOUNT_NAME, getAccount().name);
        if (dirRemotePath != null) {
            i.putExtra(FileSyncService.SYNC_FOLDER_REMOTE_PATH, dirRemotePath);
        }
        if (result != null) {
            i.putExtra(FileSyncService.SYNC_RESULT, result);
        }
        getContext().sendStickyBroadcast(i);
    }

    
    
    /**
     * Notifies the user about a failed synchronization through the status notification bar 
     */
    private void notifyFailedSynchronization() {
        Notification notification = new Notification(R.drawable.icon, getContext().getString(R.string.sync_fail_ticker), System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        // TODO put something smart in the contentIntent below
        notification.contentIntent = PendingIntent.getActivity(getContext().getApplicationContext(), (int)System.currentTimeMillis(), new Intent(), 0);
        notification.setLatestEventInfo(getContext().getApplicationContext(), 
                                        getContext().getString(R.string.sync_fail_ticker), 
                                        String.format(getContext().getString(R.string.sync_fail_content), getAccount().name), 
                                        notification.contentIntent);
        ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(R.string.sync_fail_ticker, notification);
    }

    

}
