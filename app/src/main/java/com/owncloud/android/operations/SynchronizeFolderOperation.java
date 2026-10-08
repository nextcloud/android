/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012-2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.nextcloud.client.account.User;
import com.nextcloud.utils.share.UnifiedShareSharees;
import com.nextcloud.client.jobs.download.FileDownloadHelper;
import com.nextcloud.client.jobs.folderDownload.FolderDownloadWorkerNotificationManager;
import com.nextcloud.utils.extensions.ExtensionsKt;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kotlin.Unit;

/**
 *  Remote operation performing the synchronization of the list of files contained
 *  in a folder identified with its remote path.
 *  Fetches the list and properties of the files contained in the given folder, including their
 *  properties, and updates the local database with them.
 *  Ongoing synchronization processes descendants on the calling background thread.
 */
public class SynchronizeFolderOperation extends SyncOperation {

    private static final String TAG = SynchronizeFolderOperation.class.getSimpleName();

    /** Remote path of the folder to synchronize */
    private String mRemotePath;

    /** Account where the file to synchronize belongs */
    private User user;

    /** Android context; necessary to send requests to the download service */
    private Context mContext;

    /** Locally cached information about folder to synchronize */
    private OCFile mLocalFolder;

    /**
     * 'True' means that the remote folder changed and should be fetched
     */
    private boolean mRemoteFolderChanged;

    private List<OCFile> mFilesForDirectDownload;
    // to avoid extra PROPFINDs when there was no change in the folder

    private List<SynchronizeFileOperation> mFilesToSyncContents;
    // this will be used for every file when 'folder synchronization' replaces 'folder download'

    private final AtomicBoolean mCancellationRequested;

    private final boolean useWorkerWithNotification;

    private final boolean syncAll;

    private final Consumer<String> subfolderScheduler;
    private final BooleanSupplier continueSynchronization;
    private final Deque<String> pendingSubfolders = new ArrayDeque<>();

    final FolderDownloadWorkerNotificationManager notificationManager;

    /**
     * Creates a new instance of {@link SynchronizeFolderOperation}.
     *
     * @param context         Application context.
     * @param remotePath      Path to synchronize.
     * @param user            Nextcloud account where the folder is located.
     */
    public SynchronizeFolderOperation(Context context,
                                      String remotePath,
                                      User user,
                                      FileDataStorageManager storageManager,
                                      boolean useWorkerWithNotification,
                                      boolean syncAll) {
        this(context, remotePath, user, storageManager, useWorkerWithNotification, syncAll,
             new AtomicBoolean(false),
             path -> startSyncFolderOperation(context, path, user, syncAll), () -> true);
    }

    public static SynchronizeFolderOperation forOngoingSync(Context context,
                                                           String remotePath,
                                                           User user,
                                                           FileDataStorageManager storageManager,
                                                           BooleanSupplier continueSynchronization) {
        return new SynchronizeFolderOperation(context, remotePath, user, storageManager, false, false,
                                              new AtomicBoolean(false), null, continueSynchronization);
    }

    private SynchronizeFolderOperation(Context context,
                                       String remotePath,
                                       User user,
                                       FileDataStorageManager storageManager,
                                       boolean useWorkerWithNotification,
                                       boolean syncAll,
                                       AtomicBoolean cancellationRequested,
                                       Consumer<String> subfolderScheduler,
                                       BooleanSupplier continueSynchronization) {
        super(storageManager);

        mRemotePath = remotePath;
        this.user = user;
        mContext = context;
        mRemoteFolderChanged = false;
        mFilesForDirectDownload = new Vector<>();
        mFilesToSyncContents = new Vector<>();
        mCancellationRequested = cancellationRequested;
        this.useWorkerWithNotification = useWorkerWithNotification;
        this.syncAll = syncAll;
        this.subfolderScheduler = subfolderScheduler;
        this.continueSynchronization = continueSynchronization;
        notificationManager = new FolderDownloadWorkerNotificationManager(context, false,null);
    }


