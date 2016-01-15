/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   Copyright (C) 2015 ownCloud Inc.
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
import android.support.v4.app.NotificationCompat;
import android.util.Pair;
import android.webkit.MimeTypeMap;

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
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.notifications.NotificationBuilderWithProgressBar;
import com.owncloud.android.notifications.NotificationDelayer;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.UploadListActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.UploadUtils;
import com.owncloud.android.utils.UriUtils;

import java.io.File;
import java.util.AbstractList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for uploading files. Invoke using context.startService(...). Files to
 * be uploaded are stored persistently using {@link UploadsStorageManager}.
 * 
 * On next invocation of {@link FileUploadService} uploaded files which
 * previously failed will be uploaded again until either upload succeeded or a
 * fatal error occured.
 * 
 * Every file passed to this service is uploaded. No filtering is performed.
 * However, Intent keys (e.g., KEY_WIFI_ONLY) are obeyed.
 * 
 */
public class FileUploadService extends Service implements OnDatatransferProgressListener,
        OnAccountsUpdateListener {

    private static final String TAG = FileUploadService.class.getSimpleName();

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
    /**
     * Call this Service with KEY_RETRY and KEY_RETRY_REMOTE_PATH to retry
     * download of file identified by KEY_RETRY_REMOTE_PATH.
     */
    private static final String KEY_RETRY_REMOTE_PATH = "KEY_RETRY_REMOTE_PATH";    
    /**
     * {@link Account} to which file is to be uploaded.
     */
    public static final String KEY_ACCOUNT = "ACCOUNT";
    /**
     * Set whether single file or multiple files are to be uploaded. Value must be of type {@link UploadQuantity}.
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
    /**
     * Set to future UNIX timestamp. Upload will not be performed before this timestamp. 
     */
    public static final String KEY_UPLOAD_TIMESTAMP= "KEY_UPLOAD_TIMESTAMP";
    
    public static final String KEY_LOCAL_BEHAVIOUR = "BEHAVIOUR";

    public static final String KEY_INSTANT_UPLOAD = "INSTANT_UPLOAD";

    public static final int LOCAL_BEHAVIOUR_COPY = 0;
    public static final int LOCAL_BEHAVIOUR_MOVE = 1;
    public static final int LOCAL_BEHAVIOUR_FORGET = 2;

    public static final int UPLOAD_SINGLE_FILE = 0;
    public static final int UPLOAD_MULTIPLE_FILES = 1;

    /**
     * Describes local behavior for upload.
     */
    public enum LocalBehaviour {
        /**
         * Creates a copy of file and stores it in tmp folder inside owncloud
         * folder on sd-card. After upload it is moved to local owncloud
         * storage. Original file stays untouched.
         */
        LOCAL_BEHAVIOUR_COPY(0),
        /**
         * Upload file from current storage. Afterwards original file is move to
         * local owncloud storage.
         */
        LOCAL_BEHAVIOUR_MOVE(1),
        /**
         * Just uploads file and leaves it where it is. Original file stays
         * untouched.
         */
        LOCAL_BEHAVIOUR_FORGET(2);
        private final int value;

        LocalBehaviour(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static LocalBehaviour fromValue(int value){
            switch (value)
            {
                case 0:
                    return LOCAL_BEHAVIOUR_COPY;
                case 1:
                    return LOCAL_BEHAVIOUR_MOVE;
                case 2:
                    return LOCAL_BEHAVIOUR_FORGET;
            }
            return null;
        }
    }
    
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

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private ExecutorService mUploadExecutor;

    private IBinder mBinder;
    private OwnCloudClient mUploadClient = null;
    private Account mCurrentAccount = null; // LastAccount in FileUploadService
    private FileDataStorageManager mStorageManager;
    //since there can be only one instance of an Android service, there also just one db connection.
    private UploadsStorageManager mUploadsStorageManager = null;
    
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    private final AtomicBoolean mCancellationPossible = new AtomicBoolean(false);

    /**
     * List of uploads that are currently pending. Maps from remotePath to where file
     * is being uploaded to {@link UploadFileOperation}.
     */
    //private ConcurrentMap<String, OCUpload> mPendingUploads = new ConcurrentHashMap<String, OCUpload>();
    // TODO: change kind of mPendingUploads: List to IndexedForest
    private IndexedForest<UploadFileOperation> mPendingUploads = new IndexedForest<UploadFileOperation>();

    /**
     * {@link UploadFileOperation} object of ongoing upload. Can be null. Note: There can only be one concurrent upload!
     */
    private UploadFileOperation mCurrentUpload = null;
    
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mLastPercent;

    private static final String MIME_TYPE_PDF = "application/pdf";
    private static final String FILE_EXTENSION_PDF = ".pdf";

    public FileUploadService() {
        super();
    }

    public static String getUploadFinishMessage() {
        return FileUploadService.class.getName().toString() + UPLOAD_FINISH_MESSAGE;
    }

    /**
     * Builds a key for mPendingUploads from the account and file to upload
     * 
     * @param account Account where the file to upload is stored
     * @param file File to upload
     */
    private String buildRemoteName(Account account, OCFile file) {
        return account.name + file.getRemotePath();
    }

    
    private String buildRemoteName(Account account, String remotePath) {
        return account.name + remotePath;
    }

    private String buildRemoteName(OCUpload OCUpload) {
        return OCUpload.getAccountName() + OCUpload.getRemotePath();
    }

    /**
     * Checks if an ownCloud server version should support chunked uploads.
     * 
     * @param version OwnCloud version instance corresponding to an ownCloud
     *            server.
     * @return 'True' if the ownCloud server with version supports chunked
     *         uploads.
     *
     * TODO - move to OCClient
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
        //Log_OC.d(TAG, "mPendingUploads size:" + mPendingUploads.size() + " - onCreate");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // HandlerThread thread = new HandlerThread("FileUploadService-Requester"); --> From FileUploadService
        HandlerThread thread = new HandlerThread("FileUploaderThread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        //mServiceHandler = new ServiceHandler(mServiceLooper); // FileUploaderService
        mServiceHandler = new ServiceHandler(mServiceLooper, this); // FileUploader
        mBinder = new FileUploaderBinder();

        // From FileUploaderService
        mUploadsStorageManager = new UploadsStorageManager(getContentResolver());

        //when this service starts there is no upload in progress. if db says so, app probably crashed before.
        mUploadsStorageManager.setAllCurrentToUploadLater();

        mUploadExecutor = Executors.newFixedThreadPool(1);

        Log_OC.d(TAG, "FileUploadService.retry() called by onCreate()");
        FileUploadService.retry(getApplicationContext());

        AccountManager am = AccountManager.get(getApplicationContext());
        am.addOnAccountsUpdatedListener(this, null, false);
    }

    /**
     * Service clean up
     */
    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "Destroying service" );
        mBinder = null;
        mServiceHandler = null;
        mServiceLooper.quit();
        mServiceLooper = null;
        mNotificationManager = null;

        // From FileUploader
        //Log_OC.d(TAG, "mPendingUploads size:" + mPendingUploads.size() + " - onDestroy");
        Log_OC.d(TAG, "mPendingUploads size:" + mPendingUploads.toString() + " - onDestroy");
        //mServiceLooper.quit();
        mUploadExecutor.shutdown();

        AccountManager am = AccountManager.get(getApplicationContext());
        am.removeOnAccountsUpdatedListener(this);

        super.onDestroy();

    }

    // Deprecated: TODO - Remove, not in FileUploader
