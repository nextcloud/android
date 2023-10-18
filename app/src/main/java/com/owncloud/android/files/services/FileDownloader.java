/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2016 ownCloud Inc.
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

package com.owncloud.android.files.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Pair;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.files.downloader.DownloadTask;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.DownloadType;
import com.owncloud.android.providers.DocumentsStorageProvider;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.SendShareDialog;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;
import java.security.SecureRandom;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.inject.Inject;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import dagger.android.AndroidInjection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class FileDownloader extends Service
        implements OnDatatransferProgressListener, OnAccountsUpdateListener {

    public static final String EXTRA_USER = "USER";
    public static final String EXTRA_FILE = "FILE";

    private static final String DOWNLOAD_ADDED_MESSAGE = "DOWNLOAD_ADDED";
    private static final String DOWNLOAD_FINISH_MESSAGE = "DOWNLOAD_FINISH";
    public static final String EXTRA_DOWNLOAD_RESULT = "RESULT";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_LINKED_TO_PATH = "LINKED_TO";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String DOWNLOAD_TYPE = "DOWNLOAD_TYPE";

    private static final int FOREGROUND_SERVICE_ID = 412;

    private static final String TAG = FileDownloader.class.getSimpleName();

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private OwnCloudClient mDownloadClient;
    private Optional<User> currentUser = Optional.empty();
    private FileDataStorageManager mStorageManager;

    private IndexedForest<DownloadFileOperation> mPendingDownloads = new IndexedForest<>();

    private DownloadFileOperation mCurrentDownload;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mLastPercent;

    private Notification mNotification;

    private long conflictUploadId;

    public boolean mStartedDownload = false;

    @Inject UserAccountManager accountManager;
    @Inject UploadsStorageManager uploadsStorageManager;
    @Inject LocalBroadcastManager localBroadcastManager;
    @Inject ViewThemeUtils viewThemeUtils;

    public static String getDownloadAddedMessage() {
        return FileDownloader.class.getName() + DOWNLOAD_ADDED_MESSAGE;
    }

    public static String getDownloadFinishMessage() {
        return FileDownloader.class.getName() + DOWNLOAD_FINISH_MESSAGE;
    }

    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);
        Log_OC.d(TAG, "Creating service");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileDownloaderThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileDownloaderBinder();

        NotificationCompat.Builder builder = NotificationUtils.newNotificationBuilder(this, viewThemeUtils).setContentTitle(
            getApplicationContext().getResources().getString(R.string.app_name))
            .setContentText(getApplicationContext().getResources().getString(R.string.foreground_service_download))
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notification_icon));


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD);
        }

        mNotification = builder.build();

        // add AccountsUpdatedListener
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addOnAccountsUpdatedListener(this, null, false);
    }


    /**
     * Service clean up
     */
    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "Destroying service");
        mBinder = null;
        mServiceHandler = null;
        mServiceLooper.quit();
        mServiceLooper = null;
        mNotificationManager = null;

        // remove AccountsUpdatedListener
        AccountManager am = AccountManager.get(getApplicationContext());
        am.removeOnAccountsUpdatedListener(this);
        super.onDestroy();
    }


    /**
     * Entry point to add one or several files to the queue of downloads.
     *
     * New downloads are added calling to startService(), resulting in a call to this method.
     * This ensures the service will keep on working although the caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command with id " + startId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_SERVICE_ID, mNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(FOREGROUND_SERVICE_ID, mNotification);
        }

        if (intent == null || !intent.hasExtra(EXTRA_USER) || !intent.hasExtra(EXTRA_FILE)) {
            Log_OC.e(TAG, "Not enough information provided in intent");
            return START_NOT_STICKY;
        } else {
            final User user = intent.getParcelableExtra(EXTRA_USER);
            final OCFile file = intent.getParcelableExtra(EXTRA_FILE);
            final String behaviour = intent.getStringExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR);

            DownloadType downloadType = DownloadType.DOWNLOAD;
            if (intent.hasExtra(DOWNLOAD_TYPE)) {
                downloadType = (DownloadType) intent.getSerializableExtra(DOWNLOAD_TYPE);
            }
            String activityName = intent.getStringExtra(SendShareDialog.ACTIVITY_NAME);
            String packageName = intent.getStringExtra(SendShareDialog.PACKAGE_NAME);
            conflictUploadId = intent.getLongExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, -1);
            AbstractList<String> requestedDownloads = new Vector<String>();
            try {
                DownloadFileOperation newDownload = new DownloadFileOperation(user,
                                                                              file,
                                                                              behaviour,
                                                                              activityName,
                                                                              packageName,
                                                                              getBaseContext(),
                                                                              downloadType);
                newDownload.addDatatransferProgressListener(this);
                newDownload.addDatatransferProgressListener((FileDownloaderBinder) mBinder);
                Pair<String, String> putResult = mPendingDownloads.putIfAbsent(user.getAccountName(),
                                                                               file.getRemotePath(),
                                                                               newDownload);
                if (putResult != null) {
                    String downloadKey = putResult.first;
                    requestedDownloads.add(downloadKey);
                    sendBroadcastNewDownload(newDownload, putResult.second);
                }   // else, file already in the queue of downloads; don't repeat the request

            } catch (IllegalArgumentException e) {
                Log_OC.e(TAG, "Not enough information provided in intent: " + e.getMessage());
                return START_NOT_STICKY;
            }

            if (requestedDownloads.size() > 0) {
                Message msg = mServiceHandler.obtainMessage();
                msg.arg1 = startId;
                msg.obj = requestedDownloads;
                mServiceHandler.sendMessage(msg);
            }
        }

        return START_NOT_STICKY;
    }

    /**
     * Provides a binder object that clients can use to perform operations on the queue of downloads,
     * excepting the addition of new files.
     *
     * Implemented to perform cancellation, pause and resume of existing downloads.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    /**
     * Called when ALL the bound clients were onbound.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        ((FileDownloaderBinder) mBinder).clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
         //review the current download and cancel it if its account doesn't exist
        if (mCurrentDownload != null && !accountManager.exists(mCurrentDownload.getUser().toPlatformAccount())) {
            mCurrentDownload.cancel();
        }
        // The rest of downloads are cancelled when they try to start
    }


    /**
     * Binder to let client components to perform operations on the queue of downloads.
     * <p/>
     * It provides by itself the available operations.
     */
    public class FileDownloaderBinder extends Binder implements OnDatatransferProgressListener {

        /**
         * Map of listeners that will be reported about progress of downloads from a
         * {@link FileDownloaderBinder}
         * instance.
         */
        private Map<Long, OnDatatransferProgressListener> mBoundListeners =
                new HashMap<Long, OnDatatransferProgressListener>();


        /**
         * Cancels a pending or current download of a remote file.
         *
         * @param account ownCloud account where the remote file is stored.
         * @param file    A file in the queue of pending downloads
         */
        public void cancel(Account account, OCFile file) {
            Pair<DownloadFileOperation, String> removeResult =
                mPendingDownloads.remove(account.name, file.getRemotePath());
            DownloadFileOperation download = removeResult.first;
            if (download != null) {
                download.cancel();
            } else {
                if (mCurrentDownload != null && currentUser.isPresent() &&
                    mCurrentDownload.getRemotePath().startsWith(file.getRemotePath()) &&
                        account.name.equals(currentUser.get().getAccountName())) {
                    mCurrentDownload.cancel();
                }
            }
        }

        /**
         * Cancels all the downloads for an account
         */
        public void cancel(String accountName) {
            if (mCurrentDownload != null && mCurrentDownload.getUser().nameEquals(accountName)) {
                mCurrentDownload.cancel();
            }
            // Cancel pending downloads
            cancelPendingDownloads(accountName);
        }

        public void clearListeners() {
            mBoundListeners.clear();
        }


        /**
         * Returns True when the file described by 'file' in the ownCloud account 'account'
         * is downloading or waiting to download.
         *
         * If 'file' is a directory, returns 'true' if any of its descendant files is downloading or
         * waiting to download.
         *
         * @param user    user where the remote file is stored.
         * @param file    A file that could be in the queue of downloads.
         */
        public boolean isDownloading(User user, OCFile file) {
            return user != null && file != null && mPendingDownloads.contains(user.getAccountName(), file.getRemotePath());
        }


        /**
         * Adds a listener interested in the progress of the download for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param file     {@link OCFile} of interest for listener.
         */
        public void addDatatransferProgressListener(OnDatatransferProgressListener listener, OCFile file) {
            if (file == null || listener == null) {
                return;
            }
            mBoundListeners.put(file.getFileId(), listener);
        }


        /**
         * Removes a listener interested in the progress of the download for a concrete file.
         *
         * @param listener      Object to notify about progress of transfer.
         * @param file          {@link OCFile} of interest for listener.
         */
        public void removeDatatransferProgressListener(OnDatatransferProgressListener listener, OCFile file) {
            if (file == null || listener == null) {
                return;
            }
            Long fileId = file.getFileId();
            if (mBoundListeners.get(fileId) == listener) {
                mBoundListeners.remove(fileId);
            }
        }

        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar,
                                       long totalToTransfer, String fileName) {
            OnDatatransferProgressListener boundListener =
                    mBoundListeners.get(mCurrentDownload.getFile().getFileId());
            if (boundListener != null) {
                boundListener.onTransferProgress(progressRate, totalTransferredSoFar,
                                                 totalToTransfer, fileName);
            }
        }

    }

    /**
     * Download worker. Performs the pending downloads in the order they were requested.

     * Created with the Looper of a new thread, started in {@link FileUploader#onCreate()}.
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will warn about a
        // possible memory leak
        FileDownloader mService;

        public ServiceHandler(Looper looper, FileDownloader service) {
            super(looper);
            if (service == null) {
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            }
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            AbstractList<String> requestedDownloads = (AbstractList<String>) msg.obj;
            if (msg.obj != null) {
                Iterator<String> it = requestedDownloads.iterator();
                while (it.hasNext()) {
                    String next = it.next();
                    mService.downloadFile(next);
                }
            }
            mService.mStartedDownload=false;

            (new Handler()).postDelayed(() -> {
                if(!mService.mStartedDownload){
                    mService.mNotificationManager.cancel(R.string.downloader_download_in_progress_ticker);
                }
                Log_OC.d(TAG, "Stopping after command with id " + msg.arg1);
                mService.mNotificationManager.cancel(FOREGROUND_SERVICE_ID);
                mService.stopForeground(true);
                mService.stopSelf(msg.arg1);
            }, 2000);
        }
    }


    /**
     * Core download method: requests a file to download and stores it.
     *
     * @param downloadKey Key to access the download to perform, contained in mPendingDownloads
     */
    private void downloadFile(String downloadKey) {

        mStartedDownload = true;
        mCurrentDownload = mPendingDownloads.get(downloadKey);

        if (mCurrentDownload != null) {
            // Detect if the account exists
            if (accountManager.exists(mCurrentDownload.getUser().toPlatformAccount())) {
                notifyDownloadStart(mCurrentDownload);
                RemoteOperationResult downloadResult = null;
                try {
                    /// prepare client object to send the request to the ownCloud server
                    Account currentDownloadAccount = mCurrentDownload.getUser().toPlatformAccount();
                    Optional<User> currentDownloadUser = accountManager.getUser(currentDownloadAccount.name);
                    if (!currentUser.equals(currentDownloadUser)) {
                        currentUser = currentDownloadUser;
                        mStorageManager = new FileDataStorageManager(currentUser.get(), getContentResolver());
                    }   // else, reuse storage manager from previous operation

                    // always get client from client manager, to get fresh credentials in case
                    // of update
                    OwnCloudAccount ocAccount = currentDownloadUser.get().toOwnCloudAccount();
                    mDownloadClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, this);


                    /// perform the download
                    downloadResult = mCurrentDownload.execute(mDownloadClient);
                    if (downloadResult.isSuccess() && mCurrentDownload.getDownloadType() == DownloadType.DOWNLOAD) {
                        saveDownloadedFile();
                    }

                } catch (Exception e) {
                    Log_OC.e(TAG, "Error downloading", e);
                    downloadResult = new RemoteOperationResult(e);

                } finally {
                    Pair<DownloadFileOperation, String> removeResult = mPendingDownloads.removePayload(
                        mCurrentDownload.getUser().getAccountName(), mCurrentDownload.getRemotePath());

                    if (downloadResult == null) {
                        downloadResult = new RemoteOperationResult(new RuntimeException("Error downloading…"));
                    }

                    /// notify result
                    notifyDownloadResult(mCurrentDownload, downloadResult);
                    sendBroadcastDownloadFinished(mCurrentDownload, downloadResult, removeResult.second);
                }
            } else {
                cancelPendingDownloads(mCurrentDownload.getUser().getAccountName());
            }
        }
    }


    /**
     * Updates the OC File after a successful download.
     *
     * TODO move to DownloadFileOperation
     *  unify with code from {@link DocumentsStorageProvider} and {@link DownloadTask}.
     */
    private void saveDownloadedFile() {
        OCFile file = mStorageManager.getFileById(mCurrentDownload.getFile().getFileId());

        if (file == null) {
            // try to get file via path, needed for overwriting existing files on conflict dialog
            file = mStorageManager.getFileByDecryptedRemotePath(mCurrentDownload.getFile().getRemotePath());
        }

        if (file == null) {
            Log_OC.e(this, "Could not save " + mCurrentDownload.getFile().getRemotePath());
            return;
        }

        long syncDate = System.currentTimeMillis();
        file.setLastSyncDateForProperties(syncDate);
        file.setLastSyncDateForData(syncDate);
        file.setUpdateThumbnailNeeded(true);
        file.setModificationTimestamp(mCurrentDownload.getModificationTimestamp());
        file.setModificationTimestampAtLastSyncForData(mCurrentDownload.getModificationTimestamp());
        file.setEtag(mCurrentDownload.getEtag());
        file.setMimeType(mCurrentDownload.getMimeType());
        file.setStoragePath(mCurrentDownload.getSavePath());
        file.setFileLength(new File(mCurrentDownload.getSavePath()).length());
        file.setRemoteId(mCurrentDownload.getFile().getRemoteId());
        mStorageManager.saveFile(file);
        if (MimeTypeUtil.isMedia(mCurrentDownload.getMimeType())) {
            FileDataStorageManager.triggerMediaScan(file.getStoragePath(), file);
        }
        mStorageManager.saveConflict(file, null);
    }

    /**
     * Creates a status notification to show the download progress
     *
     * @param download Download operation starting.
     */
    private void notifyDownloadStart(DownloadFileOperation download) {
        /// create status notification with a progress bar
        mLastPercent = 0;
        mNotificationBuilder = NotificationUtils.newNotificationBuilder(this, viewThemeUtils);
        mNotificationBuilder
            .setSmallIcon(R.drawable.notification_icon)
            .setTicker(getString(R.string.downloader_download_in_progress_ticker))
            .setContentTitle(getString(R.string.downloader_download_in_progress_ticker))
            .setOngoing(true)
            .setProgress(100, 0, download.getSize() < 0)
            .setContentText(
                String.format(getString(R.string.downloader_download_in_progress_content), 0,
                              new File(download.getSavePath()).getName())
                           );

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mNotificationBuilder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD);
        }

        /// includes a pending intent in the notification showing the details view of the file
        Intent showDetailsIntent = null;
        if (PreviewImageFragment.canBePreviewed(download.getFile())) {
            showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        } else {
            showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        }
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, download.getFile());
        showDetailsIntent.putExtra(FileActivity.EXTRA_USER, download.getUser());
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                                                                        showDetailsIntent, PendingIntent.FLAG_IMMUTABLE));


        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        if (mNotificationManager != null) {
            mNotificationManager.notify(R.string.downloader_download_in_progress_ticker, mNotificationBuilder.build());
        }
    }


    /**
     * Callback method to update the progress bar in the status notification.
     */
    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar,
                                   long totalToTransfer, String filePath) {
        int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
        if (percent != mLastPercent) {
            mNotificationBuilder.setProgress(100, percent, totalToTransfer < 0);
            String fileName = filePath.substring(filePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1);
            String text = String.format(getString(R.string.downloader_download_in_progress_content), percent, fileName);
            mNotificationBuilder.setContentText(text);

            if (mNotificationManager == null) {
                mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            }

            if (mNotificationManager != null) {
                mNotificationManager.notify(R.string.downloader_download_in_progress_ticker,
                        mNotificationBuilder.build());
            }
        }
        mLastPercent = percent;
    }


    /**
     * Updates the status notification with the result of a download operation.
     *
     * @param downloadResult Result of the download operation.
     * @param download       Finished download operation
     */
    @SuppressFBWarnings("DMI")
    private void notifyDownloadResult(DownloadFileOperation download,
                                      RemoteOperationResult downloadResult) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        if (!downloadResult.isCancelled()) {
            if (downloadResult.isSuccess()) {
                if (conflictUploadId > 0) {
                    uploadsStorageManager.removeUpload(conflictUploadId);
                }
                // Dont show notification except an error has occured.
                return;
            }
            int tickerId = downloadResult.isSuccess() ?
                    R.string.downloader_download_succeeded_ticker : R.string.downloader_download_failed_ticker;

            boolean needsToUpdateCredentials = ResultCode.UNAUTHORIZED == downloadResult.getCode();
            tickerId = needsToUpdateCredentials ?
                    R.string.downloader_download_failed_credentials_error : tickerId;

            mNotificationBuilder
                    .setTicker(getString(tickerId))
                    .setContentTitle(getString(tickerId))
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setProgress(0, 0, false);

            if (needsToUpdateCredentials) {
                configureUpdateCredentialsNotification(download.getUser());

            } else {
                // TODO put something smart in showDetailsIntent
                Intent showDetailsIntent = new Intent();
                mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                                                                                showDetailsIntent, PendingIntent.FLAG_IMMUTABLE));
            }

            mNotificationBuilder.setContentText(ErrorMessageAdapter.getErrorCauseMessage(downloadResult,
                    download, getResources()));

            if (mNotificationManager != null) {
                mNotificationManager.notify((new SecureRandom()).nextInt(), mNotificationBuilder.build());

                // Remove success notification
                if (downloadResult.isSuccess()) {
                    // Sleep 2 seconds, so show the notification before remove it
                    NotificationUtils.cancelWithDelay(mNotificationManager,
                                                      R.string.downloader_download_succeeded_ticker, 2000);
                }
            }
        }
    }

    private void configureUpdateCredentialsNotification(User user) {
        // let the user update credentials with one click
        Intent updateAccountCredentials = new Intent(this, AuthenticatorActivity.class);
        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, user.toPlatformAccount());
        updateAccountCredentials.putExtra(
                AuthenticatorActivity.EXTRA_ACTION,
                AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN
        );
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND);
        mNotificationBuilder.setContentIntent(
            PendingIntent.getActivity(this,
                                      (int) System.currentTimeMillis(),
                                      updateAccountCredentials,
                                      PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE)
                                             );
    }


    /**
     * Sends a broadcast when a download finishes in order to the interested activities can
     * update their view
     *
     * @param download               Finished download operation
     * @param downloadResult         Result of the download operation
     * @param unlinkedFromRemotePath Path in the downloads tree where the download was unlinked from
     */
    private void sendBroadcastDownloadFinished(
            DownloadFileOperation download,
            RemoteOperationResult downloadResult,
            String unlinkedFromRemotePath) {

        Intent end = new Intent(getDownloadFinishMessage());
        end.putExtra(EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess());
        end.putExtra(ACCOUNT_NAME, download.getUser().getAccountName());
        end.putExtra(EXTRA_REMOTE_PATH, download.getRemotePath());
        end.putExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR, download.getBehaviour());
        end.putExtra(SendShareDialog.ACTIVITY_NAME, download.getActivityName());
        end.putExtra(SendShareDialog.PACKAGE_NAME, download.getPackageName());
        if (unlinkedFromRemotePath != null) {
            end.putExtra(EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath);
        }
        end.setPackage(getPackageName());
        localBroadcastManager.sendBroadcast(end);
    }


    /**
     * Sends a broadcast when a new download is added to the queue.
     *
     * @param download           Added download operation
     * @param linkedToRemotePath Path in the downloads tree where the download was linked to
     */
    private void sendBroadcastNewDownload(DownloadFileOperation download,
                                          String linkedToRemotePath) {
        Intent added = new Intent(getDownloadAddedMessage());
        added.putExtra(ACCOUNT_NAME, download.getUser().getAccountName());
        added.putExtra(EXTRA_REMOTE_PATH, download.getRemotePath());
        added.putExtra(EXTRA_LINKED_TO_PATH, linkedToRemotePath);
        added.setPackage(getPackageName());
        localBroadcastManager.sendBroadcast(added);
    }

    private void cancelPendingDownloads(String accountName) {
        mPendingDownloads.remove(accountName);
    }
}
