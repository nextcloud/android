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

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null");
        }

        Document parentFolder = toDocument(parentDocumentId);

        FileDataStorageManager storageManager = parentFolder.getStorageManager();

        final FileCursor resultCursor = new FileCursor(projection);

        for (OCFile file : storageManager.getFolderContent(parentFolder.getFile(), false)) {
            resultCursor.addFile(new Document(storageManager, file));
        }

        boolean isLoading = false;
        if (parentFolder.isExpired()) {
            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());

            final ReloadFolderDocumentTask task = new ReloadFolderDocumentTask(arbitraryDataProvider,
                                                                               parentFolder,
                                                                               result -> {
                getContext().getContentResolver().notifyChange(toNotifyUri(parentFolder), null, false);
            });
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
        final User user = accountManager.getUser(account.name).orElseThrow(RuntimeException::new); // should exist

        if (!ocFile.isDown()) {
            Intent i = new Intent(getContext(), FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_USER, user);
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
                    FileDataStorageManager storageManager = new FileDataStorageManager(user.toPlatformAccount(),
                                                                                       context.getContentResolver());
                    RemoteOperationResult result = new SynchronizeFileOperation(finalFile, null, user,
                                                                                true, context)
                        .execute(storageManager, context);
                    if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        // ISSUE 5: if the user is not running the app (this is a service!),
                        // this can be very intrusive; a notification should be preferred
                        Intent intent = ConflictsResolveActivity.createIntent(finalFile,
                                                                              user.toPlatformAccount(),
                                                                              Intent.FLAG_ACTIVITY_NEW_TASK,
                                                                              context);
                        context.startActivity(intent);
                    } else {
                        FileStorageUtils.checkIfFileFinishedSaving(finalFile);
                        if (!result.isSuccess()) {
                            showToast();
                        }
                    }
                } catch (Exception e) {
                    Log_OC.e(TAG, "Error syncing file", e);
                    showToast();
                }
            });

            syncThread.start();
            try {
                syncThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to wait for thread to finish", e);
            }
        }

        File file = new File(ocFile.getStoragePath());
        int accessMode = ParcelFileDescriptor.parseMode(mode);
        boolean isWrite = mode.indexOf('w') != -1;

        final OCFile oldFile = ocFile;
        final OCFile newFile = ocFile;

        if (isWrite) {
            try {
                // reset last sync date to ensure we will be syncing this write to the server
                ocFile.setLastSyncDateForData(0);
                Handler handler = new Handler(context.getMainLooper());
                return ParcelFileDescriptor.open(file, accessMode, handler, l -> {
                    RemoteOperationResult result = new SynchronizeFileOperation(newFile, oldFile, user, true,
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
        Log.d(TAG, "openDocumentThumbnail(), id=" + documentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        Document document = toDocument(documentId);

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

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        Document document = toDocument(documentId);

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

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        Document document = toDocument(sourceDocumentId);

        FileDataStorageManager storageManager = document.getStorageManager();
        Document targetFolder = toDocument(targetParentDocumentId);

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

        context.getContentResolver().notifyChange(toNotifyUri(targetFolder), null, false);

        return newFile.getDocumentId();
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId)
        throws FileNotFoundException {
        Log.d(TAG, "moveDocument(), id=" + sourceDocumentId);

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        Document document = toDocument(sourceDocumentId);
        Document targetFolder = toDocument(targetParentDocumentId);

        RemoteOperationResult result = new MoveFileOperation(document.getRemotePath(), targetFolder.getRemotePath())
            .execute(document.getClient(), document.getStorageManager());

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to move document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        Document sourceFolder = toDocument(sourceParentDocumentId);

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

        String newDirPath = targetFolder.getRemotePath() + displayName + PATH_SEPARATOR;
        FileDataStorageManager storageManager = targetFolder.getStorageManager();

        RemoteOperationResult result = new CreateFolderOperation(newDirPath,
                                                                 accountManager.getUser(),
                                                                 getContext())
            .execute(targetFolder.getClient(), storageManager);

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to create document with name " +
                                                displayName + " and documentId " + targetFolder.getDocumentId());
        }

        RemoteOperationResult updateParent = new RefreshFolderOperation(targetFolder.getFile(), System.currentTimeMillis(),
                                                                        false, false, true, storageManager,
                                                                        targetFolder.getAccount(), context)
            .execute(targetFolder.getClient());

        if (!updateParent.isSuccess()) {
            throw new FileNotFoundException("Failed to create document with documentId " + targetFolder.getDocumentId());
        }

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

        String newFilePath = targetFolder.getRemotePath() + displayName;

        // perform the upload, no need for chunked operation as we have a empty file
        OwnCloudClient client = targetFolder.getClient();
        RemoteOperationResult result = new UploadFileRemoteOperation(emptyFile.getAbsolutePath(),
                                                                     newFilePath,
                                                                     null,
                                                                     "",
                                                                     String.valueOf(System.currentTimeMillis() / 1000))
            .execute(client);

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to upload document with path " + newFilePath);
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

        Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        Document document = toDocument(documentId);
        // get parent here, because it is not available anymore after the document was deleted
        Document parentFolder = document.getParent();

        recursiveRevokePermission(document);

        OCFile file = document.getStorageManager().getFileByPath(document.getRemotePath());
        RemoteOperationResult result = new RemoveFileOperation(file,
                                                               false,
                                                               document.getAccount(),
                                                               true,
                                                               context)
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
            return parentDocument.getAccount() == currentDocument.getAccount() && childPath.startsWith(parentPath);

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

    public interface OnTaskFinishedCallback {
        void onTaskFinished(RemoteOperationResult result);
    }

    static class ReloadFolderDocumentTask extends AsyncTask<Void, Void, RemoteOperationResult> {

        private final Document folder;
        private final OnTaskFinishedCallback callback;
        private final ArbitraryDataProvider arbitraryDataProvider;

        ReloadFolderDocumentTask(ArbitraryDataProvider arbitraryDataProvider,
                                 Document folder,
                                 OnTaskFinishedCallback callback) {
            this.folder = folder;
            this.callback = callback;
            this.arbitraryDataProvider = arbitraryDataProvider;
        }

        @Override
        public final RemoteOperationResult doInBackground(Void... params) {
            Log.d(TAG, "run ReloadFolderDocumentTask(), id=" + folder.getDocumentId());
            return new RefreshFolderOperation(folder.getFile(), System.currentTimeMillis(), false,
                                              false, true, folder.getStorageManager(), folder.getAccount(),
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
            long parentId = getFile().getParentId();
            if (parentId <= 0) {
                return null;
            }

            return new Document(getStorageManager(), parentId);
        }
    }
}
