/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.nextcloud.android.lib.resources.directediting.DirectEditingObtainRemoteOperation;
import com.nextcloud.client.account.User;
import com.nextcloud.client.database.entity.OfflineOperationEntity;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.utils.extensions.RemoteOperationResultExtensionsKt;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.lib.common.DirectEditing;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.shares.GetSharesForFileRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.E2EVersion;
import com.owncloud.android.lib.resources.users.GetPredefinedStatusesRemoteOperation;
import com.owncloud.android.lib.resources.users.PredefinedStatus;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.utils.DataHolderUtil;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.CapabilityUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;

/**
 * Remote operation performing the synchronization of the list of files contained in a folder identified with its remote
 * path. Fetches the list and properties of the files contained in the given folder, including their properties, and
 * updates the local database with them. Does NOT enter in the child folders to synchronize their contents also.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class RefreshFolderOperation extends RemoteOperation {

    private static final String TAG = RefreshFolderOperation.class.getSimpleName();

    public static final String EVENT_SINGLE_FOLDER_CONTENTS_SYNCED =
        RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_CONTENTS_SYNCED";
    public static final String EVENT_SINGLE_FOLDER_SHARES_SYNCED =
        RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_SHARES_SYNCED";

    /**
     * Time stamp for the synchronization process in progress
     */
    private final long mCurrentSyncTime;

    /**
     * Remote folder to synchronize
     */
    private OCFile mLocalFolder;

    /**
     * Access to the local database
     */
    private final FileDataStorageManager mStorageManager;

    /**
     * Account where the file to synchronize belongs
     */
    private final User user;

    /**
     * Android context; necessary to send requests to the download service
     */
    private final Context mContext;

    /**
     * Files and folders contained in the synchronized folder after a successful operation
     */
    private List<OCFile> mChildren;

    /**
     * Counter of conflicts found between local and remote files
     */
    private int mConflictsFound;

    /**
     * Counter of failed operations in synchronization of kept-in-sync files
     */
    private int mFailsInKeptInSyncFound;

    /**
     * Map of remote and local paths to files that where locally stored in a location out of the ownCloud folder and
     * couldn't be copied automatically into it
     **/
    private final Map<String, String> mForgottenLocalFiles;

    /**
     * 'True' means that this operation is part of a full account synchronization
     */
    private final boolean mSyncFullAccount;

    /**
     * 'True' means that the remote folder changed and should be fetched
     */
    private boolean mRemoteFolderChanged;

    /**
     * 'True' means that Etag will be ignored
     */
    private final boolean mIgnoreETag;

    /**
     * 'True' means that no share and no capabilities will be updated
     */
    private final boolean mOnlyFileMetadata;

    private final List<SynchronizeFileOperation> mFilesToSyncContents;
    // this will be used for every file when 'folder synchronization' replaces 'folder download'


    /**
     * Creates a new instance of {@link RefreshFolderOperation}.
     *
     * @param folder             Folder to synchronize.
     * @param currentSyncTime    Time stamp for the synchronization process in progress.
     * @param syncFullAccount    'True' means that this operation is part of a full account synchronization.
     * @param ignoreETag         'True' means that the content of the remote folder should be fetched and updated even
     *                           though the 'eTag' did not change.
     * @param dataStorageManager Interface with the local database.
     * @param user               ownCloud account where the folder is located.
     * @param context            Application context.
     */
    public RefreshFolderOperation(OCFile folder,
                                  long currentSyncTime,
                                  boolean syncFullAccount,
                                  boolean ignoreETag,
                                  FileDataStorageManager dataStorageManager,
                                  User user,
                                  Context context) {
        mLocalFolder = folder;
        mCurrentSyncTime = currentSyncTime;
        mSyncFullAccount = syncFullAccount;
        mStorageManager = dataStorageManager;
        this.user = user;
        mContext = context;
        mForgottenLocalFiles = new HashMap<>();
        mRemoteFolderChanged = false;
        mIgnoreETag = ignoreETag;
        mOnlyFileMetadata = false;
        mFilesToSyncContents = new Vector<>();
    }

    public RefreshFolderOperation(OCFile folder,
                                  long currentSyncTime,
                                  boolean syncFullAccount,
                                  boolean ignoreETag,
                                  boolean onlyFileMetadata,
                                  FileDataStorageManager dataStorageManager,
                                  User user,
                                  Context context) {
        mLocalFolder = folder;
        mCurrentSyncTime = currentSyncTime;
        mSyncFullAccount = syncFullAccount;
        mStorageManager = dataStorageManager;
        this.user = user;
        mContext = context;
        mForgottenLocalFiles = new HashMap<>();
        mRemoteFolderChanged = false;
        mIgnoreETag = ignoreETag;
        mOnlyFileMetadata = onlyFileMetadata;
        mFilesToSyncContents = new Vector<>();
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
     * Returns the list of files and folders contained in the synchronized folder, if called after synchronization is
     * complete.
     *
     * @return List of files and folders contained in the synchronized folder.
     */
    public List<OCFile> getChildren() {
        return mChildren;
    }

    private Pair<ArrayList<String>, ArrayList<String>> getConflictedRemoteIdsWithOfflineOperations(RemoteOperationResult operationResult) {
        List<OfflineOperationEntity> offlineOperations = mStorageManager.offlineOperationDao.getAll();
        List<OCFile> newFiles = RemoteOperationResultExtensionsKt.toOCFile(operationResult);
        if (newFiles == null) return null;

        ArrayList<String> conflictedOfflineOperationsPaths = new ArrayList<>();
        ArrayList<String> newFilesRemoteIds = new ArrayList<>();

        for (OCFile file: newFiles) {
            for (OfflineOperationEntity offlineOperation: offlineOperations) {
                if (file.getFileName().equals(offlineOperation.getFilename())) {
                    newFilesRemoteIds.add(file.getRemoteId());
                    conflictedOfflineOperationsPaths.add(offlineOperation.getPath());
                }
            }
        }

        return new Pair<>(newFilesRemoteIds, conflictedOfflineOperationsPaths);
    }

    /**
     * Performs the synchronization.
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        mFailsInKeptInSyncFound = 0;
        mConflictsFound = 0;
        mForgottenLocalFiles.clear();

        if (OCFile.ROOT_PATH.equals(mLocalFolder.getRemotePath()) && !mSyncFullAccount && !mOnlyFileMetadata) {
            updateOCVersion(client);
            updateUserProfile();
        }

        result = checkForChanges(client);

        if (result.isSuccess()) {
            if (mRemoteFolderChanged) {
                // TODO catch IllegalStateException, show properly to user
                result = fetchAndSyncRemoteFolder(client);
            } else {
                mChildren = mStorageManager.getFolderContent(mLocalFolder, false);
            }

            if (result.isSuccess()) {
                // request for the synchronization of KEPT-IN-SYNC file contents
                startContentSynchronizations(mFilesToSyncContents);
            } else {
                mLocalFolder.setEtag("");
            }

            mLocalFolder.setLastSyncDateForData(System.currentTimeMillis());
            mStorageManager.saveFile(mLocalFolder);
        }

        Pair<ArrayList<String>, ArrayList<String>> conflictedRemoteIdsAndOfflineOperationPaths = getConflictedRemoteIdsWithOfflineOperations(result);
        if (conflictedRemoteIdsAndOfflineOperationPaths != null && !conflictedRemoteIdsAndOfflineOperationPaths.first.isEmpty() && !conflictedRemoteIdsAndOfflineOperationPaths.second.isEmpty()) {
            sendFolderSyncConflictEventBroadcast(conflictedRemoteIdsAndOfflineOperationPaths);
        }

        if (!mSyncFullAccount && mRemoteFolderChanged) {
            sendLocalBroadcast(
                EVENT_SINGLE_FOLDER_CONTENTS_SYNCED, mLocalFolder.getRemotePath(), result
                              );
        }

        if (result.isSuccess() && !mSyncFullAccount && !mOnlyFileMetadata) {
            refreshSharesForFolder(client); // share result is ignored
        }

        if (!mSyncFullAccount) {
            sendLocalBroadcast(
                EVENT_SINGLE_FOLDER_SHARES_SYNCED, mLocalFolder.getRemotePath(), result
                              );
        }

        return result;
    }

    private void sendFolderSyncConflictEventBroadcast(Pair<ArrayList<String>, ArrayList<String>> conflictedRemoteIdsAndOfflineOperationPaths) {
        Intent intent = new Intent(FileDisplayActivity.FOLDER_SYNC_CONFLICT);
        intent.putStringArrayListExtra(FileDisplayActivity.FOLDER_SYNC_CONFLICT_NEW_FILES, conflictedRemoteIdsAndOfflineOperationPaths.first);
        intent.putStringArrayListExtra(FileDisplayActivity.FOLDER_SYNC_CONFLICT_OFFLINE_OPERATION_PATHS, conflictedRemoteIdsAndOfflineOperationPaths.second);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void updateOCVersion(OwnCloudClient client) {
        UpdateOCVersionOperation update = new UpdateOCVersionOperation(user, mContext);
        RemoteOperationResult result = update.execute(client);
        if (result.isSuccess()) {
            // Update Capabilities for this account
            updateCapabilities();
        }
    }

    private void updateUserProfile() {
        try {
            NextcloudClient nextcloudClient = OwnCloudClientFactory.createNextcloudClient(user, mContext);

            RemoteOperationResult<UserInfo> result = new GetUserProfileOperation(mStorageManager).execute(nextcloudClient);
            if (!result.isSuccess()) {
                Log_OC.w(TAG, "Couldn't update user profile from server");
            } else {
                Log_OC.i(TAG, "Got display name: " + result.getResultData());
            }
        } catch (AccountUtils.AccountNotFoundException | NullPointerException e) {
            Log_OC.e(this, "Error updating profile", e);
        }
    }

    private void updateCapabilities() {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(mContext);
        String oldDirectEditingEtag = arbitraryDataProvider.getValue(user,
                                                                     ArbitraryDataProvider.DIRECT_EDITING_ETAG);

        RemoteOperationResult result = new GetCapabilitiesOperation(mStorageManager).execute(mContext);
        if (result.isSuccess()) {
            String newDirectEditingEtag = mStorageManager.getCapability(user.getAccountName()).getDirectEditingEtag();

            if (!oldDirectEditingEtag.equalsIgnoreCase(newDirectEditingEtag)) {
                updateDirectEditing(arbitraryDataProvider, newDirectEditingEtag);
            }

            updatePredefinedStatus(arbitraryDataProvider);
        } else {
            Log_OC.w(TAG, "Update Capabilities unsuccessfully");
        }
    }

    private void updateDirectEditing(ArbitraryDataProvider arbitraryDataProvider, String newDirectEditingEtag) {
        RemoteOperationResult<DirectEditing> result =
            new DirectEditingObtainRemoteOperation().executeNextcloudClient(user, mContext);

        if (result.isSuccess()) {
            DirectEditing directEditing = result.getResultData();
            String json = new Gson().toJson(directEditing);
            arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), ArbitraryDataProvider.DIRECT_EDITING, json);
        } else {
            arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), ArbitraryDataProvider.DIRECT_EDITING);
        }

        arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                    ArbitraryDataProvider.DIRECT_EDITING_ETAG,
                                                    newDirectEditingEtag);
    }

    private void updatePredefinedStatus(ArbitraryDataProvider arbitraryDataProvider) {
        NextcloudClient client;

        try {
            client = OwnCloudClientFactory.createNextcloudClient(user, mContext);
        } catch (AccountUtils.AccountNotFoundException | NullPointerException e) {
            Log_OC.e(this, "Update of predefined status not possible!");
            return;
        }

        RemoteOperationResult<ArrayList<PredefinedStatus>> result =
            new GetPredefinedStatusesRemoteOperation().execute(client);

        if (result.isSuccess()) {
            ArrayList<PredefinedStatus> predefinedStatuses = result.getResultData();
            String json = new Gson().toJson(predefinedStatuses);
            arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), ArbitraryDataProvider.PREDEFINED_STATUS, json);
        } else {
            arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), ArbitraryDataProvider.PREDEFINED_STATUS);
        }
    }

    private RemoteOperationResult checkForChanges(OwnCloudClient client) {
        mRemoteFolderChanged = true;
        RemoteOperationResult result;
        String remotePath = mLocalFolder.getRemotePath();

        Log_OC.d(TAG, "Checking changes in " + user.getAccountName() + remotePath);

        // remote request
        result = new ReadFileRemoteOperation(remotePath).execute(client);

        if (result.isSuccess()) {
            OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));

            if (!mIgnoreETag) {
                // check if remote and local folder are different
                String remoteFolderETag = remoteFolder.getEtag();
                if (remoteFolderETag != null) {
                    mRemoteFolderChanged = !(remoteFolderETag.equalsIgnoreCase(mLocalFolder.getEtag()));
                } else {
                    Log_OC.e(TAG, "Checked " + user.getAccountName() + remotePath + ": No ETag received from server");
                }
            }

            result = new RemoteOperationResult(ResultCode.OK);

            Log_OC.i(TAG, "Checked " + user.getAccountName() + remotePath + " : " +
                (mRemoteFolderChanged ? "changed" : "not changed"));

        } else {
            // check failed
            if (result.getCode() == ResultCode.FILE_NOT_FOUND) {
                removeLocalFolder();
            }
            if (result.isException()) {
                Log_OC.e(TAG, "Checked " + user.getAccountName() + remotePath + " : " +
                    result.getLogMessage(), result.getException());
            } else {
                Log_OC.e(TAG, "Checked " + user.getAccountName() + remotePath + " : " +
                    result.getLogMessage());
            }
        }

        return result;
    }


    private RemoteOperationResult fetchAndSyncRemoteFolder(OwnCloudClient client) {
        String remotePath = mLocalFolder.getRemotePath();
        RemoteOperationResult result = new ReadFolderRemoteOperation(remotePath).execute(client);
        Log_OC.d(TAG, "Refresh folder " + user.getAccountName() + remotePath);
        Log_OC.d(TAG, "Refresh folder with remote id" + mLocalFolder.getRemoteId());

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
            String currentSavePath = FileStorageUtils.getSavePath(user.getAccountName());
            mStorageManager.removeFolder(
                mLocalFolder,
                true,
                mLocalFolder.isDown() && mLocalFolder.getStoragePath().startsWith(currentSavePath)
                                        );
        }
    }


    /**
     * Synchronizes the data retrieved from the server about the contents of the target folder with the current data in
     * the local database.
     * <p>
     * Grants that mChildren is updated with fresh data after execution.
     *
     * @param folderAndFiles Remote folder and children files in Folder
     */
    private void synchronizeData(List<Object> folderAndFiles) {
        // get 'fresh data' from the database
        mLocalFolder = mStorageManager.getFileByPath(mLocalFolder.getRemotePath());

        if (mLocalFolder == null) {
            Log_OC.d(TAG,"mLocalFolder cannot be null");
            return;
        }

        // parse data from remote folder
        OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) folderAndFiles.get(0));
        remoteFolder.setParentId(mLocalFolder.getParentId());
        remoteFolder.setFileId(mLocalFolder.getFileId());

        Log_OC.d(TAG, "Remote folder " + mLocalFolder.getRemotePath() + " changed - starting update of local data ");

        List<OCFile> updatedFiles = new ArrayList<>(folderAndFiles.size() - 1);
        mFilesToSyncContents.clear();

        // if local folder is encrypted, download fresh metadata
        boolean encryptedAncestor = FileStorageUtils.checkEncryptionStatus(mLocalFolder, mStorageManager);
        mLocalFolder.setEncrypted(encryptedAncestor);

        // update permission
        mLocalFolder.setPermissions(remoteFolder.getPermissions());

        // update richWorkspace
        mLocalFolder.setRichWorkspace(remoteFolder.getRichWorkspace());

        // update eTag
        mLocalFolder.setEtag(remoteFolder.getEtag());

        // update size
        mLocalFolder.setFileLength(remoteFolder.getFileLength());

        Object object = null;
        if (mLocalFolder.isEncrypted()) {
            object = getDecryptedFolderMetadata(encryptedAncestor,
                                                mLocalFolder,
                                                getClient(),
                                                user,
                                                mContext);
        }

        if (CapabilityUtils.getCapability(mContext).getEndToEndEncryptionApiVersion().compareTo(E2EVersion.V2_0) >= 0) {
            if (encryptedAncestor && object == null) {
                throw new IllegalStateException("metadata is null!");
            }
        }

        // get current data about local contents of the folder to synchronize
        Map<String, OCFile> localFilesMap;
        E2EVersion e2EVersion;
        if (object instanceof DecryptedFolderMetadataFileV1) {
            e2EVersion = E2EVersion.V1_2;
            localFilesMap = prefillLocalFilesMap((DecryptedFolderMetadataFileV1) object,
                                                 mStorageManager.getFolderContent(mLocalFolder, false));
        } else {
            e2EVersion = E2EVersion.V2_0;
            localFilesMap = prefillLocalFilesMap((DecryptedFolderMetadataFile) object,
                                                 mStorageManager.getFolderContent(mLocalFolder, false));

            // update counter
            if (object != null) {
                mLocalFolder.setE2eCounter(((DecryptedFolderMetadataFile) object).getMetadata().getCounter());
            }
        }

        // loop to update every child
        OCFile remoteFile;
        OCFile localFile;
        OCFile updatedFile;
        RemoteFile remote;

        for (int i = 1; i < folderAndFiles.size(); i++) {
            /// new OCFile instance with the data from the server
            remote = (RemoteFile) folderAndFiles.get(i);
            remoteFile = FileStorageUtils.fillOCFile(remote);

            // new OCFile instance to merge fresh data from server with local state
            updatedFile = FileStorageUtils.fillOCFile(remote);
            updatedFile.setParentId(mLocalFolder.getFileId());

            // retrieve local data for the read file
            localFile = localFilesMap.remove(remoteFile.getRemotePath());

            // TODO better implementation is needed
            if (localFile == null) {
                localFile = mStorageManager.getFileByPath(updatedFile.getRemotePath());
            }

            // add to updatedFile data about LOCAL STATE (not existing in server)
            updatedFile.setLastSyncDateForProperties(mCurrentSyncTime);

            // keep thumbnail info
            if (!updatedFile.isUpdateThumbnailNeeded() && localFile != null && localFile.getImageDimension() != null) {
                updatedFile.setImageDimension(localFile.getImageDimension());
            }

            // add to updatedFile data from local and remote file
            setLocalFileDataOnUpdatedFile(remoteFile, localFile, updatedFile, mRemoteFolderChanged);

            // check and fix, if needed, local storage path
            FileStorageUtils.searchForLocalFileInDefaultPath(updatedFile, user.getAccountName());

            // update file name for encrypted files
            if (e2EVersion == E2EVersion.V1_2) {
                updateFileNameForEncryptedFileV1(mStorageManager,
                                                 (DecryptedFolderMetadataFileV1) object,
                                                 updatedFile);
            } else {
                updateFileNameForEncryptedFile(mStorageManager,
                                               (DecryptedFolderMetadataFile) object,
                                               updatedFile);
                if (localFile != null) {
                    updatedFile.setE2eCounter(localFile.getE2eCounter());
                }
            }

            // we parse content, so either the folder itself or its direct parent (which we check) must be encrypted
            boolean encrypted = updatedFile.isEncrypted() || mLocalFolder.isEncrypted();
            updatedFile.setEncrypted(encrypted);

            updatedFiles.add(updatedFile);
        }


        // save updated contents in local database
        // update file name for encrypted files
        if (e2EVersion == E2EVersion.V1_2) {
            updateFileNameForEncryptedFileV1(mStorageManager,
                                             (DecryptedFolderMetadataFileV1) object,
                                             mLocalFolder);
        } else {
            updateFileNameForEncryptedFile(mStorageManager,
                                           (DecryptedFolderMetadataFile) object,
                                           mLocalFolder);
        }
        mStorageManager.saveFolder(remoteFolder, updatedFiles, localFilesMap.values());

        mChildren = updatedFiles;
    }

    @Nullable
    public static Object getDecryptedFolderMetadata(boolean encryptedAncestor,
                                                    OCFile localFolder,
                                                    OwnCloudClient client,
                                                    User user,
                                                    Context context) {
        Object metadata;
        if (encryptedAncestor) {
            metadata = EncryptionUtils.downloadFolderMetadata(localFolder, client, context, user);
        } else {
            metadata = null;
        }
        return metadata;
    }

    @SuppressFBWarnings("CE")
    private static void setMimeTypeAndDecryptedRemotePath(OCFile updatedFile, FileDataStorageManager storageManager, String decryptedFileName, String mimetype) {
        OCFile parentFile = storageManager.getFileById(updatedFile.getParentId());

        if (parentFile == null) {
            throw new NullPointerException("parentFile cannot be null");
        }

        String decryptedRemotePath;
        if (decryptedFileName != null) {
            decryptedRemotePath = parentFile.getDecryptedRemotePath() + decryptedFileName;
        } else {
            decryptedRemotePath = parentFile.getRemotePath() + updatedFile.getFileName();
        }

        if (updatedFile.isFolder()) {
            decryptedRemotePath += "/";
        }
        updatedFile.setDecryptedRemotePath(decryptedRemotePath);

        if (mimetype == null || mimetype.isEmpty()) {
            if (updatedFile.isFolder()) {
                updatedFile.setMimeType(MimeType.DIRECTORY);
            } else {
                updatedFile.setMimeType("application/octet-stream");
            }
        } else {
            updatedFile.setMimeType(mimetype);
        }
    }

    public static void updateFileNameForEncryptedFileV1(FileDataStorageManager storageManager,
                                                        @NonNull DecryptedFolderMetadataFileV1 metadata,
                                                        OCFile updatedFile) {
        try {
            String decryptedFileName;
            String mimetype;

            if (updatedFile.isFolder()) {
                decryptedFileName = metadata.getFiles().get(updatedFile.getFileName()).getEncrypted().getFilename();
                mimetype = MimeType.DIRECTORY;
            } else {
                com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile decryptedFile =
                    metadata.getFiles().get(updatedFile.getFileName());

                if (decryptedFile == null) {
                    throw new NullPointerException("decryptedFile cannot be null");
                }

                decryptedFileName = decryptedFile.getEncrypted().getFilename();
                mimetype = decryptedFile.getEncrypted().getMimetype();
            }

            setMimeTypeAndDecryptedRemotePath(updatedFile, storageManager, decryptedFileName, mimetype);
        } catch (NullPointerException e) {
            Log_OC.e(TAG, "DecryptedMetadata for file " + updatedFile.getFileId() + " not found!");
        }
    }

    public static void updateFileNameForEncryptedFile(FileDataStorageManager storageManager,
                                                      @NonNull DecryptedFolderMetadataFile metadata,
                                                      OCFile updatedFile) {
        try {
            String decryptedFileName;
            String mimetype;

            if (updatedFile.isFolder()) {
                decryptedFileName = metadata.getMetadata().getFolders().get(updatedFile.getFileName());
                mimetype = MimeType.DIRECTORY;
            } else {
                DecryptedFile decryptedFile = metadata.getMetadata().getFiles().get(updatedFile.getFileName());

                if (decryptedFile == null) {
                    throw new NullPointerException("decryptedFile cannot be null");
                }

                decryptedFileName = decryptedFile.getFilename();
                mimetype = decryptedFile.getMimetype();
            }

            setMimeTypeAndDecryptedRemotePath(updatedFile, storageManager, decryptedFileName, mimetype);
        } catch (NullPointerException e) {
            Log_OC.e(TAG, "DecryptedMetadata for file " + updatedFile.getFileId() + " not found!");
        }
    }

    private void setLocalFileDataOnUpdatedFile(OCFile remoteFile, OCFile localFile, OCFile updatedFile, boolean remoteFolderChanged) {
        if (localFile != null) {
            updatedFile.setFileId(localFile.getFileId());
            updatedFile.setLastSyncDateForData(localFile.getLastSyncDateForData());
            updatedFile.setInternalFolderSyncTimestamp(localFile.getInternalFolderSyncTimestamp());
            updatedFile.setModificationTimestampAtLastSyncForData(
                localFile.getModificationTimestampAtLastSyncForData()
                                                                 );
            if (localFile.isEncrypted()) {
                if (mLocalFolder.getStoragePath() == null) {
                    updatedFile.setStoragePath(FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), mLocalFolder) +
                                                   localFile.getFileName());
                } else {
                    updatedFile.setStoragePath(mLocalFolder.getStoragePath() +
                                                   PATH_SEPARATOR +
                                                   localFile.getFileName());
                }
            } else {
                updatedFile.setStoragePath(localFile.getStoragePath());
            }

            // eTag will not be updated unless file CONTENTS are synchronized
            if (!updatedFile.isFolder() && localFile.isDown() &&
                !updatedFile.getEtag().equals(localFile.getEtag())) {
                updatedFile.setEtagInConflict(updatedFile.getEtag());
            }

            updatedFile.setEtag(localFile.getEtag());

            if (updatedFile.isFolder()) {
                updatedFile.setFileLength(remoteFile.getFileLength());
                updatedFile.setMountType(remoteFile.getMountType());
            } else if (remoteFolderChanged && MimeTypeUtil.isImage(remoteFile) &&
                remoteFile.getModificationTimestamp() !=
                    localFile.getModificationTimestamp()) {
                updatedFile.setUpdateThumbnailNeeded(true);
                Log_OC.d(TAG, "Image " + remoteFile.getFileName() + " updated on the server");
            }

            updatedFile.setSharedViaLink(localFile.isSharedViaLink());
            updatedFile.setSharedWithSharee(localFile.isSharedWithSharee());
        } else {
            // remote eTag will not be updated unless file CONTENTS are synchronized
            updatedFile.setEtag("");
        }

        // eTag on Server is used for thumbnail validation
        updatedFile.setEtagOnServer(remoteFile.getEtag());
    }

    @NonNull
    @SuppressFBWarnings("OCP")
    public static Map<String, OCFile> prefillLocalFilesMap(Object metadata, List<OCFile> localFiles) {
        Map<String, OCFile> localFilesMap = Maps.newHashMapWithExpectedSize(localFiles.size());

        for (OCFile file : localFiles) {
            String remotePath = file.getRemotePath();

            if (metadata != null) {
                remotePath = file.getParentRemotePath() + file.getEncryptedFileName();
                if (file.isFolder() && !remotePath.endsWith(PATH_SEPARATOR)) {
                    remotePath = remotePath + PATH_SEPARATOR;
                }
            }
            localFilesMap.put(remotePath, file);
        }
        return localFilesMap;
    }

    /**
     * Performs a list of synchronization operations, determining if a download or upload is needed or if exists
     * conflict due to changes both in local and remote contents of the each file.
     * <p>
     * If download or upload is needed, request the operation to the corresponding service and goes on.
     *
     * @param filesToSyncContents Synchronization operations to execute.
     */
    private void startContentSynchronizations(List<SynchronizeFileOperation> filesToSyncContents) {
        RemoteOperationResult contentsResult;
        for (SynchronizeFileOperation op : filesToSyncContents) {
            contentsResult = op.execute(mContext);   // async
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
     * @param client Handler of a session with an OC server.
     * @return The result of the remote operation retrieving the Share resources in the folder refreshed by the
     * operation.
     */
    private RemoteOperationResult refreshSharesForFolder(OwnCloudClient client) {
        RemoteOperationResult result;

        // remote request
        GetSharesForFileRemoteOperation operation =
            new GetSharesForFileRemoteOperation(mLocalFolder.getRemotePath(), true, true);
        result = operation.execute(client);

        if (result.isSuccess()) {
            // update local database
            ArrayList<OCShare> shares = new ArrayList<>();
            OCShare share;
            for (Object obj : result.getData()) {
                share = (OCShare) obj;

                if (ShareType.NO_SHARED != share.getShareType()) {
                    shares.add(share);
                }
            }
            mStorageManager.saveSharesInFolder(shares, mLocalFolder);
        }

        return result;
    }

    /**
     * Sends a message to any application component interested in the progress of the synchronization.
     *
     * @param event         broadcast event (Intent Action)
     * @param dirRemotePath Remote path of a folder that was just synchronized (with or without success)
     * @param result        remote operation result
     */
    private void sendLocalBroadcast(String event, String dirRemotePath, RemoteOperationResult result) {
        Log_OC.d(TAG, "Send broadcast " + event);
        Intent intent = new Intent(event);
        intent.putExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME, user.getAccountName());

        if (dirRemotePath != null) {
            intent.putExtra(FileSyncAdapter.EXTRA_FOLDER_PATH, dirRemotePath);
        }

        DataHolderUtil dataHolderUtil = DataHolderUtil.getInstance();
        String dataHolderItemId = dataHolderUtil.nextItemId();
        dataHolderUtil.save(dataHolderItemId, result);
        intent.putExtra(FileSyncAdapter.EXTRA_RESULT, dataHolderItemId);

        intent.setPackage(mContext.getPackageName());
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(intent);
    }
}
