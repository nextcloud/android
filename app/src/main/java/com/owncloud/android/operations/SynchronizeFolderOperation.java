/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
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

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *  Remote operation performing the synchronization of the list of files contained
 *  in a folder identified with its remote path.
 *
 *  Fetches the list and properties of the files contained in the given folder, including their
 *  properties, and updates the local database with them.
 *
 *  Does NOT enter in the child folders to synchronize their contents also, BUT requests for a new operation instance
 *  doing so.
 */
public class SynchronizeFolderOperation extends SyncOperation {

    private static final String TAG = SynchronizeFolderOperation.class.getSimpleName();

    /** Time stamp for the synchronization process in progress */
    private long mCurrentSyncTime;

    /** Remote path of the folder to synchronize */
    private String mRemotePath;

    /** Account where the file to synchronize belongs */
    private User user;

    /** Android context; necessary to send requests to the download service */
    private Context mContext;

    /** Locally cached information about folder to synchronize */
    private OCFile mLocalFolder;

    /** Counter of conflicts found between local and remote files */
    private int mConflictsFound;

    /** Counter of failed operations in synchronization of kept-in-sync files */
    private int mFailsInFileSyncsFound;

    /**
     * 'True' means that the remote folder changed and should be fetched
     */
    private boolean mRemoteFolderChanged;

    private List<OCFile> mFilesForDirectDownload;
    // to avoid extra PROPFINDs when there was no change in the folder

    private List<SyncOperation> mFilesToSyncContents;
    // this will be used for every file when 'folder synchronization' replaces 'folder download'

    private final AtomicBoolean mCancellationRequested;

    /**
     * Creates a new instance of {@link SynchronizeFolderOperation}.
     *
     * @param context         Application context.
     * @param remotePath      Path to synchronize.
     * @param user            Nextcloud account where the folder is located.
     * @param currentSyncTime Time stamp for the synchronization process in progress.
     */
    public SynchronizeFolderOperation(Context context,
                                      String remotePath,
                                      User user,
                                      long currentSyncTime,
                                      FileDataStorageManager storageManager) {
        super(storageManager);

        mRemotePath = remotePath;
        mCurrentSyncTime = currentSyncTime;
        this.user = user;
        mContext = context;
        mRemoteFolderChanged = false;
        mFilesForDirectDownload = new Vector<>();
        mFilesToSyncContents = new Vector<>();
        mCancellationRequested = new AtomicBoolean(false);
    }


