/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   @author Chris Narkiewicz
 *   Copyright (C) 2016  Bartosz Przybylski <bart.p.pl@gmail.com>
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

package com.owncloud.android.providers;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ChunkedFileUploadRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.SettingsActivity;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.UriUtils;

import org.nextcloud.providers.cursors.FileCursor;
import org.nextcloud.providers.cursors.RootCursor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DocumentsStorageProvider extends DocumentsProvider {

    private static final String TAG = DocumentsStorageProvider.class.getSimpleName();

    private static final long CACHE_EXPIRATION = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);

    UserAccountManager accountManager;

    private static final String DOCUMENTID_SEPARATOR = "/";
    private static final int DOCUMENTID_PARTS = 2;
    private final SparseArray<FileDataStorageManager> rootIdToStorageManager = new SparseArray<>();

    private final Executor executor = Executors.newCachedThreadPool();

    private final OnTaskFinishedCallback<Document> loadChildrenCallback =
        (status, document, exception) -> getContext().getContentResolver().notifyChange(toNotifyUri(document), null, false);

    @Override
    public Cursor queryRoots(String[] projection) {

        Context context = MainApp.getAppContext();
        AppPreferences preferences = AppPreferencesImpl.fromContext(context);
        if (SettingsActivity.LOCK_PASSCODE.equals(preferences.getLockPreference()) ||
            SettingsActivity.LOCK_DEVICE_CREDENTIALS.equals(preferences.getLockPreference())) {
            return new FileCursor();
        }

        final RootCursor result = new RootCursor(projection);
        for(int i = 0; i < rootIdToStorageManager.size(); i++) {
            result.addRoot(new Document(rootIdToStorageManager.valueAt(i), ROOT_PATH), getContext());
        }

        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        Log.d(TAG, "queryDocument(), id=" + documentId);

        Document document = toDocument(documentId);

        final FileCursor result = new FileCursor(projection);
        result.addFile(document);

        return result;
    }

    @SuppressLint("LongLogTag")
    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
        throws FileNotFoundException {
        Log.d(TAG, "queryChildDocuments(), id=" + parentDocumentId);

        Document parentFolder = toDocument(parentDocumentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null");
        }

        FileDataStorageManager storageManager = parentFolder.getStorageManager();

        final FileCursor resultCursor = new FileCursor(projection);

        for (OCFile file : storageManager.getFolderContent(parentFolder.getFile(), false)) {
            resultCursor.addFile(new Document(storageManager, file));
        }

        boolean isLoading = false;
        if (parentFolder.isExpired()) {
            final LoadChildrenTask task = new LoadChildrenTask(parentFolder, loadChildrenCallback);
            task.executeOnExecutor(executor);
            resultCursor.setLoadingTask(task);
            isLoading = true;
        }

        final Bundle extra = new Bundle();
        extra.putBoolean(DocumentsContract.EXTRA_LOADING, isLoading);
        resultCursor.setExtras(extra);
        resultCursor.setNotificationUri(getContext().getContentResolver(), toNotifyUri(parentFolder));
        return resultCursor;
    }

    @SuppressLint("LongLogTag")
    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal cancellationSignal)
            throws FileNotFoundException {
        Log.d(TAG, "openDocument(), id=" + documentId);

        Document document = toDocument(documentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        OCFile ocFile = document.getFile();
        Account account = document.getAccount();

        if (!ocFile.isDown()) {
            Intent i = new Intent(getContext(), FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
            i.putExtra(FileDownloader.EXTRA_FILE, ocFile);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }

            do {
                if (!waitOrGetCancelled(cancellationSignal)) {
                    throw new FileNotFoundException("File with id " + documentId + " not found!");
                }
                ocFile = document.getFile();

                if (ocFile == null) {
                    throw new FileNotFoundException("File with id " + documentId + " not found!");
                }
            } while (!ocFile.isDown());
        } else {
            OCFile finalFile = ocFile;
            Thread syncThread = new Thread(() -> {
                try {
                    FileDataStorageManager storageManager = new FileDataStorageManager(account, context.getContentResolver());
                    RemoteOperationResult result = new SynchronizeFileOperation(finalFile, null, account,
                                                                                true, context)
                        .execute(storageManager, context);
                    if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        // ISSUE 5: if the user is not running the app (this is a service!),
                        // this can be very intrusive; a notification should be preferred
                        Intent i = new Intent(context, ConflictsResolveActivity.class);
                        i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra(ConflictsResolveActivity.EXTRA_FILE, finalFile);
                        i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, account);
                        context.startActivity(i);
                    } else {
                        FileStorageUtils.checkIfFileFinishedSaving(finalFile);
                        if (!result.isSuccess()) {
                            showToast();
                        }
                    }
                } catch (Exception exception) {
                    showToast();
                }
            });

            syncThread.start();
            try {
                syncThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to wait for thread to finish");
            }
        }

        File file = new File(ocFile.getStoragePath());
        int accessMode = ParcelFileDescriptor.parseMode(mode);
        boolean isWrite = mode.indexOf('w') != -1;

        final OCFile oldFile = ocFile;
        final OCFile newFile = ocFile;

        if (isWrite) {
            try {
                Handler handler = new Handler(context.getMainLooper());
                return ParcelFileDescriptor.open(file, accessMode, handler, l -> {
                    RemoteOperationResult result = new SynchronizeFileOperation(newFile, oldFile, account, true,
                                                                                context)
                        .execute(document.getClient(), document.getStorageManager());

                    boolean success = result.isSuccess();

                    if (!success) {
                        Log_OC.e(TAG, "Failed to update document with id " + documentId);
                    }
                });
            } catch (IOException e) {
                throw new FileNotFoundException("Failed to open/edit document with id " + documentId);
            }
        } else {
            return ParcelFileDescriptor.open(file, accessMode);
        }
    }

    private void showToast() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(MainApp.getAppContext(),
                R.string.file_not_synced,
                Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreate() {
        accountManager = UserAccountManagerImpl.fromContext(getContext());
        initiateStorageMap();
        return true;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId,
                                                     Point sizeHint,
                                                     CancellationSignal signal)
            throws FileNotFoundException {
        Log.d(TAG, "openDocumentThumbnail(), id=" + documentId);

        Document document = toDocument(documentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        boolean exists = ThumbnailsCacheManager.containsBitmap(ThumbnailsCacheManager.PREFIX_THUMBNAIL
                                                                   + document.getFile().getRemoteId());

        if (!exists) {
            ThumbnailsCacheManager.generateThumbnailFromOCFile(document.getFile());
        }

        Uri uri = Uri.parse(UriUtils.URI_CONTENT_SCHEME + context.getResources().getString(
            R.string.image_cache_provider_authority) + document.getRemotePath());
        Log.d(TAG, "open thumbnail, uri=" + uri);
        return context.getContentResolver().openAssetFileDescriptor(uri, "r");
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        Log.d(TAG, "renameDocument(), id=" + documentId);

        Document document = toDocument(documentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        RemoteOperationResult result = new RenameFileOperation(document.getRemotePath(), displayName)
            .execute(document.getClient(), document.getStorageManager());

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to rename document with documentId " + documentId + ": " +
                                                result.getException());
        }

        context.getContentResolver().notifyChange(toNotifyUri(document.getParent()), null, false);

        return null;
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        Log.d(TAG, "copyDocument(), id=" + sourceDocumentId);

        Document document = toDocument(sourceDocumentId);
        Document targetFolder = toDocument(targetParentDocumentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        FileDataStorageManager storageManager = document.getStorageManager();

        RemoteOperationResult result = new CopyFileOperation(document.getRemotePath(), targetFolder.getRemotePath())
            .execute(document.getClient(), storageManager);

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to copy document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        Account account = document.getAccount();

        RemoteOperationResult updateParent = new RefreshFolderOperation(targetFolder.getFile(), System.currentTimeMillis(),
                                                                        false, false, true, storageManager,
                                                                        account, context)
            .execute(targetFolder.getClient());

        if (!updateParent.isSuccess()) {
            throw new FileNotFoundException("Failed to copy document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        String newPath = targetFolder.getRemotePath() + document.getFile().getFileName();

        if (document.getFile().isFolder()) {
            newPath = newPath + PATH_SEPARATOR;
        }
        Document newFile = new Document(storageManager, newPath);

        context.getContentResolver().notifyChange(toNotifyUri(newFile.getParent()), null, false);

        return newFile.getDocumentId();
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId)
        throws FileNotFoundException {
        Log.d(TAG, "moveDocument(), id=" + sourceDocumentId);

        Document document = toDocument(sourceDocumentId);
        Document sourceFolder = toDocument(sourceParentDocumentId);
        Document targetFolder = toDocument(targetParentDocumentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        RemoteOperationResult result = new MoveFileOperation(document.getRemotePath(), targetFolder.getRemotePath())
            .execute(document.getClient(), document.getStorageManager());

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to move document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        getContext().getContentResolver().notifyChange(toNotifyUri(sourceFolder), null, false);
        getContext().getContentResolver().notifyChange(toNotifyUri(targetFolder), null, false);

        return sourceDocumentId;
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) {
        Log.d(TAG, "querySearchDocuments(), rootId=" + rootId);

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

    @Override
    public String createDocument(String documentId, String mimeType, String displayName) throws FileNotFoundException {
        Log.d(TAG, "createDocument(), id=" + documentId);

        Document folderDocument = toDocument(documentId);

        if (DocumentsContract.Document.MIME_TYPE_DIR.equalsIgnoreCase(mimeType)) {
            return createFolder(folderDocument, displayName);
        } else {
            return createFile(folderDocument, displayName);
        }
    }

    private String createFolder(Document targetFolder, String displayName) throws FileNotFoundException {

        Context context = getContext();

        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        FileDataStorageManager storageManager = targetFolder.getStorageManager();

        RemoteOperationResult result = new CreateFolderOperation(targetFolder.getRemotePath() + displayName
                                                                     + PATH_SEPARATOR, true)
            .execute(targetFolder.getClient(), storageManager);


        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to create document with name " +
                                                displayName + " and documentId " + targetFolder.getDocumentId());
        }


        Account account = targetFolder.getAccount();
        OwnCloudClient client = targetFolder.getClient();
        RemoteOperationResult updateParent = new RefreshFolderOperation(targetFolder.getFile(), System.currentTimeMillis(),
                                                                        false, false, true, storageManager,
                                                                        account, context)
            .execute(client);

        if (!updateParent.isSuccess()) {
            throw new FileNotFoundException("Failed to create document with documentId " + targetFolder.getDocumentId());
        }

        String newDirPath = targetFolder.getRemotePath() + displayName + PATH_SEPARATOR;
        Document newFolder = new Document(storageManager, newDirPath);

        context.getContentResolver().notifyChange(toNotifyUri(targetFolder), null, false);

        return newFolder.getDocumentId();
    }

    private String createFile(Document targetFolder, String displayName) throws FileNotFoundException {
        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        Account account = targetFolder.getAccount();
        OwnCloudClient client = targetFolder.getClient();

        // create dummy file
        File tempDir = new File(FileStorageUtils.getTemporalPath(account.name));

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
            throw new FileNotFoundException("File could not be created");
        }

        // perform the upload
        UploadFileRemoteOperation mUploadOperation;
        if (emptyFile.length() > ChunkedFileUploadRemoteOperation.CHUNK_SIZE_MOBILE) {
            mUploadOperation = new ChunkedFileUploadRemoteOperation(emptyFile.getAbsolutePath(),
                                                                    targetFolder.getRemotePath() + displayName,
                                                                    null,
                                                                    "",
                                                                    String.valueOf(System.currentTimeMillis()),
                                                                    false);
        } else {
            mUploadOperation = new UploadFileRemoteOperation(emptyFile.getAbsolutePath(),
                                                             targetFolder.getRemotePath() + displayName,
                                                             null,
                                                             "",
                                                             String.valueOf(System.currentTimeMillis()));
        }

        RemoteOperationResult result = mUploadOperation.execute(client);

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to upload document with path " +
                                                targetFolder.getRemotePath() + "/" + displayName);
        }

        RemoteOperationResult updateParent = new RefreshFolderOperation(targetFolder.getFile(),
                                                                        System.currentTimeMillis(),
                                                                        false,
                                                                        false,
                                                                        true,
                                                                        targetFolder.getStorageManager(),
                                                                        account,
                                                                        context)
            .execute(client);

        if (!updateParent.isSuccess()) {
            throw new FileNotFoundException("Failed to create document with documentId " + targetFolder.getDocumentId());
        }

        String newFilePath = targetFolder.getRemotePath() + displayName;
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
        Log.d(TAG, "deleteDocument(), id=" + documentId);

        Document document = toDocument(documentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        recursiveRevokePermission(document);

        Document parentFolder = document.getParent();

        RemoteOperationResult result = new RemoveFileOperation(document.getRemotePath(), false,
                                                               document.getAccount(), true, context)
            .execute(document.getClient(), document.getStorageManager());

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to delete document with documentId " + documentId);
        }

        context.getContentResolver().notifyChange(toNotifyUri(parentFolder), null, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
        Log.d(TAG, "isChildDocument(), parent=" + parentDocumentId + ", id=" + documentId);

        try {
            Document document = toDocument(documentId);
            Document folderDocument = toDocument(parentDocumentId);

            if (null == document || null == folderDocument) {
                return false;
            }

            return document.getFile().getParentId() == folderDocument.getFile().getFileId();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "failed to check for child document", e);
        }

        return false;
    }

    private FileDataStorageManager getStorageManager(String rootId) {
        for(int i = 0; i < rootIdToStorageManager.size(); i++) {
            FileDataStorageManager storageManager = rootIdToStorageManager.valueAt(i);
            if (storageManager.getAccount().name.equals(rootId)) {
                return storageManager;
            }
        }

        return null;
    }

    private void initiateStorageMap() {

        rootIdToStorageManager.clear();

        ContentResolver contentResolver = getContext().getContentResolver();

        for (Account account : accountManager.getAccounts()) {
            final FileDataStorageManager storageManager = new FileDataStorageManager(account, contentResolver);
            rootIdToStorageManager.put(account.hashCode(), storageManager);
        }
    }

    private boolean waitOrGetCancelled(CancellationSignal cancellationSignal) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            return false;
        }

        return !(cancellationSignal != null && cancellationSignal.isCanceled());
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

        FileDataStorageManager storageManager = rootIdToStorageManager.get(Integer.parseInt(separated[0]));
        if (storageManager == null) {
            throw new FileNotFoundException("No storage manager associated for " + documentId + "!");
        }

        return new Document(storageManager, Long.parseLong(separated[1]));
    }

    public interface OnTaskFinishedCallback<T> {

        int SUCCEEDED = 0;
        int FAILED = 1;
        int CANCELLED = 2;

        @IntDef({ SUCCEEDED, FAILED, CANCELLED })
        @Retention(RetentionPolicy.SOURCE)
        @interface Status {}

        void onTaskFinished(@Status int status, @Nullable T item, @Nullable Exception exception);
    }

    static class LoadChildrenTask extends AsyncTask<Void, Void, RemoteOperationResult> {

        private final Document document;
        private final OnTaskFinishedCallback<Document> callback;

        LoadChildrenTask(Document document, OnTaskFinishedCallback<Document> callback) {
            this.document = document;
            this.callback = callback;
        }

        @Override
        public final RemoteOperationResult doInBackground(Void... params) {
            Log.d(TAG, "run RefreshDocumentTask(), id=" + document.getDocumentId());
            return new RefreshFolderOperation(document.getFile(), System.currentTimeMillis(), false,
                                              false, true, document.getStorageManager(), document.getAccount(),
                                              MainApp.getAppContext()).execute(document.getClient());
        }

        @Override
        public final void onPostExecute(RemoteOperationResult result) {
            if (callback != null) {
                if (result.isSuccess()) {
                    callback.onTaskFinished(OnTaskFinishedCallback.SUCCEEDED, document, null);
                } else if (result.isCancelled()) {
                    callback.onTaskFinished(OnTaskFinishedCallback.CANCELLED, document, null);
                } else {
                    callback.onTaskFinished(OnTaskFinishedCallback.FAILED, document, result.getException());
                }
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
            for(int i = 0; i < rootIdToStorageManager.size(); i++) {
                if (Objects.equals(storageManager, rootIdToStorageManager.valueAt(i))) {
                    return rootIdToStorageManager.keyAt(i) + DOCUMENTID_SEPARATOR + fileId;
                }
            }
            return null;
        }

        FileDataStorageManager getStorageManager() {
            return storageManager;
        }

        public Account getAccount() {
            return getStorageManager().getAccount();
        }

        public OCFile getFile() {
            return getStorageManager().getFileById(fileId);
        }

        public String getRemotePath() {
            return getFile().getRemotePath();
        }

        OwnCloudClient getClient() {
            try {
                OwnCloudAccount ocAccount = new OwnCloudAccount(getAccount(), MainApp.getAppContext());
                return OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, getContext());
            } catch (OperationCanceledException | IOException | AuthenticatorException |
                com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                Log_OC.e(TAG, "Failed to set client", e);
            }
            return null;
        }

        boolean isExpired() {
            return getFile().getLastSyncDateForData() + CACHE_EXPIRATION < System.currentTimeMillis();
        }

        Document getParent() {
            return new Document(getStorageManager(), getFile().getParentId());
        }
    }
}
