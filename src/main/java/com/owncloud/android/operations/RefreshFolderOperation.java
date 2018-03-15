/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2015 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.utils.DataHolderUtil;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
 *  Remote operation performing the synchronization of the list of files contained 
 *  in a folder identified with its remote path.
 *
 *  Fetches the list and properties of the files contained in the given folder, including their 
 *  properties, and updates the local database with them.
 *
 *  Does NOT enter in the child folders to synchronize their contents also.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RefreshFolderOperation extends RemoteOperation {

    private static final String TAG = RefreshFolderOperation.class.getSimpleName();

    public static final String EVENT_SINGLE_FOLDER_CONTENTS_SYNCED =
            RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_CONTENTS_SYNCED";
    public static final String EVENT_SINGLE_FOLDER_SHARES_SYNCED =
            RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_SHARES_SYNCED";

    /** Time stamp for the synchronization process in progress */
    private long mCurrentSyncTime;

    /** Remote folder to synchronize */
    private OCFile mLocalFolder;

    /** Access to the local database */
    private FileDataStorageManager mStorageManager;

    /** Account where the file to synchronize belongs */
    private Account mAccount;

    /** Android context; necessary to send requests to the download service */
    private Context mContext;

    /** Files and folders contained in the synchronized folder after a successful operation */
    private List<OCFile> mChildren;

    /** Counter of conflicts found between local and remote files */
    private int mConflictsFound;

    /** Counter of failed operations in synchronization of kept-in-sync files */
    private int mFailsInKeptInSyncFound;

    /**
     * Map of remote and local paths to files that where locally stored in a location 
     * out of the ownCloud folder and couldn't be copied automatically into it 
     **/
    private Map<String, String> mForgottenLocalFiles;

    /**
     * 'True' means that this operation is part of a full account synchronization
     */
    private boolean mSyncFullAccount;

    /** 'True' means that Share resources bound to the files into should be refreshed also */
    private boolean mIsShareSupported;

    /** 'True' means that the remote folder changed and should be fetched */
    private boolean mRemoteFolderChanged;

    /** 'True' means that Etag will be ignored */
    private boolean mIgnoreETag;

    private List<SynchronizeFileOperation> mFilesToSyncContents;
    // this will be used for every file when 'folder synchronization' replaces 'folder download'


    /**
     * Creates a new instance of {@link RefreshFolderOperation}.
     *
     * @param   folder                  Folder to synchronize.
     * @param   currentSyncTime         Time stamp for the synchronization process in progress.
     * @param   syncFullAccount         'True' means that this operation is part of a full account 
     *                                  synchronization.
     * @param   isShareSupported        'True' means that the server supports the sharing API.           
     * @param   ignoreETag              'True' means that the content of the remote folder should
     *                                  be fetched and updated even though the 'eTag' did not 
     *                                  change.  
     * @param   dataStorageManager      Interface with the local database.
     * @param   account                 ownCloud account where the folder is located. 
     * @param   context                 Application context.
     */
    public RefreshFolderOperation(OCFile folder,
                                  long currentSyncTime,
                                  boolean syncFullAccount,
                                  boolean isShareSupported,
                                  boolean ignoreETag,
                                  FileDataStorageManager dataStorageManager,
                                  Account account,
                                  Context context) {
        mLocalFolder = folder;
        mCurrentSyncTime = currentSyncTime;
        mSyncFullAccount = syncFullAccount;
        mIsShareSupported = isShareSupported;
        mStorageManager = dataStorageManager;
        mAccount = account;
        mContext = context;
        mForgottenLocalFiles = new HashMap<String, String>();
        mRemoteFolderChanged = false;
        mIgnoreETag = ignoreETag;
        mFilesToSyncContents = new Vector<SynchronizeFileOperation>();
    }


    public int getConflictsFound() {
        return mConflictsFound;
    }

    public int getFailsInKeptInSyncFound() {
        return mFailsInKeptInSyncFound;
    }

    public Map<String, String> getForgottenLocalFiles() {
        return mForgottenLocalFiles;
    }

    /**
     * Returns the list of files and folders contained in the synchronized folder, 
     * if called after synchronization is complete.
     *
     * @return List of files and folders contained in the synchronized folder.
     */
    public List<OCFile> getChildren() {
        return mChildren;
    }

    /**
     * Performs the synchronization.
     *
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        mFailsInKeptInSyncFound = 0;
        mConflictsFound = 0;
        mForgottenLocalFiles.clear();

        if (OCFile.ROOT_PATH.equals(mLocalFolder.getRemotePath()) && !mSyncFullAccount) {
            updateOCVersion(client);
            updateUserProfile();
        }

        result = checkForChanges(client);

        if (result.isSuccess()) {
            if (mRemoteFolderChanged) {
                result = fetchAndSyncRemoteFolder(client);
            } else {
                fetchKeptInSyncFilesToSyncFromLocalData();
                mChildren = mStorageManager.getFolderContent(mLocalFolder, false);
            }

            if (result.isSuccess()) {
                // request for the synchronization of KEPT-IN-SYNC file contents
                startContentSynchronizations(mFilesToSyncContents);
            }
        }

        if (!mSyncFullAccount) {
            sendLocalBroadcast(
                    EVENT_SINGLE_FOLDER_CONTENTS_SYNCED, mLocalFolder.getRemotePath(), result
            );
        }

        if (result.isSuccess() && mIsShareSupported && !mSyncFullAccount) {
            refreshSharesForFolder(client); // share result is ignored 
        }

        if (!mSyncFullAccount) {
            sendLocalBroadcast(
                    EVENT_SINGLE_FOLDER_SHARES_SYNCED, mLocalFolder.getRemotePath(), result
            );
        }

        return result;

    }

    private void updateOCVersion(OwnCloudClient client) {
        UpdateOCVersionOperation update = new UpdateOCVersionOperation(mAccount, mContext);
        RemoteOperationResult result = update.execute(client);
        if (result.isSuccess()) {
            mIsShareSupported = update.getOCVersion().isSharedSupported();

            // Update Capabilities for this account
            if (update.getOCVersion().isVersionWithCapabilitiesAPI()) {
                updateCapabilities();
            } else {
                Log_OC.d(TAG, "Capabilities API disabled");
            }
        }
    }

    private void updateUserProfile() {
        GetUserProfileOperation update = new GetUserProfileOperation();
        RemoteOperationResult result = update.execute(mStorageManager, mContext);
        if (!result.isSuccess()) {
            Log_OC.w(TAG, "Couldn't update user profile from server");
        } else {
            Log_OC.i(TAG, "Got display name: " + result.getData().get(0));
        }
    }

    private void updateCapabilities() {
        GetCapabilitiesOperarion getCapabilities = new GetCapabilitiesOperarion();
        RemoteOperationResult result = getCapabilities.execute(mStorageManager, mContext);
        if (!result.isSuccess()) {
            Log_OC.w(TAG, "Update Capabilities unsuccessfully");
        }
    }

    private RemoteOperationResult checkForChanges(OwnCloudClient client) {
        mRemoteFolderChanged = true;
        RemoteOperationResult result = null;
        String remotePath = mLocalFolder.getRemotePath();

        Log_OC.d(TAG, "Checking changes in " + mAccount.name + remotePath);

        // remote request 
        ReadRemoteFileOperation operation = new ReadRemoteFileOperation(remotePath);
        result = operation.execute(client, true);
        if (result.isSuccess()) {
            OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));

            if (!mIgnoreETag) {
                // check if remote and local folder are different
                String remoteFolderETag = remoteFolder.getEtag();
                if (remoteFolderETag != null) {
                    mRemoteFolderChanged =
                            !(remoteFolderETag.equalsIgnoreCase(mLocalFolder.getEtag()));
                } else {
                    Log_OC.e(TAG, "Checked " + mAccount.name + remotePath + " : " +
                            "No ETag received from server");
                }
            }

            result = new RemoteOperationResult(ResultCode.OK);

            Log_OC.i(TAG, "Checked " + mAccount.name + remotePath + " : " +
                    (mRemoteFolderChanged ? "changed" : "not changed"));

        } else {
            // check failed
            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                removeLocalFolder();
            }
            if (result.isException()) {
                Log_OC.e(TAG, "Checked " + mAccount.name + remotePath + " : " +
                        result.getLogMessage(), result.getException());
            } else {
                Log_OC.e(TAG, "Checked " + mAccount.name + remotePath + " : " +
                        result.getLogMessage());
            }
        }

        return result;
    }


    private RemoteOperationResult fetchAndSyncRemoteFolder(OwnCloudClient client) {
        String remotePath = mLocalFolder.getRemotePath();
        ReadRemoteFolderOperation operation = new ReadRemoteFolderOperation(remotePath);
        RemoteOperationResult result = operation.execute(client, true);
        Log_OC.d(TAG, "Synchronizing " + mAccount.name + remotePath);

        if (result.isSuccess()) {
            synchronizeData(result.getData());
            if (mConflictsFound > 0 || mFailsInKeptInSyncFound > 0) {
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
        if (mStorageManager.fileExists(mLocalFolder.getFileId())) {
            String currentSavePath = FileStorageUtils.getSavePath(mAccount.name);
            mStorageManager.removeFolder(
                    mLocalFolder,
                    true,
                    (mLocalFolder.isDown() &&
                            mLocalFolder.getStoragePath().startsWith(currentSavePath)
                    )
            );
        }
    }


    /**
     * Synchronizes the data retrieved from the server about the contents of the target folder
     * with the current data in the local database.
     *
     * Grants that mChildren is updated with fresh data after execution.
     *
     * @param folderAndFiles Remote folder and children files in Folder
     */
    private void synchronizeData(ArrayList<Object> folderAndFiles) {
        // get 'fresh data' from the database
        mLocalFolder = mStorageManager.getFileByPath(mLocalFolder.getRemotePath());

        // parse data from remote folder 
        OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) folderAndFiles.get(0));
        remoteFolder.setParentId(mLocalFolder.getParentId());
        remoteFolder.setFileId(mLocalFolder.getFileId());

        Log_OC.d(TAG, "Remote folder " + mLocalFolder.getRemotePath() + " changed - starting update of local data ");

        List<OCFile> updatedFiles = new Vector<OCFile>(folderAndFiles.size() - 1);
        mFilesToSyncContents.clear();

        // if local folder is encrypted, download fresh metadata
        DecryptedFolderMetadata metadata;
        boolean encryptedAncestor = FileStorageUtils.checkEncryptionStatus(mLocalFolder, mStorageManager);
        mLocalFolder.setEncrypted(encryptedAncestor);
        
        if (encryptedAncestor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            metadata = EncryptionUtils.downloadFolderMetadata(mLocalFolder, getClient(), mContext, mAccount);
        } else {
            metadata = null;
        }

        // get current data about local contents of the folder to synchronize
        List<OCFile> localFiles = mStorageManager.getFolderContent(mLocalFolder, false);
        Map<String, OCFile> localFilesMap = new HashMap<String, OCFile>(localFiles.size());

        for (OCFile file : localFiles) {
            String remotePath = file.getRemotePath();

            if (metadata != null && !file.isFolder()) {
                remotePath = file.getParentRemotePath() + file.getEncryptedFileName();
            }
            localFilesMap.put(remotePath, file);
        }

        // loop to update every child
        OCFile remoteFile;
        OCFile localFile;
        OCFile updatedFile;
        RemoteFile r;

        for (int i = 1; i < folderAndFiles.size(); i++) {
            /// new OCFile instance with the data from the server
            r = (RemoteFile) folderAndFiles.get(i);
            remoteFile = FileStorageUtils.fillOCFile(r);

            // new OCFile instance to merge fresh data from server with local state
            updatedFile = FileStorageUtils.fillOCFile(r);
            updatedFile.setParentId(mLocalFolder.getFileId());

            // retrieve local data for the read file 
            localFile = localFilesMap.remove(remoteFile.getRemotePath());

            // add to updatedFile data about LOCAL STATE (not existing in server)
            updatedFile.setLastSyncDateForProperties(mCurrentSyncTime);

            if (localFile != null) {
                updatedFile.setFileId(localFile.getFileId());
                updatedFile.setAvailableOffline(localFile.isAvailableOffline());
                updatedFile.setLastSyncDateForData(localFile.getLastSyncDateForData());
                updatedFile.setModificationTimestampAtLastSyncForData(
                        localFile.getModificationTimestampAtLastSyncForData()
                );
                updatedFile.setStoragePath(localFile.getStoragePath());
                // eTag will not be updated unless file CONTENTS are synchronized
                updatedFile.setEtag(localFile.getEtag());
                if (updatedFile.isFolder()) {
                    updatedFile.setFileLength(remoteFile.getFileLength());
                } else if (mRemoteFolderChanged && MimeTypeUtil.isImage(remoteFile) &&
                        remoteFile.getModificationTimestamp() !=
                                localFile.getModificationTimestamp()) {
                    updatedFile.setNeedsUpdateThumbnail(true);
                    Log.d(TAG, "Image " + remoteFile.getFileName() + " updated on the server");
                }
                updatedFile.setPublicLink(localFile.getPublicLink());
                updatedFile.setShareViaLink(localFile.isSharedViaLink());
                updatedFile.setShareWithSharee(localFile.isSharedWithSharee());
                updatedFile.setEtagInConflict(localFile.getEtagInConflict());
            } else {
                // remote eTag will not be updated unless file CONTENTS are synchronized
                updatedFile.setEtag("");
            }

            // check and fix, if needed, local storage path
            FileStorageUtils.searchForLocalFileInDefaultPath(updatedFile, mAccount);

            // prepare content synchronization for kept-in-sync files
            if (updatedFile.isAvailableOffline()) {
                SynchronizeFileOperation operation = new SynchronizeFileOperation(localFile, remoteFile, mAccount, true,
                        mContext);

                mFilesToSyncContents.add(operation);
            }

            // update file name for encrypted files
            if (metadata != null) {
                updatedFile.setEncryptedFileName(updatedFile.getFileName());
                try {
                    String decryptedFileName = metadata.getFiles().get(updatedFile.getFileName()).getEncrypted()
                            .getFilename();
                    String mimetype = metadata.getFiles().get(updatedFile.getFileName()).getEncrypted().getMimetype();
                    updatedFile.setFileName(decryptedFileName);

                    if (mimetype == null || mimetype.isEmpty()) {
                        updatedFile.setMimetype("application/octet-stream");
                    } else {
                        updatedFile.setMimetype(mimetype);
                    }
                } catch (NullPointerException e) {
                    Log_OC.e(TAG, "Metadata for file " + updatedFile.getFileId() + " not found!");
                }
            }

            // we parse content, so either the folder itself or its direct parent (which we check) must be encrypted
            boolean encrypted = updatedFile.isEncrypted() || mLocalFolder.isEncrypted();
            updatedFile.setEncrypted(encrypted);
            
            updatedFiles.add(updatedFile);
        }

        // save updated contents in local database
        mStorageManager.saveFolder(remoteFolder, updatedFiles, localFilesMap.values());

        mChildren = updatedFiles;
    }

    /**
     * Performs a list of synchronization operations, determining if a download or upload is needed
     * or if exists conflict due to changes both in local and remote contents of the each file.
     *
     * If download or upload is needed, request the operation to the corresponding service and goes 
     * on.
     *
     * @param filesToSyncContents       Synchronization operations to execute.
     */
    private void startContentSynchronizations(List<SynchronizeFileOperation> filesToSyncContents) {
        RemoteOperationResult contentsResult;
        for (SynchronizeFileOperation op : filesToSyncContents) {
            contentsResult = op.execute(mStorageManager, mContext);   // async
            if (!contentsResult.isSuccess()) {
                if (contentsResult.getCode() == ResultCode.SYNC_CONFLICT) {
                    mConflictsFound++;
                } else {
                    mFailsInKeptInSyncFound++;
                    if (contentsResult.getException() != null) {
                        Log_OC.e(TAG, "Error while synchronizing favourites : "
                                + contentsResult.getLogMessage(), contentsResult.getException());
                    } else {
                        Log_OC.e(TAG, "Error while synchronizing favourites : "
                                + contentsResult.getLogMessage());
                    }
                }
            }   // won't let these fails break the synchronization process
        }
    }


    /**
     * Syncs the Share resources for the files contained in the folder refreshed (children, not deeper descendants).
     *
     * @param client    Handler of a session with an OC server.
     * @return The result of the remote operation retrieving the Share resources in the folder refreshed by
     *                  the operation.
     */
    private RemoteOperationResult refreshSharesForFolder(OwnCloudClient client) {
        RemoteOperationResult result;

        // remote request 
        GetRemoteSharesForFileOperation operation =
                new GetRemoteSharesForFileOperation(mLocalFolder.getRemotePath(), true, true);
        result = operation.execute(client);

        if (result.isSuccess()) {
            // update local database
            ArrayList<OCShare> shares = new ArrayList<OCShare>();
            for (Object obj : result.getData()) {
                shares.add((OCShare) obj);
            }
            mStorageManager.saveSharesInFolder(shares, mLocalFolder);
        }

        return result;
    }


    /**
     * Sends a message to any application component interested in the progress 
     * of the synchronization.
     *
     * @param event
     * @param dirRemotePath     Remote path of a folder that was just synchronized 
     *                          (with or without success)
     * @param result
     */
    private void sendLocalBroadcast(
            String event, String dirRemotePath, RemoteOperationResult result
    ) {
        Log_OC.d(TAG, "Send broadcast " + event);
        Intent intent = new Intent(event);
        intent.putExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME, mAccount.name);

        if (dirRemotePath != null) {
            intent.putExtra(FileSyncAdapter.EXTRA_FOLDER_PATH, dirRemotePath);
        }

        DataHolderUtil dataHolderUtil = DataHolderUtil.getInstance();
        String dataHolderItemId = dataHolderUtil.nextItemId();
        dataHolderUtil.save(dataHolderItemId, result);
        intent.putExtra(FileSyncAdapter.EXTRA_RESULT, dataHolderItemId);

        intent.setPackage(mContext.getPackageName());
        mContext.sendStickyBroadcast(intent);
        //LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }


    private void fetchKeptInSyncFilesToSyncFromLocalData() {
        List<OCFile> children = mStorageManager.getFolderContent(mLocalFolder, false);
        for (OCFile child : children) {
            if (!child.isFolder() && child.isAvailableOffline() && !child.isInConflict()) {
                SynchronizeFileOperation operation = new SynchronizeFileOperation(
                        child,
                        child,  // cheating with the remote file to get an update to server; to refactor
                        mAccount,
                        true,
                        mContext
                );
                mFilesToSyncContents.add(operation);
            }
        }
    }

}