    /**
     * Performs the synchronization.
     *
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        mFailsInFileSyncsFound = 0;
        mConflictsFound = 0;

        try {
            // get locally cached information about folder
            mLocalFolder = getStorageManager().getFileByPath(mRemotePath);

            result = checkForChanges(client);

            if (result.isSuccess()) {
                if (mRemoteFolderChanged) {
                    result = fetchAndSyncRemoteFolder(client);

                } else {
                    prepareOpsFromLocalKnowledge();
                }

                if (result.isSuccess()) {
                    syncContents();
                }
            }

            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }

        } catch (OperationCancelledException e) {
            result = new RemoteOperationResult(e);
        }

        return result;
    }

    private RemoteOperationResult checkForChanges(OwnCloudClient client) throws OperationCancelledException {
        Log_OC.d(TAG, "Checking changes in " + user.getAccountName() + mRemotePath);

        mRemoteFolderChanged = true;

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        // remote request
        ReadFileRemoteOperation operation = new ReadFileRemoteOperation(mRemotePath);
        RemoteOperationResult result = operation.execute(client);
        if (result.isSuccess()) {
            OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));

            // check if remote and local folder are different
            mRemoteFolderChanged = !(remoteFolder.getEtag().equalsIgnoreCase(mLocalFolder.getEtag()));

            result = new RemoteOperationResult(ResultCode.OK);

            Log_OC.i(TAG, "Checked " + user.getAccountName() + mRemotePath + " : " +
                    (mRemoteFolderChanged ? "changed" : "not changed"));

        } else {
            // check failed
            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                removeLocalFolder();
            }
            if (result.isException()) {
                Log_OC.e(TAG, "Checked " + user.getAccountName() + mRemotePath  + " : " +
                        result.getLogMessage(), result.getException());
            } else {
                Log_OC.e(TAG, "Checked " + user.getAccountName() + mRemotePath + " : " +
                        result.getLogMessage());
            }

        }

        return result;
    }


    private RemoteOperationResult fetchAndSyncRemoteFolder(OwnCloudClient client) throws OperationCancelledException {
        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        ReadFolderRemoteOperation operation = new ReadFolderRemoteOperation(mRemotePath);
        RemoteOperationResult result = operation.execute(client);
        Log_OC.d(TAG, "Synchronizing " + user.getAccountName() + mRemotePath);

        if (result.isSuccess()) {
            synchronizeData(result.getData());
            if (mConflictsFound > 0  || mFailsInFileSyncsFound > 0) {
                result = new RemoteOperationResult(ResultCode.SYNC_CONFLICT);
                    // should be a different result code, but will do the job
            }
        } else {
            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                removeLocalFolder();
            }
        }

        return result;
    }


    private void removeLocalFolder() {
        FileDataStorageManager storageManager = getStorageManager();
        if (storageManager.fileExists(mLocalFolder.getFileId())) {
            String currentSavePath = FileStorageUtils.getSavePath(user.getAccountName());
            storageManager.removeFolder(
                    mLocalFolder,
                    true,
                    mLocalFolder.isDown() // TODO: debug, I think this is always false for folders
                            && mLocalFolder.getStoragePath().startsWith(currentSavePath)
            );
        }
    }


    /**
     * Synchronizes the data retrieved from the server about the contents of the target folder
     * with the current data in the local database.
     *
     * @param folderAndFiles Remote folder and children files in Folder
     */
    private void synchronizeData(List<Object> folderAndFiles) throws OperationCancelledException {


        // parse data from remote folder
        OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) folderAndFiles.get(0));
        remoteFolder.setParentId(mLocalFolder.getParentId());
        remoteFolder.setFileId(mLocalFolder.getFileId());

        Log_OC.d(TAG, "Remote folder " + mLocalFolder.getRemotePath() + " changed - starting update of local data ");

        mFilesForDirectDownload.clear();
        mFilesToSyncContents.clear();

        if (mCancellationRequested.get()) {
            throw new OperationCancelledException();
        }

        FileDataStorageManager storageManager = getStorageManager();

        // if local folder is encrypted, download fresh metadata
        boolean encryptedAncestor = FileStorageUtils.checkEncryptionStatus(remoteFolder, storageManager);
        mLocalFolder.setEncrypted(encryptedAncestor);

        // update permission
        mLocalFolder.setPermissions(remoteFolder.getPermissions());

        // update richWorkspace
        mLocalFolder.setRichWorkspace(remoteFolder.getRichWorkspace());

        DecryptedFolderMetadataFile metadata = RefreshFolderOperation.getDecryptedFolderMetadata(encryptedAncestor,
                                                                                                 mLocalFolder,
                                                                                                 getClient(),
                                                                                                 user,
                                                                                                 mContext);

        // get current data about local contents of the folder to synchronize
        Map<String, OCFile> localFilesMap =
            RefreshFolderOperation.prefillLocalFilesMap(metadata,
                                                        storageManager.getFolderContent(mLocalFolder, false));

        // loop to synchronize every child
        List<OCFile> updatedFiles = new ArrayList<>(folderAndFiles.size() - 1);
        OCFile remoteFile;
        OCFile localFile;
        OCFile updatedFile;
        RemoteFile remote;

        for (int i = 1; i < folderAndFiles.size(); i++) {
            /// new OCFile instance with the data from the server
            remote = (RemoteFile) folderAndFiles.get(i);
            remoteFile = FileStorageUtils.fillOCFile(remote);

            /// new OCFile instance to merge fresh data from server with local state
            updatedFile = FileStorageUtils.fillOCFile(remote);
            updatedFile.setParentId(mLocalFolder.getFileId());

            /// retrieve local data for the read file
            localFile = localFilesMap.remove(remoteFile.getRemotePath());

            // TODO better implementation is needed
            if (localFile == null) {
                localFile = storageManager.getFileByPath(updatedFile.getRemotePath());
            }

            /// add to updatedFile data about LOCAL STATE (not existing in server)
            updateLocalStateData(remoteFile, localFile, updatedFile);

            /// check and fix, if needed, local storage path
            FileStorageUtils.searchForLocalFileInDefaultPath(updatedFile, user.getAccountName());

            // update file name for encrypted files
            if (metadata != null) {
                RefreshFolderOperation.updateFileNameForEncryptedFile(storageManager, metadata, updatedFile);
            }

            // we parse content, so either the folder itself or its direct parent (which we check) must be encrypted
            boolean encrypted = updatedFile.isEncrypted() || mLocalFolder.isEncrypted();
            updatedFile.setEncrypted(encrypted);

            /// classify file to sync/download contents later
            classifyFileForLaterSyncOrDownload(remoteFile, localFile);

            updatedFiles.add(updatedFile);
        }

        if (metadata != null) {
            RefreshFolderOperation.updateFileNameForEncryptedFile(storageManager, metadata, mLocalFolder);
        }

        // save updated contents in local database
        storageManager.saveFolder(remoteFolder, updatedFiles, localFilesMap.values());
        mLocalFolder.setLastSyncDateForData(System.currentTimeMillis());
        storageManager.saveFile(mLocalFolder);
    }

    private void updateLocalStateData(OCFile remoteFile, OCFile localFile, OCFile updatedFile) {
        updatedFile.setLastSyncDateForProperties(mCurrentSyncTime);
        if (localFile != null) {
            updatedFile.setFileId(localFile.getFileId());
            updatedFile.setLastSyncDateForData(localFile.getLastSyncDateForData());
            updatedFile.setModificationTimestampAtLastSyncForData(
                    localFile.getModificationTimestampAtLastSyncForData()
            );
            updatedFile.setStoragePath(localFile.getStoragePath());
            // eTag will not be updated unless file CONTENTS are synchronized
            updatedFile.setEtag(localFile.getEtag());
            if (updatedFile.isFolder()) {
                updatedFile.setFileLength(localFile.getFileLength());
                    // TODO move operations about size of folders to FileContentProvider
            } else if (mRemoteFolderChanged && MimeTypeUtil.isImage(remoteFile) &&
                    remoteFile.getModificationTimestamp() !=
                            localFile.getModificationTimestamp()) {
                updatedFile.setUpdateThumbnailNeeded(true);
                Log_OC.d(TAG, "Image " + remoteFile.getFileName() + " updated on the server");
            }
            updatedFile.setSharedViaLink(localFile.isSharedViaLink());
            updatedFile.setSharedWithSharee(localFile.isSharedWithSharee());
            updatedFile.setEtagInConflict(localFile.getEtagInConflict());
        } else {
            // remote eTag will not be updated unless file CONTENTS are synchronized
            updatedFile.setEtag("");
        }
    }

    private void classifyFileForLaterSyncOrDownload(OCFile remoteFile, OCFile localFile)
            throws OperationCancelledException {
        if (remoteFile.isFolder()) {
            /// to download children files recursively
            synchronized (mCancellationRequested) {
                if (mCancellationRequested.get()) {
                    throw new OperationCancelledException();
                }
                startSyncFolderOperation(remoteFile.getRemotePath());
            }

        } else {
            /// prepare content synchronization for files (any file, not just favorites)
            SynchronizeFileOperation operation = new SynchronizeFileOperation(
                localFile,
                remoteFile,
                user,
                true,
                mContext,
                getStorageManager()
            );
            mFilesToSyncContents.add(operation);
        }
    }


    private void prepareOpsFromLocalKnowledge() throws OperationCancelledException {
        List<OCFile> children = getStorageManager().getFolderContent(mLocalFolder, false);
        for (OCFile child : children) {
            /// classify file to sync/download contents later
            if (child.isFolder()) {
                /// to download children files recursively
                synchronized(mCancellationRequested) {
                    if (mCancellationRequested.get()) {
                        throw new OperationCancelledException();
                    }
                    startSyncFolderOperation(child.getRemotePath());
                }

            } else {
                /// synchronization for regular files
                if (!child.isDown()) {
                    mFilesForDirectDownload.add(child);

                } else {
                    /// this should result in direct upload of files that were locally modified
                    SynchronizeFileOperation operation = new SynchronizeFileOperation(
                        child,
                        child.getEtagInConflict() != null ? child : null,
                        user,
                        true,
                        mContext,
                        getStorageManager()
                    );
                    mFilesToSyncContents.add(operation);

                }

            }
        }
    }


    private void syncContents() throws OperationCancelledException {
        startDirectDownloads();
        startContentSynchronizations(mFilesToSyncContents);
    }


    private void startDirectDownloads() throws OperationCancelledException {
        for (OCFile file : mFilesForDirectDownload) {
            synchronized(mCancellationRequested) {
                if (mCancellationRequested.get()) {
                    throw new OperationCancelledException();
                }
                Intent i = new Intent(mContext, FileDownloader.class);
                i.putExtra(FileDownloader.EXTRA_USER, user);
                i.putExtra(FileDownloader.EXTRA_FILE, file);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    mContext.startForegroundService(i);
                } else {
                    mContext.startService(i);
                }
            }
        }
    }

    /**
     * Performs a list of synchronization operations, determining if a download or upload is needed
     * or if exists conflict due to changes both in local and remote contents of the each file.
     *
     * If download or upload is needed, request the operation to the corresponding service and goes on.
     *
     * @param filesToSyncContents       Synchronization operations to execute.
     */
    private void startContentSynchronizations(List<SyncOperation> filesToSyncContents)
            throws OperationCancelledException {

        Log_OC.v(TAG, "Starting content synchronization... ");
        RemoteOperationResult contentsResult;
        for (SyncOperation op: filesToSyncContents) {
            if (mCancellationRequested.get()) {
                throw new OperationCancelledException();
            }
            contentsResult = op.execute(mContext);
            if (!contentsResult.isSuccess()) {
                if (contentsResult.getCode() == ResultCode.SYNC_CONFLICT) {
                    mConflictsFound++;
                } else {
                    mFailsInFileSyncsFound++;
                    if (contentsResult.getException() != null) {
                        Log_OC.e(TAG, "Error while synchronizing file : "
                                +  contentsResult.getLogMessage(), contentsResult.getException());
                    } else {
                        Log_OC.e(TAG, "Error while synchronizing file : "
                                + contentsResult.getLogMessage());
                    }
                }
                // TODO - use the errors count in notifications
            }   // won't let these fails break the synchronization process
        }
    }

    /**
     * Scans the default location for saving local copies of files searching for
     * a 'lost' file with the same full name as the {@link com.owncloud.android.datamodel.OCFile}
     * received as parameter.
     *
     * @param file      File to associate a possible 'lost' local file.
     */
    private void searchForLocalFileInDefaultPath(OCFile file) {
        if (file.getStoragePath() == null && !file.isFolder()) {
            File f = new File(FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), file));
            if (f.exists()) {
                file.setStoragePath(f.getAbsolutePath());
                file.setLastSyncDateForData(f.lastModified());
            }
        }
    }


    /**
     * Cancel operation
     */
    public void cancel() {
        mCancellationRequested.set(true);
    }

    public String getFolderPath() {
        String path = mLocalFolder.getStoragePath();
        if (!TextUtils.isEmpty(path)) {
            return path;
        }
        return FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), mLocalFolder);
    }

    private void startSyncFolderOperation(String path){
        Intent intent = new Intent(mContext, OperationsService.class);
        intent.setAction(OperationsService.ACTION_SYNC_FOLDER);
        intent.putExtra(OperationsService.EXTRA_ACCOUNT, user.toPlatformAccount());
        intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, path);
        mContext.startService(intent);
    }

    public String getRemotePath() {
        return mRemotePath;
    }
}
