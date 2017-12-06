/**
 *   nextCloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016  Bartosz Przybylski <bart.p.pl@gmail.com>
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
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsProvider;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.utils.FileStorageUtils;

import org.nextcloud.providers.cursors.FileCursor;
import org.nextcloud.providers.cursors.RootCursor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DocumentsStorageProvider extends DocumentsProvider {

    private FileDataStorageManager mCurrentStorageManager = null;
    private static Map<Long, FileDataStorageManager> mRootIdToStorageManager;

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        initiateStorageMap();

        final RootCursor result = new RootCursor(projection);

        for (Account account : AccountUtils.getAccounts(getContext())) {
            result.addRoot(account, getContext());
        }

        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        final FileCursor result = new FileCursor(projection);
        OCFile file = mCurrentStorageManager.getFileById(docId);
        if (file != null) {
            result.addFile(file);
        }

        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {

        final long folderId = Long.parseLong(parentDocumentId);
        updateCurrentStorageManagerIfNeeded(folderId);

        final FileCursor result = new FileCursor(projection);

        final OCFile browsedDir = mCurrentStorageManager.getFileById(folderId);
        for (OCFile file : mCurrentStorageManager.getFolderContent(browsedDir, false)) {
            result.addFile(file);
        }

        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal cancellationSignal)
            throws FileNotFoundException {
        final long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile file = mCurrentStorageManager.getFileById(docId);

        Account account = mCurrentStorageManager.getAccount();
        Context context = getContext();

        if (!file.isDown()) {

            Intent i = new Intent(getContext(), FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
            i.putExtra(FileDownloader.EXTRA_FILE, file);
            context.startService(i);

            do {
                if (!waitOrGetCancelled(cancellationSignal)) {
                    return null;
                }
                file = mCurrentStorageManager.getFileById(docId);

            } while (!file.isDown());
        } else {
            FileDataStorageManager storageManager =
                    new FileDataStorageManager(account, context.getContentResolver());
            SynchronizeFileOperation sfo =
                    new SynchronizeFileOperation(file, null, account, true, context);
            RemoteOperationResult result = sfo.execute(storageManager, context);
            if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                // ISSUE 5: if the user is not running the app (this is a service!),
                // this can be very intrusive; a notification should be preferred
                Intent i = new Intent(context, ConflictsResolveActivity.class);
                i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
                i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, account);
                context.startActivity(i);
            } else {
                FileStorageUtils.checkIfFileFinishedSaving(file);
            }
        }

        return ParcelFileDescriptor.open(new File(file.getStoragePath()), ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId,
                                                     Point sizeHint,
                                                     CancellationSignal signal)
            throws FileNotFoundException {
        long docId = Long.parseLong(documentId);
        updateCurrentStorageManagerIfNeeded(docId);

        OCFile file = mCurrentStorageManager.getFileById(docId);

        File realFile = new File(file.getStoragePath());

        return new AssetFileDescriptor(
                ParcelFileDescriptor.open(realFile, ParcelFileDescriptor.MODE_READ_ONLY),
                0,
                AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) throws FileNotFoundException {
        updateCurrentStorageManagerIfNeeded(rootId);

        OCFile root = mCurrentStorageManager.getFileByPath("/");
        FileCursor result = new FileCursor(projection);

        for (OCFile f : findFiles(root, query)) {
            result.addFile(f);
        }

        return result;
    }

    private void updateCurrentStorageManagerIfNeeded(long docId) {
        if (mCurrentStorageManager == null ||
                (mRootIdToStorageManager.containsKey(docId) &&
                        mCurrentStorageManager != mRootIdToStorageManager.get(docId))) {
            mCurrentStorageManager = mRootIdToStorageManager.get(docId);
        }
    }

    private void updateCurrentStorageManagerIfNeeded(String rootId) {
        for (FileDataStorageManager data : mRootIdToStorageManager.values()) {
            if (data.getAccount().name.equals(rootId)) {
                mCurrentStorageManager = data;
            }
        }
    }

    private void initiateStorageMap() {

        mRootIdToStorageManager = new HashMap<>();

        ContentResolver contentResolver = getContext().getContentResolver();

        for (Account account : AccountUtils.getAccounts(getContext())) {
            final FileDataStorageManager storageManager =
                    new FileDataStorageManager(account, contentResolver);
            final OCFile rootDir = storageManager.getFileByPath("/");
            mRootIdToStorageManager.put(rootDir.getFileId(), storageManager);
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

    Vector<OCFile> findFiles(OCFile root, String query) {
        Vector<OCFile> result = new Vector<>();
        for (OCFile f : mCurrentStorageManager.getFolderContent(root, false)) {
            if (f.isFolder()) {
                result.addAll(findFiles(f, query));
            } else if (f.getFileName().contains(query)) {
                result.add(f);
            }
        }
        return result;
    }
}
