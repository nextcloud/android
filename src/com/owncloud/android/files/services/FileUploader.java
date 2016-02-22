/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @authro masensio
 * @author LukeOwnCloud
 * @author David A. Velasco
 * Copyright (C) 2012 Bartek Przybylski
 * Copyright (C) 2012-2016 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.files.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
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
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.notifications.NotificationBuilderWithProgressBar;
import com.owncloud.android.notifications.NotificationDelayer;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.UploadListActivity;
import com.owncloud.android.utils.ErrorMessageAdapter;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * Service for uploading files. Invoke using context.startService(...).
 * <p/>
 * Files to be uploaded are stored persistently using {@link UploadsStorageManager}.
 * <p/>
 * On next invocation of {@link FileUploader} uploaded files which
 * previously failed will be uploaded again until either upload succeeded or a
 * fatal error occured.
 * <p/>
 * Every file passed to this service is uploaded. No filtering is performed.
 * However, Intent keys (e.g., KEY_WIFI_ONLY) are obeyed.
 */
public class FileUploader extends Service
        implements OnDatatransferProgressListener, OnAccountsUpdateListener {

    private static final String TAG = FileUploader.class.getSimpleName();

    private static final String UPLOAD_START_MESSAGE = "UPLOAD_START";
    private static final String UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH";
    public static final String EXTRA_UPLOAD_RESULT = "RESULT";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_OLD_REMOTE_PATH = "OLD_REMOTE_PATH";
    public static final String EXTRA_OLD_FILE_PATH = "OLD_FILE_PATH";
    public static final String EXTRA_LINKED_TO_PATH = "LINKED_TO";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";

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
     * Call this Service with KEY_RETRY and KEY_RETRY_UPLOAD to retry
     * upload of file identified by KEY_RETRY_UPLOAD.
     */
    private static final String KEY_RETRY_UPLOAD = "KEY_RETRY_UPLOAD";
    /**
     * {@link Account} to which file is to be uploaded.
     */
    public static final String KEY_ACCOUNT = "ACCOUNT";

    /**
     * Set whether single file or multiple files are to be uploaded. SINGLE_FILES = 0, MULTIPLE_FILEs = 1.
     */
    public static final String KEY_UPLOAD_TYPE = "UPLOAD_TYPE";
    /**
     * Set to true if remote file is to be overwritten. Default action is to upload with different name.
     */
    public static final String KEY_FORCE_OVERWRITE = "KEY_FORCE_OVERWRITE";
    /**
     * Set to true if remote folder is to be created if it does not exist.
     */
    public static final String KEY_CREATE_REMOTE_FOLDER = "CREATE_REMOTE_FOLDER";
    /**
     * Set to true if upload is to performed only when connected via wifi.
     */
    public static final String KEY_WIFI_ONLY = "WIFI_ONLY";
    /**
     * Set to true if upload is to performed only when phone is being charged.
     */
    public static final String KEY_WHILE_CHARGING_ONLY = "KEY_WHILE_CHARGING_ONLY";
//    /**
//     * Set to future UNIX timestamp. Upload will not be performed before this timestamp.
//     */
//    public static final String KEY_UPLOAD_TIMESTAMP= "KEY_UPLOAD_TIMESTAMP";

    public static final String KEY_LOCAL_BEHAVIOUR = "BEHAVIOUR";

    //public static final String KEY_INSTANT_UPLOAD = "INSTANT_UPLOAD";

    public static final int LOCAL_BEHAVIOUR_COPY = 0;
    public static final int LOCAL_BEHAVIOUR_MOVE = 1;
    public static final int LOCAL_BEHAVIOUR_FORGET = 2;


    /**
     * Describes local behavior for upload.
     */
//    public enum LocalBehaviour {
//        /**
//         * Creates a copy of file and stores it in tmp folder inside owncloud
//         * folder on sd-card. After upload it is moved to local owncloud
//         * storage. Original file stays untouched.
//         */
//        LOCAL_BEHAVIOUR_COPY(0),
//        /**
//         * Upload file from current storage. Afterwards original file is move to
//         * local owncloud storage.
//         */
//        LOCAL_BEHAVIOUR_MOVE(1),
//        /**
//         * Just uploads file and leaves it where it is. Original file stays
//         * untouched.
//         */
//        LOCAL_BEHAVIOUR_FORGET(2);
//        private final int value;
//
//        LocalBehaviour(int value) {
//            this.value = value;
//        }
//
//        public int getValue() {
//            return value;
//        }
//
//        public static LocalBehaviour fromValue(int value){
//            switch (value)
//            {
//                case 0:
//                    return LOCAL_BEHAVIOUR_COPY;
//                case 1:
//                    return LOCAL_BEHAVIOUR_MOVE;
//                case 2:
//                    return LOCAL_BEHAVIOUR_FORGET;
//            }
//            return null;
//        }
//    }

//    public enum UploadQuantity {
//        UPLOAD_SINGLE_FILE(0), UPLOAD_MULTIPLE_FILES(1);
//        private final int value;
//
//        UploadQuantity(int value) {
//            this.value = value;
//        }
//
//        public int getValue() {
//            return value;
//        }
//    };

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private OwnCloudClient mUploadClient = null;
    private Account mCurrentAccount = null;
    private FileDataStorageManager mStorageManager;
    //since there can be only one instance of an Android service, there also just one db connection.
    private UploadsStorageManager mUploadsStorageManager = null;

    private IndexedForest<UploadFileOperation> mPendingUploads = new IndexedForest<UploadFileOperation>();

    /**
     * {@link UploadFileOperation} object of ongoing upload. Can be null. Note: There can only be one concurrent upload!
     */
    private UploadFileOperation mCurrentUpload = null;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mLastPercent;

    public static String getUploadStartMessage() {
        return FileUploader.class.getName() + UPLOAD_START_MESSAGE;
    }

    public static String getUploadFinishMessage() {
        return FileUploader.class.getName() + UPLOAD_FINISH_MESSAGE;
    }

    /**
     * Call to retry upload identified by remotePath
     */
    private static void retry(Context context, Account account, OCUpload upload) {
        Log_OC.d(TAG, "FileUploader.retry()");
        Intent i = new Intent(context, FileUploader.class);
        i.putExtra(FileUploader.KEY_RETRY, true);
        if (upload != null) {
            i.putExtra(FileUploader.KEY_ACCOUNT, account);
            i.putExtra(FileUploader.KEY_RETRY_UPLOAD, upload);
        }
        context.startService(i);
    }

    /**
     * Call to upload several new files
     */
    public static void uploadNewFile(Context context, Account account, String[] localPaths, String[] remotePaths,
                                     Integer behaviour, String mimeType, Boolean createRemoteFolder, Boolean wifiOnly) {
        Log_OC.d(TAG, "FileUploader.uploadNewFile()");
        Intent intent = new Intent(context, FileUploader.class);

        intent.putExtra(FileUploader.KEY_ACCOUNT, account);
        intent.putExtra(FileUploader.KEY_LOCAL_FILE, localPaths);
        intent.putExtra(FileUploader.KEY_REMOTE_FILE, remotePaths);
        intent.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, behaviour);
        intent.putExtra(FileUploader.KEY_MIME_TYPE, mimeType);
        intent.putExtra(FileUploader.KEY_CREATE_REMOTE_FOLDER, createRemoteFolder);
        intent.putExtra(FileUploader.KEY_WIFI_ONLY, wifiOnly);

        context.startService(intent);
    }

    /**
     * Call to upload a new single file
     */
    public static void uploadNewFile(Context context, Account account, String localPath, String remotePath, int
            behaviour, String mimeType, boolean createRemoteFile, boolean wifiOnly) {

        uploadNewFile(context, account, new String[]{localPath}, new String[]{remotePath}, behaviour, mimeType,
                createRemoteFile, wifiOnly);
    }

    /**
     * Call to update multiple files already uploaded
     */
    public static void uploadUpdate(Context context, Account account, OCFile[] existingFiles, Integer behaviour,
                                    Boolean forceOverwrite) {
        Log_OC.d(TAG, "FileUploader.uploadUpdate()");
        Intent intent = new Intent(context, FileUploader.class);

        intent.putExtra(FileUploader.KEY_ACCOUNT, account);
        intent.putExtra(FileUploader.KEY_FILE, existingFiles);
        intent.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, behaviour);
        intent.putExtra(FileUploader.KEY_FORCE_OVERWRITE, forceOverwrite);

        context.startService(intent);
    }

    /**
     * Call to update a dingle file already uploaded
     */
    public static void uploadUpdate(Context context, Account account, OCFile existingFile, Integer behaviour, Boolean
            forceOverwrite) {

        uploadUpdate(context, account, new OCFile[]{existingFile}, behaviour, forceOverwrite);
    }

    /**
     * Checks if an ownCloud server version should support chunked uploads.
     *
     * @param version OwnCloud version instance corresponding to an ownCloud
     *                server.
     * @return 'True' if the ownCloud server with version supports chunked
     * uploads.
     * <p/>
     * TODO - move to OwnCloudVersion
     */
    private static boolean chunkedUploadIsSupported(OwnCloudVersion version) {
        return (version != null && version.compareTo(OwnCloudVersion.owncloud_v4_5) >= 0);
    }

    /**
     * Service initialization
     */
    // TODO: Clean method: comments on extra code
    @Override
    public void onCreate() {
        super.onCreate();
        Log_OC.d(TAG, "Creating service");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileUploaderThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileUploaderBinder();

        // From FileUploaderService
        mUploadsStorageManager = new UploadsStorageManager(getContentResolver());

        //when this service starts there is no upload in progress. if db says so, app probably crashed before.
        //mUploadsStorageManager.setAllCurrentToUploadLater();  // TODO why?

        // mUploadExecutor = Executors.newFixedThreadPool(1);

//      Log_OC.d(TAG, "FileUploader.retry() called by onCreate()");
//      FileUploader.retry(getApplicationContext());

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
     * Entry point to add one or several files to the queue of uploads.
     * <p/>
     * New uploads are added calling to startService(), resulting in a call to
     * this method. This ensures the service will keep on working although the
     * caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command with id " + startId);

        boolean retry = intent.getBooleanExtra(KEY_RETRY, false);
        AbstractList<String> requestedUploads = new Vector<String>();

        if (!intent.hasExtra(KEY_ACCOUNT)) {
            Log_OC.e(TAG, "Not enough information provided in intent");
            return Service.START_NOT_STICKY;
        }

        Account account = intent.getParcelableExtra(KEY_ACCOUNT);
        if (!AccountUtils.exists(account, getApplicationContext())) {
            return Service.START_NOT_STICKY;
        }
        OwnCloudVersion ocv = AccountUtils.getServerVersion(account);
        boolean chunked = FileUploader.chunkedUploadIsSupported(ocv);

        if (!retry) {
            if (!(intent.hasExtra(KEY_LOCAL_FILE) ||
                    intent.hasExtra(KEY_FILE))) {
                Log_OC.e(TAG, "Not enough information provided in intent");
                return Service.START_NOT_STICKY;
            }

            String[] localPaths = null, remotePaths = null, mimeTypes = null;
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

            FileDataStorageManager storageManager = new FileDataStorageManager(
                    account,
                    getContentResolver()
            );

            boolean forceOverwrite = intent.getBooleanExtra(KEY_FORCE_OVERWRITE, false);
            //boolean isInstant = intent.getBooleanExtra(KEY_INSTANT_UPLOAD, false);
            int localAction = intent.getIntExtra(KEY_LOCAL_BEHAVIOUR, LOCAL_BEHAVIOUR_FORGET);

            boolean isCreateRemoteFolder = intent.getBooleanExtra(KEY_CREATE_REMOTE_FOLDER, false);
            boolean isUseWifiOnly = intent.getBooleanExtra(KEY_WIFI_ONLY, true);
            boolean isWhileChargingOnly = intent.getBooleanExtra(KEY_WHILE_CHARGING_ONLY, false);
            //long uploadTimestamp = intent.getLongExtra(KEY_UPLOAD_TIMESTAMP, -1);


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
                            ((mimeTypes != null) ? mimeTypes[i] : null)
                    );
                    if (files[i] == null) {
                        Log_OC.e(TAG, "obtainNewOCFileToUpload() returned null for remotePaths[i]:" + remotePaths[i]
                                + " and localPaths[i]:" + localPaths[i]);
                        return Service.START_NOT_STICKY;
                    }
                }
            }
            // at this point variable "OCFile[] files" is loaded correctly.

            String uploadKey = null;
            UploadFileOperation newUpload = null;
            try {
                for (int i = 0; i < files.length; i++) {

                    newUpload = new UploadFileOperation(
                            account,
                            files[i],
                            chunked,
                            forceOverwrite,
                            localAction,
                            this
                    );
                    if (isCreateRemoteFolder) {
                        newUpload.setRemoteFolderToBeCreated();
                    }
                    newUpload.addDatatransferProgressListener(this);
                    newUpload.addDatatransferProgressListener((FileUploaderBinder) mBinder);

                    // Save upload in database
                    OCUpload ocUpload = new OCUpload(files[i], account);
                    ocUpload.setForceOverwrite(forceOverwrite);
                    ocUpload.setCreateRemoteFolder(isCreateRemoteFolder);
                    ocUpload.setLocalAction(localAction);
                    ocUpload.setUseWifiOnly(isUseWifiOnly);
                    ocUpload.setWhileChargingOnly(isWhileChargingOnly);
                    ocUpload.setUploadStatus(UploadStatus.UPLOAD_LATER);

                    // storagePath inside upload is the temporary path. file
                    // contains the correct path used as db reference.
                    long id = mUploadsStorageManager.storeUpload(ocUpload);
                    newUpload.setOCUploadId(id);

                    Pair<String, String> putResult = mPendingUploads.putIfAbsent(
                            account.name,
                            files[i].getRemotePath(),
                            newUpload,
                            /*String.valueOf(id)*/ null
                    );
                    if (putResult != null) {
                        uploadKey = putResult.first;
                        requestedUploads.add(uploadKey);
                    } else {
                        mUploadsStorageManager.removeUpload(id);
                    }
                    // else, file already in the queue of uploads; don't repeat the request
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
            // *** TODO REWRITE: block inserted to request A retry; too many code copied, no control exception ***/
        } else {
            if (!intent.hasExtra(KEY_ACCOUNT) || !intent.hasExtra(KEY_RETRY_UPLOAD)) {
                Log_OC.e(TAG, "Not enough information provided in intent: no KEY_RETRY_UPLOAD_KEY");
                return START_NOT_STICKY;
            }
            OCUpload upload = intent.getParcelableExtra(KEY_RETRY_UPLOAD);

            UploadFileOperation newUpload = new UploadFileOperation(
                    account,
                    upload,
                    chunked,
                    upload.isForceOverwrite(),
                    upload.getLocalAction(),
                    this
            );
            if (upload.isCreateRemoteFolder()) {
                newUpload.setRemoteFolderToBeCreated();
            }
            newUpload.addDatatransferProgressListener(this);
            newUpload.addDatatransferProgressListener((FileUploaderBinder) mBinder);
            newUpload.setOCUploadId(upload.getUploadId());

            Pair<String, String> putResult = mPendingUploads.putIfAbsent(
                    account.name,
                    upload.getRemotePath(),
                    newUpload,
                    String.valueOf(upload.getUploadId()
                    ));
            if (putResult != null) {
                String uploadKey = putResult.first;
                requestedUploads.add(uploadKey);

                // Update upload in database
                upload.setUploadStatus(UploadStatus.UPLOAD_LATER);
                mUploadsStorageManager.updateUpload(upload);
            }
        }
        // *** TODO REWRITE END ***/

        if (requestedUploads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedUploads;
            mServiceHandler.sendMessage(msg);
        }
        return Service.START_NOT_STICKY;
    }

    /**
     * Provides a binder object that clients can use to perform operations on
     * the queue of uploads, excepting the addition of new files.
     * <p/>
     * Implemented to perform cancellation, pause and resume of existing
     * uploads.
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
        ((FileUploaderBinder) mBinder).clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // Review current upload, and cancel it if its account doen't exist
        if (mCurrentUpload != null &&
                !AccountUtils.exists(mCurrentUpload.getAccount(), getApplicationContext())) {
            mCurrentUpload.cancel();
        }
        // The rest of uploads are cancelled when they try to start
    }

    /**
     * Binder to let client components to perform operations on the queue of
     * uploads.
     * <p/>
     * It provides by itself the available operations.
     */
    public class FileUploaderBinder extends Binder implements OnDatatransferProgressListener {

        /**
         * Map of listeners that will be reported about progress of uploads from a
         * {@link FileUploaderBinder} instance
         */
        private Map<String, OnDatatransferProgressListener> mBoundListeners =
                new HashMap<String, OnDatatransferProgressListener>();


        /**
         * Cancels a pending or current upload of a remote file.
         *
         * @param account ownCloud account where the remote file will be stored.
         * @param file    A file in the queue of pending uploads
         */
        public void cancel(Account account, OCFile file) {
            cancel(account.name, file.getRemotePath(), file.getStoragePath());
        }

        /**
         * Cancels a pending or current upload that was persisted.
         *
         * @param storedUpload Upload operation persisted
         */
        public void cancel(OCUpload storedUpload) {
            cancel(storedUpload.getAccountName(), storedUpload.getRemotePath(), storedUpload.getLocalPath());
        }

        /**
         * Cancels a pending or current upload of a remote file.
         *
         * @param accountName Local name of an ownCloud account where the remote file will be stored.
         * @param remotePath  Remote target of the upload
         * @param localPath   Absolute local path to the source file
         */
        private void cancel(String accountName, String remotePath, String localPath) {
            Pair<UploadFileOperation, String> removeResult =
                    mPendingUploads.remove(accountName, remotePath);
            UploadFileOperation upload = removeResult.first;
            if (upload == null &&
                    mCurrentUpload != null && mCurrentAccount != null &&
                    mCurrentUpload.getRemotePath().startsWith(remotePath) &&
                    accountName.equals(mCurrentAccount.name)) {

                upload = mCurrentUpload;
            }
            if (upload != null) {
                boolean pending = !upload.isUploadInProgress();
                upload.cancel();
                if (pending) {
                    // need to update now table in mUploadsStorageManager, since the operation will not get
                    // to be run by FileUploader#uploadFile
                    OCUpload ocUpload =
                            mUploadsStorageManager.getUploadByLocalPath(localPath)[0];
                    // TODO bad idea, should search for account + remoteName, or uploadId
                    ocUpload.setUploadStatus(UploadStatus.UPLOAD_CANCELLED);
                    ocUpload.setLastResult(UploadResult.CANCELLED);
                    mUploadsStorageManager.updateUploadStatus(ocUpload);
                }
            }
        }

        /**
         * Cancels all the uploads for an account
         *
         * @param account ownCloud account.
         */
        public void cancel(Account account) {
            Log_OC.d(TAG, "Account= " + account.name);

            if (mCurrentUpload != null) {
                Log_OC.d(TAG, "Current Upload Account= " + mCurrentUpload.getAccount().name);
                if (mCurrentUpload.getAccount().name.equals(account.name)) {
                    mCurrentUpload.cancel();
                }
            }
            // Cancel pending uploads
            cancelUploadsForAccount(account);
        }

        // TODO: Review: Method from FileUploadService with some changes because the merge with FileUploader
        public void remove(Account account, OCFile file) {
            Pair<UploadFileOperation, String> removeResult =
                    mPendingUploads.remove(account.name, file.getRemotePath());
            UploadFileOperation upload = removeResult.first;
            //OCUpload upload = mPendingUploads.remove(buildRemoteName(account, file));
            if (upload == null) {
                Log_OC.e(TAG, "Could not delete upload " + file + " from mPendingUploads.");
            }
            int d = mUploadsStorageManager.removeUpload(upload.getStoragePath());
            if (d == 0) {
                Log_OC.e(TAG, "Could not delete upload " + file.getStoragePath() + " from database.");
            }
        }

        public void remove(OCUpload upload) {
            int d = mUploadsStorageManager.removeUpload(upload.getUploadId());
            if (d == 0) {
                Log_OC.e(TAG, "Could not delete upload " + upload.getRemotePath() + " from database.");
            }
        }

        // TODO: Review: Method from FileUploader with some changes because the merge with FileUploader
        // TODO Complete operation to retry the upload

        /**
         * Puts upload in upload list and tell FileUploader to upload items in list.
         */
        public void retry(Account account, OCUpload upload) {
//            String uploadKey = buildRemoteName(account, upload.getOCFile());
//            mPendingUploads.put(uploadKey, upload);
            FileUploader.retry(getApplicationContext(), account, upload);
        }

        public void clearListeners() {
            mBoundListeners.clear();
        }

        /**
         * Returns True when the file described by 'file' is being uploaded to
         * the ownCloud account 'account' or waiting for it
         * <p/>
         * If 'file' is a directory, returns 'true' if some of its descendant files
         * is uploading or waiting to upload.
         * <p/>
         * Warning: If remote file exists and !forceOverwrite the original file
         * is being returned here. That is, it seems as if the original file is
         * being updated when actually a new file is being uploaded.
         *
         * @param account Owncloud account where the remote file will be stored.
         * @param file    A file that could be in the queue of pending uploads
         */
        public boolean isUploading(Account account, OCFile file) {
            if (account == null || file == null)
                return false;
            return (mPendingUploads.contains(account.name, file.getRemotePath()));
        }


        /**
         * Adds a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param account  ownCloud account holding the file of interest.
         * @param file     {@link OCFile} of interest for listener.
         */
        public void addDatatransferProgressListener(
                OnDatatransferProgressListener listener,
                Account account,
                OCFile file
        ) {
            if (account == null || file == null || listener == null) return;
            String targetKey = buildRemoteName(account.name, file.getRemotePath());
            mBoundListeners.put(targetKey, listener);
        }


        /**
         * Adds a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param account  ownCloud account holding the file of interest.
         * @param ocUpload {@link OCUpload} of interest for listener.
         */
        public void addDatatransferProgressListener(
                OnDatatransferProgressListener listener,
                Account account,
                OCUpload ocUpload
        ) {
            if (account == null || ocUpload == null || listener == null) return;
            String targetKey = buildRemoteName(account.name, ocUpload.getRemotePath());
            mBoundListeners.put(targetKey, listener);
        }


        /**
         * Removes a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param account  ownCloud account holding the file of interest.
         * @param file     {@link OCFile} of interest for listener.
         */
        public void removeDatatransferProgressListener(
                OnDatatransferProgressListener listener,
                Account account,
                OCFile file
        ) {
            if (account == null || file == null || listener == null) return;
            String targetKey = buildRemoteName(account.name, file.getRemotePath());
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }


        /**
         * Removes a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param account  ownCloud account holding the file of interest.
         * @param ocUpload Stored upload of interest
         */
        public void removeDatatransferProgressListener(
                OnDatatransferProgressListener listener,
                Account account,
                OCUpload ocUpload
        ) {
            if (account == null || ocUpload == null || listener == null) return;
            String targetKey = buildRemoteName(account.name, ocUpload.getRemotePath());
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }


        // TODO: Review: Method from FileUploader with some changes because the merge with FileUploader
        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar,
                                       long totalToTransfer, String fileName) {
            String key = buildRemoteName(mCurrentUpload.getAccount().name, mCurrentUpload.getFile().getRemotePath());
            OnDatatransferProgressListener boundListener = mBoundListeners.get(key);
            if (boundListener != null) {
                boundListener.onTransferProgress(progressRate, totalTransferredSoFar,
                        totalToTransfer, fileName);
            }
        }

        /**
         * Builds a key for the map of listeners.
         * <p/>
         * TODO use method in IndexedForest, or refactor both to a common place
         * add to local database) to better policy (add to local database, then upload)
         *
         * @param accountName Local name of the ownCloud account where the file to upload belongs.
         * @param remotePath  Remote path to upload the file to.
         * @return Key
         */
        private String buildRemoteName(String accountName, String remotePath) {
            return accountName + remotePath;
        }

        /*private String buildRemoteName(Account account, OCFile file, long uploadId) {
            String suffix = String.valueOf(uploadId);
            if (uploadId != -1) {
                suffix = "";
            }
            return account.name + file.getRemotePath() + suffix;
        }*/

    }

    /**
     * Upload worker. Performs the pending uploads in the order they were
     * requested.
     * <p/>
     * Created with the Looper of a new thread, started in
     * {@link FileUploader#onCreate()}.
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will
        // warn about a possible memory leak
        FileUploader mService;

        public ServiceHandler(Looper looper, FileUploader service) {
            super(looper);
            if (service == null)
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            AbstractList<String> requestedUploads = (AbstractList<String>) msg.obj;
            if (msg.obj != null) {
                Iterator<String> it = requestedUploads.iterator();
                while (it.hasNext()) {
                    mService.uploadFile(it.next());
                }
            }
            Log_OC.d(TAG, "Stopping command after id " + msg.arg1);
            mService.stopSelf(msg.arg1);
        }
    }

    /**
     * Core upload method: sends the file(s) to upload
     *
     * @param uploadKey Key to access the upload to perform, contained in mPendingUploads
     */
    public void uploadFile(String uploadKey) {

        mCurrentUpload = mPendingUploads.get(uploadKey);

        if (mCurrentUpload != null) {
            // Detect if the account exists
            if (AccountUtils.exists(mCurrentUpload.getAccount(), getApplicationContext())) {
                Log_OC.d(TAG, "Account " + mCurrentUpload.getAccount().name + " exists");

                mUploadsStorageManager.updateDatabaseUploadStart(mCurrentUpload);

                notifyUploadStart(mCurrentUpload);

                RemoteOperationResult uploadResult = null;

                try {
                    /// prepare client object to send the request to the ownCloud server
                    if (mCurrentAccount == null || !mCurrentAccount.equals(mCurrentUpload.getAccount())) {
                        mCurrentAccount = mCurrentUpload.getAccount();
                        mStorageManager = new FileDataStorageManager(
                                mCurrentAccount,
                                getContentResolver()
                        );
                    }   // else, reuse storage manager from previous operation

                    // always get client from client manager, to get fresh credentials in case of update
                    OwnCloudAccount ocAccount = new OwnCloudAccount(mCurrentAccount, this);
                    mUploadClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, this);

                    /// perform the upload
                    uploadResult = mCurrentUpload.execute(mUploadClient, mStorageManager);


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
                        /** TODO: grant that name is also updated for mCurrentUpload.getOCUploadId */

                    } else {
                        removeResult = mPendingUploads.removePayload(
                                mCurrentAccount.name,
                                mCurrentUpload.getRemotePath()
                        );
                    }

                    mUploadsStorageManager.updateDatabaseUploadResult(uploadResult, mCurrentUpload);

                    /// notify result
                    notifyUploadResult(mCurrentUpload, uploadResult);

                    sendBroadcastUploadFinished(mCurrentUpload, uploadResult, removeResult.second);
                }

            } else {
                // Cancel the transfer
                Log_OC.d(TAG, "Account " + mCurrentUpload.getAccount().toString() +
                        " doesn't exist");
                cancelUploadsForAccount(mCurrentUpload.getAccount());

            }
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
        mNotificationBuilder =
                NotificationBuilderWithProgressBar.newNotificationBuilderWithProgressBar(this);
        mNotificationBuilder
                .setOngoing(true)
                .setSmallIcon(R.drawable.notification_icon)
                .setTicker(getString(R.string.uploader_upload_in_progress_ticker))
                .setContentTitle(getString(R.string.uploader_upload_in_progress_ticker))
                .setProgress(100, 0, false)
                .setContentText(
                        String.format(getString(R.string.uploader_upload_in_progress_content), 0, upload.getFileName
                                ()));

        /// includes a pending intent in the notification showing the details
        Intent showUploadListIntent = new Intent(this, UploadListActivity.class);
        showUploadListIntent.putExtra(FileActivity.EXTRA_FILE, upload.getFile());
        showUploadListIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
        showUploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                showUploadListIntent, 0));

        // TODO: decide where do we go to navigate when the user clicks the notification
        /// includes a pending intent in the notification showing the details view of the file
