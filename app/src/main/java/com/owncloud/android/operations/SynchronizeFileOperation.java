/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013-2016 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Bartek Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.content.Context;
import android.text.TextUtils;

import com.nextcloud.client.account.User;
import com.nextcloud.client.jobs.download.FileDownloadHelper;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;

/**
 * Remote operation performing the read of remote file in the ownCloud server.
 */
public class SynchronizeFileOperation extends SyncOperation {

    private static final String TAG = SynchronizeFileOperation.class.getSimpleName();

    private OCFile mLocalFile;
    private String mRemotePath;
    private OCFile mServerFile;
    private User mUser;
    private boolean mSyncFileContents;
    private Context mContext;
    private boolean mTransferWasRequested;

    /**
     * When 'false', uploads to the server are not done; only downloads or conflict detection. This is a temporal
     * field.
     * TODO Remove when 'folder synchronization' replaces 'folder download'.
     */
    private boolean mAllowUploads;


    /**
     * Constructor for "full synchronization mode".
     * <p/>
     * Uses remotePath to retrieve all the data both in local cache and in the remote OC server when the operation is
     * executed, instead of reusing {@link OCFile} instances.
     * <p/>
     * Useful for direct synchronization of a single file.
     *
     * @param remotePath       remote path of the file
     * @param user             Nextcloud user owning the file.
     * @param syncFileContents When 'true', transference of data will be started by the operation if needed and no
     *                         conflict is detected.
     * @param context          Android context; needed to start transfers.
     */
    public SynchronizeFileOperation(
        String remotePath,
        User user,
        boolean syncFileContents,
        Context context,
        FileDataStorageManager storageManager) {
        super(storageManager);

        mRemotePath = remotePath;
        mLocalFile = null;
        mServerFile = null;
        mUser = user;
        mSyncFileContents = syncFileContents;
        mContext = context;
        mAllowUploads = true;
    }


    /**
     * Constructor allowing to reuse {@link OCFile} instances just queried from local cache or from remote OC server.
     * <p>
     * Useful to include this operation as part of the synchronization of a folder (or a full account), avoiding the
     * repetition of fetch operations (both in local database or remote server).
     * <p>
     * At least one of localFile or serverFile MUST NOT BE NULL. If you don't have none of them, use the other
     * constructor.
     *
     * @param localFile        Data of file (just) retrieved from local cache/database.
     * @param serverFile       Data of file (just) retrieved from a remote server. If null, will be retrieved from
     *                         network by the operation when executed.
     * @param user             Nextcloud user owning the file.
     * @param syncFileContents When 'true', transference of data will be started by the operation if needed and no
     *                         conflict is detected.
     * @param context          Android context; needed to start transfers.
     */
    public SynchronizeFileOperation(
        OCFile localFile,
        OCFile serverFile,
        User user,
        boolean syncFileContents,
        Context context,
        FileDataStorageManager storageManager) {
        super(storageManager);

        mLocalFile = localFile;
        mServerFile = serverFile;
        if (mLocalFile != null) {
            mRemotePath = mLocalFile.getRemotePath();
            if (mServerFile != null && !mServerFile.getRemotePath().equals(mRemotePath)) {
                throw new IllegalArgumentException("serverFile and localFile do not correspond" +
                                                       " to the same OC file");
            }
        } else if (mServerFile != null) {
            mRemotePath = mServerFile.getRemotePath();
        } else {
            throw new IllegalArgumentException("Both serverFile and localFile are NULL");
        }
        mUser = user;
        mSyncFileContents = syncFileContents;
        mContext = context;
        mAllowUploads = true;
    }


    /**
     * Temporal constructor.
     * <p>
     * Extends the previous one to allow constrained synchronizations where uploads are never performed - only downloads
     * or conflict detection.
     * <p>
     * Do not use unless you are involved in 'folder synchronization' or 'folder download' work in progress.
     * <p>
     * TODO Remove when 'folder synchronization' replaces 'folder download'.
     *
     * @param localFile        Data of file (just) retrieved from local cache/database. MUSTN't be null.
     * @param serverFile       Data of file (just) retrieved from a remote server. If null, will be retrieved from
     *                         network by the operation when executed.
     * @param user             Nextcloud user owning the file.
     * @param syncFileContents When 'true', transference of data will be started by the operation if needed and no
     *                         conflict is detected.
     * @param allowUploads     When 'false', uploads to the server are not done; only downloads or conflict detection.
     * @param context          Android context; needed to start transfers.
     */
    public SynchronizeFileOperation(
        OCFile localFile,
        OCFile serverFile,
        User user,
        boolean syncFileContents,
        boolean allowUploads,
        Context context,
        FileDataStorageManager storageManager) {

        this(localFile, serverFile, user, syncFileContents, context, storageManager);
        mAllowUploads = allowUploads;
    }


    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        RemoteOperationResult result = null;
        mTransferWasRequested = false;

        if (mLocalFile == null) {
            // Get local file from the DB
            mLocalFile = getStorageManager().getFileByPath(mRemotePath);
        }