//    @Override
//    public void onStart(Intent intent, int intentStartId) {
//        Message msg = mServiceHandler.obtainMessage();
//        msg.arg1 = intentStartId;
//        msg.obj = intent;
//        mServiceHandler.sendMessage(msg);
//    }

    /**
     * Entry point to add one or several files to the queue of uploads.
     *
     * New uploads are added calling to startService(), resulting in a call to
     * this method. This ensures the service will keep on working although the
     * caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command with id " + startId);

        if (!intent.hasExtra(KEY_ACCOUNT) || !intent.hasExtra(KEY_UPLOAD_TYPE)
                || !(intent.hasExtra(KEY_LOCAL_FILE) || intent.hasExtra(KEY_FILE))) {
            Log_OC.e(TAG, "Not enough information provided in intent");
            return Service.START_NOT_STICKY;
        }
        int uploadType = intent.getIntExtra(KEY_UPLOAD_TYPE, -1);
        if (uploadType == -1) {
            Log_OC.e(TAG, "Incorrect upload type provided");
            return Service.START_NOT_STICKY;
        }
        Account account = intent.getParcelableExtra(KEY_ACCOUNT);
        if (!AccountUtils.exists(account, getApplicationContext())) {
            return Service.START_NOT_STICKY;
        }

        String[] localPaths = null, remotePaths = null, mimeTypes = null;
        OCFile[] files = null;
        if (uploadType == UPLOAD_SINGLE_FILE) {

            if (intent.hasExtra(KEY_FILE)) {
                files = new OCFile[] { intent.getParcelableExtra(KEY_FILE) };

            } else {
                localPaths = new String[] { intent.getStringExtra(KEY_LOCAL_FILE) };
                remotePaths = new String[] { intent.getStringExtra(KEY_REMOTE_FILE) };
                mimeTypes = new String[] { intent.getStringExtra(KEY_MIME_TYPE) };
            }

        } else { // mUploadType == UPLOAD_MULTIPLE_FILES

            if (intent.hasExtra(KEY_FILE)) {
                files = (OCFile[]) intent.getParcelableArrayExtra(KEY_FILE); // TODO
                // will
                // this
                // casting
                // work
                // fine?

            } else {
                localPaths = intent.getStringArrayExtra(KEY_LOCAL_FILE);
                remotePaths = intent.getStringArrayExtra(KEY_REMOTE_FILE);
                mimeTypes = intent.getStringArrayExtra(KEY_MIME_TYPE);
            }
        }

        FileDataStorageManager storageManager = new FileDataStorageManager(account,
                getContentResolver());

        boolean forceOverwrite = intent.getBooleanExtra(KEY_FORCE_OVERWRITE, false);
        boolean isInstant = intent.getBooleanExtra(KEY_INSTANT_UPLOAD, false);
        int localAction = intent.getIntExtra(KEY_LOCAL_BEHAVIOUR, LOCAL_BEHAVIOUR_FORGET);

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
                files[i] = obtainNewOCFileToUpload(remotePaths[i], localPaths[i],
                        ((mimeTypes != null) ? mimeTypes[i] : null));
                if (files[i] == null) {
                    // TODO @andomaex add failure Notification
                    return Service.START_NOT_STICKY;
                }
            }
        }

        OwnCloudVersion ocv = AccountUtils.getServerVersion(account);

        boolean chunked = FileUploadService.chunkedUploadIsSupported(ocv);
        AbstractList<String> requestedUploads = new Vector<String>();
        String uploadKey = null;
        UploadFileOperation newUpload = null;
        try {
            for (int i = 0; i < files.length; i++) {
                newUpload = new UploadFileOperation(
                        account,
                        files[i],
                        chunked,
                        //isInstant,
                        forceOverwrite,
                        FileUploadService.LocalBehaviour.values()[localAction], // Change for compilation
                        getApplicationContext()
                );
                if (isInstant) {
                    newUpload.setRemoteFolderToBeCreated();
                }
                newUpload.addDatatransferProgressListener(this);
                newUpload.addDatatransferProgressListener((FileUploaderBinder) mBinder);
                Pair<String, String> putResult = mPendingUploads.putIfAbsent(
                        account, files[i].getRemotePath(), newUpload
                );
                if (putResult != null) {
                    uploadKey = putResult.first;
                    requestedUploads.add(uploadKey);
                }   // else, file already in the queue of uploads; don't repeat the request
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

        if (requestedUploads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedUploads;
            mServiceHandler.sendMessage(msg);
        }
        return Service.START_NOT_STICKY;
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
     * Provides a binder object that clients can use to perform operations on
     * the queue of uploads, excepting the addition of new files.
     *
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
        return false; // not accepting rebinding (default behaviour)
    }


    /**
     * Binder to let client components to perform operations on the queue of
     * uploads.
     *
     * It provides by itself the available operations.
     */
    public class FileUploaderBinder extends Binder implements OnDatatransferProgressListener {

        /**
         * Map of listeners that will be reported about progress of uploads from
         * a {@link FileUploaderBinder} instance
         */
        private Map<String, OnDatatransferProgressListener> mBoundListeners = new HashMap<String, OnDatatransferProgressListener>();

        /**
         * Returns ongoing upload operation. May be null.
         */
        public UploadFileOperation getCurrentUploadOperation() {
            return mCurrentUpload;
        }

        /**
         * Cancels a pending or current upload of a remote file.
         *
         * @param account   Owncloud account where the remote file will be stored.
         * @param file      A file in the queue of pending uploads
         */
        public void cancel(Account account, OCFile file) {
            // From FileUploader
            Pair<UploadFileOperation, String> removeResult = mPendingUploads.remove(account, file.getRemotePath());
            UploadFileOperation upload = removeResult.first;
            if (upload != null) {
                upload.cancel();
            } else {
                if (mCurrentUpload != null && mCurrentAccount != null &&
                        mCurrentUpload.getRemotePath().startsWith(file.getRemotePath()) &&
                        account.name.equals(mCurrentAccount.name)) {
                    mCurrentUpload.cancel();
                }
            }

            // From FileUploadService
            // updating current references (i.e., uploadStatus of current
            // upload) is handled by updateDataseUploadResult() which is called
            // after upload finishes. Following cancel call makes sure that is
            // does finish right now.
            if (mCurrentUpload != null && mCurrentUpload.isUploadInProgress()) {
                Log_OC.d(TAG, "Calling cancel for " + file.getRemotePath() + " during upload operation.");
                mCurrentUpload.cancel();
            } else if(mCancellationPossible.get()){
                Log_OC.d(TAG, "Calling cancel for " + file.getRemotePath() + " during preparing for upload.");
                mCancellationRequested.set(true);
            } else {
                Log_OC.d(TAG, "Calling cancel for " + file.getRemotePath() + " while upload is pending.");
                // upload not in progress, but pending.
                // in this case we have to update the db here.
                //OCUpload upload = mPendingUploads.remove(buildRemoteName(account, file));
                OCUpload ocUpload = mUploadsStorageManager.getUploadByLocalPath(upload.getStoragePath())[0];
                ocUpload.setUploadStatus(UploadStatus.UPLOAD_CANCELLED);
                ocUpload.setLastResult(UploadResult.CANCELLED);
                // storagePath inside upload is the temporary path. file
                // contains the correct path used as db reference.
                ocUpload.getOCFile().setStoragePath(file.getStoragePath());
                mUploadsStorageManager.updateUploadStatus(ocUpload);
            }

        }

        /**
         * Cancels a pending or current upload for an account
         *
         * @param account Owncloud accountName where the remote file will be stored.
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
            Pair<UploadFileOperation, String> removeResult = mPendingUploads.remove(account, file.getRemotePath());
            UploadFileOperation upload = removeResult.first;
            //OCUpload upload = mPendingUploads.remove(buildRemoteName(account, file));
            if(upload == null) {
                Log_OC.e(TAG, "Could not delete upload "+ file +" from mPendingUploads.");
            }
            int d = mUploadsStorageManager.removeUpload(upload.getStoragePath());
            if(d == 0) {
                Log_OC.e(TAG, "Could not delete upload "+ file.getStoragePath() +" from database.");
            }
        }

        // TODO: Review: Method from FileUploadService with some changes because the merge with FileUploader
        // TODO Complete operation to retry the upload
        /**
         * Puts upload in upload list and tell FileUploadService to upload items in list.
         */
        public void retry(Account account, OCUpload upload) {
            String uploadKey = buildRemoteName(upload);
            //mPendingUploads.put(uploadKey, upload);
            FileUploadService.retry(getApplicationContext(), uploadKey);
        }

        public void clearListeners() {
            mBoundListeners.clear();
        }

        /**
         * Returns True when the file described by 'file' is being uploaded to
         * the ownCloud account 'account' or waiting for it
         *
         * If 'file' is a directory, returns 'true' if some of its descendant
         * files is uploading or waiting to upload.
         *
         * Warning: If remote file exists and !forceOverwrite the original file
         * is being returned here. That is, it seems as if the original file is
         * being updated when actually a new file is being uploaded.
         *
         * @param account Owncloud account where the remote file will be stored.
         * @param file A file that could be in the queue of pending uploads
         */
        public boolean isUploading(Account account, OCFile file) {
            if (account == null || file == null)
                return false;
            return (mPendingUploads.contains(account, file.getRemotePath()));
            // Commented code from FileUploadService
//            String targetKey = buildRemoteName(account, file);
//            synchronized (mPendingUploads) {
//                if (file.isFolder()) {
//                    // this can be slow if there are many uploads :(
//                    Iterator<String> it = mPendingUploads.keySet().iterator();
//                    boolean found = false;
//                    while (it.hasNext() && !found) {
//                        found = it.next().startsWith(targetKey);
//                    }
//                    return found;
//                } else {
//                    return (mPendingUploads.containsKey(targetKey));
//                }
//            }
        }

        /**
         * Adds a listener interested in the progress of the upload for a
         * concrete file.
         *
         * @param listener  Object to notify about progress of transfer.
         * @param account   ownCloud account holding the file of interest.
         * @param file      {@link OCFile} of interest for listener.
         */
        public void addDatatransferProgressListener(OnDatatransferProgressListener listener, Account account,
                                                    OCFile file) {
            if (account == null || file == null || listener == null)
                return;
            String targetKey = buildRemoteName(account, file);
            mBoundListeners.put(targetKey, listener);
        }

        /**
         * Removes a listener interested in the progress of the upload for a
         * concrete file.
         *
         * @param listener  Object to notify about progress of transfer.
         * @param account   ownCloud account holding the file of interest.
         * @param file      {@link OCFile} of interest for listener.
         */
        public void removeDatatransferProgressListener(OnDatatransferProgressListener listener, Account account,
                                                       OCFile file) {
            if (account == null || file == null || listener == null)
                return;
            String targetKey = buildRemoteName(account, file);
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }


        // TODO: Review: Method from FileUploadService with some changes because the merge with FileUploader
        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer,
                                       String fileName) {

            String key = buildRemoteName(mCurrentUpload.getAccount(), mCurrentUpload.getFile());
            OnDatatransferProgressListener boundListener = mBoundListeners.get(key);
            if (boundListener != null) {
                boundListener.onTransferProgress(progressRate, totalTransferredSoFar,
                        totalToTransfer, fileName);
            }
//
//            Set<Entry<String, OCUpload>> uploads = mPendingUploads.entrySet();
//            UploadFileOperation currentUpload = mCurrentUpload;
//            if (currentUpload == null) {
//                Log_OC.e(TAG, "Found no current upload with remote path " + localFileName + ". Ignore.");
//                return;
//            }
//            String key = buildRemoteName(currentUpload.getAccount(), currentUpload.getFile());
//            OnDatatransferProgressListener boundListener = mBoundListeners.get(key);
//            if (boundListener != null) {
//                boundListener.onTransferProgress(progressRate, totalTransferredSoFar, totalToTransfer, localFileName);
//            }
        }


    }

    // TODO: Remove this comment: Class totally replaced by FileUploader ServiceHandler
    // TODO: Review merge modifications
    /**
     * Upload worker. Performs the pending uploads in the order they were
     * requested.
     *
     * Created with the Looper of a new thread, started in
     * {@link FileUploadService#onCreate()}.
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will
        // warn about a possible memory leak
        FileUploadService mService;

        public ServiceHandler(Looper looper, FileUploadService service) {
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

    // TODO: Method from FileUploader
    /**
     * Core upload method: sends the file(s) to upload
     *
     * @param uploadKey     Key to access the upload to perform, contained in mPendingUploads
     */
    public void uploadFile(String uploadKey) {

        mCurrentUpload = mPendingUploads.get(uploadKey);

        if (mCurrentUpload != null) {
            // Detect if the account exists
            if (AccountUtils.exists(mCurrentUpload.getAccount(), getApplicationContext())) {
                Log_OC.d(TAG, "Account " + mCurrentUpload.getAccount().name + " exists");

                notifyUploadStart(mCurrentUpload);

                RemoteOperationResult uploadResult = null, grantResult;

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


                    /// check the existence of the parent folder for the file to upload
                    String remoteParentPath = new File(mCurrentUpload.getRemotePath()).getParent();
                    remoteParentPath = remoteParentPath.endsWith(OCFile.PATH_SEPARATOR) ?
                            remoteParentPath : remoteParentPath + OCFile.PATH_SEPARATOR;
                    grantResult = grantFolderExistence(remoteParentPath);

                    /// perform the upload
                    if (grantResult.isSuccess()) {
                        OCFile parent = mStorageManager.getFileByPath(remoteParentPath);
                        mCurrentUpload.getFile().setParentId(parent.getFileId());
                        uploadResult = mCurrentUpload.execute(mUploadClient);
                        if (uploadResult.isSuccess()) {
                            saveUploadedFile();

                        } else if (uploadResult.getCode() == ResultCode.SYNC_CONFLICT) {
                            mStorageManager.saveConflict(mCurrentUpload.getFile(),
                                    mCurrentUpload.getFile().getEtagInConflict());
                        }
                    } else {
                        uploadResult = grantResult;
                    }

                } catch (Exception e) {
                    Log_OC.e(TAG, "Error uploading", e);
                    uploadResult = new RemoteOperationResult(e);

                } finally {
                    Pair<UploadFileOperation, String> removeResult;
                    if (mCurrentUpload.wasRenamed()) {
                        removeResult = mPendingUploads.removePayload(
                                mCurrentAccount,
                                mCurrentUpload.getOldFile().getRemotePath()
                        );
                    } else {
                        removeResult = mPendingUploads.removePayload(
                                mCurrentAccount,
                                mCurrentUpload.getRemotePath()
                        );
                    }

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

    // TODO: Replaced by uploadFile(String uploadKey)
    /**
     * Core upload method: sends the file(s) to upload. This function blocks
     * until upload succeeded or failed.
     *
     * Before uploading starts mCurrentUpload is set.
     *
     * @param OCUpload Key to access the upload to perform, contained in
     *            mPendingUploads
     * @return RemoteOperationResult of upload operation.
     */
//    private RemoteOperationResult uploadFile(OCUpload OCUpload) {
//
//        if (mCurrentUpload != null) {
//            Log_OC.e(TAG,
//                    "mCurrentUpload != null. Meaning there is another upload in progress, cannot start a new one. Fix that!");
//            return new RemoteOperationResult(new IllegalStateException("mCurrentUpload != null when calling uploadFile()"));
//        }
//
//        AccountManager aMgr = AccountManager.get(this);
//        Account account = OCUpload.getAccount(getApplicationContext());
//        String version = aMgr.getUserData(account, Constants.KEY_OC_VERSION);
//        OwnCloudVersion ocv = new OwnCloudVersion(version);
//
//        boolean chunked = FileUploadService.chunkedUploadIsSupported(ocv);
//        String uploadKey = null;
//
//        uploadKey = buildRemoteName(account, OCUpload.getRemotePath());
//        OCFile file = OCUpload.getOCFile();
//        Log_OC.d(TAG, "mCurrentUpload = new UploadFileOperation");
//        mCurrentUpload = new UploadFileOperation(account, file, chunked, OCUpload.isForceOverwrite(),
//                OCUpload.getLocalAction(), getApplicationContext());
//        if (OCUpload.isCreateRemoteFolder()) {
//            mCurrentUpload.setRemoteFolderToBeCreated();
//        }
//        mCurrentUpload.addDatatransferProgressListener((FileUploaderBinder) mBinder);
//        mCurrentUpload.addDatatransferProgressListener(this);
//
//        mCancellationRequested.set(false);
//        mCancellationPossible.set(true);
//        notifyUploadStart(mCurrentUpload);
//
//        RemoteOperationResult uploadResult = null, grantResult = null;
//        try {
//            // prepare client object to send requests to the ownCloud
//            // server
//            if (mUploadClient == null || !mCurrentAccount.equals(mCurrentUpload.getAccount())) {
//                mCurrentAccount = mCurrentUpload.getAccount();
//                mStorageManager = new FileDataStorageManager(mCurrentAccount, getContentResolver());
//                OwnCloudAccount ocAccount = new OwnCloudAccount(mCurrentAccount, this);
//                mUploadClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, this);
//            }
//
//            // check the existence of the parent folder for the file to
//            // upload
//            String remoteParentPath = new File(mCurrentUpload.getRemotePath()).getParent();
//            remoteParentPath = remoteParentPath.endsWith(OCFile.PATH_SEPARATOR) ? remoteParentPath : remoteParentPath
//                    + OCFile.PATH_SEPARATOR;
//            //TODO this might take a moment and should thus be cancelable, for now: cancel afterwards.
//            grantResult = grantFolderExistence(mCurrentUpload, remoteParentPath);
//
//            if(mCancellationRequested.get()) {
//                throw new OperationCancelledException();
//            }
//            mCancellationPossible.set(false);
//
//            // perform the upload
//            if (grantResult.isSuccess()) {
//                OCFile parent = mStorageManager.getFileByPath(remoteParentPath);
//                mCurrentUpload.getFile().setParentId(parent.getFileId());
//                // inside this call the remote path may be changed (if remote
//                // file exists and !forceOverwrite) in this case the map from
//                // mPendingUploads is wrong. This results in an upload
//                // indication in the GUI showing that the original file is being
//                // uploaded (instead that a new one is created)
//                uploadResult = mCurrentUpload.execute(mUploadClient);
//                if (uploadResult.isSuccess()) {
//                    saveUploadedFile(mCurrentUpload);
//                }
//            } else {
//                uploadResult = grantResult;
//            }
//
//        } catch (AccountsException e) {
//            Log_OC.e(TAG, "Error while trying to get autorization for " + mCurrentAccount.name, e);
//            uploadResult = new RemoteOperationResult(e);
//
//        } catch (IOException e) {
//            Log_OC.e(TAG, "Error while trying to get autorization for " + mCurrentAccount.name, e);
//            uploadResult = new RemoteOperationResult(e);
//
//        } catch (OperationCancelledException e) {
//            uploadResult = new RemoteOperationResult(e);
//        } finally {
//            Log_OC.d(TAG, "Remove CurrentUploadItem from pending upload Item Map.");
//            if (uploadResult.isException()) {
//                // enforce the creation of a new client object for next
//                // uploads; this grant that a new socket will
//                // be created in the future if the current exception is due
//                // to an abrupt lose of network connection
//                mUploadClient = null;
//            }
//        }
//        return uploadResult;
//    }

    /**
     * Checks the existence of the folder where the current file will be uploaded both
     * in the remote server and in the local database.
     *
     * If the upload is set to enforce the creation of the folder, the method tries to
     * create it both remote and locally.
     *
     *  @param  pathToGrant     Full remote path whose existence will be granted.
     *  @return  An {@link OCFile} instance corresponding to the folder where the file
     *  will be uploaded.
     */
    private RemoteOperationResult grantFolderExistence(String pathToGrant) {
        RemoteOperation operation = new ExistenceCheckRemoteOperation(pathToGrant, this, false);
        RemoteOperationResult result = operation.execute(mUploadClient);
        if (!result.isSuccess() && result.getCode() == ResultCode.FILE_NOT_FOUND &&
                mCurrentUpload.isRemoteFolderToBeCreated()) {
            SyncOperation syncOp = new CreateFolderOperation( pathToGrant, true);
            result = syncOp.execute(mUploadClient, mStorageManager);
        }
        if (result.isSuccess()) {
            OCFile parentDir = mStorageManager.getFileByPath(pathToGrant);
            if (parentDir == null) {
                parentDir = createLocalFolder(pathToGrant);
            }
            if (parentDir != null) {
                result = new RemoteOperationResult(ResultCode.OK);
            } else {
                result = new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
            }
        }
        return result;
    }

    // TODO: Replaced by grantFolderExistence(String pathToGrant
//    /**
//     * Checks the existence of the folder where the current file will be
//     * uploaded both in the remote server and in the local database.
//     *
//     * If the upload is set to enforce the creation of the folder, the method
//     * tries to create it both remote and locally.
//     *
//     * @param pathToGrant Full remote path whose existence will be granted.
//     * @return An {@link OCFile} instance corresponding to the folder where the
//     *         file will be uploaded.
//     */
//    private RemoteOperationResult grantFolderExistence(UploadFileOperation currentUpload, String pathToGrant) {
//        RemoteOperation operation = new ExistenceCheckRemoteOperation(pathToGrant, this, false);
//        RemoteOperationResult result = operation.execute(mUploadClient);
//        if (!result.isSuccess() && result.getCode() == ResultCode.FILE_NOT_FOUND
//                && currentUpload.isRemoteFolderToBeCreated()) {
//            SyncOperation syncOp = new CreateFolderOperation(pathToGrant, true);
//            result = syncOp.execute(mUploadClient, mStorageManager);
//        }
//        if (result.isSuccess()) {
//            OCFile parentDir = mStorageManager.getFileByPath(pathToGrant);
//            if (parentDir == null) {
//                parentDir = createLocalFolder(pathToGrant);
//            }
//            if (parentDir != null) {
//                result = new RemoteOperationResult(ResultCode.OK);
//            } else {
//                result = new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
//            }
//        }
//        return result;
//    }

    private OCFile createLocalFolder(String remotePath) {
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ?
                parentPath : parentPath + OCFile.PATH_SEPARATOR;
        OCFile parent = mStorageManager.getFileByPath(parentPath);
        if (parent == null) {
            parent = createLocalFolder(parentPath);
        }
        if (parent != null) {
            OCFile createdFolder = new OCFile(remotePath);
            createdFolder.setMimetype("DIR");
            createdFolder.setParentId(parent.getFileId());
            mStorageManager.saveFile(createdFolder);
            return createdFolder;
        }
        return null;
    }


    /**
     * Saves a OC File after a successful upload.
     *
     * A PROPFIND is necessary to keep the props in the local database
     * synchronized with the server, specially the modification time and Etag
     * (where available)
     *
     * TODO move into UploadFileOperation
     */
    private void saveUploadedFile() {
        OCFile file = mCurrentUpload.getFile();
        if (file.fileExists()) {
            file = mStorageManager.getFileById(file.getFileId());
        }
        long syncDate = System.currentTimeMillis();
        file.setLastSyncDateForData(syncDate);

        // new PROPFIND to keep data consistent with server
        // in theory, should return the same we already have
        ReadRemoteFileOperation operation =
                new ReadRemoteFileOperation(mCurrentUpload.getRemotePath());
        RemoteOperationResult result = operation.execute(mUploadClient);
        if (result.isSuccess()) {
            updateOCFile(file, (RemoteFile) result.getData().get(0));
            file.setLastSyncDateForProperties(syncDate);
        } else {
            Log_OC.e(TAG, "Error reading properties of file after successful upload; this is gonna hurt...");
        }

        // / maybe this would be better as part of UploadFileOperation... or
        // maybe all this method
        if (mCurrentUpload.wasRenamed()) {
            OCFile oldFile = mCurrentUpload.getOldFile();
            if (oldFile.fileExists()) {
                oldFile.setStoragePath(null);
                mStorageManager.saveFile(oldFile);
                mStorageManager.saveConflict(oldFile, null);

            } // else: it was just an automatic renaming due to a name
            // coincidence; nothing else is needed, the storagePath is right
            // in the instance returned by mCurrentUpload.getFile()
        }
        file.setNeedsUpdateThumbnail(true);
        mStorageManager.saveFile(file);
        mStorageManager.saveConflict(file, null);

        mStorageManager.triggerMediaScan(file.getStoragePath());

    }

    private void updateOCFile(OCFile file, RemoteFile remoteFile) {
        file.setCreationTimestamp(remoteFile.getCreationTimestamp());
        file.setFileLength(remoteFile.getLength());
        file.setMimetype(remoteFile.getMimeType());
        file.setModificationTimestamp(remoteFile.getModifiedTimestamp());
        file.setModificationTimestampAtLastSyncForData(remoteFile.getModifiedTimestamp());
        file.setEtag(remoteFile.getEtag());
        file.setRemoteId(remoteFile.getRemoteId());
    }

    private OCFile obtainNewOCFileToUpload(String remotePath, String localPath, String mimeType) {

        // MIME type
        if (mimeType == null || mimeType.length() <= 0) {
            try {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        remotePath.substring(remotePath.lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log_OC.e(TAG, "Trying to find out MIME type of a file without extension: " + remotePath);
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        if (isPdfFileFromContentProviderWithoutExtension(localPath, mimeType)){
            remotePath += FILE_EXTENSION_PDF;
        }

        OCFile newFile = new OCFile(remotePath);
        newFile.setStoragePath(localPath);
        newFile.setLastSyncDateForProperties(0);
        newFile.setLastSyncDateForData(0);

        // size
        if (localPath != null && localPath.length() > 0) {
            File localFile = new File(localPath);
            newFile.setFileLength(localFile.length());
            newFile.setLastSyncDateForData(localFile.lastModified());
        } // don't worry about not assigning size, the problems with localPath
        // are checked when the UploadFileOperation instance is created


        newFile.setMimetype(mimeType);

        return newFile;
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
                        String.format(getString(R.string.uploader_upload_in_progress_content), 0, upload.getFileName()));

        /// includes a pending intent in the notification showing the details
        Intent showUploadListIntent = new Intent(this, UploadListActivity.class);
        showUploadListIntent.putExtra(FileActivity.EXTRA_FILE, upload.getFile());
        showUploadListIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
        showUploadListIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        showUploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                showUploadListIntent, 0));

        // TODO: Review Old behaviour in FileUploader
        /// includes a pending intent in the notification showing the details view of the file
//        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
//        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE,  (Parcelable) upload.getFile());
//        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
//        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(
//                this, (int) System.currentTimeMillis(), showDetailsIntent, 0
//        ));

        mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotificationBuilder.build());

        updateDatabaseUploadStart(mCurrentUpload);
    }

    /**
     * Updates the persistent upload database that upload is in progress.
     */
    private void updateDatabaseUploadStart(UploadFileOperation upload) {
        mUploadsStorageManager.updateUploadStatus(
                upload.getOriginalStoragePath(),
                UploadStatus.UPLOAD_IN_PROGRESS, null
        );
    }

    /**
     * Callback method to update the progress bar in the status notification
     */
    @Override
    public void onTransferProgress(
            long progressRate, long totalTransferredSoFar, long totalToTransfer, String filePath
    ) {
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
     * @param uploadResult  Result of the upload operation.
     * @param upload        Finished upload operation
     */
    private void notifyUploadResult(UploadFileOperation upload,
                                    RemoteOperationResult uploadResult) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.getCode());
        // / cancelled operation or success -> silent removal of progress
        // notification
        mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);

        // Show the result: success or fail notification
        if (!uploadResult.isCancelled()) {
            int tickerId = (uploadResult.isSuccess()) ? R.string.uploader_upload_succeeded_ticker
                    : R.string.uploader_upload_failed_ticker;

            String content; // = null;

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
                        AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN);
                updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND);
                mNotificationBuilder.setContentIntent(PendingIntent.getActivity(
                        this,
                        (int) System.currentTimeMillis(),
                        updateAccountCredentials,
                        PendingIntent.FLAG_ONE_SHOT));

                mUploadClient = null;
                // grant that future retries on the same account will get the fresh credentials

            }else {
                mNotificationBuilder.setContentText(content);
 //Changes for compilation
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

            if(!uploadResult.isSuccess()){
                //in case of failure, do not show details file view (because there is no file!)
                Intent showUploadListIntent = new Intent(this, UploadListActivity.class);
                showUploadListIntent.putExtra(FileActivity.EXTRA_FILE, upload.getFile());
                showUploadListIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
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
                // remove success notification, with a delay of 2 seconds
                NotificationDelayer.cancelWithDelay(mNotificationManager, R.string.uploader_upload_succeeded_ticker,
                        2000);
            }
        }
    }

    /**
     * Updates the persistent upload database with upload result.
     */
    private void updateDatabaseUploadResult(RemoteOperationResult uploadResult, UploadFileOperation upload) {
        // result: success or fail notification
        Log_OC.d(TAG, "updateDataseUploadResult uploadResult: " + uploadResult + " upload: " + upload);
        if (uploadResult.isCancelled()) {
            mUploadsStorageManager.updateUploadStatus(
                    upload.getOriginalStoragePath(),
                    UploadStatus.UPLOAD_CANCELLED,
                    UploadResult.CANCELLED
            );
        } else {

            if (uploadResult.isSuccess()) {
                mUploadsStorageManager.updateUploadStatus(
                        upload.getOriginalStoragePath(),
                        UploadStatus.UPLOAD_SUCCEEDED,
                        UploadResult.UPLOADED
                );
            } else {
                // TODO: Disable for testing of menu actions in uploads view
//                if (shouldRetryFailedUpload(uploadResult)) {
//                    mUploadsStorageManager.updateUploadStatus(
//                        upload.getOriginalStoragePath(), UploadStatus.UPLOAD_FAILED_RETRY, uploadResult
//                    );
//                } else {
//                    mUploadsStorageManager.updateUploadStatus(upload.getOriginalStoragePath(),
//                            UploadsStorageManager.UploadStatus.UPLOAD_FAILED_GIVE_UP, uploadResult);
//                }
            }
        }
    }

    // TODO: Disable for testing of menu actions in uploads view
