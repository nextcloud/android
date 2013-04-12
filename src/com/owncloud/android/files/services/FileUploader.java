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

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.webkit.MimeTypeMap;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.DbHandler;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.ChunkedUploadFileOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.FailedUploadActivity;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.activity.InstantUploadActivity;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.OwnCloudVersion;

import eu.alefzero.webdav.OnDatatransferProgressListener;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavEntry;
import eu.alefzero.webdav.WebdavUtils;

public class FileUploader extends Service implements OnDatatransferProgressListener {

    public static final String UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH";
    public static final String EXTRA_UPLOAD_RESULT = "RESULT";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_OLD_REMOTE_PATH = "OLD_REMOTE_PATH";
    public static final String EXTRA_OLD_FILE_PATH = "OLD_FILE_PATH";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";

    public static final String KEY_FILE = "FILE";
    public static final String KEY_LOCAL_FILE = "LOCAL_FILE";
    public static final String KEY_REMOTE_FILE = "REMOTE_FILE";
    public static final String KEY_MIME_TYPE = "MIME_TYPE";

    public static final String KEY_ACCOUNT = "ACCOUNT";

    public static final String KEY_UPLOAD_TYPE = "UPLOAD_TYPE";
    public static final String KEY_FORCE_OVERWRITE = "KEY_FORCE_OVERWRITE";
    public static final String KEY_INSTANT_UPLOAD = "INSTANT_UPLOAD";
    public static final String KEY_LOCAL_BEHAVIOUR = "BEHAVIOUR";

    public static final int LOCAL_BEHAVIOUR_COPY = 0;
    public static final int LOCAL_BEHAVIOUR_MOVE = 1;
    public static final int LOCAL_BEHAVIOUR_FORGET = 2;

    public static final int UPLOAD_SINGLE_FILE = 0;
    public static final int UPLOAD_MULTIPLE_FILES = 1;

    private static final String TAG = FileUploader.class.getSimpleName();

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private WebdavClient mUploadClient = null;
    private Account mLastAccount = null;
    private FileDataStorageManager mStorageManager;

    private ConcurrentMap<String, UploadFileOperation> mPendingUploads = new ConcurrentHashMap<String, UploadFileOperation>();
    private UploadFileOperation mCurrentUpload = null;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private int mLastPercent;
    private RemoteViews mDefaultNotificationContentView;

    /**
     * Builds a key for mPendingUploads from the account and file to upload
     * 
     * @param account   Account where the file to upload is stored
     * @param file      File to upload
     */
    private String buildRemoteName(Account account, OCFile file) {
        return account.name + file.getRemotePath();
    }

    private String buildRemoteName(Account account, String remotePath) {
        return account.name + remotePath;
    }

