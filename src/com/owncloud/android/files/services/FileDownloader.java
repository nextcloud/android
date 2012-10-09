package com.owncloud.android.files.services;

import java.io.File;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.webdav.OnDatatransferProgressListener;

import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.RemoteOperationResult;

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
    public static final String DOWNLOAD_FINISH_MESSAGE = "DOWNLOAD_FINISH";
    public static final String EXTRA_DOWNLOAD_RESULT = "RESULT";    
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_FILE_PATH = "FILE_PATH";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_FILE_SIZE = "FILE_SIZE";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    
    private static final String TAG = "FileDownloader";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private WebdavClient mDownloadClient = null;
    private Account mLastAccount = null;
    
    //private AbstractList<Account> mAccounts = new Vector<Account>();
    private ConcurrentMap<String, DownloadFileOperation> mPendingDownloads = new ConcurrentHashMap<String, DownloadFileOperation>();
    private DownloadFileOperation mCurrentDownload = null;
    
    /*
    private Account mAccount;
    private String mFilePath;
    private String mRemotePath;
    private long mTotalDownloadSize;
    private long mCurrentDownloadSize;
    */
    
    private NotificationManager mNotificationMngr;
    private Notification mNotification;
    private int mLastPercent;
    
    
    /**
     * Static map with the files being download and the path to the temporal file were are download
     */
    //private static Set<String> mDownloadsInProgress = Collections.synchronizedSet(new HashSet<String>());
    
    /**
     * Returns True when the file referred by 'remotePath' in the ownCloud account 'account' is downloading
     */
    /*public static boolean isDownloading(Account account, String remotePath) {
        return (mDownloadsInProgress.contains(buildRemoteName(account.name, remotePath)));
    }*/
    
    /**
     * Builds a key for mDownloadsInProgress from the accountName and remotePath
     */
    private static String buildRemoteName(String accountName, String remotePath) {
        return accountName + remotePath;
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
        mNotificationMngr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileDownladerThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
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
                !intent.hasExtra(EXTRA_FILE_PATH) ||
                !intent.hasExtra(EXTRA_REMOTE_PATH)
           ) {
            Log.e(TAG, "Not enough information provided in intent");
            return START_NOT_STICKY;
        }
        Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
        String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
        String remotePath = intent.getStringExtra(EXTRA_REMOTE_PATH);
        long totalDownloadSize = intent.getLongExtra(EXTRA_FILE_SIZE, -1);

        AbstractList<String> requestedDownloads = new Vector<String>(); // dvelasco: now this will always contain just one element, but that can change in a near future
        String downloadKey = buildRemoteName(account.name, remotePath);
        try {
            DownloadFileOperation newDownload = new DownloadFileOperation(account, filePath, remotePath, (String)null, totalDownloadSize, false); 
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
         * @param remotePath    URL to the remote file in the queue of downloads.
         */
        public void cancel(Account account, String remotePath) {
            synchronized (mPendingDownloads) {
                DownloadFileOperation download = mPendingDownloads.remove(buildRemoteName(account.name, remotePath));
                if (download != null) {
                    download.cancel();
                }
            }
        }
        
        
        /**
         * Returns True when the file referred by 'remotePath' in the ownCloud account 'account' is downloading
         * 
         * @param account       Owncloud account where the remote file is stored.
         * @param remotePath    URL to the remote file in the queue of downloads.
         */
        public boolean isDownloading(Account account, String remotePath) {
            synchronized (mPendingDownloads) {
                return (mPendingDownloads.containsKey(buildRemoteName(account.name, remotePath)));
            }
        }
    }
    
    /** 
     * Download worker. Performs the pending downloads in the order they were requested. 
     * 
     * Created with the Looper of a new thread, started in {@link FileUploader#onCreate()}. 
     */
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            AbstractList<String> requestedDownloads = (AbstractList<String>) msg.obj;
            if (msg.obj != null) {
                Iterator<String> it = requestedDownloads.iterator();
                while (it.hasNext()) {
                    downloadFile(it.next());
                }
            }
            stopSelf(msg.arg1);
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
            if (mDownloadClient == null || mLastAccount != mCurrentDownload.getAccount()) {
                mLastAccount = mCurrentDownload.getAccount();
                mDownloadClient = OwnCloudClientUtils.createOwnCloudClient(mLastAccount, getApplicationContext());
            }

            /// perform the download
            //mDownloadsInProgress.add(buildRemoteName(mLastAccount.name, mCurrentDownload.getRemotePath()));
            RemoteOperationResult downloadResult = null;
            File newLocalFile = null;
            //try {
                downloadResult = mCurrentDownload.execute(mDownloadClient);
                if (downloadResult.isSuccess()) {
                    ContentValues cv = new ContentValues();
                    newLocalFile = new File(getSavePath(mCurrentDownload.getAccount().name) + mCurrentDownload.getLocalPath());
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, newLocalFile.getAbsolutePath());
                    getContentResolver().update(
                            ProviderTableMeta.CONTENT_URI,
                            cv,
                            ProviderTableMeta.FILE_NAME + "=? AND "
                                    + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                    new String[] {
                                    mCurrentDownload.getLocalPath().substring(mCurrentDownload.getLocalPath().lastIndexOf('/') + 1),
                                    mLastAccount.name });
                }
            
            /*} finally {
                mDownloadsInProgress.remove(buildRemoteName(mLastAccount.name, mCurrentDownload.getRemotePath()));
            }*/
        
            mPendingDownloads.remove(downloadKey);
            
            /// notify result
            notifyDownloadResult(mCurrentDownload, downloadResult);
            
            sendFinalBroadcast(mCurrentDownload, downloadResult, (downloadResult.isSuccess())? newLocalFile.getAbsolutePath():null);
        }
    }

    
    /**
     * Callback method to update the progress bar in the status notification.
     */
    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName) {
        int percent = (int)(100.0*((double)totalTransferredSoFar)/((double)totalToTransfer));
        if (percent != mLastPercent) {
          mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, totalToTransfer == -1);
          mNotification.contentView.setTextViewText(R.id.status_text, String.format(getString(R.string.downloader_download_in_progress_content), percent, fileName));
          mNotificationMngr.notify(R.string.downloader_download_in_progress_ticker, mNotification);
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
     * Creates a status notification to show the download progress
     * 
     * @param download  Download operation starting.
     */
    private void notifyDownloadStart(DownloadFileOperation download) {
        /// create status notification to show the download progress
        mLastPercent = 0;
        mNotification = new Notification(R.drawable.icon, getString(R.string.downloader_download_in_progress_ticker), System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, download.getSize() == -1);
        mNotification.contentView.setTextViewText(R.id.status_text, String.format(getString(R.string.downloader_download_in_progress_content), 0, new File(download.getLocalPath()).getName()));
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        // TODO put something smart in the contentIntent below
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationMngr.notify(R.string.downloader_download_in_progress_ticker, mNotification);
    }

    
    /**
     * Updates the status notification with the result of a download operation.
     * 
     * @param downloadResult    Result of the download operation.
     * @param download          Finished download operation
     */
    private void notifyDownloadResult(DownloadFileOperation download, RemoteOperationResult downloadResult) {
        mNotificationMngr.cancel(R.string.downloader_download_in_progress_ticker);
        if (!downloadResult.isCancelled()) {
            int tickerId = (downloadResult.isSuccess()) ? R.string.downloader_download_succeeded_ticker : R.string.downloader_download_failed_ticker;
            int contentId = (downloadResult.isSuccess()) ? R.string.downloader_download_succeeded_content : R.string.downloader_download_failed_content;
            Notification finalNotification = new Notification(R.drawable.icon, getString(tickerId), System.currentTimeMillis());
            finalNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            // TODO put something smart in the contentIntent below
            finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            finalNotification.setLatestEventInfo(getApplicationContext(), getString(tickerId), String.format(getString(contentId), new File(download.getLocalPath()).getName()), finalNotification.contentIntent);
            mNotificationMngr.notify(tickerId, finalNotification);
        }
    }
    
    
    /**
     * Sends a broadcast in order to the interested activities can update their view
     * 
     * @param download          Finished download operation
     * @param downloadResult    Result of the download operation
     * @param newFilePath       Absolute path to the downloaded file
     */
    private void sendFinalBroadcast(DownloadFileOperation download, RemoteOperationResult downloadResult, String newFilePath) {
        Intent end = new Intent(DOWNLOAD_FINISH_MESSAGE);
        end.putExtra(EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess());
        end.putExtra(ACCOUNT_NAME, download.getAccount().name);
        end.putExtra(EXTRA_REMOTE_PATH, download.getRemotePath());
        if (downloadResult.isSuccess()) {
            end.putExtra(EXTRA_FILE_PATH, newFilePath);
        }
        sendBroadcast(end);
    }

}
