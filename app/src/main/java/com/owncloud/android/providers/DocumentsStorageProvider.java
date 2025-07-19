/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019-2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.providers;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.widget.Toast;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.nextcloud.client.utils.HashUtil;
import com.nextcloud.common.SessionTimeOut;
import com.nextcloud.utils.extensions.ContextExtensionsKt;
import com.nextcloud.utils.fileNameValidator.FileNameValidator;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.CheckEtagRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.ui.activity.SettingsActivity;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.FileUtil;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.CapabilityUtils;

import org.nextcloud.providers.cursors.FileCursor;
import org.nextcloud.providers.cursors.RootCursor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import dagger.android.AndroidInjection;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static android.os.ParcelFileDescriptor.MODE_WRITE_ONLY;
import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

public class DocumentsStorageProvider extends DocumentsProvider {

    private static final String TAG = DocumentsStorageProvider.class.getSimpleName();

    private static final long CACHE_EXPIRATION = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);

    @Inject UserAccountManager accountManager;

    private boolean isFolderPathValid = true;

    @VisibleForTesting
    static final String DOCUMENTID_SEPARATOR = "/";
    private static final int DOCUMENTID_PARTS = 2;
    private final Map<String, FileDataStorageManager> rootIdToStorageManager = new HashMap<>();

    private final Executor executor = Executors.newCachedThreadPool();

    @Override
    public Cursor queryRoots(String[] projection) {

        // always recreate storage manager collection, as it will change after account creation/removal
        // and we need to serve document(tree)s with persist permissions
        initiateStorageMap();

        Context context = MainApp.getAppContext();
        AppPreferences preferences = AppPreferencesImpl.fromContext(context);
        if (SettingsActivity.LOCK_PASSCODE.equals(preferences.getLockPreference()) ||
            SettingsActivity.LOCK_DEVICE_CREDENTIALS.equals(preferences.getLockPreference())) {
            return new FileCursor();
        }

        final RootCursor result = new RootCursor(projection);
        for(FileDataStorageManager manager: rootIdToStorageManager.values()) {
            result.addRoot(new Document(manager, ROOT_PATH), getContext());
        }

        return result;
    }

    public static void notifyRootsChanged(Context context) {
        String authority = context.getString(R.string.document_provider_authority);
        Uri rootsUri = DocumentsContract.buildRootsUri(authority);
        context.getContentResolver().notifyChange(rootsUri, null);
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        Log_OC.d(TAG, "queryDocument(), id=" + documentId);

        Document document = toDocument(documentId);

        final FileCursor result = new FileCursor(projection);
        result.addFile(document);

        return result;
    }

    @SuppressLint("LongLogTag")
    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
        throws FileNotFoundException {
        Log_OC.d(TAG, "queryChildDocuments(), id=" + parentDocumentId);

        Context context = getNonNullContext();
        Document parentFolder = toDocument(parentDocumentId);
        final FileCursor resultCursor = new FileCursor(projection);

        if (parentFolder.getFile().isEncrypted() &&
            !FileOperationsHelper.isEndToEndEncryptionSetup(context, parentFolder.getUser())) {
            Toast.makeText(context, R.string.e2e_not_yet_setup, Toast.LENGTH_LONG).show();
            return resultCursor;
        }

        FileDataStorageManager storageManager = parentFolder.getStorageManager();


        for (OCFile file : storageManager.getFolderContent(parentFolder.getFile(), false)) {
            resultCursor.addFile(new Document(storageManager, file));
        }

        boolean isLoading = false;
        if (parentFolder.isExpired()) {
            final ReloadFolderDocumentTask task = new ReloadFolderDocumentTask(parentFolder, result ->
                context.getContentResolver().notifyChange(toNotifyUri(parentFolder), null, false));
            task.executeOnExecutor(executor);
            resultCursor.setLoadingTask(task);
            isLoading = true;
        }

        final Bundle extra = new Bundle();
        extra.putBoolean(DocumentsContract.EXTRA_LOADING, isLoading);
        resultCursor.setExtras(extra);
        resultCursor.setNotificationUri(context.getContentResolver(), toNotifyUri(parentFolder));
        return resultCursor;
    }

    @SuppressLint("LongLogTag")
    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal cancellationSignal)
        throws FileNotFoundException {
        Log_OC.d(TAG, "openDocument(), id=" + documentId);

        if (!isFolderPathValid) {
            Log_OC.d(TAG, "Folder path is not valid, operation is cancelled");
            return null;
        }

        Document document = toDocument(documentId);
        Context context = getNonNullContext();

        OCFile ocFile = document.getFile();
        User user = document.getUser();

        int accessMode = ParcelFileDescriptor.parseMode(mode);
        boolean writeOnly = (accessMode & MODE_WRITE_ONLY) != 0;
        boolean needsDownload = !ocFile.existsOnDevice() || (!writeOnly && hasServerChange(document));
        if (needsDownload) {
            if (ocFile.getLocalModificationTimestamp() > ocFile.getLastSyncDateForData()) {
                // TODO show a conflict notification with a pending intent that shows a ConflictResolveDialog
                Log_OC.w(TAG, "Conflict found!");
            } else {
                // dirty threading workaround for client apps which call openDocument on the main thread, thus causing
                // a NetworkOnMainThreadException
                final AtomicBoolean downloadResult = new AtomicBoolean(false);
                final Thread downloadThread = new Thread(() -> {
                    DownloadFileOperation downloadFileOperation = new DownloadFileOperation(user, ocFile, context);
                    RemoteOperationResult result = downloadFileOperation.execute(document.getClient());
                    if (!result.isSuccess()) {
                        if (ocFile.isDown()) {
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.post(() -> Toast.makeText(MainApp.getAppContext(),
                                                              R.string.file_not_synced,
                                                              Toast.LENGTH_SHORT).show());
                            downloadResult.set(true);
                        } else {
                            Log_OC.e(TAG, result.toString());
                        }
                    } else {
                        saveDownloadedFile(document.getStorageManager(), downloadFileOperation, ocFile);
                        downloadResult.set(true);
                    }
                });
                downloadThread.start();

                try {
                    downloadThread.join();
                    if (!downloadResult.get()) {
                        throw new FileNotFoundException("Error downloading file: " + ocFile.getFileName());
                    }
                } catch (InterruptedException e) {
                    throw new FileNotFoundException("Error downloading file: " + ocFile.getFileName());
                }
            }
        }

        File file = new File(ocFile.getStoragePath());

        if (accessMode != MODE_READ_ONLY) {
            // The calling thread is not guaranteed to have a Looper, so we can't block it with the OnCloseListener.
            // Thus, we are unable to do a synchronous upload and have to start an asynchronous one.
            Handler handler = new Handler(context.getMainLooper());
            try {
                return ParcelFileDescriptor.open(file, accessMode, handler, error -> {
                    if (error == null) {
                        // no error
                        // As we can't upload the file synchronously, let's at least update its metadata here already.
                        ocFile.setFileLength(file.length());
                        ocFile.setModificationTimestamp(System.currentTimeMillis());
                        document.getStorageManager().saveFile(ocFile);

                        // TODO disable upload notifications as DocumentsProvider users already show them
                        // upload file with FileUploader service (off main thread)
                        FileUploadHelper.Companion.instance().uploadUpdatedFile(
                            user,
                            new OCFile[]{ ocFile },
                            FileUploadWorker.LOCAL_BEHAVIOUR_DELETE,
                            NameCollisionPolicy.OVERWRITE);
                    } else {
                        // error, no upload needed
                        Log_OC.e(TAG, "File was closed with an error: " + ocFile.getFileName(), error);
                    }
                });
            } catch (IOException e) {
                throw new FileNotFoundException("Failed to open document for writing " + ocFile.getFileName());
            }
        } else {
            return ParcelFileDescriptor.open(file, accessMode);
        }
    }

    private boolean hasServerChange(Document document) throws FileNotFoundException {
        Context context = getNonNullContext();
        OCFile ocFile = document.getFile();
        RemoteOperationResult result = new CheckEtagRemoteOperation(ocFile.getRemotePath(), ocFile.getEtag(), new SessionTimeOut(2000,2000))
            .execute(document.getUser(), context);
        return switch (result.getCode()) {
            case ETAG_CHANGED -> result.getData() != null;
            case ETAG_UNCHANGED -> false;
            default -> {
                Log_OC.e(TAG, result.toString());
                throw new FileNotFoundException("Error synchronizing file: " + ocFile.getFileName());
            }
        };
    }

    /**
     * Updates the OC File after a successful download.
     *
     */
    private void saveDownloadedFile(FileDataStorageManager storageManager, DownloadFileOperation dfo, OCFile file) {
        long syncDate = System.currentTimeMillis();
        file.setLastSyncDateForProperties(syncDate);
        file.setLastSyncDateForData(syncDate);
        file.setUpdateThumbnailNeeded(true);
        file.setModificationTimestamp(dfo.getModificationTimestamp());
        file.setModificationTimestampAtLastSyncForData(dfo.getModificationTimestamp());
        file.setEtag(dfo.getEtag());
        file.setMimeType(dfo.getMimeType());
        String savePath = dfo.getSavePath();
        file.setStoragePath(savePath);
        file.setFileLength(new File(savePath).length());
        file.setRemoteId(dfo.getFile().getRemoteId());
        storageManager.saveFile(file);
        if (MimeTypeUtil.isMedia(dfo.getMimeType())) {
            FileDataStorageManager.triggerMediaScan(file.getStoragePath(), file);
        }
        storageManager.saveConflict(file, null);
    }

    @Override
    public boolean onCreate() {
        AndroidInjection.inject(this);

        // initiate storage manager collection, because we need to serve document(tree)s
        // with persist permissions
        initiateStorageMap();

        return true;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId,
                                                     Point sizeHint,
                                                     CancellationSignal signal)
        throws FileNotFoundException {
        Log_OC.d(TAG, "openDocumentThumbnail(), id=" + documentId);

        Document document = toDocument(documentId);
        OCFile file = document.getFile();

        boolean exists = ThumbnailsCacheManager.containsBitmap(ThumbnailsCacheManager.PREFIX_THUMBNAIL
                                                                   + file.getRemoteId());
        if (!exists) {
            ThumbnailsCacheManager.generateThumbnailFromOCFile(file, document.getUser(), getContext());
        }

        return new AssetFileDescriptor(DiskLruImageCacheFileProvider.getParcelFileDescriptorForOCFile(file),
                                       0,
                                       file.getFileLength());
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        Log_OC.d(TAG, "renameDocument(), id=" + documentId);

        String errorMessage = checkFileName(displayName);
        if (errorMessage != null) {
            ContextExtensionsKt.showToast(getNonNullContext(), errorMessage);
            return null;
        }

        Document document = toDocument(documentId);
        RemoteOperationResult result = new RenameFileOperation(document.getRemotePath(),
                                                               displayName,
                                                               document.getStorageManager())
            .execute(document.getClient());

        if (!result.isSuccess()) {
            Log_OC.e(TAG, result.toString());
            throw new FileNotFoundException("Failed to rename document with documentId " + documentId + ": " +
                                                result.getException());
        }

        Context context = getNonNullContext();
        context.getContentResolver().notifyChange(toNotifyUri(document.getParent()), null, false);

        return null;
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        Log_OC.d(TAG, "copyDocument(), id=" + sourceDocumentId);

        Document targetFolder = toDocument(targetParentDocumentId);

        String filename = targetFolder.getFile().getFileName();
        isFolderPathValid = checkFolderPath(filename);
        if (!isFolderPathValid) {
            ContextExtensionsKt.showToast(getNonNullContext(), R.string.file_name_validator_error_contains_reserved_names_or_invalid_characters);
            return null;
        }

        Document document = toDocument(sourceDocumentId);
        FileDataStorageManager storageManager = document.getStorageManager();
        RemoteOperationResult result = new CopyFileOperation(document.getRemotePath(),
                                                             targetFolder.getRemotePath(),
                                                             document.getStorageManager())
            .execute(document.getClient());

        if (!result.isSuccess()) {
            Log_OC.e(TAG, result.toString());
            throw new FileNotFoundException("Failed to copy document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        Context context = getNonNullContext();
        User user = document.getUser();

        RemoteOperationResult updateParent = new RefreshFolderOperation(targetFolder.getFile(),
                                                                        System.currentTimeMillis(),
                                                                        false,
                                                                        false,
                                                                        true,
                                                                        storageManager,
                                                                        user,
                                                                        context)
            .execute(targetFolder.getClient());

        if (!updateParent.isSuccess()) {
            Log_OC.e(TAG, updateParent.toString());
            throw new FileNotFoundException("Failed to copy document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        String newPath = targetFolder.getRemotePath() + document.getFile().getFileName();

        if (document.getFile().isFolder()) {
            newPath = newPath + PATH_SEPARATOR;
        }
        Document newFile = new Document(storageManager, newPath);

        context.getContentResolver().notifyChange(toNotifyUri(targetFolder), null, false);

        return newFile.getDocumentId();
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId)
        throws FileNotFoundException {
        Log_OC.d(TAG, "moveDocument(), id=" + sourceDocumentId);

        Document targetFolder = toDocument(targetParentDocumentId);

        String filename = targetFolder.getFile().getFileName();
        isFolderPathValid = checkFolderPath(filename);
        if (!isFolderPathValid) {
            ContextExtensionsKt.showToast(getNonNullContext(), R.string.file_name_validator_error_contains_reserved_names_or_invalid_characters);
            return null;
        }

        Document document = toDocument(sourceDocumentId);
        RemoteOperationResult result = new MoveFileOperation(document.getRemotePath(),
                                                             targetFolder.getRemotePath(),
                                                             document.getStorageManager())
            .execute(document.getClient());

        if (!result.isSuccess()) {
            Log_OC.e(TAG, result.toString());
            throw new FileNotFoundException("Failed to move document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        Document sourceFolder = toDocument(sourceParentDocumentId);

        Context context = getNonNullContext();
        context.getContentResolver().notifyChange(toNotifyUri(sourceFolder), null, false);
        context.getContentResolver().notifyChange(toNotifyUri(targetFolder), null, false);

        return sourceDocumentId;
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) {
        Log_OC.d(TAG, "querySearchDocuments(), rootId=" + rootId);

        FileCursor result = new FileCursor(projection);

        FileDataStorageManager storageManager = getStorageManager(rootId);
        if (storageManager == null) {
            return result;
        }

        for (Document d : findFiles(new Document(storageManager, ROOT_PATH), query)) {
            result.addFile(d);
        }

        return result;
    }

    private OCCapability getCapabilities() {
        return CapabilityUtils.getCapability(accountManager.getUser(), getNonNullContext());
    }

    private boolean checkFolderPath(String filename) {
        return FileNameValidator.INSTANCE.checkFolderPath(filename, getCapabilities(), getNonNullContext());
    }

    private String checkFileName(String filename) {
        return FileNameValidator.INSTANCE.checkFileName(filename, getCapabilities(), getNonNullContext(),null);
    }

    @Override
    public String createDocument(String documentId, String mimeType, String displayName) throws FileNotFoundException {
        Log_OC.d(TAG, "createDocument(), id=" + documentId);

        String errorMessage = checkFileName(displayName);
        if (errorMessage != null) {
            ContextExtensionsKt.showToast(getNonNullContext(), errorMessage);
            return null;
        }

        Document folderDocument = toDocument(documentId);

        if (DocumentsContract.Document.MIME_TYPE_DIR.equalsIgnoreCase(mimeType)) {
            return createFolder(folderDocument, displayName);
        } else {
            return createFile(folderDocument, displayName, mimeType);
        }
    }

    private String createFolder(Document targetFolder, String displayName) throws FileNotFoundException {

        Context context = getNonNullContext();
        String newDirPath = targetFolder.getRemotePath() + displayName + PATH_SEPARATOR;
        FileDataStorageManager storageManager = targetFolder.getStorageManager();

        RemoteOperationResult result = new CreateFolderOperation(newDirPath,
                                                                 accountManager.getUser(),
                                                                 context,
                                                                 storageManager)
            .execute(targetFolder.getClient());

        if (!result.isSuccess()) {
            Log_OC.e(TAG, result.toString());
            throw new FileNotFoundException("Failed to create document with name " +
                                                displayName + " and documentId " + targetFolder.getDocumentId());
        }

        RemoteOperationResult updateParent = new RefreshFolderOperation(targetFolder.getFile(), System.currentTimeMillis(),
                                                                        false, false, true, storageManager,
                                                                        targetFolder.getUser(), context)
            .execute(targetFolder.getClient());

        if (!updateParent.isSuccess()) {
            Log_OC.e(TAG, updateParent.toString());
            throw new FileNotFoundException("Failed to create document with documentId " + targetFolder.getDocumentId());
        }

        Document newFolder = new Document(storageManager, newDirPath);

        context.getContentResolver().notifyChange(toNotifyUri(targetFolder), null, false);

        return newFolder.getDocumentId();
    }

    private String createFile(Document targetFolder, String displayName, String mimeType) throws FileNotFoundException {

        User user = targetFolder.getUser();

        // create dummy file
        File tempDir = new File(FileStorageUtils.getTemporalPath(user.getAccountName()));

        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new FileNotFoundException("Temp folder could not be created: " + tempDir.getAbsolutePath());
        }

        File emptyFile = new File(tempDir, displayName);

        if (emptyFile.exists() && !emptyFile.delete()) {
            throw new FileNotFoundException("Previous file could not be deleted");
        }

        try {
            if (!emptyFile.createNewFile()) {
                throw new FileNotFoundException("File could not be created");
            }
        } catch (IOException e) {
            throw getFileNotFoundExceptionWithCause("File could not be created", e);
        }

        String newFilePath = targetFolder.getRemotePath() + displayName;

        // FIXME we need to update the mimeType somewhere else as well

        // perform the upload, no need for chunked operation as we have a empty file
        OwnCloudClient client = targetFolder.getClient();
        RemoteOperationResult result = new UploadFileRemoteOperation(emptyFile.getAbsolutePath(),
                                                                     newFilePath,
                                                                     mimeType,
                                                                     "",
                                                                     System.currentTimeMillis() / 1000,
                                                                     FileUtil.getCreationTimestamp(emptyFile),
                                                                     false)
            .execute(client);

        if (!result.isSuccess()) {
            Log_OC.e(TAG, result.toString());
            throw new FileNotFoundException("Failed to upload document with path " + newFilePath);
        }

        Context context = getNonNullContext();

        RemoteOperationResult updateParent = new RefreshFolderOperation(targetFolder.getFile(),
                                                                        System.currentTimeMillis(),
                                                                        false,
                                                                        false,
                                                                        true,
                                                                        targetFolder.getStorageManager(),
                                                                        user,
                                                                        context)
            .execute(client);

        if (!updateParent.isSuccess()) {
            Log_OC.e(TAG, updateParent.toString());
            throw new FileNotFoundException("Failed to create document with documentId " + targetFolder.getDocumentId());
        }

        Document newFile = new Document(targetFolder.getStorageManager(), newFilePath);

        context.getContentResolver().notifyChange(toNotifyUri(targetFolder), null, false);

        return newFile.getDocumentId();
    }

    @Override
    public void removeDocument(String documentId, String parentDocumentId) throws FileNotFoundException {
        deleteDocument(documentId);
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        Log_OC.d(TAG, "deleteDocument(), id=" + documentId);

        Context context = getNonNullContext();

        Document document = toDocument(documentId);
        // get parent here, because it is not available anymore after the document was deleted
        Document parentFolder = document.getParent();

        recursiveRevokePermission(document);

        OCFile file = document.getStorageManager().getFileByPath(document.getRemotePath());
        RemoteOperationResult result = new RemoveFileOperation(file,
                                                               false,
                                                               document.getUser(),
                                                               true,
                                                               context,
                                                               document.getStorageManager())
            .execute(document.getClient());

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to delete document with documentId " + documentId);
        }
        context.getContentResolver().notifyChange(toNotifyUri(parentFolder), null, false);
    }

    private void recursiveRevokePermission(Document document) {
        FileDataStorageManager storageManager = document.getStorageManager();
        OCFile file = document.getFile();
        if (file.isFolder()) {
            for (OCFile child : storageManager.getFolderContent(file, false)) {
                recursiveRevokePermission(new Document(storageManager, child));
            }
        }

        revokeDocumentPermission(document.getDocumentId());
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        Log_OC.d(TAG, "isChildDocument(), parent=" + parentDocumentId + ", id=" + documentId);

        try {
            // get file for parent document
            Document parentDocument = toDocument(parentDocumentId);
            OCFile parentFile = parentDocument.getFile();
            if (parentFile == null) {
                throw new FileNotFoundException("No parent file with ID " + parentDocumentId);
            }
            // get file for child candidate document
            Document currentDocument = toDocument(documentId);
            OCFile childFile = currentDocument.getFile();
            if (childFile == null) {
                throw new FileNotFoundException("No child file with ID " + documentId);
            }

            String parentPath = parentFile.getDecryptedRemotePath();
            String childPath = childFile.getDecryptedRemotePath();

            // The alternative is to go up the folder hierarchy from currentDocument with getParent()
            // until we arrive at parentDocument or the storage root.
            // However, especially for long paths this is expensive and can take substantial time.
            // The solution below uses paths and is faster by a factor of 2-10 depending on the nesting level of child.
            // So far, the same document with its unique ID can never be in two places at once.
            // If this assumption ever changes, this code would need to be adapted.
            User parentDocumentOwner = parentDocument.getUser();
            User currentDocumentOwner = currentDocument.getUser();
            return parentDocumentOwner.nameEquals(currentDocumentOwner) && childPath.startsWith(parentPath);

        } catch (FileNotFoundException e) {
            Log_OC.e(TAG, "failed to check for child document", e);
        }

        return false;
    }

    private FileNotFoundException getFileNotFoundExceptionWithCause(String msg, Exception cause) {
        FileNotFoundException e = new FileNotFoundException(msg);
        e.initCause(cause);
        return e;
    }

    private FileDataStorageManager getStorageManager(String rootId) {
        return rootIdToStorageManager.get(rootId);
    }

    @VisibleForTesting
    public static String rootIdForUser(User user) {
        return HashUtil.md5Hash(user.getAccountName());
    }

    private void initiateStorageMap() {

        rootIdToStorageManager.clear();

        ContentResolver contentResolver = getContext().getContentResolver();

        for (User user : accountManager.getAllUsers()) {
            final FileDataStorageManager storageManager = new FileDataStorageManager(user, contentResolver);
            rootIdToStorageManager.put(rootIdForUser(user), storageManager);
        }
    }

    private List<Document> findFiles(Document root, String query) {
        FileDataStorageManager storageManager = root.getStorageManager();
        List<Document> result = new ArrayList<>();
        for (OCFile f : storageManager.getFolderContent(root.getFile(), false)) {
            if (f.isFolder()) {
                result.addAll(findFiles(new Document(storageManager, f), query));
            } else if (f.getFileName().contains(query)) {
                result.add(new Document(storageManager, f));
            }
        }
        return result;
    }

    private Uri toNotifyUri(Document document) {
        return DocumentsContract.buildDocumentUri(
            getContext().getString(R.string.document_provider_authority),
            document.getDocumentId());
    }

    private Document toDocument(String documentId) throws FileNotFoundException {
        String[] separated = documentId.split(DOCUMENTID_SEPARATOR, DOCUMENTID_PARTS);
        if (separated.length != DOCUMENTID_PARTS) {
            throw new FileNotFoundException("Invalid documentID " + documentId + "!");
        }

        FileDataStorageManager storageManager = rootIdToStorageManager.get(separated[0]);
        if (storageManager == null) {
            throw new FileNotFoundException("No storage manager associated for " + documentId + "!");
        }

        return new Document(storageManager, Long.parseLong(separated[1]));
    }

    /**
     * Returns a {@link Context} guaranteed to be non-null.
     *
     * @throws IllegalStateException if called before {@link #onCreate()}.
     */
    @NonNull
    private Context getNonNullContext() {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

    public interface OnTaskFinishedCallback {
        void onTaskFinished(RemoteOperationResult result);
    }

    static class ReloadFolderDocumentTask extends AsyncTask<Void, Void, RemoteOperationResult> {

        private final Document folder;
        private final OnTaskFinishedCallback callback;

        ReloadFolderDocumentTask(Document folder, OnTaskFinishedCallback callback) {
            this.folder = folder;
            this.callback = callback;
        }

        @Override
        public final RemoteOperationResult doInBackground(Void... params) {
            Log_OC.d(TAG, "run ReloadFolderDocumentTask(), id=" + folder.getDocumentId());
            return new RefreshFolderOperation(folder.getFile(),
                                              System.currentTimeMillis(),
                                              false,
                                              true,
                                              true,
                                              folder.getStorageManager(),
                                              folder.getUser(),
                                              MainApp.getAppContext())
                .execute(folder.getClient());
        }

        @Override
        public final void onPostExecute(RemoteOperationResult result) {
            if (callback != null) {
                callback.onTaskFinished(result);
            }
        }
    }

    public class Document {
        private final FileDataStorageManager storageManager;
        private final long fileId;

        Document(FileDataStorageManager storageManager, long fileId) {
            this.storageManager = storageManager;
            this.fileId = fileId;
        }

        Document(FileDataStorageManager storageManager, OCFile file) {
            this.storageManager = storageManager;
            this.fileId = file.getFileId();
        }

        Document(FileDataStorageManager storageManager, String filePath) {
            this.storageManager = storageManager;
            this.fileId = storageManager.getFileByPath(filePath).getFileId();
        }

        public String getDocumentId() {
            for(String key: rootIdToStorageManager.keySet()) {
                if (Objects.equals(storageManager, rootIdToStorageManager.get(key))) {
                    return key + DOCUMENTID_SEPARATOR + fileId;
                }
            }
            return null;
        }

        FileDataStorageManager getStorageManager() {
            return storageManager;
        }

        public User getUser() {
            return getStorageManager().getUser();
        }

        public OCFile getFile() {
            return getStorageManager().getFileById(fileId);
        }

        public String getRemotePath() {
            return getFile().getRemotePath();
        }

        OwnCloudClient getClient() {
            try {

                OwnCloudAccount ocAccount = getUser().toOwnCloudAccount();
                return OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, getContext());
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                Log_OC.e(TAG, "Failed to set client", e);
            }
            return null;
        }

        boolean isExpired() {
            return getFile().getLastSyncDateForData() + CACHE_EXPIRATION < System.currentTimeMillis();
        }

        Document getParent() {
            long parentId = getFile().getParentId();
            if (parentId <= 0) {
                return null;
            }

            return new Document(getStorageManager(), parentId);
        }
    }
}