//
//    /**
//     * Determines whether with given uploadResult the upload should be retried later.
//     * @param uploadResult
//     * @return true if upload should be retried later, false if is should be abandoned.
//     */
//    private boolean shouldRetryFailedUpload(RemoteOperationResult uploadResult) {
//        if (uploadResult.isSuccess()) {
//            return false;
//        }
//        switch (uploadResult.getCode()) {
//        case HOST_NOT_AVAILABLE:
//        case NO_NETWORK_CONNECTION:
//        case TIMEOUT:
//        case WRONG_CONNECTION: // SocketException
//            return true;
//        default:
//            return false;
//        }
//    }

    /**
     * Sends a broadcast in order to the interested activities can update their
     * view
     *
     * @param upload                    Finished upload operation
     * @param uploadResult              Result of the upload operation
     * @param unlinkedFromRemotePath    Path in the uploads tree where the upload was unlinked from
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

    // TODO: Remove. Replace by sendBroadcastUploadFinished(UploadFileOperation, RemoteOperationResult, String)
//    /**
//     * Sends a broadcast in order to the interested activities can update their
//     * view
//     * @param uploadResult Result of the upload operation
//     * @param upload Finished upload operation
//     */
//    private void sendFinalBroadcast(RemoteOperationResult uploadResult, UploadFileOperation upload) {
//        Intent end = new Intent(getUploadFinishMessage());
//        end.putExtra(EXTRA_REMOTE_PATH, upload.getRemotePath()); // real remote
//        // path, after
//        // possible
//        // automatic
//        // renaming
//        if (upload.wasRenamed()) {
//            end.putExtra(EXTRA_OLD_REMOTE_PATH, upload.getOldFile().getRemotePath());
//        }
//        end.putExtra(EXTRA_OLD_FILE_PATH, upload.getOriginalStoragePath());
//        end.putExtra(ACCOUNT_NAME, upload.getAccount().name);
//        end.putExtra(EXTRA_UPLOAD_RESULT, uploadResult.isSuccess());
//        sendStickyBroadcast(end);
//    }

    /**
     * Checks if content provider, using the content:// scheme, returns a file with mime-type
     * 'application/pdf' but file has not extension
     * @param localPath         Full path to a file in the local file system.
     * @param mimeType          MIME type of the file.
     * @return true if is needed to add the pdf file extension to the file
     */
    private boolean isPdfFileFromContentProviderWithoutExtension(String localPath,
                                                                 String mimeType) {
        return localPath.startsWith(UriUtils.URI_CONTENT_SCHEME) &&
                mimeType.equals(MIME_TYPE_PDF) &&
                !localPath.endsWith(FILE_EXTENSION_PDF);
    }

    /**
     * Remove uploads of an account
     *
     * @param account       Downloads account to remove
     */
    private void cancelUploadsForAccount(Account account){
        // Cancel pending uploads
        mPendingUploads.remove(account);
    }

