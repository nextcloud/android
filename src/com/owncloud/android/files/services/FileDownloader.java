/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

package com.owncloud.android.files.services;

import java.io.File;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import eu.alefzero.webdav.OnDatatransferProgressListener;

import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.RemoteViews;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavClient;

public class FileDownloader extends Service implements OnDatatransferProgressListener {
    
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_FILE = "FILE";
    
    public static final String DOWNLOAD_ADDED_MESSAGE = "DOWNLOAD_ADDED";
    public static final String DOWNLOAD_FINISH_MESSAGE = "DOWNLOAD_FINISH";
    public static final String EXTRA_DOWNLOAD_RESULT = "RESULT";    
    public static final String EXTRA_FILE_PATH = "FILE_PATH";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    
    private static final String TAG = "FileDownloader";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private WebdavClient mDownloadClient = null;
    private Account mLastAccount = null;
    private FileDataStorageManager mStorageManager;
    
    private ConcurrentMap<String, DownloadFileOperation> mPendingDownloads = new ConcurrentHashMap<String, DownloadFileOperation>();
    private DownloadFileOperation mCurrentDownload = null;
    
    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private int mLastPercent;
    
    
    /**
     * Builds a key for mPendingDownloads from the account and file to download
     * 
     * @param account   Account where the file to download is stored
     * @param file      File to download
     */
    private String buildRemoteName(Account account, OCFile file) {
        return account.name + file.getRemotePath();
    }

    
    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileDownloaderThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileDownloaderBinder();
    }

    
    /**
     * Entry point to add one or several files to the queue of downloads.
     * 
     * New downloads are added calling to startService(), resulting in a call to this method. This ensures the service will keep on working 
     * although the caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (    !intent.hasExtra(EXTRA_ACCOUNT) ||
                !intent.hasExtra(EXTRA_FILE)
                /*!intent.hasExtra(EXTRA_FILE_PATH) ||
                !intent.hasExtra(EXTRA_REMOTE_PATH)*/
           ) {
            Log.e(TAG, "Not enough information provided in intent");
            return START_NOT_STICKY;
        }
        Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
        OCFile file = intent.getParcelableExtra(EXTRA_FILE);
        
        AbstractList<String> requestedDownloads = new Vector<String>(); // dvelasco: now this always contains just one element, but that can change in a near future (download of multiple selection)
        String downloadKey = buildRemoteName(account, file);
        try {
            DownloadFileOperation newDownload = new DownloadFileOperation(account, file); 
            mPendingDownloads.putIfAbsent(downloadKey, newDownload);
            newDownload.addDatatransferProgressListener(this);
            newDownload.addDatatransferProgressListener((FileDownloaderBinder)mBinder);
            requestedDownloads.add(downloadKey);
            sendBroadcastNewDownload(newDownload);
            
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Not enough information provided in intent: " + e.getMessage());
            return START_NOT_STICKY;
        }
        
        if (requestedDownloads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedDownloads;
            mServiceHandler.sendMessage(msg);
        }

        return START_NOT_STICKY;
    }
    
    
    /**
     * Provides a binder object that clients can use to perform operations on the queue of downloads, excepting the addition of new files. 
     * 
     * Implemented to perform cancellation, pause and resume of existing downloads.
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }


    /**
     * Called when ALL the bound clients were onbound.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        ((FileDownloaderBinder)mBinder).clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }

    
    /**
     *  Binder to let client components to perform operations on the queue of downloads.
     * 
     *  It provides by itself the available operations.
     */
    public class FileDownloaderBinder extends Binder implements OnDatatransferProgressListener {
        
        /** 
         * Map of listeners that will be reported about progress of downloads from a {@link FileDownloaderBinder} instance 
         */
        private Map<String, OnDatatransferProgressListener> mBoundListeners = new HashMap<String, OnDatatransferProgressListener>();
        
        
        /**
         * Cancels a pending or current download of a remote file.
         * 
         * @param account       Owncloud account where the remote file is stored.
         * @param file          A file in the queue of pending downloads
         */
        public void cancel(Account account, OCFile file) {
            DownloadFileOperation download = null;
            synchronized (mPendingDownloads) {
                download = mPendingDownloads.remove(buildRemoteName(account, file));
            }
            if (download != null) {
                download.cancel();
            }
        }
        
        
        public void clearListeners() {
            mBoundListeners.clear();
        }


        /**
         * Returns True when the file described by 'file' in the ownCloud account 'account' is downloading or waiting to download.
         * 
         * If 'file' is a directory, returns 'true' if some of its descendant files is downloading or waiting to download. 
         * 
         * @param account       Owncloud account where the remote file is stored.
         * @param file          A file that could be in the queue of downloads.
         */
        public boolean isDownloading(Account account, OCFile file) {
            if (account == null || file == null) return false;
            String targetKey = buildRemoteName(account, file);
            synchronized (mPendingDownloads) {
                if (file.isDirectory()) {
                    // this can be slow if there are many downloads :(
                    Iterator<String> it = mPendingDownloads.keySet().iterator();
                    boolean found = false;
                    while (it.hasNext() && !found) {
                        found = it.next().startsWith(targetKey);
                    }
                    return found;
                } else {
                    return (mPendingDownloads.containsKey(targetKey));
                }
            }
        }

        
        /**
         * Adds a listener interested in the progress of the download for a concrete file.
         * 
         * @param listener      Object to notify about progress of transfer.    
         * @param account       ownCloud account holding the file of interest.
         * @param file          {@link OCfile} of interest for listener. 
         */
        public void addDatatransferProgressListener (OnDatatransferProgressListener listener, Account account, OCFile file) {
            if (account == null || file == null || listener == null) return;
            String targetKey = buildRemoteName(account, file);
            mBoundListeners.put(targetKey, listener);
        }
        
        
        
        /**
         * Removes a listener interested in the progress of the download for a concrete file.
         * 
         * @param listener      Object to notify about progress of transfer.    
         * @param account       ownCloud account holding the file of interest.
         * @param file          {@link OCfile} of interest for listener. 
         */
        public void removeDatatransferProgressListener (OnDatatransferProgressListener listener, Account account, OCFile file) {
            if (account == null || file == null || listener == null) return;
            String targetKey = buildRemoteName(account, file);
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }


        @Override
        public void onTransferProgress(long progressRate) {
            // old way, should not be in use any more
        }


        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer,
                String fileName) {
            String key = buildRemoteName(mCurrentDownload.getAccount(), mCurrentDownload.getFile());
            OnDatatransferProgressListener boundListener = mBoundListeners.get(key);
            if (boundListener != null) {
                boundListener.onTransferProgress(progressRate, totalTransferredSoFar, totalToTransfer, fileName);
            }
        }
        
    }
    
    
    /** 
     * Download worker. Performs the pending downloads in the order they were requested. 
     * 
     * Created with the Looper of a new thread, started in {@link FileUploader#onCreate()}. 
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will warn about a possible memory leak
        FileDownloader mService;
        public ServiceHandler(Looper looper, FileDownloader service) {
            super(looper);
            if (service == null)
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            AbstractList<String> requestedDownloads = (AbstractList<String>) msg.obj;
            if (msg.obj != null) {
                Iterator<String> it = requestedDownloads.iterator();
                while (it.hasNext()) {
                    mService.downloadFile(it.next());
                }
            }
            mService.stopSelf(msg.arg1);
        }
    }
    
    

    /**
     * Core download method: requests a file to download and stores it.
     * 
     * @param downloadKey   Key to access the download to perform, contained in mPendingDownloads 
     */
    private void downloadFile(String downloadKey) {
        
        synchronized(mPendingDownloads) {
            mCurrentDownload = mPendingDownloads.get(downloadKey);
        }
        
        if (mCurrentDownload != null) {
            
            notifyDownloadStart(mCurrentDownload);

            /// prepare client object to send the request to the ownCloud server
            if (mDownloadClient == null || !mLastAccount.equals(mCurrentDownload.getAccount())) {
                mLastAccount = mCurrentDownload.getAccount();
                mStorageManager = new FileDataStorageManager(mLastAccount, getContentResolver());
                mDownloadClient = OwnCloudClientUtils.createOwnCloudClient(mLastAccount, getApplicationContext());
            }

            /// perform the download
            RemoteOperationResult downloadResult = null;
            try {
                downloadResult = mCurrentDownload.execute(mDownloadClient);
                if (downloadResult.isSuccess()) {
                    saveDownloadedFile();
                }
            
            } finally {
                synchronized(mPendingDownloads) {
                    mPendingDownloads.remove(downloadKey);
                }
            }

            
            /// notify result
            notifyDownloadResult(mCurrentDownload, downloadResult);
            
            sendBroadcastDownloadFinished(mCurrentDownload, downloadResult);
        }
    }


    /**
     * Updates the OC File after a successful download.
     */
    private void saveDownloadedFile() {
        OCFile file = mCurrentDownload.getFile();
        long syncDate = System.currentTimeMillis();
        file.setLastSyncDateForProperties(syncDate);
        file.setLastSyncDateForData(syncDate);
        file.setModificationTimestamp(mCurrentDownload.getModificationTimestamp());
        file.setModificationTimestampAtLastSyncForData(mCurrentDownload.getModificationTimestamp());
        // file.setEtag(mCurrentDownload.getEtag());    // TODO Etag, where available
        file.setMimetype(mCurrentDownload.getMimeType());
        file.setStoragePath(mCurrentDownload.getSavePath());
        file.setFileLength((new File(mCurrentDownload.getSavePath()).length()));
        mStorageManager.saveFile(file);
    }


    /**
     * Creates a status notification to show the download progress
     * 
     * @param download  Download operation starting.
     */
    private void notifyDownloadStart(DownloadFileOperation download) {
        /// create status notification with a progress bar
        mLastPercent = 0;
        mNotification = new Notification(R.drawable.icon, getString(R.string.downloader_download_in_progress_ticker), System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, download.getSize() < 0);
        mNotification.contentView.setTextViewText(R.id.status_text, String.format(getString(R.string.downloader_download_in_progress_content), 0, new File(download.getSavePath()).getName()));
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        
        /// includes a pending intent in the notification showing the details view of the file
        Intent showDetailsIntent = null;
        if (PreviewImageFragment.canBePreviewed(download.getFile())) {
            showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        } else {
            showDetailsIntent = new Intent(this, FileDetailActivity.class);
        }
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, download.getFile());
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, download.getAccount());
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), (int)System.currentTimeMillis(), showDetailsIntent, 0);
        
        mNotificationManager.notify(R.string.downloader_download_in_progress_ticker, mNotification);
    }

    
    /**
     * Callback method to update the progress bar in the status notification.
     */
    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName) {
        int percent = (int)(100.0*((double)totalTransferredSoFar)/((double)totalToTransfer));
        if (percent != mLastPercent) {
          mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, totalToTransfer < 0);
          String text = String.format(getString(R.string.downloader_download_in_progress_content), percent, fileName);
          mNotification.contentView.setTextViewText(R.id.status_text, text);
          mNotificationManager.notify(R.string.downloader_download_in_progress_ticker, mNotification);
        }
        mLastPercent = percent;
    }
    
    
    /**
     * Callback method to update the progress bar in the status notification (old version)
     */
    @Override
    public void onTransferProgress(long progressRate) {
        // NOTHING TO DO HERE ANYMORE
    }
    

    /**
     * Updates the status notification with the result of a download operation.
     * 
     * @param downloadResult    Result of the download operation.
     * @param download          Finished download operation
     */
    private void notifyDownloadResult(DownloadFileOperation download, RemoteOperationResult downloadResult) {
        mNotificationManager.cancel(R.string.downloader_download_in_progress_ticker);
        if (!downloadResult.isCancelled()) {
            int tickerId = (downloadResult.isSuccess()) ? R.string.downloader_download_succeeded_ticker : R.string.downloader_download_failed_ticker;
            int contentId = (downloadResult.isSuccess()) ? R.string.downloader_download_succeeded_content : R.string.downloader_download_failed_content;
            Notification finalNotification = new Notification(R.drawable.icon, getString(tickerId), System.currentTimeMillis());
            finalNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            Intent showDetailsIntent = null;
            if (downloadResult.isSuccess()) {
                if (PreviewImageFragment.canBePreviewed(download.getFile())) {
                    showDetailsIntent = new Intent(this, PreviewImageActivity.class);
                } else {
                    showDetailsIntent = new Intent(this, FileDetailActivity.class);
                }
                showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, download.getFile());
                showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, download.getAccount());
                showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                
            } else {
                // TODO put something smart in showDetailsIntent
                showDetailsIntent = new Intent();
            }
            finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), (int)System.currentTimeMillis(), showDetailsIntent, 0);
            finalNotification.setLatestEventInfo(getApplicationContext(), getString(tickerId), String.format(getString(contentId), new File(download.getSavePath()).getName()), finalNotification.contentIntent);
            mNotificationManager.notify(tickerId, finalNotification);
        }
    }
    
    
    /**
     * Sends a broadcast when a download finishes in order to the interested activities can update their view
     * 
     * @param download          Finished download operation
     * @param downloadResult    Result of the download operation
     */
    private void sendBroadcastDownloadFinished(DownloadFileOperation download, RemoteOperationResult downloadResult) {
        Intent end = new Intent(DOWNLOAD_FINISH_MESSAGE);
        end.putExtra(EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess());
        end.putExtra(ACCOUNT_NAME, download.getAccount().name);
        end.putExtra(EXTRA_REMOTE_PATH, download.getRemotePath());
        end.putExtra(EXTRA_FILE_PATH, download.getSavePath());
        sendStickyBroadcast(end);
    }
    
    
    /**
     * Sends a broadcast when a new download is added to the queue.
     * 
     * @param download          Added download operation
     */
    private void sendBroadcastNewDownload(DownloadFileOperation download) {
        Intent added = new Intent(DOWNLOAD_ADDED_MESSAGE);
        added.putExtra(ACCOUNT_NAME, download.getAccount().name);
        added.putExtra(EXTRA_REMOTE_PATH, download.getRemotePath());
        added.putExtra(EXTRA_FILE_PATH, download.getSavePath());
        sendStickyBroadcast(added);
    }

}
