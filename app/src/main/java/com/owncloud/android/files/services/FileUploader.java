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
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
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
import com.nextcloud.client.files.uploader.FileUploaderIntents;
import com.nextcloud.client.files.uploader.UploadNotificationManager;
import com.nextcloud.client.jobs.FilesUploadWorker;
import com.nextcloud.client.network.Connectivity;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.utils.FileUploaderDelegate;
import com.nextcloud.java.util.Optional;
import com.nextcloud.utils.extensions.IntentExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
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
import com.owncloud.android.lib.resources.files.model.ServerFileInterface;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.FilesUploadHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import dagger.android.AndroidInjection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    public static IBinder mBinder;
    private OwnCloudClient mUploadClient;
    private Account mCurrentAccount;
    private FileDataStorageManager mStorageManager;

    private SecureRandom secureRandomGenerator = new SecureRandom();

    @Inject UserAccountManager accountManager;
    @Inject UploadsStorageManager mUploadsStorageManager;
    @Inject ConnectivityService connectivityService;
    @Inject PowerManagementService powerManagementService;
    @Inject LocalBroadcastManager localBroadcastManager;
    @Inject ViewThemeUtils viewThemeUtils;

    private IndexedForest<UploadFileOperation> mPendingUploads = new IndexedForest<>();

    /**
     * {@link UploadFileOperation} object of ongoing upload. Can be null. Note: There can only be one concurrent
     * upload!
     */
    private UploadFileOperation mCurrentUpload;

    private int mLastPercent;
    private FileUploaderDelegate fileUploaderDelegate;
    private UploadNotificationManager notificationManager;
    private FileUploaderIntents intents;

    @Override
    public void onRenameUpload() {
        mUploadsStorageManager.updateDatabaseUploadStart(mCurrentUpload);
        fileUploaderDelegate.sendBroadcastUploadStarted(mCurrentUpload, this, localBroadcastManager);
    }

    /**
     * Service initialization
     */
    @SuppressFBWarnings("ST")
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidInjection.inject(this);
        Log_OC.d(TAG, "Creating service");
        HandlerThread thread = new HandlerThread("FileUploaderThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileUploaderBinder();
        fileUploaderDelegate = new FileUploaderDelegate();

        intents = new FileUploaderIntents(this);

        notificationManager = new UploadNotificationManager(this, viewThemeUtils);
        notificationManager.init();

        // TODO Add UploadResult.KILLED?
        int failedCounter = mUploadsStorageManager.failInProgressUploads(UploadResult.SERVICE_INTERRUPTED);
        if (failedCounter > 0) {
            notificationManager.dismissWorkerNotifications();
        }

        // add AccountsUpdatedListener
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addOnAccountsUpdatedListener(this, null, false);
    }

    /**
     * Service clean up
     */
    @SuppressFBWarnings("ST")
    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "Destroying service");
        mBinder = null;
        mServiceHandler = null;
        mServiceLooper.quit();
        mServiceLooper = null;
        notificationManager.dismissWorkerNotifications();
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

        // No more needed will be deleted
        // ForegroundServiceHelper.INSTANCE.startService(this, FOREGROUND_SERVICE_ID, mNotification, ForegroundServiceType.DataSync);

        if (intent == null) {
            Log_OC.e(TAG, "Intent is null");
            return Service.START_NOT_STICKY;
        }

        if (!intent.hasExtra(FilesUploadWorker.KEY_ACCOUNT)) {
            Log_OC.e(TAG, "Not enough information provided in intent");
            return Service.START_NOT_STICKY;
        }

        final Account account = IntentExtensionsKt.getParcelableArgument(intent, FilesUploadWorker.KEY_ACCOUNT, Account.class);
        if (account == null) {
            return Service.START_NOT_STICKY;
        }
        Optional<User> optionalUser = accountManager.getUser(account.name);
        if (!optionalUser.isPresent()) {
            return Service.START_NOT_STICKY;
        }
        final User user = optionalUser.get();

        boolean retry = intent.getBooleanExtra(FilesUploadWorker.KEY_RETRY, false);
        List<String> requestedUploads = new ArrayList<>();

        boolean onWifiOnly = intent.getBooleanExtra(FilesUploadWorker.KEY_WHILE_ON_WIFI_ONLY, false);
        boolean whileChargingOnly = intent.getBooleanExtra(FilesUploadWorker.KEY_WHILE_CHARGING_ONLY, false);

        if (retry) {
            // Retry uploads
            if (!intent.hasExtra(FilesUploadWorker.KEY_ACCOUNT) || !intent.hasExtra(FilesUploadWorker.KEY_RETRY_UPLOAD)) {
                Log_OC.e(TAG, "Not enough information provided in intent: no KEY_RETRY_UPLOAD_KEY");
                return START_NOT_STICKY;
            }
            retryUploads(intent, user, requestedUploads);
        } else {
            // Start new uploads
            if (!(intent.hasExtra(FilesUploadWorker.KEY_LOCAL_FILE) || intent.hasExtra(FilesUploadWorker.KEY_FILE))) {
                Log_OC.e(TAG, "Not enough information provided in intent");
                return Service.START_NOT_STICKY;
            }

            Integer error = gatherAndStartNewUploads(intent, user, requestedUploads, onWifiOnly, whileChargingOnly);
            if (error != null) {
                return error;
            }
        }

        if (requestedUploads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedUploads;
            mServiceHandler.sendMessage(msg);
            fileUploaderDelegate.sendBroadcastUploadsAdded(this, localBroadcastManager);
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
        boolean whileChargingOnly) {
        String[] localPaths = null;
        String[] remotePaths = null;
        String[] mimeTypes = null;
        OCFile[] files = null;

        if (intent.hasExtra(FilesUploadWorker.KEY_FILE)) {
            Parcelable[] files_temp = intent.getParcelableArrayExtra(FilesUploadWorker.KEY_FILE);
            files = new OCFile[files_temp.length];
            System.arraycopy(files_temp, 0, files, 0, files_temp.length);
        } else {
            localPaths = intent.getStringArrayExtra(FilesUploadWorker.KEY_LOCAL_FILE);
            remotePaths = intent.getStringArrayExtra(FilesUploadWorker.KEY_REMOTE_FILE);
            mimeTypes = intent.getStringArrayExtra(FilesUploadWorker.KEY_MIME_TYPE);
        }

        if (intent.hasExtra(FilesUploadWorker.KEY_FILE) && files == null) {
            Log_OC.e(TAG, "Incorrect array for OCFiles provided in upload intent");
            return Service.START_NOT_STICKY;
        } else if (!intent.hasExtra(FilesUploadWorker.KEY_FILE)) {
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

        NameCollisionPolicy nameCollisionPolicy = IntentExtensionsKt.getSerializableArgument(intent, FilesUploadWorker.KEY_NAME_COLLISION_POLICY, NameCollisionPolicy.class);
        if (nameCollisionPolicy == null) {
            nameCollisionPolicy = NameCollisionPolicy.DEFAULT;
        }
        int localAction = intent.getIntExtra(FilesUploadWorker.KEY_LOCAL_BEHAVIOUR, FilesUploadWorker.LOCAL_BEHAVIOUR_FORGET);
        boolean isCreateRemoteFolder = intent.getBooleanExtra(FilesUploadWorker.KEY_CREATE_REMOTE_FOLDER, false);
        int createdBy = intent.getIntExtra(FilesUploadWorker.KEY_CREATED_BY, UploadFileOperation.CREATED_BY_USER);
        boolean disableRetries = intent.getBooleanExtra(FilesUploadWorker.KEY_DISABLE_RETRIES, true);
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
    @SuppressLint("SdCardPath")
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
        OCUpload ocUpload = new OCUpload(file, user);
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
            disableRetries,
            new FileDataStorageManager(user, getContentResolver())
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

        OCUpload upload = IntentExtensionsKt.getParcelableArgument(intent, FilesUploadWorker.KEY_RETRY_UPLOAD, OCUpload.class);

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
            true,
            new FileDataStorageManager(user, getContentResolver())
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
        if (mCurrentUpload != null && !accountManager.exists(mCurrentUpload.getUser().toPlatformAccount())) {
            mCurrentUpload.cancel(ResultCode.ACCOUNT_NOT_FOUND);
        }
        // The rest of uploads are cancelled when they try to start
    }

    /**
     * Core upload method: sends the file(s) to upload WARNING: legacy code, must be in sync with @{{@link
     * FilesUploadWorker upload(UploadFileOperation, User)}
     *
     * @param uploadKey Key to access the upload to perform, contained in mPendingUploads
     */
    public void uploadFile(String uploadKey) {
        mCurrentUpload = mPendingUploads.get(uploadKey);

        if (mCurrentUpload != null) {
            /// Check account existence
            if (!accountManager.exists(mCurrentUpload.getUser().toPlatformAccount())) {
                Log_OC.w(TAG, "Account " + mCurrentUpload.getUser().getAccountName() +
                    " does not exist anymore -> cancelling all its uploads");
                cancelPendingUploads(mCurrentUpload.getUser().getAccountName());
                return;
            }

            /// OK, let's upload
            mUploadsStorageManager.updateDatabaseUploadStart(mCurrentUpload);

            notifyUploadStart(mCurrentUpload);

            fileUploaderDelegate.sendBroadcastUploadStarted(mCurrentUpload, this, localBroadcastManager);

            RemoteOperationResult uploadResult = null;

            try {
                /// prepare client object to send the request to the ownCloud server
                if (mCurrentAccount == null || !mCurrentAccount.equals(mCurrentUpload.getUser().toPlatformAccount())) {
                    mCurrentAccount = mCurrentUpload.getUser().toPlatformAccount();
                    mStorageManager = new FileDataStorageManager(getCurrentUser().get(), getContentResolver());
                }   // else, reuse storage manager from previous operation
                // always get client from client manager, to get fresh credentials in case of update
                OwnCloudAccount ocAccount = new OwnCloudAccount(mCurrentAccount, this);
                mUploadClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, this);
                uploadResult = mCurrentUpload.execute(mUploadClient);
            } catch (Exception e) {
                Log_OC.e(TAG, "Error uploading", e);
                uploadResult = new RemoteOperationResult(e);
            } finally {
                Pair<UploadFileOperation, String> removeResult;
                if (mCurrentUpload.wasRenamed()) {
                    OCFile oldFile = mCurrentUpload.getOldFile();
                    String oldRemotePath = "";
                    if (oldFile != null) {
                        oldRemotePath = oldFile.getRemotePath();
                    }
                    removeResult = mPendingUploads.removePayload(mCurrentAccount.name, oldRemotePath);
                    // TODO: grant that name is also updated for mCurrentUpload.getOCUploadId
                } else {
                    removeResult = mPendingUploads.removePayload(mCurrentAccount.name,
                                                                 mCurrentUpload.getDecryptedRemotePath());
                }

                mUploadsStorageManager.updateDatabaseUploadResult(uploadResult, mCurrentUpload);

                /// notify result
                notifyUploadResult(mCurrentUpload, uploadResult);

                fileUploaderDelegate.sendBroadcastUploadFinished(mCurrentUpload,
                                                                 uploadResult,
                                                                 removeResult.second,
                                                                 this,
                                                                 localBroadcastManager);
            }

            // generate new Thumbnail
            Optional<User> user = getCurrentUser();
            if (user.isPresent()) {
                final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                    new ThumbnailsCacheManager.ThumbnailGenerationTask(mStorageManager, user.get());

                File file = new File(mCurrentUpload.getOriginalStoragePath());
                String remoteId = mCurrentUpload.getFile().getRemoteId();

                task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, remoteId));
            }
        }
    }

    /**
     * Convert current account to user. This is a temporary workaround until
     * service is migrated to new user model.
     *
     * @return Optional {@link User}
     */
    private Optional<User> getCurrentUser() {
        if (mCurrentAccount == null) {
            return Optional.empty();
        } else {
            return accountManager.getUser(mCurrentAccount.name);
        }
    }

    /**
     * Creates a status notification to show the upload progress
     *
     * @param upload Upload operation starting.
     */
    private void notifyUploadStart(UploadFileOperation upload) {
        mLastPercent = 0;
        notificationManager.notifyForStart(upload, intents.startIntent(upload));
    }

    /**
     * Callback method to update the progress bar in the status notification
     */
    @Override
    public void onTransferProgress(
        long progressRate,
        long totalTransferredSoFar,
        long totalToTransfer,
        String filePath) {
        int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
        if (percent != mLastPercent) {
            notificationManager.updateUploadProgressNotification(filePath, percent, mCurrentUpload);
        }
        mLastPercent = percent;
    }

    /**
     * Updates the status notification with the result of an upload operation.
     *
     * @param uploadResult Result of the upload operation.
     * @param upload       Finished upload operation
     */
    @SuppressFBWarnings("DMI")
    private void notifyUploadResult(UploadFileOperation upload, RemoteOperationResult uploadResult) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.getCode());
        // cancelled operation or success -> silent removal of progress notification

        if (uploadResult.isSuccess()){
            notificationManager.dismissOldErrorNotification(upload);
        }

        // Only notify if the upload fails
        if (!uploadResult.isCancelled() &&
            !uploadResult.isSuccess() &&
            ResultCode.LOCAL_FILE_NOT_FOUND != uploadResult.getCode() &&
            uploadResult.getCode() != ResultCode.DELAYED_FOR_WIFI &&
            uploadResult.getCode() != ResultCode.DELAYED_FOR_CHARGING &&
            uploadResult.getCode() != ResultCode.DELAYED_IN_POWER_SAVE_MODE &&
            uploadResult.getCode() != ResultCode.LOCK_FAILED) {

            int tickerId = R.string.uploader_upload_failed_ticker;

            String content;

            // check credentials error
            boolean needsToUpdateCredentials = uploadResult.getCode() == ResultCode.UNAUTHORIZED;
            if (needsToUpdateCredentials) {
                tickerId = R.string.uploader_upload_failed_credentials_error;
            } else if (uploadResult.getCode() == ResultCode.SYNC_CONFLICT) {
                // check file conflict
                tickerId = R.string.uploader_upload_failed_sync_conflict_error;
            }

            notificationManager.notifyForResult(tickerId);

            content = ErrorMessageAdapter.getErrorCauseMessage(uploadResult, upload, getResources());

            if (needsToUpdateCredentials) {
                notificationManager.setContentIntent(intents.credentialIntent(upload));
            } else {
                notificationManager.setContentIntent(intents.resultIntent(uploadResult.getCode(), upload));
            }

            notificationManager.setContentText(content);
            if (!uploadResult.isSuccess()) {
                notificationManager.showRandomNotification();
            }
        }
    }

    /**
     * Remove and 'forgets' pending uploads of a user.
     *
     * @param accountName User which uploads will be cancelled
     */
    private void cancelPendingUploads(String accountName) {
        mPendingUploads.remove(accountName);
        mUploadsStorageManager.removeUploads(accountName);
    }



    /**
     * Binder to let client components to perform operations on the queue of uploads.
     * <p>
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
        public void cancel(Account account, ServerFileInterface file) {
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
        public void cancel(String accountName, String remotePath, @Nullable ResultCode resultCode) {
            try {
                new FilesUploadHelper().cancelFileUpload(remotePath, accountManager.getUser(accountName).get());
            } catch (NoSuchElementException e) {
                Log_OC.e(TAG, "Error cancelling current upload because user does not exist!");
            }
        }

        /**
         * Cancels all the uploads for a user, both running and pending.
         *
         * @param user Nextcloud user
         */
        public void cancel(User user) {
            cancel(user.getAccountName());
        }

        public void cancel(String accountName) {
            cancelPendingUploads(accountName);
            new FilesUploadHelper().restartUploadJob(accountManager.getUser(accountName).get());
        }

        public void clearListeners() {
            FilesUploadHelper.Companion.getMBoundListeners().clear();
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

            OCUpload upload = mUploadsStorageManager.getUploadByRemotePath(file.getRemotePath());

            if (upload == null){
                return false;
            }

            return upload.getUploadStatus() == UploadStatus.UPLOAD_IN_PROGRESS;
        }

        @SuppressFBWarnings("NP")
        public boolean isUploadingNow(OCUpload upload) {
            UploadFileOperation currentUploadFileOperation = FilesUploadWorker.Companion.getCurrentUploadFileOperation();
            if (currentUploadFileOperation == null || currentUploadFileOperation.getUser() == null) return false;
            if (upload == null || (!upload.getAccountName().equals(currentUploadFileOperation.getUser().getAccountName()))) return false;
            if (currentUploadFileOperation.getOldFile() != null){
                // For file conflicts check old file remote path
                return upload.getRemotePath().equals(currentUploadFileOperation.getRemotePath()) ||
                    upload.getRemotePath().equals(currentUploadFileOperation.getOldFile().getRemotePath());
            }
            return upload.getRemotePath().equals(currentUploadFileOperation.getRemotePath());
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
            ServerFileInterface file
                                                   ) {
            if (user == null || file == null || listener == null) {
                return;
            }

            String targetKey = FilesUploadWorker.Companion.buildRemoteName(user.getAccountName(), file.getRemotePath());
            new FilesUploadHelper().addDatatransferProgressListener(listener,targetKey);
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

            String targetKey = FilesUploadWorker.Companion.buildRemoteName(ocUpload.getAccountName(), ocUpload.getRemotePath());
            new FilesUploadHelper().addDatatransferProgressListener(listener,targetKey);
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
            ServerFileInterface file
                                                      ) {
            if (user == null || file == null || listener == null) {
                return;
            }

            String targetKey = FilesUploadWorker.Companion.buildRemoteName(user.getAccountName(), file.getRemotePath());
            new FilesUploadHelper().removeDatatransferProgressListener(listener,targetKey);
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

            String targetKey = FilesUploadWorker.Companion.buildRemoteName(ocUpload.getAccountName(), ocUpload.getRemotePath());
            new FilesUploadHelper().removeDatatransferProgressListener(listener,targetKey);
        }

        @Override
        public void onTransferProgress(
            long progressRate,
            long totalTransferredSoFar,
            long totalToTransfer,
            String fileName
                                      ) {
            String key = FilesUploadWorker.Companion.buildRemoteName(mCurrentUpload.getUser().getAccountName(), mCurrentUpload.getFile().getRemotePath());
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
                        mCurrentUpload.getUser().getAccountName(),
                        mCurrentUpload.getFile().getRemotePath(),
                        cancelReason
                          );
                }
            }
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
            mService.notificationManager.dismissWorkerNotifications();
            mService.stopForeground(true);
            mService.stopSelf(msg.arg1);
        }
    }
}
