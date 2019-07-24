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
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsProvider;
import android.util.Log;
import android.widget.Toast;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
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
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.UploadFileOperation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DocumentsStorageProvider extends DocumentsProvider {

    private static final String TAG = "DocumentsStorageProvider";

    private FileDataStorageManager currentStorageManager;
    private Map<Long, FileDataStorageManager> rootIdToStorageManager;
    private OwnCloudClient client;

    UserAccountManager accountManager;

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        Context context = MainApp.getAppContext();
        AppPreferences preferences = AppPreferencesImpl.fromContext(context);
        if (SettingsActivity.LOCK_PASSCODE.equals(preferences.getLockPreference()) ||
            SettingsActivity.LOCK_DEVICE_CREDENTIALS.equals(preferences.getLockPreference())) {
            return new FileCursor();
        }

        initiateStorageMap();

        final RootCursor result = new RootCursor(projection);

        for (Account account : accountManager.getAccounts()) {
            result.addRoot(account, getContext());
        }

        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        if (currentStorageManager == null) {

            for (Map.Entry<Long, FileDataStorageManager> entry : rootIdToStorageManager.entrySet()) {
                if (entry.getValue().getFileById(docId) != null) {
                    currentStorageManager = entry.getValue();
                    break;
                }
            }
        }

        if (currentStorageManager == null) {
            throw new FileNotFoundException("File with id " + documentId + " not found");
        }

        final FileCursor result = new FileCursor(projection);
        OCFile file = currentStorageManager.getFileById(docId);
        if (file != null) {
            result.addFile(file);
        }

        return result;
    }

    @SuppressLint("LongLogTag")
    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
        throws FileNotFoundException {

        final long folderId = Long.parseLong(parentDocumentId);
        updateCurrentStorageManagerIfNeeded(folderId);

        Context context = getContext();

        if (context == null) {
            throw new FileNotFoundException("Context may not be null");
        }

        Account account = currentStorageManager.getAccount();
        final OCFile browsedDir = currentStorageManager.getFileById(folderId);
        if (Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED)) {
            RemoteOperationResult result = new RefreshFolderOperation(browsedDir, System.currentTimeMillis(), false,
                                                                      false, true, currentStorageManager, account,
                                                                      getContext()).execute(client);

            if (!result.isSuccess()) {
                throw new FileNotFoundException("Failed to update document " + parentDocumentId);
            }
        }

        final FileCursor resultCursor = new FileCursor(projection);

        for (OCFile file : currentStorageManager.getFolderContent(browsedDir, false)) {
            resultCursor.addFile(file);
        }

        return resultCursor;
    }

    @SuppressLint("LongLogTag")
    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal cancellationSignal)
            throws FileNotFoundException {
        final long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile ocFile = currentStorageManager.getFileById(docId);

        if (ocFile == null) {
            throw new FileNotFoundException("File not found: " + documentId);
        }

        Context context = getContext();

        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        Account account = currentStorageManager.getAccount();
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
                ocFile = currentStorageManager.getFileById(docId);

                if (ocFile == null) {
                    throw new FileNotFoundException("File with id " + documentId + " not found!");
                }
            } while (!ocFile.isDown());
        } else {
            OCFile finalFile = ocFile;
            Thread syncThread = new Thread(() -> {
                try {
                    FileDataStorageManager storageManager =
                            new FileDataStorageManager(account, context.getContentResolver());
                    SynchronizeFileOperation sfo =
                            new SynchronizeFileOperation(finalFile, null, account, true, context);
                    RemoteOperationResult result = sfo.execute(storageManager, context);
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
                        .execute(client, currentStorageManager);

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
        return true;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId,
                                                     Point sizeHint,
                                                     CancellationSignal signal)
            throws FileNotFoundException {
        long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile file = currentStorageManager.getFileById(docId);

        if (file == null) {
            throw new FileNotFoundException("File with id " + documentId + " not found!");
        }

        Context context = getContext();

        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        boolean exists = ThumbnailsCacheManager.containsBitmap(ThumbnailsCacheManager.PREFIX_THUMBNAIL
                                                                   + file.getRemoteId());

        if (!exists) {
            ThumbnailsCacheManager.generateThumbnailFromOCFile(file);
        }

        Uri uri = Uri.parse(UriUtils.URI_CONTENT_SCHEME + context.getResources().getString(
            R.string.image_cache_provider_authority) + file.getRemotePath());
        return context.getContentResolver().openAssetFileDescriptor(uri, "r");
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile file = currentStorageManager.getFileById(docId);

        if (file == null) {
            throw new FileNotFoundException("File " + documentId + " not found!");
        }

        RemoteOperationResult result = new RenameFileOperation(file.getRemotePath(), displayName)
            .execute(client, currentStorageManager);

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to rename document with documentId " + documentId + ": " +
                                                result.getException());
        }

        return null;
    }

    @Override
    public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
        long sourceId = Long.parseLong(sourceDocumentId);

        updateCurrentStorageManagerIfNeeded(sourceId);

        OCFile file = currentStorageManager.getFileById(sourceId);
        if (file == null) {
            throw new FileNotFoundException("File " + sourceDocumentId + " not found!");
        }

        long targetId = Long.parseLong(targetParentDocumentId);
        OCFile targetFolder = currentStorageManager.getFileById(targetId);
        if (targetFolder == null) {
            throw new FileNotFoundException("File " + targetParentDocumentId + " not found!");
        }

        RemoteOperationResult result = new CopyFileOperation(file.getRemotePath(), targetFolder.getRemotePath())
            .execute(client, currentStorageManager);

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to copy document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        Account account = currentStorageManager.getAccount();

        RemoteOperationResult updateParent = new RefreshFolderOperation(targetFolder, System.currentTimeMillis(),
                                                                        false, false, true, currentStorageManager,
                                                                        account, getContext()).execute(client);

        if (!updateParent.isSuccess()) {
            throw new FileNotFoundException("Failed to copy document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        String newPath = targetFolder.getRemotePath() + file.getFileName();

        if (file.isFolder()) {
            newPath = newPath + PATH_SEPARATOR;
        }
        OCFile newFile = currentStorageManager.getFileByPath(newPath);

        return String.valueOf(newFile.getFileId());
    }

    @Override
    public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId)
        throws FileNotFoundException {
        long sourceId = Long.parseLong(sourceDocumentId);

        updateCurrentStorageManagerIfNeeded(sourceId);

        OCFile file = currentStorageManager.getFileById(sourceId);

        if (file == null) {
            throw new FileNotFoundException("File " + sourceDocumentId + " not found!");
        }

        long targetId = Long.parseLong(targetParentDocumentId);
        OCFile targetFolder = currentStorageManager.getFileById(targetId);

        if (targetFolder == null) {
            throw new FileNotFoundException("File " + targetParentDocumentId + " not found!");
        }

        RemoteOperationResult result = new MoveFileOperation(file.getRemotePath(), targetFolder.getRemotePath())
            .execute(client, currentStorageManager);

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to move document with documentId " + sourceDocumentId
                                                + " to " + targetParentDocumentId);
        }

        return String.valueOf(file.getFileId());
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) {
        updateCurrentStorageManagerIfNeeded(rootId);

        OCFile root = currentStorageManager.getFileByPath(ROOT_PATH);
        FileCursor result = new FileCursor(projection);

        for (OCFile f : findFiles(root, query)) {
            result.addFile(f);
        }

        return result;
    }

    @Override
    public String createDocument(String documentId, String mimeType, String displayName) throws FileNotFoundException {
        long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile parent = currentStorageManager.getFileById(docId);

        if (parent == null) {
            throw new FileNotFoundException("Parent file not found");
        }

        if ("vnd.android.document/directory".equalsIgnoreCase(mimeType)) {
            return createFolder(parent, displayName, documentId);
        } else {
            return createFile(parent, displayName, documentId);
        }
    }

    private String createFolder(OCFile parent, String displayName, String documentId) throws FileNotFoundException {

        CreateFolderOperation createFolderOperation = new CreateFolderOperation(parent.getRemotePath() + displayName
                                                                                    + PATH_SEPARATOR, true);
        RemoteOperationResult result = createFolderOperation.execute(client, currentStorageManager);


        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to create document with name " +
                                                displayName + " and documentId " + documentId);
        }


        String newDirPath = parent.getRemotePath() + displayName + PATH_SEPARATOR;
        OCFile newFolder = currentStorageManager.getFileByPath(newDirPath);

        return String.valueOf(newFolder.getFileId());
    }

    private String createFile(OCFile parent, String displayName, String documentId) throws FileNotFoundException {
        Context context = getContext();

        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        Account account = currentStorageManager.getAccount();

        // create dummy file
        File tempDir = new File(FileStorageUtils.getTemporalPath(account.name));
        File emptyFile = new File(tempDir, displayName);
        try {
            if (!emptyFile.createNewFile()) {
                throw new FileNotFoundException("File could not be created");
            }
        } catch (IOException e) {
            throw new FileNotFoundException("File could not be created");
        }

        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(getContext(), account, new String[]{emptyFile.getAbsolutePath()},
                                new String[]{parent.getRemotePath() + displayName}, null,
                                FileUploader.LOCAL_BEHAVIOUR_MOVE, true, UploadFileOperation.CREATED_BY_USER, false,
                                false);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log_OC.e(TAG, "Thread interruption error");
        }

        RemoteOperationResult updateParent = new RefreshFolderOperation(parent, System.currentTimeMillis(),
                                                                        false, false, true, currentStorageManager,
                                                                        account, getContext()).execute(client);

        if (!updateParent.isSuccess()) {
            throw new FileNotFoundException("Failed to create document with documentId " + documentId);
        }

        String newFilePath = parent.getRemotePath() + displayName;
        OCFile newFile = currentStorageManager.getFileByPath(newFilePath);

        return String.valueOf(newFile.getFileId());
    }

    @Override
    public void removeDocument(String documentId, String parentDocumentId) throws FileNotFoundException {
        deleteDocument(documentId);
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile file = currentStorageManager.getFileById(docId);

        if (file == null) {
            throw new FileNotFoundException("File " + documentId + " not found!");
        }
        Account account = currentStorageManager.getAccount();

        RemoveFileOperation removeFileOperation = new RemoveFileOperation(file.getRemotePath(), false, account, true,
                                                                          getContext());

        RemoteOperationResult result = removeFileOperation.execute(client, currentStorageManager);

        if (!result.isSuccess()) {
            throw new FileNotFoundException("Failed to delete document with documentId " + documentId);
        }
    }

    @SuppressLint("LongLogTag")
    private void updateCurrentStorageManagerIfNeeded(long docId) {
        if (rootIdToStorageManager == null) {
            try {
                queryRoots(FileCursor.DEFAULT_DOCUMENT_PROJECTION);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Failed to query roots");
            }
        }

        if (currentStorageManager == null ||
            rootIdToStorageManager.containsKey(docId) && currentStorageManager != rootIdToStorageManager.get(docId)) {
            currentStorageManager = rootIdToStorageManager.get(docId);
        }

        try {
            Account account = currentStorageManager.getAccount();
            OwnCloudAccount ocAccount = new OwnCloudAccount(account, MainApp.getAppContext());
            client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, getContext());
        } catch (OperationCanceledException | IOException | AuthenticatorException |
            com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            Log_OC.e(TAG, "Failed to set client", e);
        }
    }

    private void updateCurrentStorageManagerIfNeeded(String rootId) {
        for (FileDataStorageManager data : rootIdToStorageManager.values()) {
            if (data.getAccount().name.equals(rootId)) {
                currentStorageManager = data;
            }
        }
    }

    @SuppressLint("UseSparseArrays")
    private void initiateStorageMap() throws FileNotFoundException {

        Context context = getContext();

        final Account[] allAccounts = accountManager.getAccounts();
        rootIdToStorageManager = new HashMap<>(allAccounts.length);

        if (context == null) {
            throw new FileNotFoundException("Context may not be null!");
        }

        final ContentResolver contentResolver = context.getContentResolver();
        for (Account account : allAccounts) {
            final FileDataStorageManager storageManager = new FileDataStorageManager(account, contentResolver);
            final OCFile rootDir = storageManager.getFileByPath(ROOT_PATH);
            rootIdToStorageManager.put(rootDir.getFileId(), storageManager);
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

    List<OCFile> findFiles(OCFile root, String query) {
        List<OCFile> result = new ArrayList<>();
        for (OCFile f : currentStorageManager.getFolderContent(root, false)) {
            if (f.isFolder()) {
                result.addAll(findFiles(f, query));
            } else if (f.getFileName().contains(query)) {
                result.add(f);
            }
        }
        return result;
    }
}
