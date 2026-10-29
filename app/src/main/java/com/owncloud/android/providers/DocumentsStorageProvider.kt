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
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.nextcloud.client.utils.HashUtil;
import com.nextcloud.utils.AppConstants;
import com.nextcloud.utils.extensions.ContextExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.nextcloud.utils.fileNameValidator.FileNameValidator;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.services.NameCollisionPolicy;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import dagger.android.AndroidInjection;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static android.os.ParcelFileDescriptor.MODE_TRUNCATE;
import static android.os.ParcelFileDescriptor.MODE_WRITE_ONLY;
import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

public class DocumentsStorageProvider extends DocumentsProvider {
    private static final String TAG = DocumentsStorageProvider.class.getSimpleName();

    private static final long CACHE_EXPIRATION = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);

    @Inject UserAccountManager accountManager;

    private volatile boolean isFolderPathValid = true;

    @VisibleForTesting
    static final String DOCUMENTID_SEPARATOR = "/";
    private static final int DOCUMENTID_PARTS = 2;
    private volatile Map<String, FileDataStorageManager> rootIdToStorageManager = new HashMap<>();

    private final Executor executor = Executors.newCachedThreadPool();

    @Override
    public boolean onCreate() {
        AndroidInjection.inject(this);
        initStoragePathIfMissing();

        initiateStorageMap();

        return true;
    }

    private void initStoragePathIfMissing() {
        if (MainApp.getStoragePath() != null) {
            return;
        }

        Context context = getNonNullContext();
        String defaultStoragePath = context.getFilesDir().getAbsolutePath();
        MainApp.setStoragePath(AppPreferencesImpl.fromContext(context).getStoragePath(defaultStoragePath));
    }

    public static void notifyRootsChanged(Context context) {
        String authority = context.getString(R.string.document_provider_authority);
        Uri rootsUri = DocumentsContract.buildRootsUri(authority);
        context.getContentResolver().notifyChange(rootsUri, null);
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        final long token = Binder.clearCallingIdentity();
        try {
            initiateStorageMap();

            if (isAppLocked()) {
                return new FileCursor();
            }

            final RootCursor result = new RootCursor(projection);
            for (FileDataStorageManager manager : rootIdToStorageManager.values()) {
                result.addRoot(new Document(manager, ROOT_PATH), getContext());
            }
            return result;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isAppLocked() {
        String lockPreference = AppPreferencesImpl.fromContext(MainApp.getAppContext()).getLockPreference();
        return SettingsActivity.LOCK_PASSCODE.equals(lockPreference) ||
            SettingsActivity.LOCK_DEVICE_CREDENTIALS.equals(lockPreference);
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        Log_OC.d(TAG, "queryDocument(), id=" + documentId);

        final long token = Binder.clearCallingIdentity();
        try {
            Document document = toDocument(documentId);
            document.requireFile();

            final FileCursor result = new FileCursor(projection);
            result.addFile(document);
            return result;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
        throws FileNotFoundException {
        Log_OC.d(TAG, "queryChildDocuments(), id=" + parentDocumentId);

        final long token = Binder.clearCallingIdentity();
        try {
            Context context = getNonNullContext();
            Document parentFolder = toDocument(parentDocumentId);
            OCFile parentFile = parentFolder.requireFile();
            final FileCursor resultCursor = new FileCursor(projection);

            if (!canListChildren(context, parentFolder, parentFile)) {
                return resultCursor;
            }

            addReadableChildren(resultCursor, parentFolder.getStorageManager(), parentFile);
            boolean isLoading = startReloadIfExpired(parentFolder, parentFile, resultCursor);

            final Bundle extra = new Bundle();
            extra.putBoolean(DocumentsContract.EXTRA_LOADING, isLoading);
            resultCursor.setExtras(extra);
            resultCursor.setNotificationUri(context.getContentResolver(), toNotifyUri(parentFolder));
            return resultCursor;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean canListChildren(Context context, Document folder, OCFile folderFile) {
        if (!folderFile.canRead()) {
            showToast(R.string.document_storage_provider_cannot_read);
            return false;
        }

        if (folderFile.isEncrypted() && !FileOperationsHelper.isEndToEndEncryptionSetup(context, folder.getUser())) {
            showToast(R.string.e2e_not_yet_setup);
            return false;
        }

        return true;
    }

    private void addReadableChildren(FileCursor cursor, FileDataStorageManager storageManager, OCFile folderFile) {
        for (OCFile file : storageManager.getFolderContent(folderFile, false)) {
            if (!file.canRead()) {
                Log_OC.w(TAG, "Skipping file, doesn't have read permission. RemotePath: " + file.getRemotePath());
                continue;
            }

            cursor.addFile(new Document(storageManager, file));
        }
    }

    private boolean startReloadIfExpired(Document folder, OCFile folderFile, FileCursor cursor) {
        if (folderFile.getLastSyncDateForData() + CACHE_EXPIRATION >= System.currentTimeMillis()) {
            return false;
        }

        final FutureTask<Void> task = new FutureTask<>(() -> reloadFolder(folder), null) {
            @Override
            protected void done() {
                if (!isCancelled()) {
                    notifyChanged(folder);
                }
            }
        };
        executor.execute(task);
        cursor.setLoadingTask(task);
        return true;
    }

    private void reloadFolder(Document folder) {
        Log_OC.d(TAG, "reloadFolder(), id=" + folder.getDocumentId());
        try {
            new RefreshFolderOperation(folder.requireFile(),
                                       System.currentTimeMillis(),
                                       false,
                                       true,
                                       true,
                                       folder.getStorageManager(),
                                       folder.getUser(),
                                       MainApp.getAppContext())
                .execute(folder.requireClient());
        } catch (FileNotFoundException e) {
            Log_OC.e(TAG, "reloadFolder() failed, id=" + folder.getDocumentId(), e);
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal cancellationSignal)
        throws FileNotFoundException {
        final int callerUid = Binder.getCallingUid();
        Log_OC.d(TAG, "openDocument(), id=" + documentId + ", mode=" + mode + ", callerUid=" + callerUid);

        if (!isFolderPathValid) {
            Log_OC.d(TAG, "Folder path is not valid, operation is cancelled");
            return null;
        }

        final long token = Binder.clearCallingIdentity();
        try {
            Document document = toDocument(documentId);
            Context context = getNonNullContext();

            OCFile ocFile = document.requireFile();
            if (ocFile.isFolder()) {
                Log_OC.w(TAG, "openDocument called on a folder, which is not openable: " + documentId);
                return null;
            }

            AccessMode accessMode = AccessMode.parse(mode);
            prepareLocalFile(context, document, ocFile, accessMode);
            OpenedDocumentFile openedFile =
                createOpenedDocumentFile(context, ocFile, accessMode, callerUid == Process.SYSTEM_UID);

            if (accessMode.isReadOnly()) {
                return openForRead(openedFile);
            }
            return openForWrite(context, document, ocFile, openedFile, accessMode);
        } catch (FileNotFoundException | RuntimeException e) {
            Log_OC.e(TAG, "openDocument() failed, id=" + documentId + ", mode=" + mode, e);
            throw e;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void prepareLocalFile(Context context, Document document, OCFile ocFile, AccessMode accessMode)
        throws FileNotFoundException {
        if (accessMode.isTruncate() && !ocFile.existsOnDevice()) {
            assignDefaultStoragePath(document.getUser(), ocFile);
        }

        boolean needsDownload = !accessMode.isTruncate() &&
            (!ocFile.existsOnDevice() || (!accessMode.isWriteOnly() && hasServerChange(context, document, ocFile)));
        Log_OC.d(TAG, "openDocument(), remotePath=" + ocFile.getRemotePath() + ", storagePath=" +
            ocFile.getStoragePath() + ", existsOnDevice=" + ocFile.existsOnDevice() + ", truncate=" +
            accessMode.isTruncate() + ", needsDownload=" + needsDownload);

        if (!needsDownload) {
            return;
        }

        if (ocFile.getLocalModificationTimestamp() > ocFile.getLastSyncDateForData()) {
            Log_OC.w(TAG, "Conflict found!");
            return;
        }

        downloadOffBinderThread(context, document, ocFile);
    }

    private void assignDefaultStoragePath(User user, OCFile ocFile) throws FileNotFoundException {
        File localFile = new File(FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), ocFile));
        File parent = localFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new FileNotFoundException("Local folder could not be created: " + parent.getAbsolutePath());
        }
        ocFile.setStoragePath(localFile.getAbsolutePath());
    }

    private boolean hasServerChange(Context context, Document document, OCFile ocFile) throws FileNotFoundException {
        RemoteOperationResult result = new CheckEtagRemoteOperation(ocFile.getRemotePath(), ocFile.getEtag())
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

    private void downloadOffBinderThread(Context context, Document document, OCFile ocFile)
        throws FileNotFoundException {
        FutureTask<Boolean> download = new FutureTask<>(() -> downloadFile(context, document, ocFile));
        executor.execute(download);
        try {
            if (!download.get()) {
                throw new FileNotFoundException("Error downloading file: " + ocFile.getFileName());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw getFileNotFoundExceptionWithCause("Error downloading file: " + ocFile.getFileName(), e);
        }
    }

    private boolean downloadFile(Context context, Document document, OCFile ocFile) throws FileNotFoundException {
        var downloadFileOperation = new DownloadFileOperation(document.getUser(), ocFile, context);
        final var result = downloadFileOperation.execute(document.requireClient());
        if (result.isSuccess()) {
            saveDownloadedFile(document.getStorageManager(), downloadFileOperation, ocFile);
            return true;
        }

        if (ocFile.isDown()) {
            new Handler(Looper.getMainLooper()).post(() -> showToast(R.string.file_not_synced));
            return true;
        }

        Log_OC.e(TAG, result.toString());
        return false;
    }

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

    private OpenedDocumentFile createOpenedDocumentFile(Context context,
                                                        OCFile ocFile,
                                                        AccessMode accessMode,
                                                        boolean isSystemServerCaller) throws FileNotFoundException {
        String storagePath = ocFile.getStoragePath();
        if (storagePath == null || storagePath.isEmpty()) {
            throw new FileNotFoundException("No local file for " + ocFile.getRemotePath());
        }

        File localFile = new File(storagePath);
        if (!isSystemServerCaller) {
            return new OpenedDocumentFile(localFile, localFile);
        }

        return new OpenedDocumentFile(localFile,
                                      createSystemServerAccessibleCopy(context, localFile, accessMode.isTruncate()));
    }

    private File createSystemServerAccessibleCopy(Context context, File file, boolean truncate)
        throws FileNotFoundException {
        try {
            File copy = File.createTempFile("document", null, context.getCacheDir());
            if (!truncate && file.exists()) {
                Files.copy(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return copy;
        } catch (IOException e) {
            throw getFileNotFoundExceptionWithCause("Failed to create copy of " + file.getAbsolutePath(), e);
        }
    }

    private ParcelFileDescriptor openForRead(OpenedDocumentFile openedFile) throws FileNotFoundException {
        ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(openedFile.descriptorFile(), MODE_READ_ONLY);
        openedFile.deleteCopy();
        return descriptor;
    }

    private ParcelFileDescriptor openForWrite(Context context,
                                              Document document,
                                              OCFile ocFile,
                                              OpenedDocumentFile openedFile,
                                              AccessMode accessMode) throws FileNotFoundException {
        Handler handler = new Handler(context.getMainLooper());
        try {
            return ParcelFileDescriptor.open(openedFile.descriptorFile(), accessMode.bits(), handler, error -> {
                if (error != null) {
                    Log_OC.e(TAG, "File was closed with an error: " + ocFile.getFileName(), error);
                    openedFile.deleteCopy();
                    return;
                }

                executor.execute(() -> uploadWrittenDocument(document, ocFile, openedFile));
            });
        } catch (IOException e) {
            throw getFileNotFoundExceptionWithCause("Failed to open document for writing " +
                                                        openedFile.descriptorFile().getAbsolutePath(), e);
        }
    }

    private void uploadWrittenDocument(Document document, OCFile ocFile, OpenedDocumentFile openedFile) {
        File file = openedFile.localFile();
        try {
            openedFile.moveCopyToLocalFile();
        } catch (IOException e) {
            Log_OC.e(TAG, "Failed to move written copy to " + file.getAbsolutePath(), e);
            openedFile.deleteCopy();
            return;
        }

        Log_OC.d(TAG, "openDocument(), closed " + ocFile.getRemotePath() + ", length=" + file.length() +
            ", starting upload");
        ocFile.setFileLength(file.length());
        ocFile.setModificationTimestamp(System.currentTimeMillis());
        document.getStorageManager().saveFile(ocFile);

        FileUploadHelper.Companion.instance().uploadUpdatedFile(
            document.getUser(),
            new OCFile[]{ ocFile },
            FileUploadWorker.LOCAL_BEHAVIOUR_DELETE,
            NameCollisionPolicy.OVERWRITE);
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId,
                                                     Point sizeHint,
                                                     CancellationSignal signal)
        throws FileNotFoundException {
        Log_OC.d(TAG, "openDocumentThumbnail(), id=" + documentId);

        final long token = Binder.clearCallingIdentity();
        try {
            Document document = toDocument(documentId);
            OCFile file = document.requireFile();

            if (!ThumbnailsCacheManager.containsBitmap(FileExtensionsKt.getSmallThumbnailKey(file))) {
                ThumbnailsCacheManager.generateThumbnailFromOCFile(file, document.getUser(), getContext());
            }

            return new AssetFileDescriptor(DiskLruImageCacheFileProvider.getParcelFileDescriptorForOCFile(file),
                                           0,
                                           file.getFileLength());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        Log_OC.d(TAG, "renameDocument(), id=" + documentId);

        final long token = Binder.clearCallingIdentity();
        try {
            String errorMessage = checkFileName(displayName);
            if (errorMessage != null) {
                showToast(errorMessage);
                return null;
            }

            Document document = toDocument(documentId);
            OCFile file = document.requireFile();
            if (!file.canRename()) {
                showToast(R.string.document_storage_provider_cannot_rename);
                return null;
            }

            final var result = new RenameFileOperation(file.getRemotePath(), displayName, document.getStorageManager())
                .execute(document.requireClient());

            if (!result.isSuccess()) {
                Log_OC.e(TAG, result.toString());
                throw new FileNotFoundException("Failed to rename document with documentId " + documentId + ": " +
                                                    result.getException());
            }

            document.getParent().ifPresent(this::notifyChanged);
            return null;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        Log_OC.d(TAG, "copyDocument(), id=" + sourceDocumentId);

        final long token = Binder.clearCallingIdentity();
        try {
            Document targetFolder = toDocument(targetParentDocumentId);
            OCFile targetFile = targetFolder.requireFile();
            if (!validateTargetFolder(targetFile)) {
                return null;
            }

            Document document = toDocument(sourceDocumentId);
            OCFile sourceFile = document.requireFile();
            FileDataStorageManager storageManager = document.getStorageManager();

            final var result = new CopyFileOperation(sourceFile.getRemotePath(),
                                                     targetFile.getRemotePath(),
                                                     storageManager)
                .execute(document.requireClient());

            if (!result.isSuccess()) {
                Log_OC.e(TAG, result.toString());
                throw new FileNotFoundException("Failed to copy document with documentId " + sourceDocumentId
                                                    + " to " + targetParentDocumentId);
            }

            final var updateParent = refreshFolder(targetFolder,
                                                   storageManager,
                                                   document.getUser(),
                                                   targetFolder.requireClient());

            if (!updateParent.isSuccess()) {
                Log_OC.e(TAG, updateParent.toString());
                throw new FileNotFoundException("Failed to copy document with documentId " + sourceDocumentId
                                                    + " to " + targetParentDocumentId);
            }

            String newPath = targetFile.getRemotePath() + sourceFile.getFileName() +
                (sourceFile.isFolder() ? PATH_SEPARATOR : "");
            String newDocumentId = getDocumentIdForPath(storageManager, newPath);

            notifyChanged(targetFolder);
            return newDocumentId;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId)
        throws FileNotFoundException {
        Log_OC.d(TAG, "moveDocument(), id=" + sourceDocumentId);

        final long token = Binder.clearCallingIdentity();
        try {
            Document targetFolder = toDocument(targetParentDocumentId);
            OCFile targetFile = targetFolder.requireFile();
            if (!validateTargetFolder(targetFile)) {
                return null;
            }

            Document document = toDocument(sourceDocumentId);
            OCFile sourceFile = document.requireFile();
            if (!sourceFile.canMove()) {
                showToast(R.string.document_storage_provider_cannot_move);
                return null;
            }

            final var result = new MoveFileOperation(sourceFile.getRemotePath(),
                                                     targetFile.getRemotePath(),
                                                     document.getStorageManager())
                .execute(document.requireClient());

            if (!result.isSuccess()) {
                Log_OC.e(TAG, result.toString());
                throw new FileNotFoundException("Failed to move document with documentId " + sourceDocumentId
                                                    + " to " + targetParentDocumentId);
            }

            notifyChanged(toDocument(sourceParentDocumentId));
            notifyChanged(targetFolder);
            return sourceDocumentId;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean validateTargetFolder(OCFile targetFolder) {
        isFolderPathValid = checkFolderPath(targetFolder.getFileName());
        if (!isFolderPathValid) {
            showToast(R.string.file_name_validator_error_contains_reserved_names_or_invalid_characters);
        }
        return isFolderPathValid;
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) {
        Log_OC.d(TAG, "querySearchDocuments(), rootId=" + rootId);

        final long token = Binder.clearCallingIdentity();
        try {
            FileCursor result = new FileCursor(projection);

            FileDataStorageManager storageManager = rootIdToStorageManager.get(rootId);
            if (storageManager == null) {
                return result;
            }

            OCFile root = storageManager.getFileByPath(ROOT_PATH);
            if (root == null) {
                return result;
            }

            addMatchingFiles(result, storageManager, root, query);
            return result;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void addMatchingFiles(FileCursor cursor, FileDataStorageManager storageManager, OCFile folder,
                                  String query) {
        for (OCFile file : storageManager.getFolderContent(folder, false)) {
            if (file.isFolder()) {
                addMatchingFiles(cursor, storageManager, file, query);
            } else if (file.getFileName().contains(query)) {
                cursor.addFile(new Document(storageManager, file));
            }
        }
    }

    private OCCapability getCapabilities() {
        return CapabilityUtils.getCapability(accountManager.getUser(), getNonNullContext());
    }

    private boolean checkFolderPath(String filename) {
        return FileNameValidator.INSTANCE.checkFolderPath(filename, getCapabilities(), getNonNullContext());
    }

    private String checkFileName(String filename) {
        return FileNameValidator.INSTANCE.checkFileName(filename, getCapabilities(), getNonNullContext());
    }

    @Override
    public String createDocument(String documentId, String mimeType, String displayName) throws FileNotFoundException {
        Log_OC.d(TAG, "createDocument(), id=" + documentId + ", name=" + displayName + ", mimeType=" + mimeType);

        final long token = Binder.clearCallingIdentity();
        try {
            String errorMessage = checkFileName(displayName);
            if (errorMessage != null) {
                showToast(errorMessage);
                return null;
            }

            Document folderDocument = toDocument(documentId);
            if (!folderDocument.requireFile().canCreateFileAndFolder()) {
                showToast(R.string.document_storage_provider_cannot_create_file_and_folder);
                return null;
            }

            return DocumentsContract.Document.MIME_TYPE_DIR.equalsIgnoreCase(mimeType)
                ? createFolder(folderDocument, displayName)
                : createFile(folderDocument, displayName, mimeType);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private String createFolder(Document targetFolder, String displayName) throws FileNotFoundException {
        OCFile targetFile = targetFolder.requireFile();
        if (!targetFile.canCreateFileAndFolder()) {
            showToast(R.string.document_storage_provider_cannot_create_folder_inside_folder);
            return null;
        }

        String newDirPath = targetFile.getRemotePath() + displayName + PATH_SEPARATOR;
        FileDataStorageManager storageManager = targetFolder.getStorageManager();
        OwnCloudClient client = targetFolder.requireClient();

        final var result = new CreateFolderOperation(newDirPath,
                                                     accountManager.getUser(),
                                                     getNonNullContext(),
                                                     storageManager)
            .execute(client);

        if (!result.isSuccess()) {
            Log_OC.e(TAG, result.toString());
            throw new FileNotFoundException("Failed to create document with name " +
                                                displayName + " and documentId " + targetFolder.getDocumentId());
        }

        final var updateParent = refreshFolder(targetFolder, storageManager, targetFolder.getUser(), client);
        if (!updateParent.isSuccess()) {
            Log_OC.e(TAG, updateParent.toString());
            throw new FileNotFoundException("Failed to create document with documentId "
                                                + targetFolder.getDocumentId());
        }

        String newFolderId = getDocumentIdForPath(storageManager, newDirPath);
        notifyChanged(targetFolder);
        return newFolderId;
    }

    private String createFile(Document targetFolder, String displayName, String mimeType) throws FileNotFoundException {
        OCFile targetFile = targetFolder.requireFile();
        if (!targetFile.canCreateFileAndFolder()) {
            showToast(R.string.document_storage_provider_cannot_create_file_inside_folder);
            return null;
        }

        User user = targetFolder.getUser();
        File emptyFile = createEmptyTempFile(user, displayName);
        String newFilePath = targetFile.getRemotePath() + displayName;

        try {
            OwnCloudClient client = targetFolder.requireClient();
            uploadEmptyFile(client, emptyFile, newFilePath, mimeType);

            final var updateParent = refreshFolder(targetFolder, targetFolder.getStorageManager(), user, client);
            if (!updateParent.isSuccess()) {
                Log_OC.e(TAG, updateParent.toString());
                notifyRemoteOperationError(updateParent);
                throw new FileNotFoundException("Failed to create document with documentId "
                                                    + targetFolder.getDocumentId());
            }

            String newFileId = getDocumentIdForPath(targetFolder.getStorageManager(), newFilePath);
            notifyChanged(targetFolder);
            return newFileId;
        } finally {
            if (emptyFile.exists() && !emptyFile.delete()) {
                Log_OC.w(TAG, "Temp file could not be deleted: " + emptyFile.getAbsolutePath());
            }
        }
    }

    private File createEmptyTempFile(User user, String displayName) throws FileNotFoundException {
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

        return emptyFile;
    }

    private void uploadEmptyFile(OwnCloudClient client, File emptyFile, String newFilePath, String mimeType)
        throws FileNotFoundException {
        final long nowSeconds = System.currentTimeMillis() / AppConstants.MILLIS_PER_SECOND;
        final var result = new UploadFileRemoteOperation(emptyFile.getAbsolutePath(),
                                                         newFilePath,
                                                         mimeType,
                                                         "",
                                                         nowSeconds,
                                                         FileUtil.getCreationTimestamp(emptyFile),
                                                         false)
            .execute(client);

        if (!result.isSuccess()) {
            Log_OC.e(TAG, result.toString());
            notifyRemoteOperationError(result);
            throw new FileNotFoundException("Failed to upload document with path " + newFilePath);
        }
    }

    private RemoteOperationResult refreshFolder(Document folder,
                                                FileDataStorageManager storageManager,
                                                User user,
                                                OwnCloudClient client) throws FileNotFoundException {
        return new RefreshFolderOperation(folder.requireFile(),
                                          System.currentTimeMillis(),
                                          false,
                                          false,
                                          true,
                                          storageManager,
                                          user,
                                          getNonNullContext())
            .execute(client);
    }

    private void notifyRemoteOperationError(RemoteOperationResult result) {
        if (result.getCode() == RemoteOperationResult.ResultCode.MAINTENANCE_MODE) {
            showToast(R.string.maintenance_mode);
        }
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
        OCFile file = document.requireFile();
        if (!file.canDeleteOrLeaveShare()) {
            showToast(R.string.document_storage_provider_cannot_delete);
            return;
        }

        Optional<Document> parentFolder = document.getParent();

        recursiveRevokePermission(document);

        final long token = Binder.clearCallingIdentity();
        try {
            final var result = new RemoveFileOperation(file,
                                                       false,
                                                       document.getUser(),
                                                       true,
                                                       context,
                                                       document.getStorageManager())
                .execute(document.requireClient());

            if (!result.isSuccess()) {
                throw new FileNotFoundException("Failed to delete document with documentId " + documentId);
            }
            parentFolder.ifPresent(this::notifyChanged);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void recursiveRevokePermission(Document document) {
        FileDataStorageManager storageManager = document.getStorageManager();
        OCFile file = document.getFile();
        if (file != null && file.isFolder()) {
            for (OCFile child : storageManager.getFolderContent(file, false)) {
                recursiveRevokePermission(new Document(storageManager, child));
            }
        }

        revokeDocumentPermission(document.getDocumentId());
    }

    @Override
    public boolean isChildDocument(String parentDocumentId, String documentId) {
        Log_OC.d(TAG, "isChildDocument(), parent=" + parentDocumentId + ", id=" + documentId);

        final long token = Binder.clearCallingIdentity();
        try {
            Document parentDocument = toDocument(parentDocumentId);
            OCFile parentFile = parentDocument.getFile();
            if (parentFile == null) {
                throw new FileNotFoundException("No parent file with ID " + parentDocumentId);
            }

            Document currentDocument = toDocument(documentId);
            OCFile childFile = currentDocument.getFile();
            if (childFile == null) {
                throw new FileNotFoundException("No child file with ID " + documentId);
            }

            return parentDocument.getUser().nameEquals(currentDocument.getUser()) &&
                childFile.getDecryptedRemotePath().startsWith(parentFile.getDecryptedRemotePath());
        } catch (FileNotFoundException e) {
            Log_OC.e(TAG, "failed to check for child document", e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static FileNotFoundException getFileNotFoundExceptionWithCause(String msg, Exception cause) {
        FileNotFoundException e = new FileNotFoundException(msg);
        e.initCause(cause);
        return e;
    }

    @VisibleForTesting
    public static String rootIdForUser(User user) {
        return HashUtil.md5Hash(user.getAccountName());
    }

    private void initiateStorageMap() {
        ContentResolver contentResolver = getNonNullContext().getContentResolver();
        rootIdToStorageManager = accountManager.getAllUsers().stream()
            .collect(Collectors.toMap(DocumentsStorageProvider::rootIdForUser,
                                      user -> new FileDataStorageManager(user, contentResolver),
                                      (first, second) -> second));
    }

    private String getDocumentIdForPath(FileDataStorageManager storageManager, String remotePath)
        throws FileNotFoundException {
        OCFile file = storageManager.getFileByPath(remotePath);
        if (file == null) {
            throw new FileNotFoundException("No file in database for path " + remotePath);
        }
        return new Document(storageManager, file).getDocumentId();
    }

    private void notifyChanged(Document document) {
        getNonNullContext().getContentResolver().notifyChange(toNotifyUri(document), null, false);
    }

    private Uri toNotifyUri(Document document) {
        return DocumentsContract.buildDocumentUri(
            getNonNullContext().getString(R.string.document_provider_authority),
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

        try {
            return new Document(storageManager, Long.parseLong(separated[1]));
        } catch (NumberFormatException e) {
            throw getFileNotFoundExceptionWithCause("Invalid documentID " + documentId + "!", e);
        }
    }

    @NonNull
    private Context getNonNullContext() {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

    private void showToast(int messageId) {
        ContextExtensionsKt.showToast(getNonNullContext(), messageId);
    }

    private void showToast(String message) {
        ContextExtensionsKt.showToast(getNonNullContext(), message);
    }

    private record AccessMode(int bits) {
        static AccessMode parse(String mode) {
            return new AccessMode(ParcelFileDescriptor.parseMode(mode));
        }

        boolean isReadOnly() {
            return bits == MODE_READ_ONLY;
        }

        boolean isWriteOnly() {
            return (bits & MODE_WRITE_ONLY) != 0;
        }

        boolean isTruncate() {
            return (bits & MODE_TRUNCATE) != 0;
        }
    }

    private record OpenedDocumentFile(File localFile, File descriptorFile) {
        boolean isCopy() {
            return !descriptorFile.equals(localFile);
        }

        void moveCopyToLocalFile() throws IOException {
            if (isCopy()) {
                Files.move(descriptorFile.toPath(), localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        void deleteCopy() {
            if (isCopy() && !descriptorFile.delete()) {
                Log_OC.w(TAG, "Copy could not be deleted: " + descriptorFile.getAbsolutePath());
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
            this(storageManager, file.getFileId());
        }

        Document(FileDataStorageManager storageManager, String filePath) {
            this(storageManager, storageManager.getFileByPath(filePath).getFileId());
        }

        public String getDocumentId() {
            return rootIdForUser(getUser()) + DOCUMENTID_SEPARATOR + fileId;
        }

        FileDataStorageManager getStorageManager() {
            return storageManager;
        }

        public User getUser() {
            return storageManager.getUser();
        }

        public OCFile getFile() {
            return storageManager.getFileById(fileId);
        }

        OCFile requireFile() throws FileNotFoundException {
            OCFile file = getFile();
            if (file == null) {
                throw new FileNotFoundException("No file in database for documentId " + getDocumentId());
            }
            return file;
        }

        OwnCloudClient requireClient() throws FileNotFoundException {
            try {
                return OwnCloudClientManagerFactory.getDefaultSingleton()
                    .getClientFor(getUser().toOwnCloudAccount(), getContext());
            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                Log_OC.e(TAG, "Failed to set client", e);
                throw getFileNotFoundExceptionWithCause("Failed to get client for " + getDocumentId(), e);
            }
        }

        Optional<Document> getParent() {
            OCFile file = getFile();
            if (file == null || file.getParentId() <= 0) {
                return Optional.empty();
            }
            return Optional.of(new Document(storageManager, file.getParentId()));
        }
    }
}
