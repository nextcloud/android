package com.owncloud.android.files.services;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.webdav.OnDatatransferProgressListener;

import com.owncloud.android.network.OwnCloudClientUtils;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
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

    private NotificationManager mNotificationMngr;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Account mAccount;
    private String mFilePath;
    private String mRemotePath;
    private int mLastPercent;
    private long mTotalDownloadSize;
    private long mCurrentDownloadSize;
    private Notification mNotification;
    
    /**
     * Static map with the files being download and the path to the temporal file were are download
     */
    private static Map<String, String> mDownloadsInProgress = Collections.synchronizedMap(new HashMap<String, String>());
    
    /**
     * Returns True when the file referred by 'remotePath' in the ownCloud account 'account' is downloading
     */
    public static boolean isDownloading(Account account, String remotePath) {
        return (mDownloadsInProgress.get(buildRemoteName(account.name, remotePath)) != null);
    }
    
    /**
     * Builds a key for mDownloadsInProgress from the accountName and remotePath
     */
    private static String buildRemoteName(String accountName, String remotePath) {
        return accountName + remotePath;
    }

    
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            downloadFile();
            stopSelf(msg.arg1);
        }
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

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationMngr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileDownladerThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (    !intent.hasExtra(EXTRA_ACCOUNT) ||
                !intent.hasExtra(EXTRA_FILE_PATH) ||
                !intent.hasExtra(EXTRA_REMOTE_PATH)
           ) {
            Log.e(TAG, "Not enough information provided in intent");
            return START_NOT_STICKY;
        }
        mAccount = intent.getParcelableExtra(EXTRA_ACCOUNT);
        mFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
        mRemotePath = intent.getStringExtra(EXTRA_REMOTE_PATH);
        mTotalDownloadSize = intent.getLongExtra(EXTRA_FILE_SIZE, -1);
        mCurrentDownloadSize = mLastPercent = 0;

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    /**
     * Core download method: requests the file to download and stores it.
     */
    private void downloadFile() {
        boolean downloadResult = false;

        /// prepare client object to send the request to the ownCloud server
        WebdavClient wdc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getApplicationContext());
        wdc.setDataTransferProgressListener(this);
        
        /// download will be in a temporal file
        File tmpFile = new File(getTemporalPath(mAccount.name) + mFilePath);
        
        /// create status notification to show the download progress
        mNotification = new Notification(R.drawable.icon, getString(R.string.downloader_download_in_progress_ticker), System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, mTotalDownloadSize == -1);
        mNotification.contentView.setTextViewText(R.id.status_text, String.format(getString(R.string.downloader_download_in_progress_content), 0, tmpFile.getName()));
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        // TODO put something smart in the contentIntent below
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationMngr.notify(R.string.downloader_download_in_progress_ticker, mNotification);
        

        /// perform the download
        tmpFile.getParentFile().mkdirs();
        mDownloadsInProgress.put(buildRemoteName(mAccount.name, mRemotePath), tmpFile.getAbsolutePath());
        File newFile = null;
        try {
            if (wdc.downloadFile(mRemotePath, tmpFile)) {
                newFile = new File(getSavePath(mAccount.name) + mFilePath);
                newFile.getParentFile().mkdirs();
                boolean moved = tmpFile.renameTo(newFile);
            
                if (moved) {
                    ContentValues cv = new ContentValues();
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, newFile.getAbsolutePath());
                    getContentResolver().update(
                            ProviderTableMeta.CONTENT_URI,
                            cv,
                            ProviderTableMeta.FILE_NAME + "=? AND "
                                    + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                            new String[] {
                                mFilePath.substring(mFilePath.lastIndexOf('/') + 1),
                                mAccount.name });
                    downloadResult = true;
                }
            }
        } finally {
            mDownloadsInProgress.remove(buildRemoteName(mAccount.name, mRemotePath));
        }

        
        /// notify result
        mNotificationMngr.cancel(R.string.downloader_download_in_progress_ticker);
        int tickerId = (downloadResult) ? R.string.downloader_download_succeeded_ticker : R.string.downloader_download_failed_ticker;
        int contentId = (downloadResult) ? R.string.downloader_download_succeeded_content : R.string.downloader_download_failed_content;
        Notification finalNotification = new Notification(R.drawable.icon, getString(tickerId), System.currentTimeMillis());
        finalNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        // TODO put something smart in the contentIntent below
        finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        finalNotification.setLatestEventInfo(getApplicationContext(), getString(tickerId), String.format(getString(contentId), tmpFile.getName()), finalNotification.contentIntent);
        mNotificationMngr.notify(tickerId, finalNotification);
            
        sendFinalBroadcast(downloadResult, (downloadResult)?newFile.getAbsolutePath():null);
    }

    /**
     * Callback method to update the progress bar in the status notification.
     */
    @Override
    public void transferProgress(long progressRate) {
        mCurrentDownloadSize += progressRate;
        int percent = (int)(100.0*((double)mCurrentDownloadSize)/((double)mTotalDownloadSize));
        if (percent != mLastPercent) {
          mNotification.contentView.setProgressBar(R.id.status_progress, 100, (int)(100*mCurrentDownloadSize/mTotalDownloadSize), mTotalDownloadSize == -1);
          mNotification.contentView.setTextViewText(R.id.status_text, String.format(getString(R.string.downloader_download_in_progress_content), percent, new File(mFilePath).getName()));
          mNotificationMngr.notify(R.string.downloader_download_in_progress_ticker, mNotification);
        }
        
        mLastPercent = percent;
    }
    

    /**
     * Sends a broadcast in order to the interested activities can update their view
     * 
     * @param downloadResult        'True' if the download was successful
     * @param newFilePath           Absolute path to the download file
     */
    private void sendFinalBroadcast(boolean downloadResult, String newFilePath) {
        Intent end = new Intent(DOWNLOAD_FINISH_MESSAGE);
        end.putExtra(EXTRA_DOWNLOAD_RESULT, downloadResult);
        end.putExtra(ACCOUNT_NAME, mAccount.name);
        end.putExtra(EXTRA_REMOTE_PATH, mRemotePath);
        if (downloadResult) {
            end.putExtra(EXTRA_FILE_PATH, newFilePath);
        }
        sendBroadcast(end);
    }

}
