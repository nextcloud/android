/**
 *  ownCloud Android client application
 *
 *  @author Bartek Przybylski
 *  @author masensio
 *  @author LukeOwnCloud
 *  @author David A. Velasco
 *  @author Chris Narkiewicz
 *
 *  Copyright (C) 2012 Bartek Przybylski
 *  Copyright (C) 2012-2016 ownCloud Inc.
 *  Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.files.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.util.Pair;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.BatteryStatus;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.Connectivity;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.UploadListActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import dagger.android.AndroidInjection;

/**
 * Service for uploading files. Invoke using context.startService(...).
 *
 * Files to be uploaded are stored persistently using {@link UploadsStorageManager}.
 *
 * On next invocation of {@link FileUploader} uploaded files which previously failed will be uploaded again until either
 * upload succeeded or a fatal error occurred.
 *
 * Every file passed to this service is uploaded. No filtering is performed. However, Intent keys (e.g., KEY_WIFI_ONLY)
 * are obeyed.
 */
public class FileUploader extends Service
    implements OnDatatransferProgressListener, OnAccountsUpdateListener, UploadFileOperation.OnRenameListener {

    private static final String TAG = FileUploader.class.getSimpleName();

    private static final String UPLOADS_ADDED_MESSAGE = "UPLOADS_ADDED";
    private static final String UPLOAD_START_MESSAGE = "UPLOAD_START";
    private static final String UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH";
    public static final String EXTRA_UPLOAD_RESULT = "RESULT";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_OLD_REMOTE_PATH = "OLD_REMOTE_PATH";
    public static final String EXTRA_OLD_FILE_PATH = "OLD_FILE_PATH";
    public static final String EXTRA_LINKED_TO_PATH = "LINKED_TO";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";

    private static final int FOREGROUND_SERVICE_ID = 411;

    public static final String KEY_FILE = "FILE";
    public static final String KEY_LOCAL_FILE = "LOCAL_FILE";
    public static final String KEY_REMOTE_FILE = "REMOTE_FILE";
    public static final String KEY_MIME_TYPE = "MIME_TYPE";

    /**
     * Call this Service with only this Intent key if all pending uploads are to be retried.
     */
    private static final String KEY_RETRY = "KEY_RETRY";
//    /**
//     * Call this Service with KEY_RETRY and KEY_RETRY_REMOTE_PATH to retry
//     * upload of file identified by KEY_RETRY_REMOTE_PATH.
//     */
//    private static final String KEY_RETRY_REMOTE_PATH = "KEY_RETRY_REMOTE_PATH";
    /**
     * Call this Service with KEY_RETRY and KEY_RETRY_UPLOAD to retry upload of file identified by KEY_RETRY_UPLOAD.
     */
    private static final String KEY_RETRY_UPLOAD = "KEY_RETRY_UPLOAD";
    /**
     * {@link Account} to which file is to be uploaded.
     */
    public static final String KEY_ACCOUNT = "ACCOUNT";

    /**
     * What {@link NameCollisionPolicy} to do when the file already exists on the remote.
     */
    public static final String KEY_NAME_COLLISION_POLICY = "KEY_NAME_COLLISION_POLICY";

    /**
     * Set to true if remote folder is to be created if it does not exist.
     */
    public static final String KEY_CREATE_REMOTE_FOLDER = "CREATE_REMOTE_FOLDER";
    /**
     * Key to signal what is the origin of the upload request
     */
    public static final String KEY_CREATED_BY = "CREATED_BY";

    public static final String KEY_WHILE_ON_WIFI_ONLY = "KEY_ON_WIFI_ONLY";

    /**
     * Set to true if upload is to performed only when phone is being charged.
     */
    public static final String KEY_WHILE_CHARGING_ONLY = "KEY_WHILE_CHARGING_ONLY";

    public static final String KEY_LOCAL_BEHAVIOUR = "BEHAVIOUR";

    /**
     * Set to true if the HTTP library should disable automatic retries of uploads.
     */
    public static final String KEY_DISABLE_RETRIES = "DISABLE_RETRIES";

    public static final int LOCAL_BEHAVIOUR_COPY = 0;
    public static final int LOCAL_BEHAVIOUR_MOVE = 1;
    public static final int LOCAL_BEHAVIOUR_FORGET = 2;
    public static final int LOCAL_BEHAVIOUR_DELETE = 3;


    private Notification mNotification;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private OwnCloudClient mUploadClient;
    private Account mCurrentAccount;
    private FileDataStorageManager mStorageManager;

    @Inject UserAccountManager accountManager;
    @Inject UploadsStorageManager mUploadsStorageManager;
    @Inject ConnectivityService connectivityService;
    @Inject PowerManagementService powerManagementService;
    @Inject LocalBroadcastManager localBroadcastManager;

    private IndexedForest<UploadFileOperation> mPendingUploads = new IndexedForest<>();

    /**
     * {@link UploadFileOperation} object of ongoing upload. Can be null. Note: There can only be one concurrent
     * upload!
     */
    private UploadFileOperation mCurrentUpload;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mLastPercent;


    @Override
    public void onRenameUpload() {
        mUploadsStorageManager.updateDatabaseUploadStart(mCurrentUpload);
        sendBroadcastUploadStarted(mCurrentUpload);
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
        HandlerThread thread = new HandlerThread("FileUploaderThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileUploaderBinder();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setContentTitle(
            getApplicationContext().getResources().getString(R.string.app_name))
            .setContentText(getApplicationContext().getResources().getString(R.string.foreground_service_upload))
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notification_icon))
            .setColor(ThemeUtils.primaryColor(getApplicationContext(), true));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD);
        }

        mNotification = builder.build();

        // TODO Add UploadResult.KILLED?
        int failedCounter = mUploadsStorageManager.failInProgressUploads(UploadResult.SERVICE_INTERRUPTED);
        if (failedCounter > 0) {
            resurrection();
        }

        // add AccountsUpdatedListener
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addOnAccountsUpdatedListener(this, null, false);
    }

    /**
     * Service clean-up when restarted after being killed
     */
    private void resurrection() {
        // remove stucked notification
        mNotificationManager.cancel(FOREGROUND_SERVICE_ID);
    }

    /**
     * Service clean up
     */
    @SuppressWarnings("PMD.NullAssignment")
    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "Destroying service");
        mBinder = null;
        mServiceHandler = null;
        mServiceLooper.quit();
        mServiceLooper = null;
        if (mNotificationManager != null) {
            mNotificationManager.cancel(FOREGROUND_SERVICE_ID);
        }
        mNotificationManager = null;

        // remove AccountsUpdatedListener
        AccountManager am = AccountManager.get(getApplicationContext());
        am.removeOnAccountsUpdatedListener(this);

        super.onDestroy();
    }

    /**
     * Entry point to add one or several files to the queue of uploads.
     *
     * New uploads are added calling to startService(), resulting in a call to this method. This ensures the service
     * will keep on working although the caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command with id " + startId);

        startForeground(FOREGROUND_SERVICE_ID, mNotification);

        if (intent == null) {
            Log_OC.e(TAG, "Intent is null");
            return Service.START_NOT_STICKY;
        }

        if (!intent.hasExtra(KEY_ACCOUNT)) {
            Log_OC.e(TAG, "Not enough information provided in intent");
            return Service.START_NOT_STICKY;
        }

        final Account account = intent.getParcelableExtra(KEY_ACCOUNT);
        if (account == null) {
            return Service.START_NOT_STICKY;
        }
        Optional<User> optionalUser = accountManager.getUser(account.name);
        if (!optionalUser.isPresent()) {
            return Service.START_NOT_STICKY;
        }
        final User user = optionalUser.get();

        boolean retry = intent.getBooleanExtra(KEY_RETRY, false);
        List<String> requestedUploads = new ArrayList<>();

        boolean onWifiOnly = intent.getBooleanExtra(KEY_WHILE_ON_WIFI_ONLY, false);
        boolean whileChargingOnly = intent.getBooleanExtra(KEY_WHILE_CHARGING_ONLY, false);

        if (!retry) { // Start new uploads
            if (!(intent.hasExtra(KEY_LOCAL_FILE) || intent.hasExtra(KEY_FILE))) {
                Log_OC.e(TAG, "Not enough information provided in intent");
                return Service.START_NOT_STICKY;
            }

            Integer error = gatherAndStartNewUploads(intent, user, requestedUploads, onWifiOnly, whileChargingOnly);
            if (error != null) {
                return error;
            }
        } else { // Retry uploads
            if (!intent.hasExtra(KEY_ACCOUNT) || !intent.hasExtra(KEY_RETRY_UPLOAD)) {
                Log_OC.e(TAG, "Not enough information provided in intent: no KEY_RETRY_UPLOAD_KEY");
                return START_NOT_STICKY;
            }
            retryUploads(intent, user, requestedUploads);
        }

        if (requestedUploads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedUploads;
            mServiceHandler.sendMessage(msg);
            sendBroadcastUploadsAdded();
        }
        return Service.START_NOT_STICKY;
    }

    /**
     * Gather and start new uploads.
     *
     * @return A {@link Service} constant in case of error, {@code null} otherwise.
     */
    @Nullable
    private Integer gatherAndStartNewUploads(
        Intent intent,
        User user,
        List<String> requestedUploads,
        boolean onWifiOnly,
        boolean whileChargingOnly
    ) {
        String[] localPaths = null;
        String[] remotePaths = null;
        String[] mimeTypes = null;
        OCFile[] files = null;

        if (intent.hasExtra(KEY_FILE)) {
            Parcelable[] files_temp = intent.getParcelableArrayExtra(KEY_FILE);
            files = new OCFile[files_temp.length];
            System.arraycopy(files_temp, 0, files, 0, files_temp.length);
        } else {
            localPaths = intent.getStringArrayExtra(KEY_LOCAL_FILE);
            remotePaths = intent.getStringArrayExtra(KEY_REMOTE_FILE);
            mimeTypes = intent.getStringArrayExtra(KEY_MIME_TYPE);
        }

        if (intent.hasExtra(KEY_FILE) && files == null) {
            Log_OC.e(TAG, "Incorrect array for OCFiles provided in upload intent");
            return Service.START_NOT_STICKY;
        } else if (!intent.hasExtra(KEY_FILE)) {
            if (localPaths == null) {
                Log_OC.e(TAG, "Incorrect array for local paths provided in upload intent");
                return Service.START_NOT_STICKY;
            }
            if (remotePaths == null) {
                Log_OC.e(TAG, "Incorrect array for remote paths provided in upload intent");
                return Service.START_NOT_STICKY;
            }
            if (localPaths.length != remotePaths.length) {
                Log_OC.e(TAG, "Different number of remote paths and local paths!");
                return Service.START_NOT_STICKY;
            }

            files = new OCFile[localPaths.length];
            for (int i = 0; i < localPaths.length; i++) {
                files[i] = UploadFileOperation.obtainNewOCFileToUpload(
                    remotePaths[i],
                    localPaths[i],
                    mimeTypes != null ? mimeTypes[i] : null
                );
                if (files[i] == null) {
                    Log_OC.e(TAG, "obtainNewOCFileToUpload() returned null for remotePaths[i]:" + remotePaths[i]
                        + " and localPaths[i]:" + localPaths[i]);
                    return Service.START_NOT_STICKY;
                }
            }
        }
        // at this point variable "OCFile[] files" is loaded correctly.

        NameCollisionPolicy nameCollisionPolicy = (NameCollisionPolicy) intent.getSerializableExtra(KEY_NAME_COLLISION_POLICY);
        if (nameCollisionPolicy == null) {
            nameCollisionPolicy = NameCollisionPolicy.DEFAULT;
        }
        int localAction = intent.getIntExtra(KEY_LOCAL_BEHAVIOUR, LOCAL_BEHAVIOUR_FORGET);
        boolean isCreateRemoteFolder = intent.getBooleanExtra(KEY_CREATE_REMOTE_FOLDER, false);
        int createdBy = intent.getIntExtra(KEY_CREATED_BY, UploadFileOperation.CREATED_BY_USER);
        boolean disableRetries = intent.getBooleanExtra(KEY_DISABLE_RETRIES, true);
        try {
            for (OCFile file : files) {
                startNewUpload(
                    user,
                    requestedUploads,
                    onWifiOnly,
                    whileChargingOnly,
                    nameCollisionPolicy,
                    localAction,
                    isCreateRemoteFolder,
                    createdBy,
                    file,
                    disableRetries
                              );
            }
        } catch (IllegalArgumentException e) {
            Log_OC.e(TAG, "Not enough information provided in intent: " + e.getMessage());
            return START_NOT_STICKY;
        } catch (IllegalStateException e) {
            Log_OC.e(TAG, "Bad information provided in intent: " + e.getMessage());
            return START_NOT_STICKY;
        } catch (Exception e) {
            Log_OC.e(TAG, "Unexpected exception while processing upload intent", e);
            return START_NOT_STICKY;
        }
        return null;
    }

    /**
     * Start a new {@link UploadFileOperation}.
     */
    private void startNewUpload(
        User user,
        List<String> requestedUploads,
        boolean onWifiOnly,
        boolean whileChargingOnly,
        NameCollisionPolicy nameCollisionPolicy,
        int localAction,
        boolean isCreateRemoteFolder,
        int createdBy,
        OCFile file,
        boolean disableRetries
                               ) {
        OCUpload ocUpload = new OCUpload(file, user.toPlatformAccount());
        ocUpload.setFileSize(file.getFileLength());
        ocUpload.setNameCollisionPolicy(nameCollisionPolicy);
        ocUpload.setCreateRemoteFolder(isCreateRemoteFolder);
        ocUpload.setCreatedBy(createdBy);
        ocUpload.setLocalAction(localAction);
        ocUpload.setUseWifiOnly(onWifiOnly);
        ocUpload.setWhileChargingOnly(whileChargingOnly);
        ocUpload.setUploadStatus(UploadStatus.UPLOAD_IN_PROGRESS);

        UploadFileOperation newUpload = new UploadFileOperation(
            mUploadsStorageManager,
            connectivityService,
            powerManagementService,
            user,
            file,
            ocUpload,
            nameCollisionPolicy,
            localAction,
            this,
            onWifiOnly,
            whileChargingOnly,
            disableRetries
        );
        newUpload.setCreatedBy(createdBy);
        if (isCreateRemoteFolder) {
            newUpload.setRemoteFolderToBeCreated();
        }
        newUpload.addDataTransferProgressListener(this);
        newUpload.addDataTransferProgressListener((FileUploaderBinder) mBinder);

        newUpload.addRenameUploadListener(this);

        Pair<String, String> putResult = mPendingUploads.putIfAbsent(
            user.getAccountName(),
            file.getRemotePath(),
            newUpload
        );

        if (putResult != null) {
            requestedUploads.add(putResult.first);

            // Save upload in database
            long id = mUploadsStorageManager.storeUpload(ocUpload);
            newUpload.setOCUploadId(id);
        }
    }

    /**
     * Retries a list of uploads.
     */
    private void retryUploads(Intent intent, User user, List<String> requestedUploads) {
        boolean onWifiOnly;
        boolean whileChargingOnly;
        OCUpload upload = intent.getParcelableExtra(KEY_RETRY_UPLOAD);

        onWifiOnly = upload.isUseWifiOnly();
        whileChargingOnly = upload.isWhileChargingOnly();

        UploadFileOperation newUpload = new UploadFileOperation(
            mUploadsStorageManager,
            connectivityService,
            powerManagementService,
            user,
            null,
            upload,
            upload.getNameCollisionPolicy(),
            upload.getLocalAction(),
            this,
            onWifiOnly,
            whileChargingOnly,
            true
        );

        newUpload.addDataTransferProgressListener(this);
        newUpload.addDataTransferProgressListener((FileUploaderBinder) mBinder);

        newUpload.addRenameUploadListener(this);

        Pair<String, String> putResult = mPendingUploads.putIfAbsent(
            user.getAccountName(),
            upload.getRemotePath(),
            newUpload
        );
        if (putResult != null) {
            String uploadKey = putResult.first;
            requestedUploads.add(uploadKey);

            // Update upload in database
            upload.setUploadStatus(UploadStatus.UPLOAD_IN_PROGRESS);
            mUploadsStorageManager.updateUpload(upload);
        }
    }

    /**
     * Provides a binder object that clients can use to perform operations on the queue of uploads, excepting the
     * addition of new files.
     *
     * Implemented to perform cancellation, pause and resume of existing uploads.
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
        ((FileUploaderBinder) mBinder).clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // Review current upload, and cancel it if its account doesn't exist
        if (mCurrentUpload != null && !accountManager.exists(mCurrentUpload.getAccount())) {
            mCurrentUpload.cancel(ResultCode.ACCOUNT_NOT_FOUND);
        }
        // The rest of uploads are cancelled when they try to start
    }

    /**
     * Core upload method: sends the file(s) to upload
     *
     * @param uploadKey Key to access the upload to perform, contained in mPendingUploads
     */
    public void uploadFile(String uploadKey) {
        mCurrentUpload = mPendingUploads.get(uploadKey);

        if (mCurrentUpload != null) {
            /// Check account existence
            if (!accountManager.exists(mCurrentUpload.getAccount())) {
                Log_OC.w(TAG, "Account " + mCurrentUpload.getAccount().name +
                    " does not exist anymore -> cancelling all its uploads");
                cancelUploadsForAccount(mCurrentUpload.getAccount());
                return;
            }

            /// OK, let's upload
            mUploadsStorageManager.updateDatabaseUploadStart(mCurrentUpload);

            notifyUploadStart(mCurrentUpload);

            sendBroadcastUploadStarted(mCurrentUpload);

            RemoteOperationResult uploadResult = null;

            try {
                /// prepare client object to send the request to the ownCloud server
                if (mCurrentAccount == null || !mCurrentAccount.equals(mCurrentUpload.getAccount())) {
                    mCurrentAccount = mCurrentUpload.getAccount();
                    mStorageManager = new FileDataStorageManager(mCurrentAccount, getContentResolver());
                }   // else, reuse storage manager from previous operation

                // always get client from client manager, to get fresh credentials in case of update
                OwnCloudAccount ocAccount = new OwnCloudAccount(mCurrentAccount, this);
                mUploadClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, this);


//                // If parent folder is encrypted, upload file encrypted
//                OCFile parent = mStorageManager.getFileByPath(mCurrentUpload.getFile().getParentRemotePath());

//                if (parent.isEncrypted()) {
//                    UploadEncryptedFileOperation uploadEncryptedFileOperation =
//                            new UploadEncryptedFileOperation(parent, mCurrentUpload);
//
//                    uploadResult = uploadEncryptedFileOperation.execute(mUploadClient, mStorageManager);
//                } else {
                /// perform the regular upload
                uploadResult = mCurrentUpload.execute(mUploadClient, mStorageManager);
//                }
            } catch (Exception e) {
                Log_OC.e(TAG, "Error uploading", e);
                uploadResult = new RemoteOperationResult(e);
            } finally {
                Pair<UploadFileOperation, String> removeResult;
                if (mCurrentUpload.wasRenamed()) {
                    removeResult = mPendingUploads.removePayload(
                        mCurrentAccount.name,
                        mCurrentUpload.getOldFile().getRemotePath()
                    );
                    // TODO: grant that name is also updated for mCurrentUpload.getOCUploadId

                } else {
                    removeResult = mPendingUploads.removePayload(mCurrentAccount.name,
                                                                 mCurrentUpload.getDecryptedRemotePath());
                }

                mUploadsStorageManager.updateDatabaseUploadResult(uploadResult, mCurrentUpload);

                /// notify result
                notifyUploadResult(mCurrentUpload, uploadResult);

                sendBroadcastUploadFinished(mCurrentUpload, uploadResult, removeResult.second);
            }

            // generate new Thumbnail
            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                new ThumbnailsCacheManager.ThumbnailGenerationTask(mStorageManager, mCurrentAccount);

            File file = new File(mCurrentUpload.getOriginalStoragePath());
            String remoteId = mCurrentUpload.getFile().getRemoteId();

            task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, remoteId));
        }
    }


    /**
     * Creates a status notification to show the upload progress
     *
     * @param upload Upload operation starting.
     */
    private void notifyUploadStart(UploadFileOperation upload) {
        // / create status notification with a progress bar
        mLastPercent = 0;
        mNotificationBuilder = NotificationUtils.newNotificationBuilder(this);
        mNotificationBuilder
            .setOngoing(true)
            .setSmallIcon(R.drawable.notification_icon)
            .setTicker(getString(R.string.uploader_upload_in_progress_ticker))
            .setContentTitle(getString(R.string.uploader_upload_in_progress_ticker))
            .setProgress(100, 0, false)
            .setContentText(
                String.format(getString(R.string.uploader_upload_in_progress_content), 0, upload.getFileName())
            );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationBuilder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD);
        }

        /// includes a pending intent in the notification showing the details
        Intent intent = UploadListActivity.createIntent(upload.getFile(),
                                                        upload.getAccount(),
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                                                        this);
        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this,
                                                                        (int) System.currentTimeMillis(),
                                                                        intent,
                                                                        0)
                                             );

        if (!upload.isInstantPicture() && !upload.isInstantVideo()) {
            if (mNotificationManager == null) {
                mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            }

            mNotificationManager.notify(FOREGROUND_SERVICE_ID, mNotificationBuilder.build());
        }   // else wait until the upload really start (onTransferProgress is called), so that if it's discarded
        // due to lack of Wifi, no notification is shown
        // TODO generalize for automated uploads
    }

    /**
     * Callback method to update the progress bar in the status notification
     */
    @Override
    public void onTransferProgress(
        long progressRate,
        long totalTransferredSoFar,
        long totalToTransfer,
        String filePath
    ) {
        int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
        if (percent != mLastPercent) {
            mNotificationBuilder.setProgress(100, percent, false);
            String fileName = filePath.substring(filePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1);
            String text = String.format(getString(R.string.uploader_upload_in_progress_content), percent, fileName);
            mNotificationBuilder.setContentText(text);
            if (mNotificationManager == null) {
                mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            }
            mNotificationManager.notify(FOREGROUND_SERVICE_ID, mNotificationBuilder.build());
        }
        mLastPercent = percent;
    }

    /**
     * Updates the status notification with the result of an upload operation.
     *
     * @param uploadResult Result of the upload operation.
     * @param upload       Finished upload operation
     */
    private void notifyUploadResult(UploadFileOperation upload, RemoteOperationResult uploadResult) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.getCode());
        // cancelled operation or success -> silent removal of progress notification
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        mNotificationManager.cancel(FOREGROUND_SERVICE_ID);

        // Only notify if the upload fails
        if (!uploadResult.isCancelled() &&
            !uploadResult.isSuccess() &&
            !ResultCode.LOCAL_FILE_NOT_FOUND.equals(uploadResult.getCode()) &&
            !uploadResult.getCode().equals(ResultCode.DELAYED_FOR_WIFI) &&
            !uploadResult.getCode().equals(ResultCode.DELAYED_FOR_CHARGING) &&
            !uploadResult.getCode().equals(ResultCode.DELAYED_IN_POWER_SAVE_MODE) &&
            !uploadResult.getCode().equals(ResultCode.LOCK_FAILED)) {

            int tickerId = R.string.uploader_upload_failed_ticker;

            String content;

            // check credentials error
            boolean needsToUpdateCredentials = uploadResult.getCode() == ResultCode.UNAUTHORIZED;
            if (needsToUpdateCredentials) {
                tickerId = R.string.uploader_upload_failed_credentials_error;
            } else if (uploadResult.getCode() == ResultCode.SYNC_CONFLICT) { // check file conflict
                tickerId = R.string.uploader_upload_failed_sync_conflict_error;
            }

            mNotificationBuilder
                .setTicker(getString(tickerId))
                .setContentTitle(getString(tickerId))
                .setAutoCancel(true)
                .setOngoing(false)
                .setProgress(0, 0, false);

            content = ErrorMessageAdapter.getErrorCauseMessage(uploadResult, upload, getResources());

            if (needsToUpdateCredentials) {
                // let the user update credentials with one click
                Intent updateAccountCredentials = new Intent(this, AuthenticatorActivity.class);
                updateAccountCredentials.putExtra(
                    AuthenticatorActivity.EXTRA_ACCOUNT, upload.getAccount()
                );
                updateAccountCredentials.putExtra(
                    AuthenticatorActivity.EXTRA_ACTION,
                    AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN
                );

                updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND);
                mNotificationBuilder.setContentIntent(PendingIntent.getActivity(
                    this,
                    (int) System.currentTimeMillis(),
                    updateAccountCredentials,
                    PendingIntent.FLAG_ONE_SHOT
                ));
            } else {
                Intent intent;
                if (uploadResult.getCode().equals(ResultCode.SYNC_CONFLICT)) {
                    intent = ConflictsResolveActivity.createIntent(upload.getFile(),
                                                                   upload.getAccount(),
                                                                   upload.getOCUploadId(),
                                                                   Intent.FLAG_ACTIVITY_CLEAR_TOP,
                                                                   this);
                } else {
                    intent = UploadListActivity.createIntent(upload.getFile(),
                                                             upload.getAccount(),
                                                             Intent.FLAG_ACTIVITY_CLEAR_TOP,
                                                             this);
                }

                mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this,
                                                                                (int) System.currentTimeMillis(),
                                                                                intent,
                                                                                0)
                                                     );
            }

            mNotificationBuilder.setContentText(content);
            mNotificationManager.notify(tickerId, mNotificationBuilder.build());
        }
    }

    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners
     */
    private void sendBroadcastUploadsAdded() {
        Intent start = new Intent(getUploadsAddedMessage());
        // nothing else needed right now
        start.setPackage(getPackageName());
        localBroadcastManager.sendBroadcast(start);
    }

    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners
     *
     * @param upload Finished upload operation
     */
    private void sendBroadcastUploadStarted(UploadFileOperation upload) {
        Intent start = new Intent(getUploadStartMessage());
        start.putExtra(EXTRA_REMOTE_PATH, upload.getRemotePath()); // real remote
        start.putExtra(EXTRA_OLD_FILE_PATH, upload.getOriginalStoragePath());
        start.putExtra(ACCOUNT_NAME, upload.getAccount().name);

        start.setPackage(getPackageName());
        localBroadcastManager.sendBroadcast(start);
    }

    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners
     *
     * @param upload                 Finished upload operation
     * @param uploadResult           Result of the upload operation
     * @param unlinkedFromRemotePath Path in the uploads tree where the upload was unlinked from
     */
    private void sendBroadcastUploadFinished(
        UploadFileOperation upload,
        RemoteOperationResult uploadResult,
        String unlinkedFromRemotePath
    ) {
        Intent end = new Intent(getUploadFinishMessage());
        end.putExtra(EXTRA_REMOTE_PATH, upload.getRemotePath()); // real remote
        // path, after
        // possible
        // automatic
        // renaming
        if (upload.wasRenamed()) {
            end.putExtra(EXTRA_OLD_REMOTE_PATH, upload.getOldFile().getRemotePath());
        }
        end.putExtra(EXTRA_OLD_FILE_PATH, upload.getOriginalStoragePath());
        end.putExtra(ACCOUNT_NAME, upload.getAccount().name);
        end.putExtra(EXTRA_UPLOAD_RESULT, uploadResult.isSuccess());
        if (unlinkedFromRemotePath != null) {
            end.putExtra(EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath);
        }
        end.setPackage(getPackageName());
        localBroadcastManager.sendBroadcast(end);
    }

    /**
     * Remove and 'forgets' pending uploads of an account.
     *
     * @param account Account which uploads will be cancelled
     */
    private void cancelUploadsForAccount(Account account) {
        mPendingUploads.remove(account.name);
        mUploadsStorageManager.removeUploads(account.name);
    }


    /**
     * Upload a new file
     */
    public static void uploadNewFile(
        Context context,
        Account account,
        String localPath,
        String remotePath,
        int behaviour,
        String mimeType,
        boolean createRemoteFile,
        int createdBy,
        boolean requiresWifi,
        boolean requiresCharging,
        NameCollisionPolicy nameCollisionPolicy
    ) {
        uploadNewFile(
            context,
            account,
            new String[]{localPath},
            new String[]{remotePath},
            new String[]{mimeType},
            behaviour,
            createRemoteFile,
            createdBy,
            requiresWifi,
            requiresCharging,
            nameCollisionPolicy
        );
    }

    /**
     * Upload multiple new files
     */
    public static void uploadNewFile(
        Context context,
        Account account,
        String[] localPaths,
        String[] remotePaths,
        String[] mimeTypes,
        Integer behaviour,
        Boolean createRemoteFolder,
        int createdBy,
        boolean requiresWifi,
        boolean requiresCharging,
        NameCollisionPolicy nameCollisionPolicy
    ) {
        Intent intent = new Intent(context, FileUploader.class);

        intent.putExtra(FileUploader.KEY_ACCOUNT, account);
        intent.putExtra(FileUploader.KEY_LOCAL_FILE, localPaths);
        intent.putExtra(FileUploader.KEY_REMOTE_FILE, remotePaths);
        intent.putExtra(FileUploader.KEY_MIME_TYPE, mimeTypes);
        intent.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, behaviour);
        intent.putExtra(FileUploader.KEY_CREATE_REMOTE_FOLDER, createRemoteFolder);
        intent.putExtra(FileUploader.KEY_CREATED_BY, createdBy);
        intent.putExtra(FileUploader.KEY_WHILE_ON_WIFI_ONLY, requiresWifi);
        intent.putExtra(FileUploader.KEY_WHILE_CHARGING_ONLY, requiresCharging);
        intent.putExtra(FileUploader.KEY_NAME_COLLISION_POLICY, nameCollisionPolicy);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Upload and overwrite an already uploaded file with disabled retries
     */
    public static void uploadUpdateFile(
        Context context,
        Account account,
        OCFile existingFile,
        Integer behaviour,
        NameCollisionPolicy nameCollisionPolicy
                                       ) {
        uploadUpdateFile(context, account, new OCFile[]{existingFile}, behaviour, nameCollisionPolicy, true);
    }

    /**
     * Upload and overwrite an already uploaded file
     */
    public static void uploadUpdateFile(
        Context context,
        Account account,
        OCFile existingFile,
        Integer behaviour,
        NameCollisionPolicy nameCollisionPolicy,
        boolean disableRetries
                                       ) {
        uploadUpdateFile(context, account, new OCFile[]{existingFile}, behaviour, nameCollisionPolicy, disableRetries);
    }

    /**
     * Upload and overwrite already uploaded files
     */
    public static void uploadUpdateFile(
        Context context,
        Account account,
        OCFile[] existingFiles,
        Integer behaviour,
        NameCollisionPolicy nameCollisionPolicy,
        boolean disableRetries
                                       ) {
        Intent intent = new Intent(context, FileUploader.class);

        intent.putExtra(FileUploader.KEY_ACCOUNT, account);
        intent.putExtra(FileUploader.KEY_FILE, existingFiles);
        intent.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, behaviour);
        intent.putExtra(FileUploader.KEY_NAME_COLLISION_POLICY, nameCollisionPolicy);
        intent.putExtra(FileUploader.KEY_DISABLE_RETRIES, disableRetries);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Retry a failed {@link OCUpload} identified by {@link OCUpload#getRemotePath()}
     */
    public static void retryUpload(@NonNull Context context, @NonNull Account account, @NonNull OCUpload upload) {
        Intent i = new Intent(context, FileUploader.class);
        i.putExtra(FileUploader.KEY_RETRY, true);
        i.putExtra(FileUploader.KEY_ACCOUNT, account);
        i.putExtra(FileUploader.KEY_RETRY_UPLOAD, upload);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }

    /**
     * Retry a subset of all the stored failed uploads.
     *
     * @param context      Caller {@link Context}
     * @param account      If not null, only failed uploads to this OC account will be retried; otherwise, uploads of
     *                     all accounts will be retried.
     * @param uploadResult If not null, only failed uploads with the result specified will be retried; otherwise, failed
     *                     uploads due to any result will be retried.
     */
    public static void retryFailedUploads(
        @NonNull final Context context,
        @Nullable final Account account,
        @NonNull final UploadsStorageManager uploadsStorageManager,
        @NonNull final ConnectivityService connectivityService,
        @NonNull final UserAccountManager accountManager,
        @NonNull final PowerManagementService powerManagementService,
        @Nullable final UploadResult uploadResult
    ) {
        OCUpload[] failedUploads = uploadsStorageManager.getFailedUploads();
        Account currentAccount = null;
        boolean resultMatch;
        boolean accountMatch;

        final Connectivity connectivity = connectivityService.getConnectivity();
        final boolean gotNetwork = connectivity.isConnected() && !connectivityService.isInternetWalled();
        final boolean gotWifi = connectivity.isWifi();
        final BatteryStatus batteryStatus = powerManagementService.getBattery();
        final boolean charging = batteryStatus.isCharging() || batteryStatus.isFull();
        final boolean isPowerSaving = powerManagementService.isPowerSavingEnabled();

        for (OCUpload failedUpload : failedUploads) {
            accountMatch = account == null || account.name.equals(failedUpload.getAccountName());
            resultMatch = uploadResult == null || uploadResult == failedUpload.getLastResult();
            if (accountMatch && resultMatch) {
                if (currentAccount == null || !currentAccount.name.equals(failedUpload.getAccountName())) {
                    currentAccount = failedUpload.getAccount(accountManager);
                }

                if (!new File(failedUpload.getLocalPath()).exists()) {
                    if (failedUpload.getLastResult() != UploadResult.FILE_NOT_FOUND) {
                        failedUpload.setLastResult(UploadResult.FILE_NOT_FOUND);
                        uploadsStorageManager.updateUpload(failedUpload);
                    }
                } else {

                    if (!isPowerSaving && gotNetwork && canUploadBeRetried(failedUpload, gotWifi, charging)) {
                        retryUpload(context, currentAccount, failedUpload);
                    }
                }
            }
        }
    }

    private static boolean canUploadBeRetried(OCUpload upload, boolean gotWifi, boolean isCharging) {
        File file = new File(upload.getLocalPath());
        boolean needsWifi = upload.isUseWifiOnly();
        boolean needsCharging = upload.isWhileChargingOnly();

        return file.exists() && (!needsWifi || gotWifi) && (!needsCharging || isCharging);
    }

    public static String getUploadsAddedMessage() {
        return FileUploader.class.getName() + UPLOADS_ADDED_MESSAGE;
    }

    public static String getUploadStartMessage() {
        return FileUploader.class.getName() + UPLOAD_START_MESSAGE;
    }

    public static String getUploadFinishMessage() {
        return FileUploader.class.getName() + UPLOAD_FINISH_MESSAGE;
    }


    /**
     * Ordinal of enumerated constants is important for old data compatibility.
     */
    public enum NameCollisionPolicy {
        RENAME, // Ordinal corresponds to old forceOverwrite = false (0 in database)
        OVERWRITE, // Ordinal corresponds to old forceOverwrite = true (1 in database)
        CANCEL,
        ASK_USER;

        public static final NameCollisionPolicy DEFAULT = RENAME;

        public static NameCollisionPolicy deserialize(int ordinal) {
            NameCollisionPolicy[] values = NameCollisionPolicy.values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DEFAULT;
        }

        public int serialize() {
            return this.ordinal();
        }
    }

    /**
     * Binder to let client components to perform operations on the queue of uploads.
     *
     * It provides by itself the available operations.
     */
    public class FileUploaderBinder extends Binder implements OnDatatransferProgressListener {

        /**
         * Map of listeners that will be reported about progress of uploads from a {@link FileUploaderBinder} instance
         */
        private Map<String, OnDatatransferProgressListener> mBoundListeners = new HashMap<>();

        /**
         * Cancels a pending or current upload of a remote file.
         *
         * @param account ownCloud account where the remote file will be stored.
         * @param file    A file in the queue of pending uploads
         */
        public void cancel(Account account, OCFile file) {
            cancel(account.name, file.getRemotePath(), null);
        }

        /**
         * Cancels a pending or current upload that was persisted.
         *
         * @param storedUpload Upload operation persisted
         */
        public void cancel(OCUpload storedUpload) {
            cancel(storedUpload.getAccountName(), storedUpload.getRemotePath(), null);
        }

        /**
         * Cancels a pending or current upload of a remote file.
         *
         * @param accountName Local name of an ownCloud account where the remote file will be stored.
         * @param remotePath  Remote target of the upload
         * @param resultCode  Setting result code will pause rather than cancel the job
         */
        private void cancel(String accountName, String remotePath, @Nullable ResultCode resultCode) {
            Pair<UploadFileOperation, String> removeResult = mPendingUploads.remove(accountName, remotePath);
            UploadFileOperation upload = removeResult.first;
            if (upload == null && mCurrentUpload != null && mCurrentAccount != null &&
                mCurrentUpload.getRemotePath().startsWith(remotePath) && accountName.equals(mCurrentAccount.name)) {

                upload = mCurrentUpload;
            }

            if (upload != null) {
                upload.cancel(resultCode);
                // need to update now table in mUploadsStorageManager,
                // since the operation will not get to be run by FileUploader#uploadFile
                if (resultCode != null) {
                    mUploadsStorageManager.updateDatabaseUploadResult(new RemoteOperationResult(resultCode), upload);
                    notifyUploadResult(upload, new RemoteOperationResult(resultCode));
                } else {
                    mUploadsStorageManager.removeUpload(accountName, remotePath);
                }
            }
        }

        /**
         * Cancels all the uploads for an account.
         *
         * @param account ownCloud account.
         */
        public void cancel(Account account) {
            Log_OC.d(TAG, "Account= " + account.name);

            if (mCurrentUpload != null) {
                Log_OC.d(TAG, "Current Upload Account= " + mCurrentUpload.getAccount().name);
                if (mCurrentUpload.getAccount().name.equals(account.name)) {
                    mCurrentUpload.cancel(ResultCode.CANCELLED);
                }
            }

            // Cancel pending uploads
            cancelUploadsForAccount(account);
        }

        public void clearListeners() {
            mBoundListeners.clear();
        }

        /**
         * Returns True when the file described by 'file' is being uploaded to the ownCloud account 'account' or waiting
         * for it
         *
         * If 'file' is a directory, returns 'true' if some of its descendant files is uploading or waiting to upload.
         *
         * Warning: If remote file exists and target was renamed the original file is being returned here. That is, it
         * seems as if the original file is being updated when actually a new file is being uploaded.
         *
         * @param user    user where the remote file will be stored.
         * @param file    A file that could be in the queue of pending uploads
         */
        public boolean isUploading(User user, OCFile file) {
            if (user == null || file == null) {
                return false;
            }

            return mPendingUploads.contains(user.getAccountName(), file.getRemotePath());
        }

        public boolean isUploadingNow(OCUpload upload) {
            return upload != null &&
                mCurrentAccount != null &&
                mCurrentUpload != null &&
                upload.getAccountName().equals(mCurrentAccount.name) &&
                upload.getRemotePath().equals(mCurrentUpload.getRemotePath());
        }

        /**
         * Adds a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param user  user owning the file of interest.
         * @param file     {@link OCFile} of interest for listener.
         */
        public void addDatatransferProgressListener(
            OnDatatransferProgressListener listener,
            User user,
            OCFile file
        ) {
            if (user == null || file == null || listener == null) {
                return;
            }

            String targetKey = buildRemoteName(user.getAccountName(), file.getRemotePath());
            mBoundListeners.put(targetKey, listener);
        }

        /**
         * Adds a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param ocUpload {@link OCUpload} of interest for listener.
         */
        public void addDatatransferProgressListener(
            OnDatatransferProgressListener listener,
            OCUpload ocUpload
        ) {
            if (ocUpload == null || listener == null) {
                return;
            }

            String targetKey = buildRemoteName(ocUpload.getAccountName(), ocUpload.getRemotePath());
            mBoundListeners.put(targetKey, listener);
        }

        /**
         * Removes a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param user user owning the file of interest.
         * @param file {@link OCFile} of interest for listener.
         */
        public void removeDatatransferProgressListener(
            OnDatatransferProgressListener listener,
            User user,
            OCFile file
        ) {
            if (user == null || file == null || listener == null) {
                return;
            }

            String targetKey = buildRemoteName(user.getAccountName(), file.getRemotePath());
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }

        /**
         * Removes a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param ocUpload Stored upload of interest
         */
        public void removeDatatransferProgressListener(
            OnDatatransferProgressListener listener,
            OCUpload ocUpload
        ) {
            if (ocUpload == null || listener == null) {
                return;
            }

            String targetKey = buildRemoteName(ocUpload.getAccountName(), ocUpload.getRemotePath());
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }

        @Override
        public void onTransferProgress(
            long progressRate,
            long totalTransferredSoFar,
            long totalToTransfer,
            String fileName
        ) {
            String key = buildRemoteName(mCurrentUpload.getAccount().name, mCurrentUpload.getFile().getRemotePath());
            OnDatatransferProgressListener boundListener = mBoundListeners.get(key);

            if (boundListener != null) {
                boundListener.onTransferProgress(progressRate, totalTransferredSoFar, totalToTransfer, fileName);
            }

            Context context = MainApp.getAppContext();
            if (context != null) {
                ResultCode cancelReason = null;
                Connectivity connectivity = connectivityService.getConnectivity();
                if (mCurrentUpload.isWifiRequired() && !connectivity.isWifi()) {
                    cancelReason = ResultCode.DELAYED_FOR_WIFI;
                } else if (mCurrentUpload.isChargingRequired() && !powerManagementService.getBattery().isCharging()) {
                    cancelReason = ResultCode.DELAYED_FOR_CHARGING;
                } else if (!mCurrentUpload.isIgnoringPowerSaveMode() && powerManagementService.isPowerSavingEnabled()) {
                    cancelReason = ResultCode.DELAYED_IN_POWER_SAVE_MODE;
                }

                if (cancelReason != null) {
                    cancel(
                        mCurrentUpload.getAccount().name,
                        mCurrentUpload.getFile().getRemotePath(),
                        cancelReason
                    );
                }
            }
        }

        /**
         * Builds a key for the map of listeners.
         *
         * TODO use method in IndexedForest, or refactor both to a common place add to local database) to better policy
         * (add to local database, then upload)
         *
         * @param accountName Local name of the ownCloud account where the file to upload belongs.
         * @param remotePath  Remote path to upload the file to.
         * @return Key
         */
        private String buildRemoteName(String accountName, String remotePath) {
            return accountName + remotePath;
        }
    }


    /**
     * Upload worker. Performs the pending uploads in the order they were requested.
     *
     * Created with the Looper of a new thread, started in {@link FileUploader#onCreate()}.
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will
        // warn about a possible memory leak
        private FileUploader mService;

        public ServiceHandler(Looper looper, FileUploader service) {
            super(looper);
            if (service == null) {
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            }
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            List<String> requestedUploads = (List<String>) msg.obj;
            if (msg.obj != null) {
                for (String requestedUpload : requestedUploads) {
                    mService.uploadFile(requestedUpload);
                }
            }
            Log_OC.d(TAG, "Stopping command after id " + msg.arg1);
            mService.stopForeground(true);
            mService.stopSelf(msg.arg1);
        }
    }
}