        if (!mLocalFile.isDown()) {
            /// easy decision
            requestForDownload(mLocalFile);
            result = new RemoteOperationResult(ResultCode.OK);
        } else {
            /// local copy in the device -> need to think a bit more before do anything
            if (mServerFile == null) {
                ReadFileRemoteOperation operation = new ReadFileRemoteOperation(mRemotePath);
                result = operation.execute(client);

                if (result.isSuccess()) {
                    mServerFile = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));
                    mServerFile.setLastSyncDateForProperties(System.currentTimeMillis());
                } else if (result.getCode() != ResultCode.FILE_NOT_FOUND) {
                    return result;
                }
            }

            if (mServerFile != null) {
                /// check changes in server and local file
                boolean serverChanged;
                if (TextUtils.isEmpty(mLocalFile.getEtag())) {
                    // file uploaded (null) or downloaded ("") before upgrade to version 1.8.0; check the old condition
                    serverChanged = mServerFile.getModificationTimestamp() !=
                        mLocalFile.getModificationTimestampAtLastSyncForData();
                } else {
                    serverChanged = !mServerFile.getEtag().equals(mLocalFile.getEtag());
                }
                boolean localChanged =
                    mLocalFile.getLocalModificationTimestamp() > mLocalFile.getLastSyncDateForData();

                /// decide action to perform depending upon changes
                //if (!mLocalFile.getEtag().isEmpty() && localChanged && serverChanged) {
                if (localChanged && serverChanged) {
                    result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
                    getStorageManager().saveConflict(mLocalFile, mServerFile.getEtag());

                } else if (localChanged) {
                    if (mSyncFileContents && mAllowUploads) {
                        requestForUpload(mLocalFile);
                        // the local update of file properties will be done by the FileUploader
                        // service when the upload finishes
                    } else {
                        // NOTHING TO DO HERE: updating the properties of the file in the server
                        // without uploading the contents would be stupid;
                        // So, an instance of SynchronizeFileOperation created with
                        // syncFileContents == false is completely useless when we suspect
                        // that an upload is necessary (for instance, in FileObserverService).
                        Log_OC.d(TAG, "Nothing to do here");
                    }
                    result = new RemoteOperationResult(ResultCode.OK);

                } else if (serverChanged) {
                    mLocalFile.setRemoteId(mServerFile.getRemoteId());

                    if (mSyncFileContents) {
                        requestForDownload(mLocalFile); // local, not server; we won't to keep
                        // the value of favorite!
                        // the update of local data will be done later by the FileUploader
                        // service when the upload finishes
                    } else {
                        // TODO CHECK: is this really useful in some point in the code?
                        mServerFile.setFavorite(mLocalFile.isFavorite());
                        mServerFile.setHidden(mLocalFile.shouldHide());
                        mServerFile.setLastSyncDateForData(mLocalFile.getLastSyncDateForData());
                        mServerFile.setStoragePath(mLocalFile.getStoragePath());
                        mServerFile.setParentId(mLocalFile.getParentId());
                        mServerFile.setEtag(mLocalFile.getEtag());
                        getStorageManager().saveFile(mServerFile);

                    }
                    result = new RemoteOperationResult(ResultCode.OK);

                } else {
                    // nothing changed, nothing to do
                    result = new RemoteOperationResult(ResultCode.OK);
                }

                // safe blanket: sync'ing a not in-conflict file will clean wrong conflict markers in ancestors
                if (result.getCode() != ResultCode.SYNC_CONFLICT) {
                    getStorageManager().saveConflict(mLocalFile, null);
                }
            } else {
                // remote file does not exist, deleting local copy
                boolean deleteResult = getStorageManager().removeFile(mLocalFile, true, true);

                if (deleteResult) {
                    result = new RemoteOperationResult(ResultCode.FILE_NOT_FOUND);
                } else {
                    Log_OC.e(TAG, "Removal of local copy failed (remote file does not exist any longer).");
                }
            }

        }

        Log_OC.i(TAG, "Synchronizing " + mUser.getAccountName() + ", file " + mLocalFile.getRemotePath() +
            ": " + result.getLogMessage());

        return result;
    }


    /**
     * Requests for an upload to the FileUploader service
     *
     * @param file OCFile object representing the file to upload
     */
    private void requestForUpload(OCFile file) {
        FileUploadHelper.Companion.instance().uploadUpdatedFile(
            mUser,
            new OCFile[]{ file },
            FileUploadWorker.LOCAL_BEHAVIOUR_MOVE,
            NameCollisionPolicy.OVERWRITE);

        mTransferWasRequested = true;
    }

    private void requestForDownload(OCFile file) {
        FileDownloadHelper.Companion.instance().downloadFile(
            mUser,
            file);

        mTransferWasRequested = true;
    }

    public boolean transferWasRequested() {
        return mTransferWasRequested;
    }

    public OCFile getLocalFile() {
        return mLocalFile;
    }
}