//    TODO: Remove
//    /**
//     * Remove uploads of an account
//     * @param accountName       Name of an OC account
//     */
//    private void cancelUploadForAccount(String accountName){
//        // this can be slow if there are many uploads :(
//        Iterator<String> it = mPendingUploads.keySet().iterator();
//        Log_OC.d(TAG, "Number of pending updloads= "  + mPendingUploads.size());
//        while (it.hasNext()) {
//            String key = it.next();
//            Log_OC.d(TAG, "mPendingUploads CANCELLED " + key);
//            if (key.startsWith(accountName)) {
//                synchronized (mPendingUploads) {
//                    mPendingUploads.remove(key);
//                }
//            }
//        }
//    }


    // TODO: onHandleIntent not used in the new FileUploadService
    // TODO: Remove when clean code
    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns,
     * IntentService stops the service, as appropriate.
     *
     * Note: We use an IntentService here. It does not provide simultaneous
     * requests, but instead internally queues them and gets them to this
     * onHandleIntent method one after another. This makes implementation less
     * error-prone but prevents files to be added to list while another upload
     * is active. If everything else works fine, fixing this should be a TODO.
     *
     * Entry point to add one or several files to the queue of uploads.
     *
     * New uploads are added calling to startService(), resulting in a call to
     * this method. This ensures the service will keep on working although the
     * caller activity goes away.
     *
     * First, onHandleIntent() stores all information associated with the upload
     * in a {@link OCUpload} which is stored persistently using
     * {@link UploadsStorageManager}. Then, the oldest, pending upload from
     * {@link UploadsStorageManager} is taken and upload is started.
     * @param intentStartId
     */
