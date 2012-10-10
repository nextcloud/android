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
import java.util.Vector;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.json.JSONObject;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
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
import eu.alefzero.webdav.WebdavEntry;
import eu.alefzero.webdav.WebdavUtils;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 * 
 * @author Bartek Przybylski
 */
public class FileSyncAdapter extends AbstractOwnCloudSyncAdapter {

    private final static String TAG = "FileSyncAdapter"; 
    
    /*  Commented code for ugly performance tests
    private final static int MAX_DELAYS = 100;
    private static long[] mResponseDelays = new long[MAX_DELAYS]; 
    private static long[] mSaveDelays = new long[MAX_DELAYS];
    private int mDelaysIndex = 0;
    private int mDelaysCount = 0;
    */
    
    private long mCurrentSyncTime;
    private boolean mCancellation;
    private boolean mIsManualSync;
    private boolean mRightSync;
    
    public FileSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public synchronized void onPerformSync(Account account, Bundle extras,
            String authority, ContentProviderClient provider,
            SyncResult syncResult) {

        mCancellation = false;
        mIsManualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        mRightSync = true;
        
        this.setAccount(account);
        this.setContentProvider(provider);
        this.setStorageManager(new FileDataStorageManager(account, getContentProvider()));
        
        /*  Commented code for ugly performance tests
        mDelaysIndex = 0;
        mDelaysCount = 0;
        */

        Log.d(TAG, "syncing owncloud account " + account.name);

        sendStickyBroadcast(true, null);  // message to signal the start to the UI
        
        updateOCVersion();

        String uri = getUri().toString();
        PropFindMethod query = null;
        try {
            mCurrentSyncTime = System.currentTimeMillis();
            query = new PropFindMethod(uri + "/");
            int status = getClient().executeMethod(query);
            if (status != HttpStatus.SC_UNAUTHORIZED) {
                MultiStatus resp = query.getResponseBodyAsMultiStatus();

                if (resp.getResponses().length > 0) {
                    WebdavEntry we = new WebdavEntry(resp.getResponses()[0], getUri().getPath());
                    OCFile file = fillOCFile(we);
                    file.setParentId(0);
                    getStorageManager().saveFile(file);
                    if (!mCancellation) {
                        fetchData(uri, syncResult, file.getFileId());
                    }
                }

            } else {
                syncResult.stats.numAuthExceptions++;
            }
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            logException(e, uri + "/");
            
        } catch (DavException e) {
            syncResult.stats.numParseExceptions++;
            logException(e, uri + "/");
            
        } catch (Exception e) {
            // TODO something smart with syncresult
            logException(e, uri + "/");
            mRightSync = false;
            
        } finally {
            if (query != null)
                query.releaseConnection();  // let the connection available for other methods
            mRightSync &= (syncResult.stats.numIoExceptions == 0 && syncResult.stats.numAuthExceptions == 0 && syncResult.stats.numParseExceptions == 0);
            if (!mRightSync && mIsManualSync) {
                /// don't let the system synchronization manager retries MANUAL synchronizations
                //      (be careful: "MANUAL" currently includes the synchronization requested when a new account is created and when the user changes the current account)
                syncResult.tooManyRetries = true;
                
                /// notify the user about the failure of MANUAL synchronization
                Notification notification = new Notification(R.drawable.icon, getContext().getString(R.string.sync_fail_ticker), System.currentTimeMillis());
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                // TODO put something smart in the contentIntent below
                notification.contentIntent = PendingIntent.getActivity(getContext().getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
                notification.setLatestEventInfo(getContext().getApplicationContext(), 
                                                getContext().getString(R.string.sync_fail_ticker), 
                                                String.format(getContext().getString(R.string.sync_fail_content), account.name), 
                                                notification.contentIntent);
                ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(R.string.sync_fail_ticker, notification);
            }
            sendStickyBroadcast(false, null);        // message to signal the end to the UI
        }
        
        /*  Commented code for ugly performance tests
        long sum = 0, mean = 0, max = 0, min = Long.MAX_VALUE;
        for (int i=0; i<MAX_DELAYS && i<mDelaysCount; i++) {
            sum += mResponseDelays[i];
            max = Math.max(max, mResponseDelays[i]);
            min = Math.min(min, mResponseDelays[i]);
        }
        mean = sum / mDelaysCount;
        Log.e(TAG, "SYNC STATS - response: mean time = " + mean + " ; max time = " + max + " ; min time = " + min);
        
        sum = 0; max = 0; min = Long.MAX_VALUE;
        for (int i=0; i<MAX_DELAYS && i<mDelaysCount; i++) {
            sum += mSaveDelays[i];
            max = Math.max(max, mSaveDelays[i]);
            min = Math.min(min, mSaveDelays[i]);
        }
        mean = sum / mDelaysCount;
        Log.e(TAG, "SYNC STATS - save:     mean time = " + mean + " ; max time = " + max + " ; min time = " + min);
        Log.e(TAG, "SYNC STATS - folders measured: " + mDelaysCount);
        */
        
    }

    private void fetchData(String uri, SyncResult syncResult, long parentId) {
        PropFindMethod query = null;
        try {
            Log.d(TAG, "fetching " + uri);
            
            // remote request 
            query = new PropFindMethod(uri);
            /*  Commented code for ugly performance tests
            long responseDelay = System.currentTimeMillis();
            */
            int status = getClient().executeMethod(query);
            /*  Commented code for ugly performance tests
            responseDelay = System.currentTimeMillis() - responseDelay;
            Log.e(TAG, "syncing: RESPONSE TIME for " + uri + " contents, " + responseDelay + "ms");
            */
            if (status != HttpStatus.SC_UNAUTHORIZED) {
                MultiStatus resp = query.getResponseBodyAsMultiStatus();
            
                // insertion or update of files
                List<OCFile> updatedFiles = new Vector<OCFile>(resp.getResponses().length - 1);
                for (int i = 1; i < resp.getResponses().length; ++i) {
                    WebdavEntry we = new WebdavEntry(resp.getResponses()[i], getUri().getPath());
                    OCFile file = fillOCFile(we);
                    file.setParentId(parentId);
                    if (getStorageManager().getFileByPath(file.getRemotePath()) != null &&
                            getStorageManager().getFileByPath(file.getRemotePath()).keepInSync() &&
                            file.getModificationTimestamp() > getStorageManager().getFileByPath(file.getRemotePath())
                                                                         .getModificationTimestamp()) {
                        Intent intent = new Intent(this.getContext(), FileDownloader.class);
                        intent.putExtra(FileDownloader.EXTRA_ACCOUNT, getAccount());
                        intent.putExtra(FileDownloader.EXTRA_FILE, file);
                        /*intent.putExtra(FileDownloader.EXTRA_FILE_PATH, file.getRemotePath());
                        intent.putExtra(FileDownloader.EXTRA_REMOTE_PATH, file.getRemotePath());
                        intent.putExtra(FileDownloader.EXTRA_FILE_SIZE, file.getFileLength());*/
                        file.setKeepInSync(true);
                        getContext().startService(intent);
                    }
                    if (getStorageManager().getFileByPath(file.getRemotePath()) != null)
                        file.setKeepInSync(getStorageManager().getFileByPath(file.getRemotePath()).keepInSync());
                
                    // Log.v(TAG, "adding file: " + file);
                    updatedFiles.add(file);
                    if (parentId == 0)
                        parentId = file.getFileId();
                }
                /*  Commented code for ugly performance tests
                long saveDelay = System.currentTimeMillis();
                 */            
                getStorageManager().saveFiles(updatedFiles);    // all "at once" ; trying to get a best performance in database update
                /*  Commented code for ugly performance tests
                saveDelay = System.currentTimeMillis() - saveDelay;
                Log.e(TAG, "syncing: SAVE TIME for " + uri + " contents, " + mSaveDelays[mDelaysIndex] + "ms");
                 */
            
                // removal of obsolete files
                Vector<OCFile> files = getStorageManager().getDirectoryContent(
                        getStorageManager().getFileById(parentId));
                OCFile file;
                String currentSavePath = FileDownloader.getSavePath(getAccount().name);
                for (int i=0; i < files.size(); ) {
                    file = files.get(i);
                    if (file.getLastSyncDate() != mCurrentSyncTime) {
                        Log.v(TAG, "removing file: " + file);
                        getStorageManager().removeFile(file, (file.isDown() && file.getStoragePath().startsWith(currentSavePath)));
                        files.remove(i);
                    } else {
                        i++;
                    }
                }
            
                // recursive fetch
                for (int i=0; i < files.size() && !mCancellation; i++) {
                    OCFile newFile = files.get(i);
                    if (newFile.getMimetype().equals("DIR")) {
                        fetchData(getUri().toString() + WebdavUtils.encodePath(newFile.getRemotePath()), syncResult, newFile.getFileId());
                    }
                }
                if (mCancellation) Log.d(TAG, "Leaving " + uri + " because cancelation request");
                
                /*  Commented code for ugly performance tests
                mResponseDelays[mDelaysIndex] = responseDelay;
                mSaveDelays[mDelaysIndex] = saveDelay;
                mDelaysCount++;
                mDelaysIndex++;
                if (mDelaysIndex >= MAX_DELAYS)
                    mDelaysIndex = 0;
                 */

            } else {
                syncResult.stats.numAuthExceptions++;
            }
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            logException(e, uri);
            
        } catch (DavException e) {
            syncResult.stats.numParseExceptions++;
            logException(e, uri);
            
        } catch (Exception e) {
            // TODO something smart with syncresult
            mRightSync = false;
            logException(e, uri);

        } finally {
            if (query != null)
                query.releaseConnection();  // let the connection available for other methods

            // synchronized folder -> notice to UI
            sendStickyBroadcast(true, getStorageManager().getFileById(parentId).getRemotePath());
        }
    }

    private OCFile fillOCFile(WebdavEntry we) {
        OCFile file = new OCFile(we.decodedPath());
        file.setCreationTimestamp(we.createTimestamp());
        file.setFileLength(we.contentLength());
        file.setMimetype(we.contentType());
        file.setModificationTimestamp(we.modifiedTimesamp());
        file.setLastSyncDate(mCurrentSyncTime);
        return file;
    }
    
    
    private void sendStickyBroadcast(boolean inProgress, String dirRemotePath) {
        Intent i = new Intent(FileSyncService.SYNC_MESSAGE);
        i.putExtra(FileSyncService.IN_PROGRESS, inProgress);
        i.putExtra(FileSyncService.ACCOUNT_NAME, getAccount().name);
        if (dirRemotePath != null) {
            i.putExtra(FileSyncService.SYNC_FOLDER_REMOTE_PATH, dirRemotePath);
        }
        getContext().sendStickyBroadcast(i);
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
     * Logs an exception triggered in a synchronization request. 
     * 
     * @param e       Caught exception.
     * @param uri     Uri to the remote directory that was fetched when the synchronization failed 
     */
    private void logException(Exception e, String uri) {
        if (e instanceof IOException) {
            Log.e(TAG, "Unrecovered transport exception while synchronizing " + uri + " at " + getAccount().name, e);

        } else if (e instanceof DavException) {
            Log.e(TAG, "Unexpected WebDAV exception while synchronizing " + uri + " at " + getAccount().name, e);

        } else {
            Log.e(TAG, "Unexpected exception while synchronizing " + uri  + " at " + getAccount().name, e);
        }
    }

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
}