    /**
     * Performs the synchronization.
     *
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        long startedAt = System.nanoTime();
        RemoteOperationResult result = null;
        mFilesForDirectDownload.clear();
        mFilesToSyncContents.clear();
        pendingSubfolders.clear();
        Log_OC.i(TAG, "SyncTrace start folder=" + mRemotePath + " notificationWorker=" +
                 useWorkerWithNotification + " syncAll=" + syncAll);

        try {
            // get locally cached information about folder
            mLocalFolder = getStorageManager().getFileByPath(mRemotePath);
            if (mLocalFolder == null) {
                Log_OC.e(TAG, "Local folder is null, cannot run synchronize folder operation, remote path: " + mRemotePath);
                result = new RemoteOperationResult<>(ResultCode.FILE_NOT_FOUND);
                return result;
            }

            result = checkForChanges(client);

            if (result.isSuccess()) {
                if (mRemoteFolderChanged || syncAll) {
                    result = fetchAndSyncRemoteFolder(client);
                } else {
                    prepareOpsFromLocalKnowledge();
                }

                if (result.isSuccess()) {
                    result = syncContents(client);
                }
            }

            if (subfolderScheduler == null && !pendingSubfolders.isEmpty()) {
                RemoteOperationResult descendantResult = synchronizeSubfolders(client);
                if (result.isSuccess()) {
                    result = descendantResult;
                }
            }

            if (isCancellationRequested()) {
                throw new OperationCancelledException();
            }

        } catch (OperationCancelledException e) {
            result = new RemoteOperationResult(e);
        } finally {
            Log_OC.i(TAG, "SyncTrace end folder=" + mRemotePath + " elapsedMs=" +
                     TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt) + " directFiles=" +
                     mFilesForDirectDownload.size() + " contentOperations=" + mFilesToSyncContents.size() +
                     " result=" + (result == null ? "exception" : result.getCode()));
        }

        return result;
    }

    private RemoteOperationResult synchronizeSubfolders(OwnCloudClient client) throws OperationCancelledException {
        RemoteOperationResult result = new RemoteOperationResult<>(ResultCode.OK);
        Set<String> visited = new HashSet<>(pendingSubfolders.size() + 1, 1.0f);
        visited.add(mRemotePath);

        while (!pendingSubfolders.isEmpty()) {
            if (isCancellationRequested()) {
                throw new OperationCancelledException();
            }
            String path = pendingSubfolders.removeLast();
            if (!visited.add(path)) {
                continue;
            }

            SynchronizeFolderOperation child = new SynchronizeFolderOperation(
                mContext, path, user, getStorageManager(), false, syncAll,
                mCancellationRequested, pendingSubfolders::addLast, continueSynchronization
            );
            RemoteOperationResult childResult = child.execute(client);
            if (result.isSuccess() && !childResult.isSuccess()) {
                result = childResult;
            }
        }
        return result;
    }

    @SuppressFBWarnings(
        value = "AT_STALE_THREAD_WRITE_OF_PRIMITIVE",
        justification = "Folder metadata is confined to the executing thread; cancellation uses an atomic flag."
    )
    private RemoteOperationResult checkForChanges(OwnCloudClient client) throws OperationCancelledException {
        Log_OC.d(TAG, "Checking changes in " + user.getAccountName() + mRemotePath);

        mRemoteFolderChanged = true;

        if (isCancellationRequested()) {
            throw new OperationCancelledException();
        }

        // remote request
        ReadFileRemoteOperation operation = new ReadFileRemoteOperation(mRemotePath);
        var result = operation.execute(client);
        if (result.isSuccess() && result.getData().get(0) instanceof RemoteFile remoteFile) {
            OCFile remoteFolder = FileStorageUtils.fillOCFile(remoteFile);

            // check if remote and local folder are different
            mRemoteFolderChanged = !(remoteFolder.getEtag().equalsIgnoreCase(mLocalFolder.getEtag()));

            result = new RemoteOperationResult<>(ResultCode.OK);

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
        if (isCancellationRequested()) {
            throw new OperationCancelledException();
        }

        ReadFolderRemoteOperation operation = new ReadFolderRemoteOperation(mRemotePath);
        var result = operation.execute(client);
        Log_OC.d(TAG, "Synchronizing " + user.getAccountName() + mRemotePath);
        Log_OC.d(TAG, "Synchronizing remote id" + mLocalFolder.getRemoteId());

        if (result.isSuccess()) {
            synchronizeData(result.getData());
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

        if (isCancellationRequested()) {
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

        Object object = RefreshFolderOperation.getDecryptedFolderMetadata(encryptedAncestor,
                                                                                                 mLocalFolder,
                                                                                                 getClient(),
                                                                                                 user,
                                                                                                 mContext);
        if (mLocalFolder.isEncrypted() && object == null) {
            throw new IllegalStateException("metadata is null!");
        }

        // get current data about local contents of the folder to synchronize
        Map<String, OCFile> localFilesMap = RefreshFolderOperation.prefillLocalFilesMap(object,storageManager.getFolderContent(mLocalFolder, false));

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
            if (object instanceof DecryptedFolderMetadataFileV1 metadataFile) {
                RefreshFolderOperation.updateFileNameForEncryptedFileV1(storageManager, metadataFile, updatedFile);
            } else if (object instanceof DecryptedFolderMetadataFile metadataFile) {
                RefreshFolderOperation.updateFileNameForEncryptedFile(storageManager, metadataFile, updatedFile);
            }

            // we parse content, so either the folder itself or its direct parent (which we check) must be encrypted
            boolean encrypted = updatedFile.isEncrypted() || mLocalFolder.isEncrypted();
            updatedFile.setEncrypted(encrypted);

            if (!updatedFile.isFolder() && updatedFile.isDown()) {
                syncFileOrFolder(remoteFile, localFile == null ? updatedFile : localFile);
            }

            updatedFiles.add(updatedFile);
        }

        // update file name for encrypted files
        if (object instanceof DecryptedFolderMetadataFileV1 metadataFile) {
            RefreshFolderOperation.updateFileNameForEncryptedFileV1(storageManager, metadataFile, mLocalFolder);
        } else if (object instanceof DecryptedFolderMetadataFile metadataFile) {
            RefreshFolderOperation.updateFileNameForEncryptedFile(storageManager, metadataFile, mLocalFolder);
        }

        // save updated contents in local database
        UnifiedShareSharees.fillBlocking(user, updatedFiles);

        storageManager.saveFolder(remoteFolder, updatedFiles, localFilesMap.values());
        mLocalFolder.setLastSyncDateForData(System.currentTimeMillis());
        storageManager.saveFile(mLocalFolder);

        // Child operations need their folder entries to exist in the database before they start.
        for (OCFile child : updatedFiles) {
            if (child.isFolder()) {
                syncFileOrFolder(child, null);
            } else if (!child.isDown()) {
                mFilesForDirectDownload.add(child);
            }
        }
    }

    private void updateLocalStateData(OCFile remoteFile, OCFile localFile, OCFile updatedFile) {
        updatedFile.setLastSyncDateForProperties(System.currentTimeMillis());
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

    /**
     * Schedules synchronization for the given remote file or folder.
     * <p>
     * If the remote file is a regular file, a {@link SynchronizeFileOperation} is created
     * and added to the list of pending file synchronizations.
     * If the remote file is a folder, the method triggers a folder synchronization operation,
     * which recursively synchronizes all nested files and subfolders.
     * </p>
     *
     * @param remoteFile the remote file or folder to synchronize
     * @param localFile the corresponding local file or folder
     * @throws OperationCancelledException if the synchronization was cancelled
     */
    private void syncFileOrFolder(OCFile remoteFile, OCFile localFile) throws OperationCancelledException {
        if (remoteFile.isFolder()) {
            if (isCancellationRequested()) {
                throw new OperationCancelledException();
            }
            scheduleSubfolderSynchronization(remoteFile.getRemotePath());
        } else {
            SynchronizeFileOperation operation = new SynchronizeFileOperation(
                localFile,
                remoteFile,
                user,
                true,
                mContext,
                getStorageManager(),
                useWorkerWithNotification
            );
            mFilesToSyncContents.add(operation);
        }
    }