//
//    protected void onHandleIntent(Intent intent, int intentStartId) {
//        Log_OC.d(TAG, "onHandleIntent start");
//        //Log_OC.d(TAG, "mPendingUploads size:" + mPendingUploads.size() + " - before adding new uploads.");
//        Log_OC.d(TAG, "mPendingUploads size:" + mPendingUploads.toString() + " - before adding new uploads.");
//        if (intent == null || intent.hasExtra(KEY_RETRY)) {
//            Log_OC.d(TAG, "Received null intent.");
//            // service was restarted by OS or connectivity change was detected or
//            // retry of pending upload was requested.
//            // ==> First check persistent uploads, then perform upload.
//            int countAddedEntries = 0;
//            OCUpload[] list = mUploadsStorageManager.getPendingUploads();
//            for (OCUpload OCUpload : list) {
//                Log_OC.d(TAG, "Retrieved from DB: " + OCUpload.toFormattedString());
//
//                String uploadKey = buildRemoteName(OCUpload);
//                OCUpload previous = mPendingUploads.putIfAbsent(uploadKey, OCUpload);
//                if(previous == null) {
//                    Log_OC.d(TAG, "mPendingUploads added: " + OCUpload.toFormattedString());
//                    countAddedEntries++;
//                } else {
//                    //already pending. ignore.
//                }
//            }
//            Log_OC.d(TAG, "added " + countAddedEntries
//                    + " entrie(s) to mPendingUploads (this should be 0 except for the first time).");
//            // null intent is received when charging or wifi state changes.
//            // fake a mUploadsStorageManager change event, so that GUI can update the reason for
//            // LATER status of uploads.
//            mUploadsStorageManager.notifyObserversNow();
//        } else {
//            Log_OC.d(TAG, "Receive upload intent.");
//            UploadQuantity uploadType = (UploadQuantity) intent.getSerializableExtra(KEY_UPLOAD_TYPE);
//            if (uploadType == null) {
//                Log_OC.e(TAG, "Incorrect or no upload type provided");
//                return;
//            }
//
//            Account account = intent.getParcelableExtra(KEY_ACCOUNT);
//            if (!AccountUtils.exists(account, getApplicationContext())) {
//                Log_OC.e(TAG, "KEY_ACCOUNT no set or provided KEY_ACCOUNT does not exist");
//                return;
//            }
//            mStorageManager = new FileDataStorageManager(account, getContentResolver());
//
//            OCFile[] files = null;
//            // if KEY_FILE given, use it
//            if (intent.hasExtra(KEY_FILE)) {
//                if (uploadType == UploadQuantity.UPLOAD_SINGLE_FILE) {
//                    files = new OCFile[] { intent.getParcelableExtra(KEY_FILE) };
//                } else {
//                    files = (OCFile[]) intent.getParcelableArrayExtra(KEY_FILE);
//                }
//
//            } else { // else use KEY_LOCAL_FILE and KEY_REMOTE_FILE
//
//                if (!intent.hasExtra(KEY_LOCAL_FILE) || !intent.hasExtra(KEY_REMOTE_FILE)) {
//                    Log_OC.e(TAG, "Not enough information provided in intent");
//                    return;
//                }
//
//                String[] localPaths;
//                String[] remotePaths;
//                String[] mimeTypes;
//                if (uploadType == UploadQuantity.UPLOAD_SINGLE_FILE) {
//                    localPaths = new String[] { intent.getStringExtra(KEY_LOCAL_FILE) };
//                    remotePaths = new String[] { intent.getStringExtra(KEY_REMOTE_FILE) };
//                    mimeTypes = new String[] { intent.getStringExtra(KEY_MIME_TYPE) };
//                } else {
//                    localPaths = intent.getStringArrayExtra(KEY_LOCAL_FILE);
//                    remotePaths = intent.getStringArrayExtra(KEY_REMOTE_FILE);
//                    mimeTypes = intent.getStringArrayExtra(KEY_MIME_TYPE);
//                }
//                if (localPaths.length != remotePaths.length) {
//                    Log_OC.e(TAG, "Different number of remote paths and local paths!");
//                    return;
//                }
//
//                files = new OCFile[localPaths.length];
//
//                for (int i = 0; i < localPaths.length; i++) {
//                    files[i] = obtainNewOCFileToUpload(remotePaths[i], localPaths[i],
//                            ((mimeTypes != null) ? mimeTypes[i] : (String) null));
//                    if (files[i] == null) {
//                        Log_OC.e(TAG, "obtainNewOCFileToUpload() returned null for remotePaths[i]:" + remotePaths[i]
//                                + " and localPaths[i]:" + localPaths[i]);
//                        return;
//                    }
//                    mStorageManager.saveFile(files[i]);
//                    files[i] = mStorageManager.getFileByLocalPath(files[i].getStoragePath());
//                }
//            }
//
//            // at this point variable "OCFile[] files" is loaded correctly.
//
//            boolean forceOverwrite = intent.getBooleanExtra(KEY_FORCE_OVERWRITE, false);
//            boolean isCreateRemoteFolder = intent.getBooleanExtra(KEY_CREATE_REMOTE_FOLDER, false);
//            boolean isUseWifiOnly = intent.getBooleanExtra(KEY_WIFI_ONLY, true);
//            boolean isWhileChargingOnly = intent.getBooleanExtra(KEY_WHILE_CHARGING_ONLY, false);
//            long uploadTimestamp = intent.getLongExtra(KEY_UPLOAD_TIMESTAMP, -1);
//
//            LocalBehaviour localAction = (LocalBehaviour) intent.getSerializableExtra(KEY_LOCAL_BEHAVIOUR);
//            if (localAction == null)
//                localAction = LocalBehaviour.LOCAL_BEHAVIOUR_COPY;
//
//            // save always persistently path of upload, so it can be retried if
//            // failed.
//            for (int i = 0; i < files.length; i++) {
//                OCUpload upload = new OCUpload(files[i]);
//                upload.setAccountName(account.name);
//                upload.setForceOverwrite(forceOverwrite);
//                upload.setCreateRemoteFolder(isCreateRemoteFolder);
//                upload.setLocalAction(localAction);
//                upload.setUseWifiOnly(isUseWifiOnly);
//                upload.setWhileChargingOnly(isWhileChargingOnly);
//                upload.setUploadTimestamp(uploadTimestamp);
//                upload.setUploadStatus(UploadStatus.UPLOAD_LATER);
//
//                String uploadKey = buildRemoteName(upload);
//                OCUpload previous = mPendingUploads.putIfAbsent(uploadKey, upload);
//
//                if(previous == null)
//                {
//                    Log_OC.d(TAG, "mPendingUploads added: " + upload.toFormattedString());
//
//                    // if upload was not queued before, we can add it to
//                    // database because the user wants the file to be uploaded.
//                    // however, it can happened that the user uploaded the same
//                    // file before in which case there is an old db entry.
//                    // delete that to be sure we have the latest one.
//                    if(mUploadsStorageManager.removeUpload(upload.getLocalPath())>0) {
//                        Log_OC.w(TAG, "There was an old DB entry " + upload.getLocalPath()
//                                + " which had to be removed in order to add new one.");
//                    }
//                    boolean success = mUploadsStorageManager.storeUpload(upload);
//                    if(!success) {
//                        Log_OC.e(TAG, "Could not add upload " + upload.getLocalPath()
//                                + " to database. This should not happen.");
//                    }
//                } else {
//                    Log_OC.w(TAG, "FileUploadService got upload intent for file which is already queued: "
//                            + upload.getRemotePath());
//                    // upload already pending. ignore.
//                }
//            }
//        }
//        // at this point mPendingUploads is filled.
//
//
//
//        Iterator<String> it;
//        if (intent != null && intent.getStringExtra(KEY_RETRY_REMOTE_PATH) != null) {
//            ArrayList<String> list = new ArrayList<String>(1);
//            String remotePath = intent.getStringExtra(KEY_RETRY_REMOTE_PATH);
//            list.add(remotePath);
//            it = list.iterator();
//            // update db status for upload
//            OCUpload OCUpload = mPendingUploads.get(remotePath);
//            OCUpload.setUploadStatus(UploadStatus.UPLOAD_LATER);
//            OCUpload.setLastResult(null);
//            mUploadsStorageManager.updateUploadStatus(OCUpload);
//
//            Log_OC.d(TAG, "Start uploading " + remotePath);
//        } else {
//            it = mPendingUploads.keySet().iterator();
//        }
//        if (it.hasNext()) {
//            while (it.hasNext()) {
//                String upload = it.next();
//                OCUpload OCUpload = mPendingUploads.get(upload);
//
//                if (OCUpload == null) {
//                    Log_OC.e(TAG, "Cannot upload null. Fix that!");
//                    continue;
//                }
//
//                UploadTask uploadTask = new UploadTask(OCUpload);
//                mUploadExecutor.submit(uploadTask);
//            }
//            StopSelfTask stopSelfTask = new StopSelfTask(intentStartId);
//            mUploadExecutor.submit(stopSelfTask);
//
//        } else {
//            stopSelf(intentStartId);
//        }
//
//        Log_OC.d(TAG, "onHandleIntent end");
//    }

    // TODO: StopSelfTask not used in the new FileUploadService
    // TODO: Remove when clean code
    /**
     * Stops this services if latest intent id is intentStartId.
     */