    /**
     * Checks if an ownCloud server version should support chunked uploads.
     * 
     * @param version OwnCloud version instance corresponding to an ownCloud
     *            server.
     * @return 'True' if the ownCloud server with version supports chunked
     *         uploads.
     */
    private static boolean chunkedUploadIsSupported(OwnCloudVersion version) {
        return (version != null && version.compareTo(OwnCloudVersion.owncloud_v4_5) >= 0);
    }

    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "mPendingUploads size:" + mPendingUploads.size());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileUploaderThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileUploaderBinder();
    }

    /**
     * Entry point to add one or several files to the queue of uploads.
     * 
     * New uploads are added calling to startService(), resulting in a call to
     * this method. This ensures the service will keep on working although the
     * caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(KEY_ACCOUNT) || !intent.hasExtra(KEY_UPLOAD_TYPE)
                || !(intent.hasExtra(KEY_LOCAL_FILE) || intent.hasExtra(KEY_FILE))) {
            Log.e(TAG, "Not enough information provided in intent");
            return Service.START_NOT_STICKY;
        }
        int uploadType = intent.getIntExtra(KEY_UPLOAD_TYPE, -1);
        if (uploadType == -1) {
            Log.e(TAG, "Incorrect upload type provided");
            return Service.START_NOT_STICKY;
        }
        Account account = intent.getParcelableExtra(KEY_ACCOUNT);

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

        FileDataStorageManager storageManager = new FileDataStorageManager(account, getContentResolver());

        boolean forceOverwrite = intent.getBooleanExtra(KEY_FORCE_OVERWRITE, false);
        boolean isInstant = intent.getBooleanExtra(KEY_INSTANT_UPLOAD, false);
        int localAction = intent.getIntExtra(KEY_LOCAL_BEHAVIOUR, LOCAL_BEHAVIOUR_COPY);
        boolean fixed = false;
        if (isInstant) {
            fixed = checkAndFixInstantUploadDirectory(storageManager); // MUST
                                                                       // be
                                                                       // done
                                                                       // BEFORE
                                                                       // calling
                                                                       // obtainNewOCFileToUpload
        }

        if (intent.hasExtra(KEY_FILE) && files == null) {
            Log.e(TAG, "Incorrect array for OCFiles provided in upload intent");
            return Service.START_NOT_STICKY;

        } else if (!intent.hasExtra(KEY_FILE)) {
            if (localPaths == null) {
                Log.e(TAG, "Incorrect array for local paths provided in upload intent");
                return Service.START_NOT_STICKY;
            }
            if (remotePaths == null) {
                Log.e(TAG, "Incorrect array for remote paths provided in upload intent");
                return Service.START_NOT_STICKY;
            }
            if (localPaths.length != remotePaths.length) {
                Log.e(TAG, "Different number of remote paths and local paths!");
                return Service.START_NOT_STICKY;
            }

            files = new OCFile[localPaths.length];
            for (int i = 0; i < localPaths.length; i++) {
                files[i] = obtainNewOCFileToUpload(remotePaths[i], localPaths[i], ((mimeTypes != null) ? mimeTypes[i]
                        : (String) null), storageManager);
                if (files[i] == null) {
                    // TODO @andromaex add failure Notiification
                    return Service.START_NOT_STICKY;
                }
            }
        }

        OwnCloudVersion ocv = new OwnCloudVersion(AccountManager.get(this).getUserData(account,
                AccountAuthenticator.KEY_OC_VERSION));
        boolean chunked = FileUploader.chunkedUploadIsSupported(ocv);
        AbstractList<String> requestedUploads = new Vector<String>();
        String uploadKey = null;
        UploadFileOperation newUpload = null;
        try {
            for (int i = 0; i < files.length; i++) {
                uploadKey = buildRemoteName(account, files[i].getRemotePath());
                if (chunked) {
                    newUpload = new ChunkedUploadFileOperation(account, files[i], isInstant, forceOverwrite,
                            localAction);
                } else {
                    newUpload = new UploadFileOperation(account, files[i], isInstant, forceOverwrite, localAction);
                }
                if (fixed && i == 0) {
                    newUpload.setRemoteFolderToBeCreated();
                }
                mPendingUploads.putIfAbsent(uploadKey, newUpload);
                newUpload.addDatatransferProgressListener(this);
                newUpload.addDatatransferProgressListener((FileUploaderBinder)mBinder);
                requestedUploads.add(uploadKey);
            }

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Not enough information provided in intent: " + e.getMessage());
            return START_NOT_STICKY;

        } catch (IllegalStateException e) {
            Log.e(TAG, "Bad information provided in intent: " + e.getMessage());
            return START_NOT_STICKY;

        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception while processing upload intent", e);
            return START_NOT_STICKY;

        }

        if (requestedUploads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedUploads;
            mServiceHandler.sendMessage(msg);
        }
        Log.i(TAG, "mPendingUploads size:" + mPendingUploads.size());
        return Service.START_NOT_STICKY;
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
        ((FileUploaderBinder)mBinder).clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }
    

    /**
     * Binder to let client components to perform operations on the queue of
     * uploads.
     * 
     * It provides by itself the available operations.
     */
    public class FileUploaderBinder extends Binder implements OnDatatransferProgressListener {
        
        /** 
         * Map of listeners that will be reported about progress of uploads from a {@link FileUploaderBinder} instance 
         */
        private Map<String, OnDatatransferProgressListener> mBoundListeners = new HashMap<String, OnDatatransferProgressListener>();
        
        /**
         * Cancels a pending or current upload of a remote file.
         * 
         * @param account Owncloud account where the remote file will be stored.
         * @param file A file in the queue of pending uploads
         */
        public void cancel(Account account, OCFile file) {
            UploadFileOperation upload = null;
            synchronized (mPendingUploads) {
                upload = mPendingUploads.remove(buildRemoteName(account, file));
            }
            if (upload != null) {
                upload.cancel();
            }
        }
        
        
        
        public void clearListeners() {
            mBoundListeners.clear();
        }


        
        
        /**
         * Returns True when the file described by 'file' is being uploaded to
         * the ownCloud account 'account' or waiting for it
         * 
         * If 'file' is a directory, returns 'true' if some of its descendant files is uploading or waiting to upload. 
         * 
         * @param account Owncloud account where the remote file will be stored.
         * @param file A file that could be in the queue of pending uploads
         */
        public boolean isUploading(Account account, OCFile file) {
            if (account == null || file == null)
                return false;
            String targetKey = buildRemoteName(account, file);
            synchronized (mPendingUploads) {
                if (file.isDirectory()) {
                    // this can be slow if there are many uploads :(
                    Iterator<String> it = mPendingUploads.keySet().iterator();
                    boolean found = false;
                    while (it.hasNext() && !found) {
                        found = it.next().startsWith(targetKey);
                    }
                    return found;
                } else {
                    return (mPendingUploads.containsKey(targetKey));
                }
            }
        }


        /**
         * Adds a listener interested in the progress of the upload for a concrete file.
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
         * Removes a listener interested in the progress of the upload for a concrete file.
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
            String key = buildRemoteName(mCurrentUpload.getAccount(), mCurrentUpload.getFile());
            OnDatatransferProgressListener boundListener = mBoundListeners.get(key);
            if (boundListener != null) {
                boundListener.onTransferProgress(progressRate, totalTransferredSoFar, totalToTransfer, fileName);
            }
        }
        
    }

    /**
     * Upload worker. Performs the pending uploads in the order they were
     * requested.
     * 
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
            mService.stopSelf(msg.arg1);
        }
    }

    /**
     * Core upload method: sends the file(s) to upload
     * 
     * @param uploadKey Key to access the upload to perform, contained in
     *            mPendingUploads
     */
    public void uploadFile(String uploadKey) {

        synchronized (mPendingUploads) {
            mCurrentUpload = mPendingUploads.get(uploadKey);
        }

        if (mCurrentUpload != null) {

            notifyUploadStart(mCurrentUpload);

            // / prepare client object to send requests to the ownCloud server
            if (mUploadClient == null || !mLastAccount.equals(mCurrentUpload.getAccount())) {
                mLastAccount = mCurrentUpload.getAccount();
                mStorageManager = new FileDataStorageManager(mLastAccount, getContentResolver());
                mUploadClient = OwnCloudClientUtils.createOwnCloudClient(mLastAccount, getApplicationContext());
            }

            // / create remote folder for instant uploads
            if (mCurrentUpload.isRemoteFolderToBeCreated()) {
                mUploadClient.createDirectory(InstantUploadService.INSTANT_UPLOAD_DIR);
                // ignoring result fail could just mean that it already exists,
                // but local database is not synchronized the upload will be
                // tried anyway
            }

            // / perform the upload
            RemoteOperationResult uploadResult = null;
            try {
                uploadResult = mCurrentUpload.execute(mUploadClient);
                if (uploadResult.isSuccess()) {
                    saveUploadedFile();
                }

            } finally {
                synchronized (mPendingUploads) {
                    mPendingUploads.remove(uploadKey);
                    Log.i(TAG, "Remove CurrentUploadItem from pending upload Item Map.");
                }
            }

            // notify result
            notifyUploadResult(uploadResult, mCurrentUpload);
            sendFinalBroadcast(mCurrentUpload, uploadResult);

        }

    }

    /**
     * Saves a OC File after a successful upload.
     * 
     * A PROPFIND is necessary to keep the props in the local database
     * synchronized with the server, specially the modification time and Etag
     * (where available)
     * 
     * TODO refactor this ugly thing
     */
    private void saveUploadedFile() {
        OCFile file = mCurrentUpload.getFile();
        long syncDate = System.currentTimeMillis();
        file.setLastSyncDateForData(syncDate);

        // / new PROPFIND to keep data consistent with server in theory, should
        // return the same we already have
        PropFindMethod propfind = null;
        RemoteOperationResult result = null;
        try {
            propfind = new PropFindMethod(mUploadClient.getBaseUri()
                    + WebdavUtils.encodePath(mCurrentUpload.getRemotePath()));
            int status = mUploadClient.executeMethod(propfind);
            boolean isMultiStatus = (status == HttpStatus.SC_MULTI_STATUS);
            if (isMultiStatus) {
                MultiStatus resp = propfind.getResponseBodyAsMultiStatus();
                WebdavEntry we = new WebdavEntry(resp.getResponses()[0], mUploadClient.getBaseUri().getPath());
                updateOCFile(file, we);
                file.setLastSyncDateForProperties(syncDate);

            } else {
                mUploadClient.exhaustResponse(propfind.getResponseBodyAsStream());
            }

            result = new RemoteOperationResult(isMultiStatus, status);
            Log.i(TAG, "Update: synchronizing properties for uploaded " + mCurrentUpload.getRemotePath() + ": "
                    + result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Update: synchronizing properties for uploaded " + mCurrentUpload.getRemotePath() + ": "
                    + result.getLogMessage(), e);

        } finally {
            if (propfind != null)
                propfind.releaseConnection();
        }

        // / maybe this would be better as part of UploadFileOperation... or
        // maybe all this method
        if (mCurrentUpload.wasRenamed()) {
            OCFile oldFile = mCurrentUpload.getOldFile();
            if (oldFile.fileExists()) {
                oldFile.setStoragePath(null);
                mStorageManager.saveFile(oldFile);

            } // else: it was just an automatic renaming due to a name
              // coincidence; nothing else is needed, the storagePath is right
              // in the instance returned by mCurrentUpload.getFile()
        }

        mStorageManager.saveFile(file);
    }

    private void updateOCFile(OCFile file, WebdavEntry we) {
        file.setCreationTimestamp(we.createTimestamp());
        file.setFileLength(we.contentLength());
        file.setMimetype(we.contentType());
        file.setModificationTimestamp(we.modifiedTimestamp());
        file.setModificationTimestampAtLastSyncForData(we.modifiedTimestamp());
        // file.setEtag(mCurrentUpload.getEtag());    // TODO Etag, where available
    }

    private boolean checkAndFixInstantUploadDirectory(FileDataStorageManager storageManager) {
        OCFile instantUploadDir = storageManager.getFileByPath(InstantUploadService.INSTANT_UPLOAD_DIR);
        if (instantUploadDir == null) {
            // first instant upload in the account, or never account not
            // synchronized after the remote InstantUpload folder was created
            OCFile newDir = new OCFile(InstantUploadService.INSTANT_UPLOAD_DIR);
            newDir.setMimetype("DIR");
            OCFile path = storageManager.getFileByPath(OCFile.PATH_SEPARATOR);

            if (path != null) {
                newDir.setParentId(path.getFileId());
                storageManager.saveFile(newDir);
                return true;
            } else {
                return false;
            }

        }
        return false;
    }

    private OCFile obtainNewOCFileToUpload(String remotePath, String localPath, String mimeType,
            FileDataStorageManager storageManager) {
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

        // MIME type
        if (mimeType == null || mimeType.length() <= 0) {
            try {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        remotePath.substring(remotePath.lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Trying to find out MIME type of a file without extension: " + remotePath);
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        newFile.setMimetype(mimeType);

        // parent dir
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
        OCFile parentDir = storageManager.getFileByPath(parentPath);
        if (parentDir == null) {
            Toast t = Toast
                    .makeText(
                            getApplicationContext(),
                            "The first time the InstantUpload is running you must be online, so the target folder can successfully created by the upload process",
                            30);
            t.show();
            return null;
        }
        long parentDirId = parentDir.getFileId();
        newFile.setParentId(parentDirId);
        return newFile;
    }

    /**
     * Creates a status notification to show the upload progress
     * 
     * @param upload Upload operation starting.
     */
    @SuppressWarnings("deprecation")
    private void notifyUploadStart(UploadFileOperation upload) {
        // / create status notification with a progress bar
        mLastPercent = 0;
        mNotification = new Notification(R.drawable.icon, getString(R.string.uploader_upload_in_progress_ticker),
                System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mDefaultNotificationContentView = mNotification.contentView;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);
        mNotification.contentView.setTextViewText(R.id.status_text,
                String.format(getString(R.string.uploader_upload_in_progress_content), 0, upload.getFileName()));
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        
        /// includes a pending intent in the notification showing the details view of the file
        Intent showDetailsIntent = null;
        if (PreviewImageFragment.canBePreviewed(upload.getFile())) {
            showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        } else {
            showDetailsIntent = new Intent(this, FileDetailActivity.class);
            showDetailsIntent.putExtra(FileDetailActivity.EXTRA_MODE, FileDetailActivity.MODE_DETAILS);
        }
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, upload.getFile());
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, upload.getAccount());
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                (int) System.currentTimeMillis(), showDetailsIntent, 0);

        mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
    }

    /**
     * Callback method to update the progress bar in the status notification
     */
    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName) {
        int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
        if (percent != mLastPercent) {
            mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, false);
            String text = String.format(getString(R.string.uploader_upload_in_progress_content), percent, fileName);
            mNotification.contentView.setTextViewText(R.id.status_text, text);
            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
        }
        mLastPercent = percent;
    }

    /**
     * Callback method to update the progress bar in the status notification
     * (old version)
     */
    @Override
    public void onTransferProgress(long progressRate) {
        // NOTHING TO DO HERE ANYMORE
    }

    /**
     * Updates the status notification with the result of an upload operation.
     * 
     * @param uploadResult Result of the upload operation.
     * @param upload Finished upload operation
     */
    private void notifyUploadResult(RemoteOperationResult uploadResult, UploadFileOperation upload) {
        Log.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.getCode());
        if (uploadResult.isCancelled()) {
            // / cancelled operation -> silent removal of progress notification
            mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);

        } else if (uploadResult.isSuccess()) {
            // / success -> silent update of progress notification to success
            // message
            mNotification.flags ^= Notification.FLAG_ONGOING_EVENT; // remove
                                                                    // the
                                                                    // ongoing
                                                                    // flag
            mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            mNotification.contentView = mDefaultNotificationContentView;
            
            /// includes a pending intent in the notification showing the details view of the file
            Intent showDetailsIntent = null;
            if (PreviewImageFragment.canBePreviewed(upload.getFile())) {
                showDetailsIntent = new Intent(this, PreviewImageActivity.class); 
            } else {
                showDetailsIntent = new Intent(this, FileDetailActivity.class); 
                showDetailsIntent.putExtra(FileDetailActivity.EXTRA_MODE, FileDetailActivity.MODE_DETAILS);
            }
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, upload.getFile());
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, upload.getAccount());
            showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                    (int) System.currentTimeMillis(), showDetailsIntent, 0);

            mNotification.setLatestEventInfo(getApplicationContext(),
                    getString(R.string.uploader_upload_succeeded_ticker),
                    String.format(getString(R.string.uploader_upload_succeeded_content_single), upload.getFileName()),
                    mNotification.contentIntent);

            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification); // NOT
                                                                                                     // AN
            DbHandler db = new DbHandler(this.getBaseContext());
            db.removeIUPendingFile(mCurrentUpload.getFile().getStoragePath());
            db.close();

        } else {

            // / fail -> explicit failure notification
            mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);
            Notification finalNotification = new Notification(R.drawable.icon,
                    getString(R.string.uploader_upload_failed_ticker), System.currentTimeMillis());
            finalNotification.flags |= Notification.FLAG_AUTO_CANCEL;

            String content = null;
            if (uploadResult.getCode() == ResultCode.LOCAL_STORAGE_FULL
                    || uploadResult.getCode() == ResultCode.LOCAL_STORAGE_NOT_COPIED) {
                // TODO we need a class to provide error messages for the users
                // from a RemoteOperationResult and a RemoteOperation
                content = String.format(getString(R.string.error__upload__local_file_not_copied), upload.getFileName(),
                        getString(R.string.app_name));
            } else if (uploadResult.getCode() == ResultCode.QUOTA_EXCEEDED) {
                content = getString(R.string.failed_upload_quota_exceeded_text);
            } else {
                content = String
                        .format(getString(R.string.uploader_upload_failed_content_single), upload.getFileName());
            }

            // we add only for instant-uploads the InstantUploadActivity and the
            // db entry
            Intent detailUploadIntent = null;
            if (upload.isInstant() && InstantUploadActivity.IS_ENABLED) {
                detailUploadIntent = new Intent(this, InstantUploadActivity.class);
                detailUploadIntent.putExtra(FileUploader.KEY_ACCOUNT, upload.getAccount());
            } else {
                detailUploadIntent = new Intent(this, FailedUploadActivity.class);
                detailUploadIntent.putExtra(FailedUploadActivity.MESSAGE, content);
            }
            finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                    (int) System.currentTimeMillis(), detailUploadIntent, PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_ONE_SHOT);

            if (upload.isInstant()) {
                DbHandler db = null;
                try {
                    db = new DbHandler(this.getBaseContext());
                    String message = uploadResult.getLogMessage() + " errorCode: " + uploadResult.getCode();
                    Log.e(TAG, message + " Http-Code: " + uploadResult.getHttpCode());
                    if (uploadResult.getCode() == ResultCode.QUOTA_EXCEEDED) {
                        message = getString(R.string.failed_upload_quota_exceeded_text);
                    }
                    if (db.updateFileState(upload.getOriginalStoragePath(), DbHandler.UPLOAD_STATUS_UPLOAD_FAILED,
                            message) == 0) {
                        db.putFileForLater(upload.getOriginalStoragePath(), upload.getAccount().name, message);
                    }
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }
            finalNotification.setLatestEventInfo(getApplicationContext(),
                    getString(R.string.uploader_upload_failed_ticker), content, finalNotification.contentIntent);

            mNotificationManager.notify(R.string.uploader_upload_failed_ticker, finalNotification);
        }

    }

    /**
     * Sends a broadcast in order to the interested activities can update their
     * view
     * 
     * @param upload Finished upload operation
     * @param uploadResult Result of the upload operation
     */
    private void sendFinalBroadcast(UploadFileOperation upload, RemoteOperationResult uploadResult) {
        Intent end = new Intent(UPLOAD_FINISH_MESSAGE);
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
        sendStickyBroadcast(end);
    }

}
