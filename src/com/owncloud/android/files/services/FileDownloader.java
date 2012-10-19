package com.owncloud.android.files.services;

import java.io.File;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.webdav.OnDatatransferProgressListener;

import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.fragment.FileDetailFragment;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.RemoteViews;

import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavClient;

public class FileDownloader extends Service implements OnDatatransferProgressListener {
    
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_FILE = "FILE";
    
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
    
    public static final String getSavePath(String accountName) {
        File sdCard = Environment.getExternalStorageDirectory();
        return sdCard.getAbsolutePath() + "/owncloud/" + Uri.encode(accountName, "@");   
            // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names, that can be in the accountName since 0.1.190B
    }
    
    public static final String getTemporalPath(String accountName) {
        File sdCard = Environment.getExternalStorageDirectory();
        return sdCard.getAbsolutePath() + "/owncloud/tmp/" + Uri.encode(accountName, "@");
            // URL encoding is an 'easy fix' to overcome that NTFS and FAT32 don't allow ":" in file names, that can be in the accountName since 0.1.190B
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
            requestedDownloads.add(downloadKey);
            
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
     *  Binder to let client components to perform operations on the queue of downloads.
     * 
     *  It provides by itself the available operations.
     */
    public class FileDownloaderBinder extends Binder {
        
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
        
        
        /**
         * Returns True when the file described by 'file' in the ownCloud account 'account' is downloading or waiting to download
         * 
         * @param account       Owncloud account where the remote file is stored.
         * @param file          A file that could be in the queue of downloads.
         */
        public boolean isDownloading(Account account, OCFile file) {
            synchronized (mPendingDownloads) {
                return (mPendingDownloads.containsKey(buildRemoteName(account, file)));
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
                mDownloadClient = OwnCloudClientUtils.createOwnCloudClient(mLastAccount, getApplicationContext());
            }

            /// perform the download
            RemoteOperationResult downloadResult = null;
            try {
                downloadResult = mCurrentDownload.execute(mDownloadClient);
                if (downloadResult.isSuccess()) {
                    ContentValues cv = new ContentValues();
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, mCurrentDownload.getSavePath());
                    getContentResolver().update(
                            ProviderTableMeta.CONTENT_URI,
                            cv,
                            ProviderTableMeta.FILE_NAME + "=? AND "
                                    + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                    new String[] {
                                    mCurrentDownload.getSavePath().substring(mCurrentDownload.getSavePath().lastIndexOf('/') + 1),
                                    mLastAccount.name });
                }
            
            } finally {
                synchronized(mPendingDownloads) {
                    mPendingDownloads.remove(downloadKey);
                }
            }

            
            /// notify result
            notifyDownloadResult(mCurrentDownload, downloadResult);
            
            sendFinalBroadcast(mCurrentDownload, downloadResult);
        }
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
        Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
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
            // TODO put something smart in the contentIntent below
            finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), (int)System.currentTimeMillis(), new Intent(), 0);
            finalNotification.setLatestEventInfo(getApplicationContext(), getString(tickerId), String.format(getString(contentId), new File(download.getSavePath()).getName()), finalNotification.contentIntent);
            mNotificationManager.notify(tickerId, finalNotification);
        }
    }
    
    
    /**
     * Sends a broadcast in order to the interested activities can update their view
     * 
     * @param download          Finished download operation
     * @param downloadResult    Result of the download operation
     */
    private void sendFinalBroadcast(DownloadFileOperation download, RemoteOperationResult downloadResult) {
        Intent end = new Intent(DOWNLOAD_FINISH_MESSAGE);
        end.putExtra(EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess());
        end.putExtra(ACCOUNT_NAME, download.getAccount().name);
        end.putExtra(EXTRA_REMOTE_PATH, download.getRemotePath());
        if (downloadResult.isSuccess()) {
            end.putExtra(EXTRA_FILE_PATH, download.getSavePath());
        }
        sendBroadcast(end);
    }

}
