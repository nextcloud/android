/*
 * ownCloud Android client application
 *
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.owncloud.android.MainApp;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.lib.common.network.WebdavEntry;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.ShareeUser;
import com.owncloud.android.lib.resources.status.CapabilityBooleanType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.RemoteOperationFailedException;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;


@Getter
public class FileDataStorageManager {
    private static final String TAG = FileDataStorageManager.class.getSimpleName();

    private static final String AND = " = ? AND ";
    private static final String FAILED_TO_INSERT_MSG = "Fail to insert file to database ";
    private static final String SENDING_TO_FILECONTENTPROVIDER_MSG = "Sending %d operations to FileContentProvider";
    private static final String EXCEPTION_MSG = "Exception in batch of operations ";

    public static final int ROOT_PARENT_ID = 0;
    public static final String NULL_STRING = "null";

    private ContentResolver contentResolver;
    private ContentProviderClient contentProviderClient;
    @Setter private Account account;

    public FileDataStorageManager(Account account, ContentResolver contentResolver) {
        this.contentProviderClient = null;
        this.contentResolver = contentResolver;
        this.account = account;
    }

    public FileDataStorageManager(Account account, ContentProviderClient contentProviderClient) {
        this.contentProviderClient = contentProviderClient;
        this.contentResolver = null;
        this.account = account;
    }

    private Cursor executeQuery(Uri uri, String[] projection, String selection,
                                String[] selectionArgs, String sortOrder, String errorMessage) {
        Cursor cursor;
        ContentResolver contentResolver = getContentResolver();

        if (contentResolver != null) {
            cursor = contentResolver.query(uri,
                                           projection,
                                           selection,
                                           selectionArgs,
                                           sortOrder);
        } else {
            try {
                cursor = getContentProviderClient().query(uri,
                                                          projection,
                                                          selection,
                                                          selectionArgs,
                                                          sortOrder);
            } catch (RemoteException e) {
                Log_OC.e(TAG, errorMessage + e.getMessage(), e);
                cursor = null;
            }
        }

        return cursor;
    }

    private Cursor executeQuery(Uri uri, String selection, String[] selectionArgs, String errorMessage) {
        return executeQuery(uri, null, selection, selectionArgs, null, errorMessage);
    }

    public OCFile getFileByPath(String path) {
        Cursor cursor = getFileCursorForValue(ProviderTableMeta.FILE_PATH, path);
        OCFile file = null;
        if (cursor.moveToFirst()) {
            file = createFileInstance(cursor);
        }
        cursor.close();
        if (file == null && OCFile.ROOT_PATH.equals(path)) {
            return createRootDir(); // root should always exist
        }
        return file;
    }

    public @Nullable
    OCFile getFileById(long id) {
        Cursor cursor = getFileCursorForValue(ProviderTableMeta._ID, String.valueOf(id));
        OCFile file = null;
        if (cursor.moveToFirst()) {
            file = createFileInstance(cursor);
        }
        cursor.close();
        return file;
    }

    public OCFile getFileByLocalPath(String path) {
        Cursor cursor = getFileCursorForValue(ProviderTableMeta.FILE_STORAGE_PATH, path);
        OCFile file = null;
        if (cursor.moveToFirst()) {
            file = createFileInstance(cursor);
        }
        cursor.close();
        return file;
    }

    public @Nullable
    OCFile getFileByRemoteId(String remoteId) {
        Cursor cursor = getFileCursorForValue(ProviderTableMeta.FILE_REMOTE_ID, remoteId);
        OCFile file = null;
        if (cursor.moveToFirst()) {
            file = createFileInstance(cursor);
        }
        cursor.close();
        return file;
    }

    public boolean fileExists(long id) {
        return fileExists(ProviderTableMeta._ID, String.valueOf(id));
    }

    public boolean fileExists(String path) {
        return fileExists(ProviderTableMeta.FILE_PATH, path);
    }


    public List<OCFile> getFolderContent(OCFile ocFile, boolean onlyOnDevice) {
        if (ocFile != null && ocFile.isFolder() && ocFile.fileExists()) {
            return getFolderContent(ocFile.getFileId(), onlyOnDevice);
        } else {
            return new ArrayList<>();
        }
    }


    public List<OCFile> getFolderImages(OCFile folder, boolean onlyOnDevice) {
        List<OCFile> ret = new ArrayList<>();

        if (folder != null) {
            // TODO better implementation, filtering in the access to database instead of here
            List<OCFile> tmp = getFolderContent(folder, onlyOnDevice);

            for (OCFile file : tmp) {
                if (MimeTypeUtil.isImage(file)) {
                    ret.add(file);
                }
            }
        }
        return ret;
    }

    public boolean saveFile(OCFile file) {
        boolean overridden = false;
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_MODIFIED, file.getModificationTimestamp());
        cv.put(
            ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
            file.getModificationTimestampAtLastSyncForData()
        );
        cv.put(ProviderTableMeta.FILE_CREATION, file.getCreationTimestamp());
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.getFileLength());
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.getMimeType());
        cv.put(ProviderTableMeta.FILE_NAME, file.getFileName());
        cv.put(ProviderTableMeta.FILE_ENCRYPTED_NAME, file.getEncryptedFileName());
        cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
        cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
        if (!file.isFolder()) {
            cv.put(ProviderTableMeta.FILE_IS_ENCRYPTED, file.isEncrypted());
            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
        }
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, account.name);
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
        cv.put(ProviderTableMeta.FILE_ETAG_ON_SERVER, file.getEtagOnServer());
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, file.isSharedViaLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, file.isSharedWithSharee() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, file.getPublicLink());
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.getPermissions());
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.getRemoteId());
        cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.isUpdateThumbnailNeeded());
        cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading());
        cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, file.getEtagInConflict());
        cv.put(ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT, file.getUnreadCommentsCount());
        cv.put(ProviderTableMeta.FILE_OWNER_ID, file.getOwnerId());
        cv.put(ProviderTableMeta.FILE_OWNER_DISPLAY_NAME, file.getOwnerDisplayName());
        cv.put(ProviderTableMeta.FILE_NOTE, file.getNote());
        cv.put(ProviderTableMeta.FILE_SHAREES, new Gson().toJson(file.getSharees()));
        cv.put(ProviderTableMeta.FILE_RICH_WORKSPACE, file.getRichWorkspace());

        boolean sameRemotePath = fileExists(file.getRemotePath());
        if (sameRemotePath ||
            fileExists(file.getFileId())) {  // for renamed files; no more delete and create


            if (sameRemotePath) {
                OCFile oldFile = getFileByPath(file.getRemotePath());
                file.setFileId(oldFile.getFileId());
            }

            overridden = true;
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv,
                                            ProviderTableMeta._ID + " = ?",
                                            new String[]{String.valueOf(file.getFileId())});
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI,
                                                      cv, ProviderTableMeta._ID + " = ?",
                                                      new String[]{String.valueOf(file.getFileId())});
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }
        } else {
            Uri result_uri = null;
            if (getContentResolver() != null) {
                result_uri = getContentResolver().insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
            } else {
                try {
                    result_uri = getContentProviderClient().insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }
            if (result_uri != null) {
                long new_id = Long.parseLong(result_uri.getPathSegments().get(1));
                file.setFileId(new_id);
            }
        }

        return overridden;
    }

    /**
     * traverses a files parent tree to be able to store a file with its parents. Throws a
     * RemoteOperationFailedException in case the parent can't be retrieved.
     *
     * @param file    the file
     * @param context the app context
     * @return the parent file
     */
    public OCFile saveFileWithParent(OCFile file, Context context) {
        if (file.getParentId() == 0 && !OCFile.ROOT_PATH.equals(file.getRemotePath())) {
            String remotePath = file.getRemotePath();
            String parentPath = remotePath.substring(0, remotePath.lastIndexOf(file.getFileName()));

            OCFile parentFile = getFileByPath(parentPath);

            OCFile returnFile;
            if (parentFile == null) {
                // remote request
                ReadFileRemoteOperation operation = new ReadFileRemoteOperation(parentPath);
                RemoteOperationResult result = operation.execute(getAccount(), context);
                if (result.isSuccess()) {
                    OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));

                    returnFile = saveFileWithParent(remoteFolder, context);
                } else {
                    Exception exception = result.getException();
                    String message = "Error during saving file with parents: " + file.getRemotePath() + " / "
                        + result.getLogMessage();

                    if (exception != null) {
                        throw new RemoteOperationFailedException(message, exception);
                    } else {
                        throw new RemoteOperationFailedException(message);
                    }
                }
            } else {
                returnFile = saveFileWithParent(parentFile, context);
            }

            file.setParentId(returnFile.getFileId());
            saveFile(file);
        }

        return file;
    }

    private boolean isFileExists(ArrayList<OCFile> filesExists, OCFile file) {
        for (Iterator<OCFile> iterator = filesExists.iterator(); iterator.hasNext(); ) {
            OCFile ocFile = iterator.next();
            if (file.getFileId() == ocFile.getFileId()
                || file.getRemotePath().equals(ocFile.getRemotePath())) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts or updates the list of files contained in a given folder.
     * <p>
     * CALLER IS RESPONSIBLE FOR GRANTING RIGHT UPDATE OF INFORMATION, NOT THIS METHOD. HERE ONLY DATA CONSISTENCY
     * SHOULD BE GRANTED
     *
     * @param folder
     * @param updatedFiles
     * @param filesToRemove
     */
    public void saveFolder(OCFile folder, ArrayList<OCFile> updatedFiles, Collection<OCFile> filesToRemove) {
        Log_OC.d(TAG, "Saving folder " + folder.getRemotePath() + " with " + updatedFiles.size()
            + " children and " + filesToRemove.size() + " files to remove");

        ArrayList<ContentProviderOperation> operations = new ArrayList<>(updatedFiles.size());

        ArrayList<OCFile> fileExistList = getFilesExistsID(updatedFiles);

        // prepare operations to insert or update files to save in the given folder
        for (OCFile file : updatedFiles) {
            ContentValues contentValues = createContentValueForFile(file, folder);

            if (isFileExists(fileExistList, file)) {
                long fileId;
                if (file.fileExists()) {
                    fileId = file.getFileId();
                } else {
                    fileId = getFileByPath(file.getRemotePath()).getFileId();
                }
                // updating an existing file
                operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI)
                                   .withValues(contentValues)
                                   .withSelection(ProviderTableMeta._ID + " = ?", new String[]{String.valueOf(fileId)})
                                   .build());
            } else {
                // adding a new file
                operations.add(ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI).withValues(contentValues).build());
            }
        }

        // prepare operations to remove files in the given folder
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " = ?";
        String[] whereArgs = new String[2];
        for (OCFile file : filesToRemove) {
            if (file.getParentId() == folder.getFileId()) {
                whereArgs[0] = account.name;
                whereArgs[1] = file.getRemotePath();
                if (file.isFolder()) {
                    operations.add(ContentProviderOperation.newDelete(
                        ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, file.getFileId()))
                                       .withSelection(where, whereArgs).build());

                    File localFolder = new File(FileStorageUtils.getDefaultSavePathFor(account.name, file));
                    if (localFolder.exists()) {
                        removeLocalFolder(localFolder);
                    }
                } else {
                    operations.add(ContentProviderOperation.newDelete(
                        ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, file.getFileId()))
                                       .withSelection(where, whereArgs).build());

                    if (file.isDown()) {
                        String path = file.getStoragePath();
                        if (new File(path).delete() && MimeTypeUtil.isMedia(file.getMimeType())) {
                            triggerMediaScan(path); // notify MediaScanner about removed file
                        }
                    }
                }
            }
        }

        // update metadata of folder
        ContentValues cv = createContentValueForFile(folder);

        operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI)
                           .withValues(cv)
                           .withSelection(ProviderTableMeta._ID + " = ?", new String[]{String.valueOf(folder.getFileId())})
                           .build());

        // apply operations in batch
        ContentProviderResult[] results = null;
        Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size()));

        try {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver != null) {
                results = contentResolver.applyBatch(MainApp.getAuthority(), operations);
            } else {
                results = getContentProviderClient().applyBatch(operations);
            }

        } catch (OperationApplicationException | RemoteException e) {
            Log_OC.e(TAG, EXCEPTION_MSG + e.getMessage(), e);
        }

        // update new id in file objects for insertions
        if (results != null) {
            long newId;
            Iterator<OCFile> filesIt = updatedFiles.iterator();
            OCFile file;
            for (ContentProviderResult result : results) {
                if (filesIt.hasNext()) {
                    file = filesIt.next();
                } else {
                    file = null;
                }
                if (result.uri != null) {
                    newId = Long.parseLong(result.uri.getPathSegments().get(1));
                    if (file != null) {
                        file.setFileId(newId);
                    }
                }
            }
        }
    }

    private ContentValues createContentValueForFile(OCFile folder) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_MODIFIED, folder.getModificationTimestamp());
        cv.put(
            ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
            folder.getModificationTimestampAtLastSyncForData()
        );
        cv.put(ProviderTableMeta.FILE_CREATION, folder.getCreationTimestamp());
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, 0);
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, folder.getMimeType());
        cv.put(ProviderTableMeta.FILE_NAME, folder.getFileName());
        cv.put(ProviderTableMeta.FILE_PARENT, folder.getParentId());
        cv.put(ProviderTableMeta.FILE_PATH, folder.getRemotePath());
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, account.name);
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, folder.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, folder.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_ETAG, folder.getEtag());
        cv.put(ProviderTableMeta.FILE_ETAG_ON_SERVER, folder.getEtagOnServer());
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, folder.isSharedViaLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, folder.isSharedWithSharee() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, folder.getPublicLink());
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, folder.getPermissions());
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, folder.getRemoteId());
        cv.put(ProviderTableMeta.FILE_FAVORITE, folder.isFavorite());
        cv.put(ProviderTableMeta.FILE_IS_ENCRYPTED, folder.isEncrypted());
        cv.put(ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT, folder.getUnreadCommentsCount());
        cv.put(ProviderTableMeta.FILE_OWNER_ID, folder.getOwnerId());
        cv.put(ProviderTableMeta.FILE_OWNER_DISPLAY_NAME, folder.getOwnerDisplayName());
        cv.put(ProviderTableMeta.FILE_NOTE, folder.getNote());
        cv.put(ProviderTableMeta.FILE_SHAREES, new Gson().toJson(folder.getSharees()));
        cv.put(ProviderTableMeta.FILE_RICH_WORKSPACE, folder.getRichWorkspace());

        return cv;
    }

    private ContentValues createContentValueForFile(OCFile file, OCFile folder) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_MODIFIED, file.getModificationTimestamp());
        cv.put(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA, file.getModificationTimestampAtLastSyncForData());
        cv.put(ProviderTableMeta.FILE_CREATION, file.getCreationTimestamp());
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.getFileLength());
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.getMimeType());
        cv.put(ProviderTableMeta.FILE_NAME, file.getFileName());
        cv.put(ProviderTableMeta.FILE_ENCRYPTED_NAME, file.getEncryptedFileName());
        cv.put(ProviderTableMeta.FILE_PARENT, folder.getFileId());
        cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
        if (!file.isFolder()) {
            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
        }
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, account.name);
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
        cv.put(ProviderTableMeta.FILE_ETAG_ON_SERVER, file.getEtagOnServer());
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, file.isSharedViaLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, file.isSharedWithSharee() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, file.getPublicLink());
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.getPermissions());
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.getRemoteId());
        cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.isUpdateThumbnailNeeded());
        cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading());
        cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, file.getEtagInConflict());
        cv.put(ProviderTableMeta.FILE_FAVORITE, file.isFavorite());
        cv.put(ProviderTableMeta.FILE_IS_ENCRYPTED, file.isEncrypted());
        cv.put(ProviderTableMeta.FILE_MOUNT_TYPE, file.getMountType().ordinal());
        cv.put(ProviderTableMeta.FILE_HAS_PREVIEW, file.isPreviewAvailable() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT, file.getUnreadCommentsCount());
        cv.put(ProviderTableMeta.FILE_OWNER_ID, file.getOwnerId());
        cv.put(ProviderTableMeta.FILE_OWNER_DISPLAY_NAME, file.getOwnerDisplayName());
        cv.put(ProviderTableMeta.FILE_NOTE, file.getNote());
        cv.put(ProviderTableMeta.FILE_SHAREES, new Gson().toJson(file.getSharees()));
        cv.put(ProviderTableMeta.FILE_RICH_WORKSPACE, file.getRichWorkspace());

        return cv;
    }


    public boolean removeFile(OCFile file, boolean removeDBData, boolean removeLocalCopy) {
        boolean success = true;
        if (file != null) {
            if (file.isFolder()) {
                success = removeFolder(file, removeDBData, removeLocalCopy);
            } else {
                if (removeDBData) {
                    //Uri file_uri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE,
                    // ""+file.getFileId());
                    Uri file_uri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, file.getFileId());
                    String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " = ?";
                    String[] whereArgs = new String[]{account.name, file.getRemotePath()};
                    int deleted = 0;
                    if (getContentProviderClient() != null) {
                        try {
                            deleted = getContentProviderClient().delete(file_uri, where, whereArgs);
                        } catch (RemoteException e) {
                            Log_OC.d(TAG, e.getMessage(), e);
                        }
                    } else {
                        deleted = getContentResolver().delete(file_uri, where, whereArgs);
                    }
                    success = deleted > 0;
                }
                String localPath = file.getStoragePath();
                if (removeLocalCopy && file.isDown() && localPath != null && success) {
                    success = new File(localPath).delete();
                    if (success) {
                        deleteFileInMediaScan(localPath);
                    }
                    if (!removeDBData && success) {
                        // maybe unnecessary, but should be checked TODO remove if unnecessary
                        file.setStoragePath(null);
                        saveFile(file);
                        saveConflict(file, null);
                    }
                }
            }
        } else {
            return false;
        }

        return success;
    }


    public boolean removeFolder(OCFile folder, boolean removeDBData, boolean removeLocalContent) {
        boolean success = true;
        if (folder != null && folder.isFolder()) {
            if (removeDBData && folder.getFileId() != -1) {
                success = removeFolderInDb(folder);
            }
            if (removeLocalContent && success) {
                success = removeLocalFolder(folder);
            }
        } else {
            success = false;
        }

        return success;
    }

    private boolean removeFolderInDb(OCFile folder) {
        Uri folder_uri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, String.valueOf(folder.getFileId())); // URI
        // for recursive deletion
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " = ?";
        String[] whereArgs = new String[]{account.name, folder.getRemotePath()};
        int deleted = 0;
        if (getContentProviderClient() != null) {
            try {
                deleted = getContentProviderClient().delete(folder_uri, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.d(TAG, e.getMessage(), e);
            }
        } else {
            deleted = getContentResolver().delete(folder_uri, where, whereArgs);
        }
        return deleted > 0;
    }

    private boolean removeLocalFolder(OCFile folder) {
        boolean success = true;
        String localFolderPath = FileStorageUtils.getDefaultSavePathFor(account.name, folder);
        File localFolder = new File(localFolderPath);
        if (localFolder.exists()) {
            // stage 1: remove the local files already registered in the files database
            List<OCFile> files = getFolderContent(folder.getFileId(), false);
            for (OCFile file : files) {
                if (file.isFolder()) {
                    success &= removeLocalFolder(file);
                } else {
                    if (file.isDown()) {
                        File localFile = new File(file.getStoragePath());
                        success &= localFile.delete();
                        if (success) {
                            // notify MediaScanner about removed file
                            deleteFileInMediaScan(file.getStoragePath());
                            file.setStoragePath(null);
                            saveFile(file);
                        }
                    }
                }
            }

            // stage 2: remove the folder itself and any local file inside out of sync;
            //          for instance, after clearing the app cache or reinstalling
            success &= removeLocalFolder(localFolder);
        }
        return success;
    }

    private boolean removeLocalFolder(File localFolder) {
        boolean success = true;
        File[] localFiles = localFolder.listFiles();
        if (localFiles != null) {
            for (File localFile : localFiles) {
                if (localFile.isDirectory()) {
                    success &= removeLocalFolder(localFile);
                } else {
                    success &= localFile.delete();
                }
            }
        }
        success &= localFolder.delete();
        return success;
    }

    /**
     * Updates database and file system for a file or folder that was moved to a different location.
     * <p>
     * TODO explore better (faster) implementations TODO throw exceptions up !
     */
    public void moveLocalFile(OCFile file, String targetPath, String targetParentPath) {

        if (file != null && file.fileExists() && !OCFile.ROOT_PATH.equals(file.getFileName())) {

            OCFile targetParent = getFileByPath(targetParentPath);
            if (targetParent == null) {
                throw new IllegalStateException("Parent folder of the target path does not exist!!");
            }

            /// 1. get all the descendants of the moved element in a single QUERY
            Cursor c = null;
            if (getContentProviderClient() != null) {
                try {
                    c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " LIKE ? ",
                        new String[]{account.name, file.getRemotePath() + "%"},
                        ProviderTableMeta.FILE_PATH + " ASC "
                    );
                } catch (RemoteException e) {
                    Log_OC.e(TAG, e.getMessage(), e);
                }

            } else {
                c = getContentResolver().query(
                    ProviderTableMeta.CONTENT_URI,
                    null,
                    ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " LIKE ? ",
                    new String[]{account.name, file.getRemotePath() + "%"},
                    ProviderTableMeta.FILE_PATH + " ASC "
                );
            }

            /// 2. prepare a batch of update operations to change all the descendants
            ArrayList<ContentProviderOperation> operations = new ArrayList<>(c.getCount());
            String defaultSavePath = FileStorageUtils.getSavePath(account.name);
            List<String> originalPathsToTriggerMediaScan = new ArrayList<>();
            List<String> newPathsToTriggerMediaScan = new ArrayList<>();
            if (c.moveToFirst()) {
                int lengthOfOldPath = file.getRemotePath().length();
                int lengthOfOldStoragePath = defaultSavePath.length() + lengthOfOldPath;
                String[] fileId = new String[1];
                do {
                    ContentValues cv = new ContentValues(); // keep construction in the loop
                    OCFile child = createFileInstance(c);
                    cv.put(
                        ProviderTableMeta.FILE_PATH,
                        targetPath + child.getRemotePath().substring(lengthOfOldPath)
                    );
                    if (child.getStoragePath() != null && child.getStoragePath().startsWith(defaultSavePath)) {
                        // update link to downloaded content - but local move is not done here!
                        String targetLocalPath = defaultSavePath + targetPath +
                            child.getStoragePath().substring(lengthOfOldStoragePath);

                        cv.put(ProviderTableMeta.FILE_STORAGE_PATH, targetLocalPath);

                        if (MimeTypeUtil.isMedia(child.getMimeType())) {
                            originalPathsToTriggerMediaScan.add(child.getStoragePath());
                            newPathsToTriggerMediaScan.add(targetLocalPath);
                        }

                    }
                    if (child.getRemotePath().equals(file.getRemotePath())) {
                        cv.put(ProviderTableMeta.FILE_PARENT, targetParent.getFileId());
                    }
                    fileId[0] = String.valueOf(child.getFileId());
                    operations.add(
                        ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                            withValues(cv).
                            withSelection(ProviderTableMeta._ID + " = ?", fileId)
                            .build());

                } while (c.moveToNext());
            }
            c.close();

            /// 3. apply updates in batch
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);
                } else {
                    getContentProviderClient().applyBatch(operations);
                }

            } catch (Exception e) {
                Log_OC.e(TAG, "Fail to update " + file.getFileId() + " and descendants in database", e);
            }

            /// 4. move in local file system
            String originalLocalPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
            String targetLocalPath = defaultSavePath + targetPath;
            File localFile = new File(originalLocalPath);
            boolean renamed = false;
            if (localFile.exists()) {
                File targetFile = new File(targetLocalPath);
                File targetFolder = targetFile.getParentFile();
                if (!targetFolder.exists() && !targetFolder.mkdirs()) {
                    Log_OC.e(TAG, "Unable to create parent folder " + targetFolder.getAbsolutePath());
                }
                renamed = localFile.renameTo(targetFile);
            }

            if (renamed) {
                Iterator<String> it = originalPathsToTriggerMediaScan.iterator();
                while (it.hasNext()) {
                    // Notify MediaScanner about removed file
                    deleteFileInMediaScan(it.next());
                }
                it = newPathsToTriggerMediaScan.iterator();
                while (it.hasNext()) {
                    // Notify MediaScanner about new file/folder
                    triggerMediaScan(it.next());
                }
            }
        }
    }

    public void copyLocalFile(OCFile file, String targetPath) {

        if (file != null && file.fileExists() && !OCFile.ROOT_PATH.equals(file.getFileName())) {
            String localPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
            File localFile = new File(localPath);
            boolean copied = false;
            String defaultSavePath = FileStorageUtils.getSavePath(account.name);
            if (localFile.exists()) {
                File targetFile = new File(defaultSavePath + targetPath);
                File targetFolder = targetFile.getParentFile();
                if (!targetFolder.exists() && !targetFolder.mkdirs()) {
                    Log_OC.e(TAG, "Unable to create parent folder " + targetFolder.getAbsolutePath());
                }
                copied = FileStorageUtils.copyFile(localFile, targetFile);
            }
            Log_OC.d(TAG, "Local file COPIED : " + copied);
        }
    }

    public void migrateStoredFiles(String srcPath, String dstPath)
        throws RemoteException, OperationApplicationException {
        Cursor cursor;
        try {
            if (getContentResolver() != null) {
                cursor = getContentResolver().query(ProviderTableMeta.CONTENT_URI_FILE,
                                                    null,
                                                    ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL",
                                                    null,
                                                    null);

            } else {
                cursor = getContentProviderClient().query(ProviderTableMeta.CONTENT_URI_FILE,
                                                          new String[]{ProviderTableMeta._ID, ProviderTableMeta.FILE_STORAGE_PATH},
                                                          ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL",
                                                          null,
                                                          null);
            }
        } catch (RemoteException e) {
            Log_OC.e(TAG, e.getMessage(), e);
            throw e;
        }

        ArrayList<ContentProviderOperation> operations = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            String[] fileId = new String[1];
            do {
                ContentValues cv = new ContentValues();
                fileId[0] = String.valueOf(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta._ID)));
                String oldFileStoragePath =
                    cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));

                if (oldFileStoragePath.startsWith(srcPath)) {

                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, oldFileStoragePath.replaceFirst(srcPath, dstPath));

                    operations.add(
                        ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                            withValues(cv).
                            withSelection(ProviderTableMeta._ID + " = ?", fileId)
                            .build());
                }

            } while (cursor.moveToNext());
        }
        cursor.close();

        /// 3. apply updates in batch
        if (getContentResolver() != null) {
            getContentResolver().applyBatch(MainApp.getAuthority(), operations);
        } else {
            getContentProviderClient().applyBatch(operations);
        }
    }

    private List<OCFile> getFolderContent(long parentId, boolean onlyOnDevice) {
        List<OCFile> folderContent = new ArrayList<>();

        Uri requestURI = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, String.valueOf(parentId));

        String selection = ProviderTableMeta.FILE_PARENT + " = ?";
        String[] selectionArgs = {String.valueOf(parentId)};

        Cursor cursor = executeQuery(requestURI, selection, selectionArgs, "");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    OCFile child = createFileInstance(cursor);
                    if (!onlyOnDevice || child.existsOnDevice()) {
                        folderContent.add(child);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return folderContent;
    }


    private OCFile createRootDir() {
        OCFile file = new OCFile(OCFile.ROOT_PATH);
        file.setMimeType(MimeType.DIRECTORY);
        file.setParentId(FileDataStorageManager.ROOT_PARENT_ID);
        saveFile(file);
        return file;
    }

    private boolean fileExists(String cmp_key, String value) {
        Cursor cursor = getFileCursorForValue(cmp_key, value);
        boolean isExists = false;

        if (cursor == null) {
            Log_OC.e(TAG, "Couldn't determine file existance, assuming non existance");
        } else {
            isExists = cursor.moveToFirst();
            cursor.close();
        }

        return isExists;
    }

    private ArrayList<OCFile> getFilesExistsID(ArrayList<OCFile> updatedFiles) {

        ArrayList<OCFile> existsFiles = new ArrayList<>();
        ArrayList<String> listIDString = new ArrayList<>();
        ArrayList<String> listPathString = new ArrayList<>();

        int totalSize, processSize, loopSize;
        totalSize = updatedFiles.size();
        processSize = 0;

        do {
            loopSize = Math.min((totalSize - processSize), 499);

            listIDString.clear();
            listPathString.clear();
            StringBuilder inList = new StringBuilder(loopSize * 2);
            for (int i = 0; i < loopSize; i++, processSize++) {
                OCFile file = updatedFiles.get(processSize);
                if (i > 0) {
                    inList.append(",");
                }
                inList.append("?");
                listIDString.add(String.valueOf(file.getFileId()));
                listPathString.add(file.getRemotePath());
            }

            String selection = ProviderTableMeta.FILE_ACCOUNT_OWNER
                + " = ? AND ("
                + ProviderTableMeta._ID
                + " IN (" + inList + ") OR "
                + ProviderTableMeta.FILE_PATH
                + " IN (" + inList + "))";

            ArrayList<String> selectionArgsList = new ArrayList<>();
            selectionArgsList.add(account.name);
            selectionArgsList.addAll(listIDString);
            selectionArgsList.addAll(listPathString);
            String[] selectionArgs = selectionArgsList.toArray(new String[0]);

            Cursor cursor = executeQuery(ProviderTableMeta.CONTENT_URI, selection, selectionArgs, "getFilesExistsID ");

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        OCFile file = new OCFile(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_PATH)));
                        file.setFileId(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta._ID)));
                        existsFiles.add(file);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } while ((totalSize - processSize) > 0);

        return existsFiles;
    }

    private Cursor getFileCursorForValue(String key, String value) {
        String selection = key
            + AND
            + ProviderTableMeta.FILE_ACCOUNT_OWNER
            + " = ?";
        String[] selectionArgs = {value, account.name};

        return executeQuery(ProviderTableMeta.CONTENT_URI, selection, selectionArgs, "Could not get file details: ");
    }

    @Nullable
    private OCFile createFileInstanceFromVirtual(Cursor cursor) {
        long fileId = cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.VIRTUAL_OCFILE_ID));

        return getFileById(fileId);
    }

    private OCFile createFileInstance(Cursor c) {
        OCFile file = null;
        if (c != null) {
            file = new OCFile(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH)));
            file.setFileId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
            file.setParentId(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_PARENT)));
            file.setEncryptedFileName(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ENCRYPTED_NAME)));
            file.setMimeType(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)));
            file.setStoragePath(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)));
            if (file.getStoragePath() == null) {
                // try to find existing file and bind it with current account;
                // with the current update of SynchronizeFolderOperation, this won't be
                // necessary anymore after a full synchronization of the account
                File f = new File(FileStorageUtils.getDefaultSavePathFor(account.name, file));
                if (f.exists()) {
                    file.setStoragePath(f.getAbsolutePath());
                    file.setLastSyncDateForData(f.lastModified());
                }
            }
            file.setFileLength(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH)));
            file.setCreationTimestamp(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_CREATION)));
            file.setModificationTimestamp(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_MODIFIED)));
            file.setModificationTimestampAtLastSyncForData(c.getLong(
                c.getColumnIndex(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)));
            file.setLastSyncDateForProperties(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_LAST_SYNC_DATE)));
            file.setLastSyncDateForData(c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA)));
            file.setEtag(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG)));
            file.setEtagOnServer(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG_ON_SERVER)));
            file.setSharedViaLink(c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_SHARED_VIA_LINK)) == 1);
            file.setSharedWithSharee(c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_SHARED_WITH_SHAREE)) == 1);
            file.setPublicLink(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PUBLIC_LINK)));
            file.setPermissions(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PERMISSIONS)));
            file.setRemoteId(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID)));
            file.setUpdateThumbnailNeeded(c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_UPDATE_THUMBNAIL)) == 1);
            file.setDownloading(c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_IS_DOWNLOADING)) == 1);
            file.setEtagInConflict(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG_IN_CONFLICT)));
            file.setFavorite(c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_FAVORITE)) == 1);
            file.setEncrypted(c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_IS_ENCRYPTED)) == 1);
            if (file.isEncrypted()) {
                file.setFileName(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_NAME)));
            }
            file.setMountType(WebdavEntry.MountType.values()[c.getInt(
                c.getColumnIndex(ProviderTableMeta.FILE_MOUNT_TYPE))]);
            file.setPreviewAvailable(c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_HAS_PREVIEW)) == 1);
            file.setUnreadCommentsCount(c.getInt(c.getColumnIndex(ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT)));
            file.setOwnerId(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_OWNER_ID)));
            file.setOwnerDisplayName(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_OWNER_DISPLAY_NAME)));
            file.setNote(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_NOTE)));
            file.setRichWorkspace(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_RICH_WORKSPACE)));

            String sharees = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_SHAREES));

            if (sharees == null || NULL_STRING.equals(sharees) || sharees.isEmpty()) {
                file.setSharees(new ArrayList<>());
            } else {
                try {
                    ShareeUser[] shareesArray = new Gson().fromJson(sharees, ShareeUser[].class);

                    file.setSharees(new ArrayList<>(Arrays.asList(shareesArray)));
                } catch (JsonSyntaxException e) {
                    // ignore saved value due to api change
                    file.setSharees(new ArrayList<>());
                }
            }
        }

        return file;
    }

    public void saveShare(OCShare share) {
        Uri contentUriShare = ProviderTableMeta.CONTENT_URI_SHARE;
        ContentValues contentValues = new ContentValues();
        contentValues.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, share.getFileSource());
        contentValues.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, share.getItemSource());
        contentValues.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, share.getShareType().getValue());
        contentValues.put(ProviderTableMeta.OCSHARES_SHARE_WITH, share.getShareWith());
        contentValues.put(ProviderTableMeta.OCSHARES_PATH, share.getPath());
        contentValues.put(ProviderTableMeta.OCSHARES_PERMISSIONS, share.getPermissions());
        contentValues.put(ProviderTableMeta.OCSHARES_SHARED_DATE, share.getSharedDate());
        contentValues.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, share.getExpirationDate());
        contentValues.put(ProviderTableMeta.OCSHARES_TOKEN, share.getToken());
        contentValues.put(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME, share.getSharedWithDisplayName());
        contentValues.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, share.isFolder() ? 1 : 0);
        contentValues.put(ProviderTableMeta.OCSHARES_USER_ID, share.getUserId());
        contentValues.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getRemoteId());
        contentValues.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, account.name);
        contentValues.put(ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED, share.isPasswordProtected() ? 1 : 0);
        contentValues.put(ProviderTableMeta.OCSHARES_NOTE, share.getNote());
        contentValues.put(ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD, share.isHideFileDownload());

        if (shareExistsForRemoteId(share.getRemoteId())) {// for renamed files; no more delete and create
            String where = ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + " = ?";
            String[] selectionArgs = {String.valueOf(share.getRemoteId())};

            if (getContentResolver() != null) {
                getContentResolver().update(contentUriShare,
                                            contentValues,
                                            where,
                                            selectionArgs);
            } else {
                try {
                    getContentProviderClient().update(contentUriShare,
                                                      contentValues,
                                                      where,
                                                      selectionArgs);
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }
        } else {
            Uri resultUri = null;
            if (getContentResolver() != null) {
                resultUri = getContentResolver().insert(contentUriShare, contentValues);
            } else {
                try {
                    resultUri = getContentProviderClient().insert(contentUriShare, contentValues);
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }
            if (resultUri != null) {
                long new_id = Long.parseLong(resultUri.getPathSegments().get(1));
                share.setId(new_id);
            }
        }
    }

    /**
     * Retrieves an stored {@link OCShare} given its id.
     *
     * @param id Identifier.
     * @return Stored {@link OCShare} given its id.
     */
    public OCShare getShareById(long id) {
        OCShare share = null;
        Cursor cursor = getShareCursorForValue(ProviderTableMeta._ID, String.valueOf(id));
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                share = createShareInstance(cursor);
            }
            cursor.close();
        }
        return share;
    }


    /**
     * Checks the existance of an stored {@link OCShare} matching the given remote id (not to be confused with the local
     * id) in the current account.
     *
     * @param remoteId Remote of the share in the server.
     * @return 'True' if a matching {@link OCShare} is stored in the current account.
     */
    private boolean shareExistsForRemoteId(long remoteId) {
        return shareExistsForValue(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, String.valueOf(remoteId));
    }


    /**
     * Checks the existance of an stored {@link OCShare} in the current account matching a given column and a value for
     * that column
     *
     * @param key   Name of the column to match.
     * @param value Value of the column to match.
     * @return 'True' if a matching {@link OCShare} is stored in the current account.
     */
    private boolean shareExistsForValue(String key, String value) {
        Cursor cursor = getShareCursorForValue(key, value);
        boolean retval = cursor.moveToFirst();
        cursor.close();

        return retval;
    }


    /**
     * Gets a {@link Cursor} for an stored {@link OCShare} in the current account matching a given column and a value
     * for that column
     *
     * @param key   Name of the column to match.
     * @param value Value of the column to match.
     * @return 'True' if a matching {@link OCShare} is stored in the current account.
     */
    private Cursor getShareCursorForValue(String key, String value) {
        String selection = key + AND
            + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = ?";
        String[] selectionArgs = {value, account.name};

        return executeQuery(ProviderTableMeta.CONTENT_URI_SHARE,
                            null,
                            selection,
                            selectionArgs,
                            null,
                            "Could not get details, assuming share does not exist: "
        );
    }


    /**
     * Get first share bound to a file with a known path and given {@link ShareType}.
     *
     * @param path      Path of the file.
     * @param type      Type of the share to get
     * @param shareWith Target of the share. Ignored in type is {@link ShareType#PUBLIC_LINK}
     * @return First {@link OCShare} instance found in DB bound to the file in 'path'
     */
    public OCShare getFirstShareByPathAndType(String path, ShareType type, String shareWith) {
        Cursor cursor;
        if (shareWith == null) {
            shareWith = "";
        }

        String selection = ProviderTableMeta.OCSHARES_PATH + AND
            + ProviderTableMeta.OCSHARES_SHARE_TYPE + AND
            + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = ?";
        if (!ShareType.PUBLIC_LINK.equals(type)) {
            selection += " AND " + ProviderTableMeta.OCSHARES_SHARE_WITH + " = ?";
        }

        String[] selectionArgs;
        if (ShareType.PUBLIC_LINK.equals(type)) {
            selectionArgs = new String[]{
                path,
                Integer.toString(type.getValue()),
                account.name
            };
        } else {
            selectionArgs = new String[]{
                path,
                Integer.toString(type.getValue()),
                account.name,
                shareWith
            };
        }

        cursor = executeQuery(
            ProviderTableMeta.CONTENT_URI_SHARE,
            null,
            selection,
            selectionArgs,
            null,
            "Could not get file details: ");


        OCShare share = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                share = createShareInstance(cursor);
            }
            cursor.close();
        }
        return share;
    }

    private OCShare createShareInstance(Cursor cursor) {
        OCShare share = new OCShare(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_PATH)));
        share.setId(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta._ID)));
        share.setFileSource(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_ITEM_SOURCE)));
        share.setShareType(ShareType.fromValue(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_TYPE))));
        share.setShareWith(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH)));
        share.setPermissions(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_PERMISSIONS)));
        share.setSharedDate(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARED_DATE)));
        share.setExpirationDate(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_EXPIRATION_DATE)));
        share.setToken(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_TOKEN)));
        share.setSharedWithDisplayName(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME)));
        share.setFolder(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_IS_DIRECTORY)) == 1);
        share.setUserId(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_USER_ID)));
        share.setRemoteId(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED)));
        share.setPasswordProtected(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED)) == 1);
        share.setNote(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_NOTE)));
        share.setHideFileDownload(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD)) == 1);

        return share;
    }

    private void resetShareFlagsInFolder(OCFile folder) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, Boolean.FALSE);
        contentValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, Boolean.FALSE);
        contentValues.put(ProviderTableMeta.FILE_PUBLIC_LINK, "");
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PARENT + " = ?";
        String[] whereArgs = new String[]{account.name, String.valueOf(folder.getFileId())};

        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, contentValues, where, whereArgs);
        } else {
            try {
                getContentProviderClient().update(ProviderTableMeta.CONTENT_URI, contentValues, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInFolder " + e.getMessage(), e);
            }
        }
    }

    private void resetShareFlagInAFile(String filePath) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, Boolean.FALSE);
        contentValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, Boolean.FALSE);
        contentValues.put(ProviderTableMeta.FILE_PUBLIC_LINK, "");
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " = ?";
        String[] whereArgs = new String[]{account.name, filePath};

        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, contentValues, where, whereArgs);
        } else {
            try {
                getContentProviderClient().update(ProviderTableMeta.CONTENT_URI, contentValues, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInFolder " + e.getMessage(), e);
            }
        }
    }

    private void cleanShares() {
        Uri contentUriShare = ProviderTableMeta.CONTENT_URI_SHARE;
        String where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = ?";
        String[] whereArgs = new String[]{account.name};

        if (getContentResolver() != null) {
            getContentResolver().delete(contentUriShare, where, whereArgs);
        } else {
            try {
                getContentProviderClient().delete(contentUriShare, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in cleanShares" + e.getMessage(), e);
            }
        }
    }

    public void saveShares(Collection<OCShare> shares) {
        cleanShares();
        ArrayList<ContentProviderOperation> operations = new ArrayList<>(shares.size());

        // prepare operations to insert or update files to save in the given folder
        for (OCShare share : shares) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, share.getFileSource());
            contentValues.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, share.getItemSource());
            contentValues.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, share.getShareType().getValue());
            contentValues.put(ProviderTableMeta.OCSHARES_SHARE_WITH, share.getShareWith());
            contentValues.put(ProviderTableMeta.OCSHARES_PATH, share.getPath());
            contentValues.put(ProviderTableMeta.OCSHARES_PERMISSIONS, share.getPermissions());
            contentValues.put(ProviderTableMeta.OCSHARES_SHARED_DATE, share.getSharedDate());
            contentValues.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, share.getExpirationDate());
            contentValues.put(ProviderTableMeta.OCSHARES_TOKEN, share.getToken());
            contentValues.put(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME, share.getSharedWithDisplayName());
            contentValues.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, share.isFolder() ? 1 : 0);
            contentValues.put(ProviderTableMeta.OCSHARES_USER_ID, share.getUserId());
            contentValues.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getRemoteId());
            contentValues.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, account.name);
            contentValues.put(ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED, share.isPasswordProtected() ? 1 : 0);
            contentValues.put(ProviderTableMeta.OCSHARES_NOTE, share.getNote());
            contentValues.put(ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD, share.isHideFileDownload());

            if (shareExistsForRemoteId(share.getRemoteId())) {
                // updating an existing file
                operations.add(
                    ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI_SHARE)
                        .withValues(contentValues)
                        .withSelection(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + " = ?",
                                       new String[]{String.valueOf(share.getRemoteId())})
                        .build());
            } else {
                // adding a new file
                operations.add(
                    ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI_SHARE)
                        .withValues(contentValues)
                        .build()
                );
            }
        }

        // apply operations in batch
        if (operations.size() > 0) {
            Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size()));
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);
                } else {
                    getContentProviderClient().applyBatch(operations);
                }
            } catch (OperationApplicationException | RemoteException e) {
                Log_OC.e(TAG, EXCEPTION_MSG + e.getMessage(), e);
            }
        }
    }

    public void removeShare(OCShare share) {
        Uri contentUriShare = ProviderTableMeta.CONTENT_URI_SHARE;
        String where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + AND +
            ProviderTableMeta._ID + " = ?";
        String[] whereArgs = {account.name, Long.toString(share.getId())};
        if (getContentProviderClient() != null) {
            try {
                getContentProviderClient().delete(contentUriShare, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.d(TAG, e.getMessage(), e);
            }
        } else {
            getContentResolver().delete(contentUriShare, where, whereArgs);
        }
    }

    public void saveSharesDB(List<OCShare> shares) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        // Reset flags & Remove shares for this files
        String filePath = "";
        for (OCShare share : shares) {
            if (!filePath.equals(share.getPath())) {
                filePath = share.getPath();
                resetShareFlagInAFile(filePath);
                operations = prepareRemoveSharesInFile(filePath, operations);
            }
        }

        // Add operations to insert shares
        operations = prepareInsertShares(shares, operations);

        // apply operations in batch
        if (operations.size() > 0) {
            Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size()));
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);
                } else {
                    getContentProviderClient().applyBatch(operations);
                }

            } catch (OperationApplicationException | RemoteException e) {
                Log_OC.e(TAG, EXCEPTION_MSG + e.getMessage(), e);
            }
        }
    }

    public void removeSharesForFile(String remotePath) {
        resetShareFlagInAFile(remotePath);
        ArrayList<ContentProviderOperation> operations = prepareRemoveSharesInFile(remotePath, new ArrayList<>());
        // apply operations in batch
        if (!operations.isEmpty()) {
            Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size()));
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);

                } else {
                    getContentProviderClient().applyBatch(operations);
                }

            } catch (OperationApplicationException | RemoteException e) {
                Log_OC.e(TAG, EXCEPTION_MSG + e.getMessage(), e);
            }
        }
    }


    public void saveSharesInFolder(ArrayList<OCShare> shares, OCFile folder) {
        resetShareFlagsInFolder(folder);
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations = prepareRemoveSharesInFolder(folder, operations);

        // prepare operations to insert or update files to save in the given folder
        operations = prepareInsertShares(shares, operations);

        // apply operations in batch
        if (operations.size() > 0) {
            Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size()));
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);
                } else {
                    getContentProviderClient().applyBatch(operations);
                }

            } catch (OperationApplicationException | RemoteException e) {
                Log_OC.e(TAG, EXCEPTION_MSG + e.getMessage(), e);
            }
        }

    }

    /**
     * Prepare operations to insert or update files to save in the given folder
     *
     * @param shares     List of shares to insert
     * @param operations List of operations
     * @return
     */
    private ArrayList<ContentProviderOperation> prepareInsertShares(
        List<OCShare> shares, ArrayList<ContentProviderOperation> operations) {

        ContentValues contentValues;
        // prepare operations to insert or update files to save in the given folder
        for (OCShare share : shares) {
            contentValues = new ContentValues();
            contentValues.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, share.getFileSource());
            contentValues.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, share.getItemSource());
            contentValues.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, share.getShareType().getValue());
            contentValues.put(ProviderTableMeta.OCSHARES_SHARE_WITH, share.getShareWith());
            contentValues.put(ProviderTableMeta.OCSHARES_PATH, share.getPath());
            contentValues.put(ProviderTableMeta.OCSHARES_PERMISSIONS, share.getPermissions());
            contentValues.put(ProviderTableMeta.OCSHARES_SHARED_DATE, share.getSharedDate());
            contentValues.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, share.getExpirationDate());
            contentValues.put(ProviderTableMeta.OCSHARES_TOKEN, share.getToken());
            contentValues.put(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME, share.getSharedWithDisplayName());
            contentValues.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, share.isFolder() ? 1 : 0);
            contentValues.put(ProviderTableMeta.OCSHARES_USER_ID, share.getUserId());
            contentValues.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getRemoteId());
            contentValues.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, account.name);
            contentValues.put(ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED, share.isPasswordProtected() ? 1 : 0);
            contentValues.put(ProviderTableMeta.OCSHARES_NOTE, share.getNote());
            contentValues.put(ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD, share.isHideFileDownload());

            // adding a new share resource
            operations.add(ContentProviderOperation
                               .newInsert(ProviderTableMeta.CONTENT_URI_SHARE)
                               .withValues(contentValues)
                               .build());
        }

        return operations;
    }

    private ArrayList<ContentProviderOperation> prepareRemoveSharesInFolder(
        OCFile folder, ArrayList<ContentProviderOperation> preparedOperations) {

        if (folder != null) {
            List<OCFile> folderContent = getFolderContent(folder, false);

            ArrayList<String> listPathString = new ArrayList<>();

            int totalSize, processSize, loopSize;
            totalSize = folderContent.size();
            processSize = 0;

            do {
                loopSize = Math.min((totalSize - processSize), 998);

                listPathString.clear();
                StringBuilder inList = new StringBuilder(folderContent.size() * 2);
                for (int i = 0; i < loopSize; i++, processSize++) {
                    OCFile file = folderContent.get(processSize);
                    if (i > 0) {
                        inList.append(",");
                    }
                    inList.append("?");

                    listPathString.add(file.getRemotePath());
                }

                String selection = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + AND
                    + ProviderTableMeta.OCSHARES_PATH
                    + " IN (" + inList + ")";

                ArrayList<String> selectionArgsList = new ArrayList<>();
                selectionArgsList.add(account.name);
                selectionArgsList.addAll(listPathString);
                String[] selectionArgs = selectionArgsList.toArray(new String[0]);

                preparedOperations.add(
                    ContentProviderOperation
                        .newDelete(ProviderTableMeta.CONTENT_URI_SHARE)
                        .withSelection(selection, selectionArgs)
                        .build()
                );
            } while ((totalSize - processSize) > 0);
        }

        return preparedOperations;
    }

    private ArrayList<ContentProviderOperation> prepareRemoveSharesInFile(
        String filePath, ArrayList<ContentProviderOperation> preparedOperations) {

        String where = ProviderTableMeta.OCSHARES_PATH + AND
            + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = ?";
        String[] whereArgs = new String[]{filePath, account.name};

        preparedOperations.add(
            ContentProviderOperation
                .newDelete(ProviderTableMeta.CONTENT_URI_SHARE)
                .withSelection(where, whereArgs)
                .build()
        );

        return preparedOperations;

    }

    public List<OCShare> getSharesWithForAFile(String filePath, String accountName) {
        String selection = ProviderTableMeta.OCSHARES_PATH + AND
            + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + AND
            + " (" + ProviderTableMeta.OCSHARES_SHARE_TYPE + " = ? OR "
            + ProviderTableMeta.OCSHARES_SHARE_TYPE + " = ? OR "
            + ProviderTableMeta.OCSHARES_SHARE_TYPE + " = ? OR "
            + ProviderTableMeta.OCSHARES_SHARE_TYPE + " = ? OR "
            + ProviderTableMeta.OCSHARES_SHARE_TYPE + " = ? ) ";
        String[] selectionArgs = new String[]{filePath, accountName,
            Integer.toString(ShareType.USER.getValue()),
            Integer.toString(ShareType.GROUP.getValue()),
            Integer.toString(ShareType.EMAIL.getValue()),
            Integer.toString(ShareType.FEDERATED.getValue()),
            Integer.toString(ShareType.ROOM.getValue())};

        Cursor cursor = executeQuery(ProviderTableMeta.CONTENT_URI_SHARE,
                                     null,
                                     selection,
                                     selectionArgs,
                                     null,
                                     "Could not get list of shares with: ");

        ArrayList<OCShare> shares = new ArrayList<>();
        OCShare share;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    share = createShareInstance(cursor);
                    shares.add(share);
                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return shares;
    }

    public static void triggerMediaScan(String path) {
        if (path != null) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(new File(path)));
            MainApp.getAppContext().sendBroadcast(intent);
        }
    }

    public void deleteFileInMediaScan(String path) {
        String mimetypeString = FileStorageUtils.getMimeTypeFromName(path);
        ContentResolver contentResolver = getContentResolver();

        String[] selectionArgs = {path};
        if (contentResolver != null) {
            if (MimeTypeUtil.isImage(mimetypeString)) {
                // Images
                contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                       MediaStore.Images.Media.DATA + " = ?", selectionArgs);
            } else if (MimeTypeUtil.isAudio(mimetypeString)) {
                // Audio
                contentResolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                       MediaStore.Audio.Media.DATA + " = ?", selectionArgs);
            } else if (MimeTypeUtil.isVideo(mimetypeString)) {
                // Video
                contentResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                       MediaStore.Video.Media.DATA + " = ?", selectionArgs);
            }
        } else {
            ContentProviderClient contentProviderClient = getContentProviderClient();
            try {
                if (MimeTypeUtil.isImage(mimetypeString)) {
                    // Images
                    contentProviderClient.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                 MediaStore.Images.Media.DATA + " = ?", selectionArgs);
                } else if (MimeTypeUtil.isAudio(mimetypeString)) {
                    // Audio
                    contentProviderClient.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                 MediaStore.Audio.Media.DATA + " = ?", selectionArgs);
                } else if (MimeTypeUtil.isVideo(mimetypeString)) {
                    // Video
                    contentProviderClient.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                 MediaStore.Video.Media.DATA + " = ?", selectionArgs);
                }
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception deleting media file in MediaStore " + e.getMessage(), e);
            }
        }
    }

    public void saveConflict(OCFile file, String etagInConflict) {
        if (!file.isDown()) {
            etagInConflict = null;
        }

        Uri contentUriFile = ProviderTableMeta.CONTENT_URI_FILE;
        ContentValues contentValues = new ContentValues();
        contentValues.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, etagInConflict);
        String where = ProviderTableMeta._ID + " = ?";
        String[] selectionArgs = {String.valueOf(file.getFileId())};
        int updated = 0;
        if (getContentResolver() != null) {
            updated = getContentResolver().update(contentUriFile, contentValues, where, selectionArgs);
        } else {
            try {
                updated = getContentProviderClient().update(contentUriFile, contentValues, where, selectionArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Failed saving conflict in database " + e.getMessage(), e);
            }
        }

        Log_OC.d(TAG, "Number of files updated with CONFLICT: " + updated);

        if (updated > 0) {
            if (etagInConflict != null) {
                /// set conflict in all ancestor folders

                long parentId = file.getParentId();
                Set<String> ancestorIds = new HashSet<>();
                while (parentId != FileDataStorageManager.ROOT_PARENT_ID) {
                    ancestorIds.add(Long.toString(parentId));
                    parentId = getFileById(parentId).getParentId();
                }

                if (ancestorIds.size() > 0) {
                    //TODO bug if ancestorIds.size() > 1000
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(ProviderTableMeta._ID).append(" IN (");
                    for (int i = 0; i < ancestorIds.size() - 1; i++) {
                        stringBuilder.append("?, ");
                    }
                    stringBuilder.append("?)");

                    if (getContentResolver() != null) {
                        getContentResolver().update(
                            contentUriFile,
                            contentValues,
                            stringBuilder.toString(),
                            ancestorIds.toArray(new String[]{})
                        );
                    } else {
                        try {
                            getContentProviderClient().update(
                                contentUriFile,
                                contentValues,
                                stringBuilder.toString(),
                                ancestorIds.toArray(new String[]{})
                            );
                        } catch (RemoteException e) {
                            Log_OC.e(TAG, "Failed saving conflict in database " + e.getMessage(), e);
                        }
                    }
                } // else file is ROOT folder, no parent to set in conflict

            } else {
                /// update conflict in ancestor folders
                // (not directly unset; maybe there are more conflicts below them)
                String parentPath = file.getRemotePath();
                if (parentPath.endsWith(OCFile.PATH_SEPARATOR)) {
                    parentPath = parentPath.substring(0, parentPath.length() - 1);
                }
                parentPath = parentPath.substring(0, parentPath.lastIndexOf(OCFile.PATH_SEPARATOR) + 1);

                Log_OC.d(TAG, "checking parents to remove conflict; STARTING with " + parentPath);
                while (parentPath.length() > 0) {

                    String[] projection = {ProviderTableMeta._ID};
                    String whereForDescencentsInConflict =
                        ProviderTableMeta.FILE_ETAG_IN_CONFLICT + " IS NOT NULL AND " +
                            ProviderTableMeta.FILE_CONTENT_TYPE + " != 'DIR' AND " +
                            ProviderTableMeta.FILE_ACCOUNT_OWNER + AND +
                            ProviderTableMeta.FILE_PATH + " LIKE ?";
                    selectionArgs = new String[]{account.name, parentPath + "%"};

                    Cursor descendentsInConflict = executeQuery(
                        contentUriFile,
                        projection,
                        whereForDescencentsInConflict,
                        selectionArgs,
                        null,
                        "Failed querying for descendents in conflict "
                    );

                    if (descendentsInConflict == null || descendentsInConflict.getCount() == 0) {
                        Log_OC.d(TAG, "NO MORE conflicts in " + parentPath);
                        where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND +
                            ProviderTableMeta.FILE_PATH + " = ?";
                        selectionArgs = new String[]{account.name, parentPath};
                        if (getContentResolver() != null) {
                            getContentResolver().update(
                                contentUriFile,
                                contentValues,
                                where,
                                selectionArgs
                            );
                        } else {
                            try {
                                getContentProviderClient().update(
                                    contentUriFile,
                                    contentValues,
                                    where,
                                    selectionArgs
                                );
                            } catch (RemoteException e) {
                                Log_OC.e(TAG, "Failed saving conflict in database " + e.getMessage(), e);
                            }
                        }

                    } else {
                        Log_OC.d(TAG, "STILL " + descendentsInConflict.getCount() + " in " + parentPath);
                    }

                    if (descendentsInConflict != null) {
                        descendentsInConflict.close();
                    }

                    parentPath = parentPath.substring(0, parentPath.length() - 1);  // trim last /
                    parentPath = parentPath.substring(0, parentPath.lastIndexOf(OCFile.PATH_SEPARATOR) + 1);
                    Log_OC.d(TAG, "checking parents to remove conflict; NEXT " + parentPath);
                }
            }
        }

    }

    public void saveCapabilities(OCCapability capability) {
        Uri contentUriCapabilities = ProviderTableMeta.CONTENT_URI_CAPABILITIES;
        ContentValues contentValues = createContentValues(account.name, capability);

        if (capabilityExists(account.name)) {
            String where = ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + " = ?";
            String[] selectionArgs = {account.name};
            if (getContentResolver() != null) {
                getContentResolver().update(contentUriCapabilities, contentValues, where, selectionArgs);
            } else {
                try {
                    getContentProviderClient().update(contentUriCapabilities, contentValues, where, selectionArgs);
                } catch (RemoteException e) {
                    Log_OC.e(TAG, "Failed saveCapabilities update" + e.getMessage(), e);
                }
            }
        } else {
            Uri resultUri = null;
            if (getContentResolver() != null) {
                resultUri = getContentResolver().insert(contentUriCapabilities, contentValues);
            } else {
                try {
                    resultUri = getContentProviderClient().insert(contentUriCapabilities, contentValues);
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }

            if (resultUri != null) {
                long newId = Long.parseLong(resultUri.getPathSegments().get(1));
                capability.setId(newId);
                capability.setAccountName(account.name);
            }
        }
    }

    @NonNull
    private ContentValues createContentValues(String accountName, OCCapability capability) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME,
                          accountName);
        contentValues.put(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR,
                          capability.getVersionMayor());
        contentValues.put(ProviderTableMeta.CAPABILITIES_VERSION_MINOR,
                          capability.getVersionMinor());
        contentValues.put(ProviderTableMeta.CAPABILITIES_VERSION_MICRO,
                          capability.getVersionMicro());
        contentValues.put(ProviderTableMeta.CAPABILITIES_VERSION_STRING,
                          capability.getVersionString());
        contentValues.put(ProviderTableMeta.CAPABILITIES_VERSION_EDITION,
                          capability.getVersionEdition());
        contentValues.put(ProviderTableMeta.CAPABILITIES_EXTENDED_SUPPORT,
                          capability.getExtendedSupport().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL,
                          capability.getCorePollInterval());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED,
                          capability.getFilesSharingApiEnabled().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED,
                          capability.getFilesSharingPublicEnabled().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED,
                          capability.getFilesSharingPublicPasswordEnforced().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD,
                          capability.getFilesSharingPublicAskForOptionalPassword().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED,
                          capability.getFilesSharingPublicExpireDateEnabled().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS,
                          capability.getFilesSharingPublicExpireDateDays());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED,
                          capability.getFilesSharingPublicExpireDateEnforced().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL,
                          capability.getFilesSharingPublicSendMail().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD,
                          capability.getFilesSharingPublicUpload().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL,
                          capability.getFilesSharingUserSendMail().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_RESHARING,
                          capability.getFilesSharingResharing().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING,
                          capability.getFilesSharingFederationOutgoing().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING,
                          capability.getFilesSharingFederationIncoming().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING,
                          capability.getFilesBigFileChunking().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_FILES_UNDELETE,
                          capability.getFilesUndelete().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_FILES_VERSIONING,
                          capability.getFilesVersioning().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_FILES_DROP,
                          capability.getFilesFileDrop().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_EXTERNAL_LINKS,
                          capability.getExternalLinks().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_NAME,
                          capability.getServerName());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_COLOR,
                          capability.getServerColor());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR,
                          capability.getServerTextColor());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR,
                          capability.getServerElementColor());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL,
                          capability.getServerBackground());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN,
                          capability.getServerSlogan());
        contentValues.put(ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION,
                          capability.getEndToEndEncryption().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT,
                          capability.getServerBackgroundDefault().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN,
                          capability.getServerBackgroundPlain().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_ACTIVITY,
                          capability.getActivity().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_RICHDOCUMENT,
                          capability.getRichDocuments().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST,
                          TextUtils.join(",", capability.getRichDocumentsMimeTypeList()));
        contentValues.put(ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST,
                          TextUtils.join(",", capability.getRichDocumentsOptionalMimeTypeList()));
        contentValues.put(ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING,
                          capability.getRichDocumentsDirectEditing().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES,
                          capability.getRichDocumentsTemplatesAvailable().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME,
                          capability.getRichDocumentsProductName());
        contentValues.put(ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG,
                          capability.getDirectEditingEtag());

        return contentValues;
    }

    private boolean capabilityExists(String accountName) {
        Cursor cursor = getCapabilityCursorForAccount(accountName);
        boolean exists = false;

        if (cursor != null) {
            exists = cursor.moveToFirst();
            cursor.close();
        }

        return exists;
    }

    private Cursor getCapabilityCursorForAccount(String accountName) {
        String selection = ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + " = ?";
        String[] selectionArgs = {accountName};

        return executeQuery(ProviderTableMeta.CONTENT_URI_CAPABILITIES,
                            selection,
                            selectionArgs,
                            "Couldn't determine capability existence, assuming non existance: ");
    }

    @NonNull
    public OCCapability getCapability(String accountName) {
        OCCapability capability;
        Cursor cursor = getCapabilityCursorForAccount(accountName);

        if (cursor.moveToFirst()) {
            capability = createCapabilityInstance(cursor);
        } else {
            capability = new OCCapability();    // return default with all UNKNOWN
        }
        cursor.close();

        return capability;
    }

    private OCCapability createCapabilityInstance(Cursor cursor) {
        OCCapability capability = null;
        if (cursor != null) {
            capability = new OCCapability();
            capability.setId(getLong(cursor, ProviderTableMeta._ID));
            capability.setAccountName(getString(cursor, ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME));
            capability.setVersionMayor(getInt(cursor, ProviderTableMeta.CAPABILITIES_VERSION_MAYOR));
            capability.setVersionMinor(getInt(cursor, ProviderTableMeta.CAPABILITIES_VERSION_MINOR));
            capability.setVersionMicro(getInt(cursor, ProviderTableMeta.CAPABILITIES_VERSION_MICRO));
            capability.setVersionString(getString(cursor, ProviderTableMeta.CAPABILITIES_VERSION_STRING));
            capability.setVersionEdition(getString(cursor, ProviderTableMeta.CAPABILITIES_VERSION_EDITION));
            capability.setExtendedSupport(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_EXTENDED_SUPPORT));
            capability.setCorePollInterval(getInt(cursor, ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL));
            capability.setFilesSharingApiEnabled(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED));
            capability.setFilesSharingPublicEnabled(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED));
            capability.setFilesSharingPublicPasswordEnforced(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED));
            capability.setFilesSharingPublicAskForOptionalPassword(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD));
            capability.setFilesSharingPublicExpireDateEnabled(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED));
            capability.setFilesSharingPublicExpireDateDays(
                getInt(cursor, ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS));
            capability.setFilesSharingPublicExpireDateEnforced(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED));
            capability.setFilesSharingPublicSendMail(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL));
            capability.setFilesSharingPublicUpload(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD));
            capability.setFilesSharingUserSendMail(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL));
            capability.setFilesSharingResharing(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_RESHARING));
            capability.setFilesSharingFederationOutgoing(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING));
            capability.setFilesSharingFederationIncoming(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING));
            capability.setFilesBigFileChunking(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING));
            capability.setFilesUndelete(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_FILES_UNDELETE));
            capability.setFilesVersioning(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_FILES_VERSIONING));
            capability.setFilesFileDrop(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_FILES_DROP));
            capability.setExternalLinks(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_EXTERNAL_LINKS));
            capability.setServerName(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_NAME));
            capability.setServerColor(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_COLOR));
            capability.setServerTextColor(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR));
            capability.setServerElementColor(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR));
            capability.setServerBackground(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL));
            capability.setServerSlogan(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN));
            capability.setEndToEndEncryption(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION));
            capability.setServerBackgroundDefault(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT));
            capability.setServerBackgroundPlain(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN));
            capability.setActivity(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_ACTIVITY));
            capability.setRichDocuments(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT));
            capability.setRichDocumentsDirectEditing(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING));
            capability.setRichDocumentsTemplatesAvailable(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES));
            String mimetypes = cursor.getString(cursor.getColumnIndex(ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST));
            if (mimetypes == null) {
                mimetypes = "";
            }
            capability.setRichDocumentsMimeTypeList(Arrays.asList(mimetypes.split(",")));

            String optionalMimetypes = getString(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST);
            if (optionalMimetypes == null) {
                optionalMimetypes = "";
            }
            capability.setRichDocumentsOptionalMimeTypeList(Arrays.asList(optionalMimetypes.split(",")));
            capability.setRichDocumentsProductName(getString(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME));
            capability.setDirectEditingEtag(getString(cursor, ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG));
        }
        return capability;
    }

    public void deleteVirtuals(VirtualFolderType type) {
        Uri contentUriVirtual = ProviderTableMeta.CONTENT_URI_VIRTUAL;
        String where = ProviderTableMeta.VIRTUAL_TYPE + " = ?";
        String[] selectionArgs = {String.valueOf(type)};

        if (getContentResolver() != null) {
            getContentResolver().delete(contentUriVirtual, where, selectionArgs);
        } else {
            try {
                getContentProviderClient().delete(contentUriVirtual, where, selectionArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "deleteVirtuals" + e.getMessage(), e);
            }
        }
    }

    public void saveVirtuals(List<ContentValues> values) {
        Uri contentUriVirtual = ProviderTableMeta.CONTENT_URI_VIRTUAL;
        ContentValues[] arrayValues = values.toArray(new ContentValues[0]);

        if (getContentResolver() != null) {
            getContentResolver().bulkInsert(contentUriVirtual, arrayValues);
        } else {
            try {
                getContentProviderClient().bulkInsert(contentUriVirtual, arrayValues);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "saveVirtuals" + e.getMessage(), e);
            }
        }
    }

    public List<OCFile> getVirtualFolderContent(VirtualFolderType type, boolean onlyImages) {
        List<OCFile> ocFiles = new ArrayList<>();

        String selection = ProviderTableMeta.VIRTUAL_TYPE + " = ?";
        String[] selectionArgs = {String.valueOf(type)};

        Cursor cursor = executeQuery(ProviderTableMeta.CONTENT_URI_VIRTUAL,
                                     selection,
                                     selectionArgs,
                                     "getVirtualFolderContent ");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    OCFile ocFile = createFileInstanceFromVirtual(cursor);

                    if (ocFile != null) {
                        if (onlyImages) {
                            if (MimeTypeUtil.isImage(ocFile)) {
                                ocFiles.add(ocFile);
                            }
                        } else {
                            ocFiles.add(ocFile);
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        if (!ocFiles.isEmpty()) {
            Collections.sort(ocFiles);
        }

        return ocFiles;
    }

    public void deleteAllFiles() {
        Uri contentUriDir = ProviderTableMeta.CONTENT_URI_DIR;
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND
            + ProviderTableMeta.FILE_PATH + "= ?";
        String[] whereArgs = new String[]{account.name, OCFile.ROOT_PATH};

        if (getContentResolver() != null) {
            getContentResolver().delete(contentUriDir, where, whereArgs);
        } else {
            try {
                getContentProviderClient().delete(contentUriDir, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in deleteAllFiles for account " + account.name + ": " + e.getMessage(), e);
            }
        }
    }

    private String getString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    private int getInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    private long getLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    private CapabilityBooleanType getBoolean(Cursor cursor, String columnName) {
        return CapabilityBooleanType.fromValue(cursor.getInt(cursor.getColumnIndex(columnName)));
    }
}