//    public class StopSelfTask implements Runnable {
//        int intentStartId;
//
//        public StopSelfTask(int intentStartId) {
//            this.intentStartId = intentStartId;
//        }
//
//        @Override
//        public void run() {
//            stopSelf(intentStartId);
//        }
//    }

    // TODO: UploaTask not used in the new FileUploadService
    // TODO: Remove when clean code
    /**
     * Tries uploading OCUpload, creates notifications, and updates mUploadsStorageManager.
     */
//    public class UploadTask implements Runnable {
//        OCUpload OCUpload;
//
//        public UploadTask(OCUpload OCUpload) {
//            this.OCUpload = OCUpload;
//        }
//
//        @Override
//        public void run() {
//            Log_OC.d(TAG, "mPendingUploads size:" + mPendingUploads.size() + " - before uploading.");
//            switch (canUploadFileNow(OCUpload)) {
//            case NOW:
//                Log_OC.d(TAG, "Calling uploadFile for " + OCUpload.getRemotePath());
//                RemoteOperationResult uploadResult = uploadFile(OCUpload);
//                //TODO store renamed upload path?
//                updateDatabaseUploadResult(uploadResult, mCurrentUpload);
//                notifyUploadResult(uploadResult, mCurrentUpload);
//                sendFinalBroadcast(uploadResult, mCurrentUpload);
//                // TODO: Disable for testing of menu actions in uploads view
////                if (!shouldRetryFailedUpload(uploadResult)) {
////                    Log_OC.d(TAG, "Upload with result " + uploadResult.getCode() + ": " + uploadResult.getLogMessage()
////                            + " will be abandoned.");
////                    mPendingUploads.remove(buildRemoteName(OCUpload));
////                }
//                Log_OC.d(TAG, "mCurrentUpload = null");
//                mCurrentUpload = null;
//                break;
//            case LATER:
//                // Schedule retry for later upload. Delay can be due to:
//                // KEY_WIFI_ONLY - done by ConnectivityActionReceiver
//                // KEY_WHILE_CHARGING_ONLY - TODO add PowerConnectionReceiver
//                // KEY_UPLOAD_TIMESTAMP - TODO use AlarmManager to wake up this service
//                break;
//            case FILE_GONE:
//                mUploadsStorageManager.updateUploadStatus(
//                        OCUpload.getLocalPath(),
//                        UploadStatus.UPLOAD_FAILED_GIVE_UP,
//                        com.owncloud.android.db.UploadResult.FILE_ERROR
//                );
//                if (mPendingUploads.remove(OCUpload.getRemotePath()) == null) {
//                    Log_OC.w(TAG, "Could remove " + OCUpload.getRemotePath()
//                            + " from mPendingUploads because it does not exist.");
//                }
//
//                break;
//            case ERROR:
//                Log_OC.e(TAG, "canUploadFileNow() returned ERROR. Fix that!");
//                break;
//            }
//            Log_OC.d(TAG, "mPendingUploads size:" + mPendingUploads.size() + " - after uploading.");
//        }
//
//    }

    /**
     * Returns the reason as String why state of OCUpload is LATER. If
     * upload state != LATER return null.
     */
    static public String getUploadLaterReason(Context context, OCUpload OCUpload) {
        StringBuilder reason = new StringBuilder();
        Date now = new Date();
        if (now.getTime() < OCUpload.getUploadTimestamp()) {
            reason.append("Waiting for " + DisplayUtils.unixTimeToHumanReadable(OCUpload.getUploadTimestamp()));
        }
        if (OCUpload.isUseWifiOnly() && !UploadUtils.isConnectedViaWiFi(context)) {
            if (reason.length() > 0) {
                reason.append(" and wifi connectivity");
            } else {
                reason.append("Waiting for wifi connectivity");
            }
        }
        if (OCUpload.isWhileChargingOnly() && !UploadUtils.isCharging(context)) {
            if (reason.length() > 0) {
                reason.append(" and charging");
            } else {
                reason.append("Waiting for charging");
            }
        }
        reason.append(".");
        if (reason.length() > 1) {
            return reason.toString();
        }
        if (OCUpload.getUploadStatus() == UploadStatus.UPLOAD_LATER) {
            return "Upload is pending and will start shortly.";
        }
        return null;
    }
    
    enum CanUploadFileNowStatus {NOW, LATER, FILE_GONE, ERROR};
    
    /**
     * Returns true when the file may be uploaded now. This methods checks all
     * restraints of the passed {@link OCUpload}, these include
     * isUseWifiOnly(), check if local file exists, check if file was already
     * uploaded...
     * 
     * If return value is CanUploadFileNowStatus.NOW, uploadFile() may be
     * called.
     * 
     * @return CanUploadFileNowStatus.NOW is upload may proceed, <br>
     *         CanUploadFileNowStatus.LATER if upload should be performed at a
     *         later time, <br>
     *         CanUploadFileNowStatus.ERROR if a severe error happened, calling
     *         entity should remove upload from queue.
     * 
     */
    private CanUploadFileNowStatus canUploadFileNow(OCUpload OCUpload) {

        if (OCUpload.getUploadStatus() == UploadStatus.UPLOAD_SUCCEEDED) {
            Log_OC.w(TAG, "Already succeeded uploadObject was again scheduled for upload. Fix that!");
            return CanUploadFileNowStatus.ERROR;
        }

        if (OCUpload.isUseWifiOnly()
                && !UploadUtils.isConnectedViaWiFi(getApplicationContext())) {
            Log_OC.d(TAG, "Do not start upload because it is wifi-only.");
            return CanUploadFileNowStatus.LATER;
        }
        
        if(OCUpload.isWhileChargingOnly() && !UploadUtils.isCharging(getApplicationContext())) {
            Log_OC.d(TAG, "Do not start upload because it is while charging only.");
            return CanUploadFileNowStatus.LATER;
        }
        Date now = new Date();
        if (now.getTime() < OCUpload.getUploadTimestamp()) {
            Log_OC.d(
                    TAG,
                    "Do not start upload because it is schedule for "
                            + DisplayUtils.unixTimeToHumanReadable(OCUpload.getUploadTimestamp()));
            return CanUploadFileNowStatus.LATER;
        }
        

        if (!new File(OCUpload.getLocalPath()).exists()) {
            Log_OC.d(TAG, "Do not start upload because local file does not exist.");
            return CanUploadFileNowStatus.FILE_GONE;
        }
        return CanUploadFileNowStatus.NOW;        
    }



    /**
     * Call if all pending uploads are to be retried.
     */
    public static void retry(Context context) {
        retry(context, null);    
    }

    /**
     * Call to retry upload identified by remotePath
     */
    private static void retry(Context context, String remotePath) {
        Log_OC.d(TAG, "FileUploadService.retry()");
        Intent i = new Intent(context, FileUploadService.class);
        i.putExtra(FileUploadService.KEY_RETRY, true);
        if(remotePath != null) {
            i.putExtra(FileUploadService.KEY_RETRY_REMOTE_PATH, remotePath);
        }
        context.startService(i);        
    }

}
