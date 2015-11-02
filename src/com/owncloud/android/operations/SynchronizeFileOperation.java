/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author masensio
 *   Copyright (C) 2012 Bartek Przybylski
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

package com.owncloud.android.operations;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;

/**
 * Remote operation performing the read of remote file in the ownCloud server.
 */

public class SynchronizeFileOperation extends SyncOperation {

    private String TAG = SynchronizeFileOperation.class.getSimpleName();
    
    private OCFile mLocalFile;
    private String mRemotePath;
    private OCFile mServerFile;
    private Account mAccount;
    private boolean mSyncFileContents;
    private Context mContext;
    
    private boolean mTransferWasRequested = false;

    /** 
     * When 'false', uploads to the server are not done; only downloads or conflict detection.  
     * This is a temporal field. 
     * TODO Remove when 'folder synchronization' replaces 'folder download'.
     */    
    private boolean mAllowUploads;

    
    /**
     * Constructor for "full synchronization mode".
     * 
     * Uses remotePath to retrieve all the data both in local cache and in the remote OC server
     * when the operation is executed, instead of reusing {@link OCFile} instances.
     * 
     * Useful for direct synchronization of a single file.
     * 
     * @param 
     * @param account               ownCloud account holding the file.
     * @param syncFileContents      When 'true', transference of data will be started by the 
     *                              operation if needed and no conflict is detected.
     * @param context               Android context; needed to start transfers.
     */
    public SynchronizeFileOperation(
            String remotePath,  
            Account account, 
            boolean syncFileContents,
            Context context) {
        
        mRemotePath = remotePath;
        mLocalFile = null;
        mServerFile = null;
        mAccount = account;
        mSyncFileContents = syncFileContents;
        mContext = context;
        mAllowUploads = true;
    }

    
    /**
     * Constructor allowing to reuse {@link OCFile} instances just queried from local cache or
     * from remote OC server.
     * 
     * Useful to include this operation as part of the synchronization of a folder
     * (or a full account), avoiding the repetition of fetch operations (both in local database
     * or remote server).
     * 
     * At least one of localFile or serverFile MUST NOT BE NULL. If you don't have none of them,
     * use the other constructor.
     * 
     * @param localFile             Data of file (just) retrieved from local cache/database.
     * @param serverFile            Data of file (just) retrieved from a remote server. If null,
     *                              will be retrieved from network by the operation when executed.
     * @param account               ownCloud account holding the file.
     * @param syncFileContents      When 'true', transference of data will be started by the 
     *                              operation if needed and no conflict is detected.
     * @param context               Android context; needed to start transfers.
     */
    public SynchronizeFileOperation(
            OCFile localFile,
            OCFile serverFile, 
            Account account, 
            boolean syncFileContents,
            Context context) {
        
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
        mAccount = account;
        mSyncFileContents = syncFileContents;
        mContext = context;
        mAllowUploads = true;
    }
    

    /**
     * Temporal constructor.
     * 
     * Extends the previous one to allow constrained synchronizations where uploads are never
     * performed - only downloads or conflict detection.
     * 
     * Do not use unless you are involved in 'folder synchronization' or 'folder download' work
     * in progress.
     * 
     * TODO Remove when 'folder synchronization' replaces 'folder download'.
     * 
     * @param localFile             Data of file (just) retrieved from local cache/database.
     *                              MUSTN't be null.
     * @param serverFile            Data of file (just) retrieved from a remote server.
     *                              If null, will be retrieved from network by the operation
     *                              when executed.
     * @param account               ownCloud account holding the file.
     * @param syncFileContents      When 'true', transference of data will be started by the 
     *                              operation if needed and no conflict is detected.
     * @param allowUploads          When 'false', uploads to the server are not done;
     *                              only downloads or conflict detection.
     * @param context               Android context; needed to start transfers.
     */
    public SynchronizeFileOperation(
            OCFile localFile,
            OCFile serverFile, 
            Account account, 
            boolean syncFileContents,
            boolean allowUploads, 
            Context context) {
        
        this(localFile, serverFile, account, syncFileContents, context);
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
                ReadRemoteFileOperation operation = new ReadRemoteFileOperation(mRemotePath);
                result = operation.execute(client);
                if (result.isSuccess()){
                    mServerFile = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));
                    mServerFile.setLastSyncDateForProperties(System.currentTimeMillis());
                }
            }

            if (mServerFile != null) {   

                /// check changes in server and local file
                boolean serverChanged = false;
                if (mLocalFile.getEtag() == null || mLocalFile.getEtag().length() == 0) {
                    // file uploaded (null) or downloaded ("") before upgrade to version 1.8.0; check the old condition
                    serverChanged = mServerFile.getModificationTimestamp() !=
                            mLocalFile.getModificationTimestampAtLastSyncForData();
                } else {
                    serverChanged = (!mServerFile.getEtag().equals(mLocalFile.getEtag()));
                }
                boolean localChanged = (
                    mLocalFile.getLocalModificationTimestamp() > mLocalFile.getLastSyncDateForData()
                );

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
            }

        }

        Log_OC.i(TAG, "Synchronizing " + mAccount.name + ", file " + mLocalFile.getRemotePath() +
                ": " + result.getLogMessage());

        return result;
    }

    
    /**
     * Requests for an upload to the FileUploader service
     * 
     * @param file     OCFile object representing the file to upload
     */
    private void requestForUpload(OCFile file) {
        Intent i = new Intent(mContext, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT, mAccount);
        i.putExtra(FileUploader.KEY_FILE, file);
        /*i.putExtra(FileUploader.KEY_REMOTE_FILE, mRemotePath);
        // doing this we would lose the value of isFavorite in the road, and maybe
        // it's not updated in the database when the FileUploader service gets it!
        i.putExtra(FileUploader.KEY_LOCAL_FILE, localFile.getStoragePath());*/
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        i.putExtra(FileUploader.KEY_FORCE_OVERWRITE, true);
        mContext.startService(i);
        mTransferWasRequested = true;
    }


    /**
     * Requests for a download to the FileDownloader service
     * 
     * @param file     OCFile object representing the file to download
     */
    private void requestForDownload(OCFile file) {
        Intent i = new Intent(mContext, FileDownloader.class);
        i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
        i.putExtra(FileDownloader.EXTRA_FILE, file);
        mContext.startService(i);
        mTransferWasRequested = true;
    }


    public boolean transferWasRequested() {
        return mTransferWasRequested;
    }


    public OCFile getLocalFile() {
        return mLocalFile;
    }

}