    private void prepareOpsFromLocalKnowledge() throws OperationCancelledException {
        List<OCFile> children = getStorageManager().getFolderContent(mLocalFolder, false);
        for (OCFile child : children) {
            if (child.isFolder()) {
                syncFileOrFolder(child, child);
                continue;
            }

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
                    getStorageManager(),
                    useWorkerWithNotification
                );
                mFilesToSyncContents.add(operation);
            }
        }
    }

    private RemoteOperationResult syncContents(OwnCloudClient client) throws OperationCancelledException {
        RemoteOperationResult downloadResult = startDirectDownloads();
        if (isCancellationRequested()) {
            throw new OperationCancelledException();
        }
        RemoteOperationResult contentResult = startContentSynchronizations(mFilesToSyncContents);
        updateETag(client);
        return downloadResult.isSuccess() ? contentResult : downloadResult;
    }

    /**
     * Updates the eTag of the local folder after a successful synchronization.
     * This ensures that any changes to local files, which may alter the eTag, are correctly reflected.
     *
     * @param client the OwnCloudClient instance used to execute remote operations.
     */
    private void updateETag(OwnCloudClient client) {
        ReadFileRemoteOperation operation = new ReadFileRemoteOperation(mRemotePath);
        final var result = operation.execute(client);
        if (!result.isSuccess()) {
            Log_OC.w(TAG, "Cannot update eTag, read file operation failed");
            return;
        }

        if (result.getData().get(0) instanceof RemoteFile remoteFile) {
            String eTag = remoteFile.getEtag();
            mLocalFolder.setEtag(eTag);

            final FileDataStorageManager storageManager = getStorageManager();
            storageManager.saveFile(mLocalFolder);
        }
    }

    private RemoteOperationResult startDirectDownloads() throws OperationCancelledException {
        final var fileDownloadHelper = FileDownloadHelper.Companion.instance();
        RemoteOperationResult result = new RemoteOperationResult<>(ResultCode.OK);
        if (useWorkerWithNotification) {
            if (!mFilesForDirectDownload.isEmpty()) {
                fileDownloadHelper.downloadFolder(mLocalFolder, user.getAccountName());
            }
            return result;
        }

        for (OCFile file : mFilesForDirectDownload) {
            if (isCancellationRequested()) {
                throw new OperationCancelledException();
            }
            RemoteOperationResult downloadResult = downloadFile(file, fileDownloadHelper);
            if (result.isSuccess() && !downloadResult.isSuccess()) {
                result = downloadResult;
            }
        }
        return result;
    }

    private RemoteOperationResult downloadFile(OCFile file, FileDownloadHelper fileDownloadHelper) {
        try {
            final var operation = new DownloadFileOperation(user, file, mContext);
            var result = operation.execute(getClient());
            if (result.isSuccess()) {
                fileDownloadHelper.saveFile(file, operation, getStorageManager());
            } else {
                Log_OC.w(TAG, "SyncTrace download failed path=" + file.getRemotePath() +
                         " result=" + result.getCode());
            }
            return result;
        } catch (Exception e) {
            Log_OC.e(TAG, "SyncTrace direct download failed path=" + file.getRemotePath(), e);
            return new RemoteOperationResult(e);
        }
    }


    /**
     * Performs a list of synchronization operations, determining if a download or upload is needed
     * or if exists conflict due to changes both in local and remote contents of the each file.
     * <p>
     * If download or upload is needed, request the operation to the corresponding service and goes on.
     *
     * @param filesToSyncContents       Synchronization operations to execute.
     */
    private RemoteOperationResult startContentSynchronizations(List<SynchronizeFileOperation> filesToSyncContents)
        throws OperationCancelledException {
        RemoteOperationResult result = new RemoteOperationResult<>(ResultCode.OK);
        int total = filesToSyncContents.size();
        String folderName = mLocalFolder.getFileName();
        try {
            for (int current = 0; current < total; current++) {
                if (isCancellationRequested()) {
                    throw new OperationCancelledException();
                }
                final var operation = filesToSyncContents.get(current);
                final var fileResult = operation.execute(mContext);
                final var file = operation.getLocalFile();
                if (fileResult.isSuccess()) {
                    if (file != null) {
                        notificationManager.showProgressNotification(folderName, file.getFileName(), current, total);
                    }
                } else {
                    if (result.isSuccess()) {
                        result = fileResult;
                    }
                    Log_OC.e(TAG, "Error while synchronizing file: " + fileResult.getLogMessage());
                }
            }
            return result;
        } finally {
            ExtensionsKt.mainThread(0L, () -> {
                notificationManager.dismiss();
                return Unit.INSTANCE;
            });
        }
    }


    /**
     * Cancel operation
     */
    public void cancel() {
        mCancellationRequested.set(true);
    }

    public Optional<String> getFolderNameFromPath() {
        if (mLocalFolder == null) {
            return Optional.empty();
        }

        String path = mLocalFolder.getStoragePath();
        if (!TextUtils.isEmpty(path)) {
            File folder = new File(path);
            return Optional.of(folder.getName());
        }

        String filepath = FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), mLocalFolder);
        File folder = new File(filepath);
        return Optional.of(folder.getName());
    }

    private boolean isCancellationRequested() {
        return mCancellationRequested.get() || !continueSynchronization.getAsBoolean();
    }

    private static void startSyncFolderOperation(Context context, String path, User user, boolean syncAll) {
        Intent intent = new Intent(context, OperationsService.class);
        intent.setAction(OperationsService.ACTION_SYNC_FOLDER);
        intent.putExtra(OperationsService.EXTRA_ACCOUNT, user.toPlatformAccount());
        intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, path);
        intent.putExtra(OperationsService.EXTRA_SYNC_ALL, syncAll);
        context.startService(intent);
    }

    private void scheduleSubfolderSynchronization(String path) {
        if (subfolderScheduler != null) {
            subfolderScheduler.accept(path);
        } else {
            pendingSubfolders.addLast(path);
        }
    }

    public String getRemotePath() {
        return mRemotePath;
    }

    public String getAccountName() {
        return user.getAccountName();
    }

    public Long getFolderId() {
        if (mLocalFolder == null) {
            return null;
        }

        return mLocalFolder.getFileId();
    }
}