//        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
//        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE,  (Parcelable) upload.getFile());
//        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
//        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(
//                this, (int) System.currentTimeMillis(), showDetailsIntent, 0
//        ));

        mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotificationBuilder.build());

        // TODO really needed?
        sendBroadcastUploadStarted(mCurrentUpload);
    }

    /**
     * Callback method to update the progress bar in the status notification
     */
    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar,
                                   long totalToTransfer, String filePath) {
        int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
        if (percent != mLastPercent) {
            mNotificationBuilder.setProgress(100, percent, false);
            String fileName = filePath.substring(filePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1);
            String text = String.format(getString(R.string.uploader_upload_in_progress_content), percent, fileName);
            mNotificationBuilder.setContentText(text);
            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotificationBuilder.build());
        }
        mLastPercent = percent;
    }

    /**
     * Updates the status notification with the result of an upload operation.
     *
     * @param uploadResult Result of the upload operation.
     * @param upload       Finished upload operation
     */
    private void notifyUploadResult(UploadFileOperation upload,
                                    RemoteOperationResult uploadResult) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.getCode());
        // / cancelled operation or success -> silent removal of progress notification
        mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);

        // Show the result: success or fail notification
        if (!uploadResult.isCancelled()) {
            int tickerId = (uploadResult.isSuccess()) ? R.string.uploader_upload_succeeded_ticker :
                    R.string.uploader_upload_failed_ticker;

            String content;

            // check credentials error
            boolean needsToUpdateCredentials = (
                    uploadResult.getCode() == ResultCode.UNAUTHORIZED ||
                            uploadResult.isIdPRedirection()
            );
            tickerId = (needsToUpdateCredentials) ?
                    R.string.uploader_upload_failed_credentials_error : tickerId;

            mNotificationBuilder
                    .setTicker(getString(tickerId))
                    .setContentTitle(getString(tickerId))
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setProgress(0, 0, false);

            content = ErrorMessageAdapter.getErrorCauseMessage(
                    uploadResult, upload, getResources()
            );

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

                mUploadClient = null;
                // grant that future retries on the same account will get the fresh credentials
            } else {
                mNotificationBuilder.setContentText(content);
// Changes for compilation
//                if (upload.isInstant()) {
//                    DbHandler db = null;
//                    try {
//                        db = new DbHandler(this.getBaseContext());
//                        String message = uploadResult.getLogMessage() + " errorCode: " +
//                                uploadResult.getCode();
//                        Log_OC.e(TAG, message + " Http-Code: " + uploadResult.getHttpCode());
//                        if (uploadResult.getCode() == ResultCode.QUOTA_EXCEEDED) {
//                            //message = getString(R.string.failed_upload_quota_exceeded_text);
//                            if (db.updateFileState(
//                                    upload.getOriginalStoragePath(),
//                                    DbHandler.UPLOAD_STATUS_UPLOAD_FAILED,
//                                    message) == 0) {
//                                db.putFileForLater(
//                                        upload.getOriginalStoragePath(),
//                                        upload.getAccount().name,
//                                        message
//                                );
//                            }
//                        }
//                    } finally {
//                        if (db != null) {
//                            db.close();
//                        }
//                    }
//                }
            }

            if (!uploadResult.isSuccess()) {
                //in case of failure, do not show details file view (because there is no file!)
                Intent showUploadListIntent = new Intent(this, UploadListActivity.class);
                showUploadListIntent.putExtra(FileActivity.EXTRA_FILE, upload.getFile());
                showUploadListIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
                showUploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                        showUploadListIntent, 0));
            }

            mNotificationBuilder.setContentText(content);
            mNotificationManager.notify(tickerId, mNotificationBuilder.build());

            if (uploadResult.isSuccess()) {
//Changes for compilation
//                DbHandler db = new DbHandler(this.getBaseContext());
//                db.removeIUPendingFile(mCurrentUpload.getOriginalStoragePath());
//                db.close();
                mPendingUploads.remove(upload.getAccount().name, upload.getFile().getRemotePath());
                //updateDatabaseUploadResult(uploadResult, mCurrentUpload);
                // remove success notification, with a delay of 2 seconds
                NotificationDelayer.cancelWithDelay(
                        mNotificationManager,
                        R.string.uploader_upload_succeeded_ticker,
                        2000);
            }
        }
    }

    /**
     * Sends a broadcast in order to the interested activities can update their
     * view
     *
     * @param upload Finished upload operation
     */
    private void sendBroadcastUploadStarted(
            UploadFileOperation upload) {

        Intent start = new Intent(getUploadStartMessage());
        start.putExtra(EXTRA_REMOTE_PATH, upload.getRemotePath()); // real remote
        start.putExtra(EXTRA_OLD_FILE_PATH, upload.getOriginalStoragePath());
        start.putExtra(ACCOUNT_NAME, upload.getAccount().name);

        sendStickyBroadcast(start);
    }

    /**
     * Sends a broadcast in order to the interested activities can update their
     * view
     *
     * @param upload                 Finished upload operation
     * @param uploadResult           Result of the upload operation
     * @param unlinkedFromRemotePath Path in the uploads tree where the upload was unlinked from
     */
    private void sendBroadcastUploadFinished(
            UploadFileOperation upload,
            RemoteOperationResult uploadResult,
            String unlinkedFromRemotePath) {

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

        sendStickyBroadcast(end);
    }

    /**
     * Remove uploads of an account
     *
     * @param account Downloads account to remove
     */
    private void cancelUploadsForAccount(Account account) {
        // Cancel pending uploads
        mPendingUploads.remove(account.name);
    }

    /**
     * Call if all pending uploads are to be retried.
     */
//    public static void retry(Context context) {
//        retry(context, null);
//    }


}
