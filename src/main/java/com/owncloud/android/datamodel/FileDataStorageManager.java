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
import android.os.Build;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nextcloud.client.account.User;
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
import androidx.annotation.VisibleForTesting;


public class FileDataStorageManager {
    private static final String TAG = FileDataStorageManager.class.getSimpleName();

    private static final String AND = " = ? AND ";
    private static final String FAILED_TO_INSERT_MSG = "Fail to insert insert file to database ";
    private static final String SENDING_TO_FILECONTENTPROVIDER_MSG = "Sending %d operations to FileContentProvider";
    private static final String EXCEPTION_MSG = "Exception in batch of operations ";

    public static final int ROOT_PARENT_ID = 0;
    public static final String NULL_STRING = "null";

    private ContentResolver contentResolver;
    private ContentProviderClient contentProviderClient;
    private User user;

    public FileDataStorageManager(User user, ContentResolver contentResolver) {
        this.contentProviderClient = null;
        this.contentResolver = contentResolver;
        this.user = user;
    }

    public FileDataStorageManager(User user, ContentProviderClient contentProviderClient) {
        this.contentProviderClient = contentProviderClient;
        this.contentResolver = null;
        this.user = user;
    }

    /**
     * Use getFileByEncryptedRemotePath() or getFileByDecryptedRemotePath()
     */
    @Deprecated
    public OCFile getFileByPath(String path) {
        return getFileByEncryptedRemotePath(path);
    }

    public OCFile getFileByEncryptedRemotePath(String path) {
        return getFileByPath(ProviderTableMeta.FILE_PATH, path);
    }

    public @Nullable
    OCFile getFileByDecryptedRemotePath(String path) {
        return getFileByPath(ProviderTableMeta.FILE_PATH_DECRYPTED, path);
    }

    private @Nullable
    OCFile getFileByPath(String type, String path) {
        Cursor cursor = getFileCursorForValue(type, path);
        OCFile ocFile = null;
        if (cursor.moveToFirst()) {
            ocFile = createFileInstance(cursor);
        }
        cursor.close();

        if (ocFile == null && OCFile.ROOT_PATH.equals(path)) {
            return createRootDir(); // root should always exist
        }

        return ocFile;
    }

    public @Nullable
    OCFile getFileById(long id) {
        Cursor cursor = getFileCursorForValue(ProviderTableMeta._ID, String.valueOf(id));
        OCFile ocFile = null;

        if (cursor.moveToFirst()) {
            ocFile = createFileInstance(cursor);
        }
        cursor.close();

        return ocFile;
    }

    public @Nullable
    OCFile getFileByLocalPath(String path) {
        Cursor cursor = getFileCursorForValue(ProviderTableMeta.FILE_STORAGE_PATH, path);
        OCFile ocFile = null;

        if (cursor.moveToFirst()) {
            ocFile = createFileInstance(cursor);
        }
        cursor.close();

        return ocFile;
    }

    public @Nullable
    OCFile getFileByRemoteId(String remoteId) {
        Cursor cursor = getFileCursorForValue(ProviderTableMeta.FILE_REMOTE_ID, remoteId);
        OCFile ocFile = null;

        if (cursor.moveToFirst()) {
            ocFile = createFileInstance(cursor);
        }
        cursor.close();

        return ocFile;
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
        List<OCFile> imageList = new ArrayList<>();

        if (folder != null) {
            // TODO better implementation, filtering in the access to database instead of here
            List<OCFile> folderContent = getFolderContent(folder, onlyOnDevice);

            for (OCFile ocFile : folderContent) {
                if (MimeTypeUtil.isImage(ocFile)) {
                    imageList.add(ocFile);
                }
            }
        }

        return imageList;
    }

    public boolean saveFile(OCFile ocFile) {
        boolean overridden = false;
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_MODIFIED, ocFile.getModificationTimestamp());
        cv.put(
            ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
            ocFile.getModificationTimestampAtLastSyncForData()
        );
        cv.put(ProviderTableMeta.FILE_CREATION, ocFile.getCreationTimestamp());
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, ocFile.getFileLength());
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, ocFile.getMimeType());
        cv.put(ProviderTableMeta.FILE_NAME, ocFile.getFileName());
        cv.put(ProviderTableMeta.FILE_ENCRYPTED_NAME, ocFile.getEncryptedFileName());
        cv.put(ProviderTableMeta.FILE_PARENT, ocFile.getParentId());
        cv.put(ProviderTableMeta.FILE_PATH, ocFile.getRemotePath());
        cv.put(ProviderTableMeta.FILE_PATH_DECRYPTED, ocFile.getDecryptedRemotePath());
        cv.put(ProviderTableMeta.FILE_IS_ENCRYPTED, ocFile.isEncrypted());
        if (!ocFile.isFolder()) {
            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, ocFile.getStoragePath());
        }
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, user.getAccountName());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, ocFile.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, ocFile.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_ETAG, ocFile.getEtag());
        cv.put(ProviderTableMeta.FILE_ETAG_ON_SERVER, ocFile.getEtagOnServer());
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, ocFile.isSharedViaLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, ocFile.isSharedWithSharee() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, ocFile.getPermissions());
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, ocFile.getRemoteId());
        cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, ocFile.isUpdateThumbnailNeeded());
        cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, ocFile.isDownloading());
        cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, ocFile.getEtagInConflict());
        cv.put(ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT, ocFile.getUnreadCommentsCount());
        cv.put(ProviderTableMeta.FILE_OWNER_ID, ocFile.getOwnerId());
        cv.put(ProviderTableMeta.FILE_OWNER_DISPLAY_NAME, ocFile.getOwnerDisplayName());
        cv.put(ProviderTableMeta.FILE_NOTE, ocFile.getNote());
        cv.put(ProviderTableMeta.FILE_SHAREES, new Gson().toJson(ocFile.getSharees()));
        cv.put(ProviderTableMeta.FILE_RICH_WORKSPACE, ocFile.getRichWorkspace());
        cv.put(ProviderTableMeta.FILE_HAS_PREVIEW, ocFile.isPreviewAvailable() ? 1 : 0);

        boolean sameRemotePath = fileExists(ocFile.getRemotePath());
        if (sameRemotePath ||
            fileExists(ocFile.getFileId())) {  // for renamed files; no more delete and create

            if (sameRemotePath) {
                OCFile oldFile = getFileByPath(ocFile.getRemotePath());
                ocFile.setFileId(oldFile.getFileId());
            }

            overridden = true;
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv,
                                            ProviderTableMeta._ID + "=?",
                                            new String[]{String.valueOf(ocFile.getFileId())});
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI,
                                                      cv, ProviderTableMeta._ID + "=?",
                                                      new String[]{String.valueOf(ocFile.getFileId())});
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
                ocFile.setFileId(new_id);
            }
        }

        return overridden;
    }

    /**
     * traverses a files parent tree to be able to store a file with its parents. Throws a
     * RemoteOperationFailedException in case the parent can't be retrieved.
     *
     * @param ocFile  the file
     * @param context the app context
     * @return the parent file
     */
    public OCFile saveFileWithParent(OCFile ocFile, Context context) {
        if (ocFile.getParentId() == 0 && !OCFile.ROOT_PATH.equals(ocFile.getRemotePath())) {
            String remotePath = ocFile.getRemotePath();
            String parentPath = remotePath.substring(0, remotePath.lastIndexOf(ocFile.getFileName()));

            OCFile parentFile = getFileByPath(parentPath);
            OCFile returnFile;

            if (parentFile == null) {
                // remote request
                ReadFileRemoteOperation operation = new ReadFileRemoteOperation(parentPath);
                // TODO Deprecated
                RemoteOperationResult result = operation.execute(getAccount(), context);
                if (result.isSuccess()) {
                    OCFile remoteFolder = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));

                    returnFile = saveFileWithParent(remoteFolder, context);
                } else {
                    Exception exception = result.getException();
                    String message = "Error during saving file with parents: " + ocFile.getRemotePath() + " / "
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

            ocFile.setParentId(returnFile.getFileId());
            saveFile(ocFile);
        }

        return ocFile;
    }

    public void saveNewFile(OCFile newFile) {
        String remoteParentPath = new File(newFile.getRemotePath()).getParent();
        remoteParentPath = remoteParentPath.endsWith(OCFile.PATH_SEPARATOR) ?
            remoteParentPath : remoteParentPath + OCFile.PATH_SEPARATOR;
        OCFile parent = getFileByPath(remoteParentPath);
        if (parent != null) {
            newFile.setParentId(parent.getFileId());
            saveFile(newFile);
        } else {
            throw new IllegalArgumentException("Saving a new file in an unexisting folder");
        }
    }


    /**
     * Inserts or updates the list of files contained in a given folder.
     *
     * CALLER IS RESPONSIBLE FOR GRANTING RIGHT UPDATE OF INFORMATION, NOT THIS METHOD. HERE ONLY DATA CONSISTENCY
     * SHOULD BE GRANTED
     *
     * @param folder
     * @param updatedFiles
     * @param filesToRemove
     */
    public void saveFolder(OCFile folder, List<OCFile> updatedFiles, Collection<OCFile> filesToRemove) {
        Log_OC.d(TAG, "Saving folder " + folder.getRemotePath() + " with " + updatedFiles.size()
            + " children and " + filesToRemove.size() + " files to remove");

        ArrayList<ContentProviderOperation> operations = new ArrayList<>(updatedFiles.size());

        // prepare operations to insert or update files to save in the given folder
        for (OCFile ocFile : updatedFiles) {
            ContentValues contentValues = createContentValueForFile(ocFile, folder);

            if (fileExists(ocFile.getFileId()) || fileExists(ocFile.getRemotePath())) {
                long fileId;
                if (ocFile.getFileId() != -1) {
                    fileId = ocFile.getFileId();
                } else {
                    fileId = getFileByPath(ocFile.getRemotePath()).getFileId();
                }
                // updating an existing file
                operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI)
                                   .withValues(contentValues)
                                   .withSelection(ProviderTableMeta._ID + " = ?", new String[]{String.valueOf(fileId)})
                                   .build());
            } else {
                // adding a new file
                operations.add(ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI)
                                   .withValues(contentValues)
                                   .build());
            }
        }

        // prepare operations to remove files in the given folder
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " = ?";
        String[] whereArgs = new String[2];
        whereArgs[0] = user.getAccountName();
        for (OCFile ocFile : filesToRemove) {
            if (ocFile.getParentId() == folder.getFileId()) {
                whereArgs[1] = ocFile.getRemotePath();
                if (ocFile.isFolder()) {
                    operations.add(ContentProviderOperation.newDelete(
                        ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, ocFile.getFileId()))
                                       .withSelection(where, whereArgs).build());

                    File localFolder = new File(FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), ocFile));
                    if (localFolder.exists()) {
                        removeLocalFolder(localFolder);
                    }
                } else {
                    operations.add(ContentProviderOperation.newDelete(
                        ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, ocFile.getFileId()))
                                       .withSelection(where, whereArgs).build());

                    if (ocFile.isDown()) {
                        String path = ocFile.getStoragePath();
                        if (new File(path).delete() && MimeTypeUtil.isMedia(ocFile.getMimeType())) {
                            triggerMediaScan(path, ocFile); // notify MediaScanner about removed file
                        }
                    }
                }
            }
        }

        // update metadata of folder
        ContentValues contentValues = createContentValueForFile(folder);

        operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI)
                           .withValues(contentValues)
                           .withSelection(ProviderTableMeta._ID + " = ?", new String[]{String.valueOf(folder.getFileId())})
                           .build());

        // apply operations in batch
        ContentProviderResult[] results = null;
        Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size()));

        try {
            if (getContentResolver() != null) {
                results = getContentResolver().applyBatch(MainApp.getAuthority(), operations);

            } else {
                results = getContentProviderClient().applyBatch(operations);
            }

        } catch (OperationApplicationException | RemoteException e) {
            Log_OC.e(TAG, EXCEPTION_MSG + e.getMessage(), e);
        }

        // update new id in file objects for insertions
        if (results != null) {
            long newId;
            Iterator<OCFile> fileIterator = updatedFiles.iterator();
            OCFile ocFile;
            for (ContentProviderResult result : results) {
                if (fileIterator.hasNext()) {
                    ocFile = fileIterator.next();
                } else {
                    ocFile = null;
                }
                if (result.uri != null) {
                    newId = Long.parseLong(result.uri.getPathSegments().get(1));
                    if (ocFile != null) {
                        ocFile.setFileId(newId);
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
        cv.put(ProviderTableMeta.FILE_PATH_DECRYPTED, folder.getDecryptedRemotePath());
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, user.getAccountName());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, folder.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, folder.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_ETAG, folder.getEtag());
        cv.put(ProviderTableMeta.FILE_ETAG_ON_SERVER, folder.getEtagOnServer());
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, folder.isSharedViaLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, folder.isSharedWithSharee() ? 1 : 0);
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
        cv.put(ProviderTableMeta.FILE_PATH_DECRYPTED, file.getDecryptedRemotePath());
        cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, user.getAccountName());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
        cv.put(ProviderTableMeta.FILE_ETAG_ON_SERVER, file.getEtagOnServer());
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, file.isSharedViaLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, file.isSharedWithSharee() ? 1 : 0);
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

    public boolean removeFile(OCFile ocFile, boolean removeDBData, boolean removeLocalCopy) {
        boolean success = true;

        if (ocFile != null) {
            if (ocFile.isFolder()) {
                success = removeFolder(ocFile, removeDBData, removeLocalCopy);
            } else {

                if (removeDBData) {
                    //Uri file_uri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE,
                    // ""+file.getFileId());
                    Uri file_uri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, ocFile.getFileId());
                    String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + "=?";
                    String[] whereArgs = new String[]{user.getAccountName(), ocFile.getRemotePath()};
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

                String localPath = ocFile.getStoragePath();
                if (removeLocalCopy && ocFile.isDown() && localPath != null && success) {
                    success = new File(localPath).delete();
                    if (success) {
                        deleteFileInMediaScan(localPath);
                    }

                    if (success && !removeDBData) {
                        // maybe unnecessary, but should be checked TODO remove if unnecessary
                        ocFile.setStoragePath(null);
                        saveFile(ocFile);
                        saveConflict(ocFile, null);
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
        Uri folderUri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, String.valueOf(folder.getFileId()));
        // for recursive deletion
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + "=?";
        String[] whereArgs = new String[]{user.getAccountName(), folder.getRemotePath()};
        int deleted = 0;
        if (getContentProviderClient() != null) {
            try {
                deleted = getContentProviderClient().delete(folderUri, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.d(TAG, e.getMessage(), e);
            }
        } else {
            deleted = getContentResolver().delete(folderUri, where, whereArgs);
        }
        return deleted > 0;
    }

    private boolean removeLocalFolder(OCFile folder) {
        boolean success = true;
        String localFolderPath = FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), folder);
        File localFolder = new File(localFolderPath);

        if (localFolder.exists()) {
            // stage 1: remove the local files already registered in the files database
            List<OCFile> files = getFolderContent(folder.getFileId(), false);
            for (OCFile ocFile : files) {
                if (ocFile.isFolder()) {
                    success &= removeLocalFolder(ocFile);
                } else if (ocFile.isDown()) {
                    File localFile = new File(ocFile.getStoragePath());
                    success &= localFile.delete();

                    if (success) {
                        // notify MediaScanner about removed file
                        deleteFileInMediaScan(ocFile.getStoragePath());
                        ocFile.setStoragePath(null);
                        saveFile(ocFile);
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
     *
     * TODO explore better (faster) implementations TODO throw exceptions up !
     */
    public void moveLocalFile(OCFile ocFile, String targetPath, String targetParentPath) {
        if (ocFile.fileExists() && !OCFile.ROOT_PATH.equals(ocFile.getFileName())) {

            OCFile targetParent = getFileByPath(targetParentPath);
            if (targetParent == null) {
                throw new IllegalStateException("Parent folder of the target path does not exist!!");
            }

            /// 1. get all the descendants of the moved element in a single QUERY
            Cursor cursor = null;
            if (getContentProviderClient() != null) {
                try {
                    cursor = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " LIKE ? ",
                        new String[]{user.getAccountName(), ocFile.getRemotePath() + "%"},
                        ProviderTableMeta.FILE_PATH + " ASC "
                    );
                } catch (RemoteException e) {
                    Log_OC.e(TAG, e.getMessage(), e);
                }

            } else {
                cursor = getContentResolver().query(
                    ProviderTableMeta.CONTENT_URI,
                    null,
                    ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " LIKE ? ",
                    new String[]{user.getAccountName(), ocFile.getRemotePath() + "%"},
                    ProviderTableMeta.FILE_PATH + " ASC "
                );
            }

            /// 2. prepare a batch of update operations to change all the descendants
            ArrayList<ContentProviderOperation> operations = new ArrayList<>(cursor.getCount());
            String defaultSavePath = FileStorageUtils.getSavePath(user.getAccountName());
            List<String> originalPathsToTriggerMediaScan = new ArrayList<>();
            List<String> newPathsToTriggerMediaScan = new ArrayList<>();

            if (cursor.moveToFirst()) {
                int lengthOfOldPath = ocFile.getRemotePath().length();
                int lengthOfOldStoragePath = defaultSavePath.length() + lengthOfOldPath;
                do {
                    ContentValues contentValues = new ContentValues(); // keep construction in the loop
                    OCFile childFile = createFileInstance(cursor);
                    contentValues.put(
                        ProviderTableMeta.FILE_PATH,
                        targetPath + childFile.getRemotePath().substring(lengthOfOldPath)
                    );

                    if (childFile.getStoragePath() != null && childFile.getStoragePath().startsWith(defaultSavePath)) {
                        // update link to downloaded content - but local move is not done here!
                        String targetLocalPath = defaultSavePath + targetPath +
                            childFile.getStoragePath().substring(lengthOfOldStoragePath);

                        contentValues.put(ProviderTableMeta.FILE_STORAGE_PATH, targetLocalPath);

                        if (MimeTypeUtil.isMedia(childFile.getMimeType())) {
                            originalPathsToTriggerMediaScan.add(childFile.getStoragePath());
                            newPathsToTriggerMediaScan.add(targetLocalPath);
                        }

                    }

                    if (childFile.getRemotePath().equals(ocFile.getRemotePath())) {
                        contentValues.put(ProviderTableMeta.FILE_PARENT, targetParent.getFileId());
                    }

                    operations.add(
                        ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI)
                            .withValues(contentValues)
                            .withSelection(ProviderTableMeta._ID + " = ?", new String[]{String.valueOf(childFile.getFileId())})
                            .build());

                } while (cursor.moveToNext());
            }
            cursor.close();

            /// 3. apply updates in batch
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);
                } else {
                    getContentProviderClient().applyBatch(operations);
                }

            } catch (Exception e) {
                Log_OC.e(TAG, "Fail to update " + ocFile.getFileId() + " and descendants in database", e);
            }

            /// 4. move in local file system
            String originalLocalPath = FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), ocFile);
            String targetLocalPath = defaultSavePath + targetPath;
            File localFile = new File(originalLocalPath);
            boolean renamed = false;

            if (localFile.exists()) {
                File targetFile = new File(targetLocalPath);
                File targetFolder = targetFile.getParentFile();
                if (targetFolder != null && !targetFolder.exists() && !targetFolder.mkdirs()) {
                    Log_OC.e(TAG, "Unable to create parent folder " + targetFolder.getAbsolutePath());
                }
                renamed = localFile.renameTo(targetFile);
            }

            if (renamed) {
                Iterator<String> pathIterator = originalPathsToTriggerMediaScan.iterator();
                while (pathIterator.hasNext()) {
                    // Notify MediaScanner about removed file
                    deleteFileInMediaScan(pathIterator.next());
                }

                pathIterator = newPathsToTriggerMediaScan.iterator();
                while (pathIterator.hasNext()) {
                    // Notify MediaScanner about new file/folder
                    triggerMediaScan(pathIterator.next());
                }
            }
        }
    }

    public void copyLocalFile(OCFile ocFile, String targetPath) {
        if (ocFile.fileExists() && !OCFile.ROOT_PATH.equals(ocFile.getFileName())) {
            String localPath = FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), ocFile);
            File localFile = new File(localPath);
            boolean copied = false;
            String defaultSavePath = FileStorageUtils.getSavePath(user.getAccountName());
            if (localFile.exists()) {
                File targetFile = new File(defaultSavePath + targetPath);
                File targetFolder = targetFile.getParentFile();
                if (targetFolder != null && !targetFolder.exists() && !targetFolder.mkdirs()) {
                    Log_OC.e(TAG, "Unable to create parent folder " + targetFolder.getAbsolutePath());
                }
                copied = FileStorageUtils.copyFile(localFile, targetFile);
            }
            Log_OC.d(TAG, "Local file COPIED : " + copied);
        }
    }

    /**
     * This method does not require {@link FileDataStorageManager} being initialized
     * with any specific user. Migration can be performed with {@link com.nextcloud.client.account.AnonymousUser}.
     */
    public void migrateStoredFiles(String sourcePath, String destinationPath)
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

                if (oldFileStoragePath.startsWith(sourcePath)) {

                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH,
                           oldFileStoragePath.replaceFirst(sourcePath, destinationPath));

                    operations.add(
                        ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                            withValues(cv).
                            withSelection(ProviderTableMeta._ID + "=?", fileId)
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
        Cursor cursor;

        if (getContentProviderClient() != null) {
            try {
                cursor = getContentProviderClient().query(
                    requestURI,
                    null,
                    ProviderTableMeta.FILE_PARENT + "=?",
                    new String[]{String.valueOf(parentId)},
                    null
                );
            } catch (RemoteException e) {
                Log_OC.e(TAG, e.getMessage(), e);
                return folderContent;
            }
        } else {
            cursor = getContentResolver().query(
                requestURI,
                null,
                ProviderTableMeta.FILE_PARENT + "=?",
                new String[]{String.valueOf(parentId)},
                null
            );
        }

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
        OCFile ocFile = new OCFile(OCFile.ROOT_PATH);
        ocFile.setMimeType(MimeType.DIRECTORY);
        ocFile.setParentId(FileDataStorageManager.ROOT_PARENT_ID);
        saveFile(ocFile);

        return ocFile;
    }

    // TODO write test
    private boolean fileExists(String key, String value) {
        Cursor cursor = getFileCursorForValue(key, value);
        boolean isExists = false;

        if (cursor == null) {
            Log_OC.e(TAG, "Couldn't determine file existance, assuming non existance");
        } else {
            isExists = cursor.moveToFirst();
            cursor.close();
        }

        return isExists;
    }

    private Cursor getFileCursorForValue(String key, String value) {
        Cursor cursor;
        if (getContentResolver() != null) {
            cursor = getContentResolver()
                .query(ProviderTableMeta.CONTENT_URI,
                       null,
                       key + AND
                           + ProviderTableMeta.FILE_ACCOUNT_OWNER
                           + "=?",
                       new String[]{value, user.getAccountName()}, null);
        } else {
            try {
                cursor = getContentProviderClient().query(
                    ProviderTableMeta.CONTENT_URI,
                    null,
                    key + AND + ProviderTableMeta.FILE_ACCOUNT_OWNER
                        + "=?", new String[]{value, user.getAccountName()},
                    null);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Could not get file details: " + e.getMessage(), e);
                cursor = null;
            }
        }
        return cursor;
    }

    @Nullable
    private OCFile createFileInstanceFromVirtual(Cursor cursor) {
        long fileId = cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.VIRTUAL_OCFILE_ID));

        return getFileById(fileId);
    }

    private OCFile createFileInstance(Cursor cursor) {
        OCFile ocFile = null;
        if (cursor != null) {
            ocFile = new OCFile(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_PATH)));
            ocFile.setDecryptedRemotePath(getString(cursor, ProviderTableMeta.FILE_PATH_DECRYPTED));
            ocFile.setFileId(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta._ID)));
            ocFile.setParentId(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.FILE_PARENT)));
            ocFile.setMimeType(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)));
            ocFile.setStoragePath(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)));
            if (ocFile.getStoragePath() == null) {
                // try to find existing file and bind it with current account;
                // with the current update of SynchronizeFolderOperation, this won't be
                // necessary anymore after a full synchronization of the account
                File file = new File(FileStorageUtils.getDefaultSavePathFor(user.getAccountName(), ocFile));
                if (file.exists()) {
                    ocFile.setStoragePath(file.getAbsolutePath());
                    ocFile.setLastSyncDateForData(file.lastModified());
                }
            }
            ocFile.setFileLength(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH)));
            ocFile.setCreationTimestamp(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.FILE_CREATION)));
            ocFile.setModificationTimestamp(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.FILE_MODIFIED)));
            ocFile.setModificationTimestampAtLastSyncForData(cursor.getLong(
                cursor.getColumnIndex(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)));
            ocFile.setLastSyncDateForProperties(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.FILE_LAST_SYNC_DATE)));
            ocFile.setLastSyncDateForData(cursor.getLong(cursor.getColumnIndex(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA)));
            ocFile.setEtag(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_ETAG)));
            ocFile.setEtagOnServer(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_ETAG_ON_SERVER)));
            ocFile.setSharedViaLink(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.FILE_SHARED_VIA_LINK)) == 1);
            ocFile.setSharedWithSharee(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.FILE_SHARED_WITH_SHAREE)) == 1);
            ocFile.setPermissions(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_PERMISSIONS)));
            ocFile.setRemoteId(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID)));
            ocFile.setUpdateThumbnailNeeded(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.FILE_UPDATE_THUMBNAIL)) == 1);
            ocFile.setDownloading(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.FILE_IS_DOWNLOADING)) == 1);
            ocFile.setEtagInConflict(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_ETAG_IN_CONFLICT)));
            ocFile.setFavorite(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.FILE_FAVORITE)) == 1);
            ocFile.setEncrypted(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.FILE_IS_ENCRYPTED)) == 1);
//            if (ocFile.isEncrypted()) {
//                ocFile.setFileName(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_NAME)));
//            }
            ocFile.setMountType(WebdavEntry.MountType.values()[cursor.getInt(
                cursor.getColumnIndex(ProviderTableMeta.FILE_MOUNT_TYPE))]);
            ocFile.setPreviewAvailable(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.FILE_HAS_PREVIEW)) == 1);
            ocFile.setUnreadCommentsCount(cursor.getInt(cursor.getColumnIndex(ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT)));
            ocFile.setOwnerId(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_OWNER_ID)));
            ocFile.setOwnerDisplayName(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_OWNER_DISPLAY_NAME)));
            ocFile.setNote(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_NOTE)));
            ocFile.setRichWorkspace(cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_RICH_WORKSPACE)));

            String sharees = cursor.getString(cursor.getColumnIndex(ProviderTableMeta.FILE_SHAREES));

            if (sharees == null || NULL_STRING.equals(sharees) || sharees.isEmpty()) {
                ocFile.setSharees(new ArrayList<>());
            } else {
                try {
                    ShareeUser[] shareesArray = new Gson().fromJson(sharees, ShareeUser[].class);

                    ocFile.setSharees(new ArrayList<>(Arrays.asList(shareesArray)));
                } catch (JsonSyntaxException e) {
                    // ignore saved value due to api change
                    ocFile.setSharees(new ArrayList<>());
                }
            }
        }

        return ocFile;
    }

    public boolean saveShare(OCShare share) {
        boolean overridden = false;

        ContentValues contentValues = createContentValueForShare(share);

        if (shareExistsForRemoteId(share.getRemoteId())) {// for renamed files; no more delete and create
            overridden = true;
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI_SHARE,
                                            contentValues,
                                            ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                                            new String[]{String.valueOf(share.getRemoteId())});
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI_SHARE,
                                                      contentValues,
                                                      ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                                                      new String[]{String.valueOf(share.getRemoteId())});
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }
        } else {
            Uri result_uri = null;
            if (getContentResolver() != null) {
                result_uri = getContentResolver().insert(ProviderTableMeta.CONTENT_URI_SHARE, contentValues);
            } else {
                try {
                    result_uri = getContentProviderClient().insert(ProviderTableMeta.CONTENT_URI_SHARE, contentValues);
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }
            if (result_uri != null) {
                long new_id = Long.parseLong(result_uri.getPathSegments().get(1));
                share.setId(new_id);
            }
        }

        return overridden;
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
     * Checks the existence of an stored {@link OCShare} matching the given remote id (not to be confused with the local
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
        Cursor cursor;
        if (getContentResolver() != null) {
            cursor = getContentResolver()
                .query(ProviderTableMeta.CONTENT_URI_SHARE,
                       null,
                       key + AND
                           + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                       new String[]{value, user.getAccountName()},
                       null
                );
        } else {
            try {
                cursor = getContentProviderClient().query(
                    ProviderTableMeta.CONTENT_URI_SHARE,
                    null,
                    key + AND + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                    new String[]{value, user.getAccountName()},
                    null
                );
            } catch (RemoteException e) {
                Log_OC.w(TAG, "Could not get details, assuming share does not exist: " + e.getMessage());
                cursor = null;
            }
        }
        return cursor;
    }


    /**
     * Get first share bound to a file with a known path and given {@link ShareType}.
     *
     * @param path      Path of the file.
     * @param type      Type of the share to get
     * @param shareWith Target of the share. Ignored in type is {@link ShareType#PUBLIC_LINK}
     * @return All {@link OCShare} instance found in DB bound to the file in 'path'
     */
    public List<OCShare> getSharesByPathAndType(String path, ShareType type, String shareWith) {
        Cursor cursor;

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
                user.getAccountName()
            };
        } else {
            if (shareWith == null) {
                selectionArgs = new String[]{
                    path,
                    Integer.toString(type.getValue()),
                    user.getAccountName(),
                    ""
                };
            } else {
                selectionArgs = new String[]{
                    path,
                    Integer.toString(type.getValue()),
                    user.getAccountName(),
                    shareWith
                };
            }
        }

        if (getContentResolver() != null) {
            cursor = getContentResolver().query(
                ProviderTableMeta.CONTENT_URI_SHARE,
                null,
                selection, selectionArgs,
                null);
        } else {
            try {
                cursor = getContentProviderClient().query(
                    ProviderTableMeta.CONTENT_URI_SHARE,
                    null,
                    selection, selectionArgs,
                    null);

            } catch (RemoteException e) {
                Log_OC.e(TAG, "Could not get file details: " + e.getMessage(), e);
                cursor = null;
            }
        }

        List<OCShare> shares = new ArrayList<>();
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

    private ContentValues createContentValueForShare(OCShare share) {
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
        contentValues.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, user.getAccountName());
        contentValues.put(ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED, share.isPasswordProtected() ? 1 : 0);
        contentValues.put(ProviderTableMeta.OCSHARES_NOTE, share.getNote());
        contentValues.put(ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD, share.isHideFileDownload());
        contentValues.put(ProviderTableMeta.OCSHARES_SHARE_LINK, share.getShareLink());
        contentValues.put(ProviderTableMeta.OCSHARES_SHARE_LABEL, share.getLabel());

        return contentValues;
    }

    // test with null cursor?
    private OCShare createShareInstance(Cursor cursor) {
        OCShare share = new OCShare(getString(cursor, ProviderTableMeta.OCSHARES_PATH));
        share.setId(getLong(cursor, ProviderTableMeta._ID));
        share.setFileSource(getLong(cursor, ProviderTableMeta.OCSHARES_ITEM_SOURCE));
        share.setShareType(ShareType.fromValue(getInt(cursor, ProviderTableMeta.OCSHARES_SHARE_TYPE)));
        share.setShareWith(getString(cursor, ProviderTableMeta.OCSHARES_SHARE_WITH));
        share.setPermissions(getInt(cursor, ProviderTableMeta.OCSHARES_PERMISSIONS));
        share.setSharedDate(getLong(cursor, ProviderTableMeta.OCSHARES_SHARED_DATE));
        share.setExpirationDate(getLong(cursor, ProviderTableMeta.OCSHARES_EXPIRATION_DATE));
        share.setToken(getString(cursor, ProviderTableMeta.OCSHARES_TOKEN));
        share.setSharedWithDisplayName(getString(cursor, ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME));
        share.setFolder(getInt(cursor, ProviderTableMeta.OCSHARES_IS_DIRECTORY) == 1);
        share.setUserId(getString(cursor, ProviderTableMeta.OCSHARES_USER_ID));
        share.setRemoteId(getLong(cursor, ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED));
        share.setPasswordProtected(getInt(cursor, ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED) == 1);
        share.setNote(getString(cursor, ProviderTableMeta.OCSHARES_NOTE));
        share.setHideFileDownload(getInt(cursor, ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD) == 1);
        share.setShareLink(getString(cursor, ProviderTableMeta.OCSHARES_SHARE_LINK));
        share.setLabel(getString(cursor, ProviderTableMeta.OCSHARES_SHARE_LABEL));

        return share;
    }

    private void resetShareFlagsInAllFiles() {
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, Boolean.FALSE);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, Boolean.FALSE);
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{user.getAccountName()};

        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs);

        } else {
            try {
                getContentProviderClient().update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInAllFiles" + e.getMessage(), e);
            }
        }
    }

    private void resetShareFlagsInFolder(OCFile folder) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, Boolean.FALSE);
        contentValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, Boolean.FALSE);
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PARENT + " = ?";
        String[] whereArgs = new String[]{user.getAccountName(), String.valueOf(folder.getFileId())};

        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, contentValues, where, whereArgs);

        } else {
            try {
                getContentProviderClient().update(ProviderTableMeta.CONTENT_URI, contentValues, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInFiles" + e.getMessage(), e);
            }
        }
    }

    private void resetShareFlagInAFile(String filePath) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, Boolean.FALSE);
        contentValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, Boolean.FALSE);
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " = ?";
        String[] whereArgs = new String[]{user.getAccountName(), filePath};

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

    @VisibleForTesting
    public void cleanShares() {
        String where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{user.getAccountName()};

        if (getContentResolver() != null) {
            getContentResolver().delete(ProviderTableMeta.CONTENT_URI_SHARE, where, whereArgs);

        } else {
            try {
                getContentProviderClient().delete(ProviderTableMeta.CONTENT_URI_SHARE, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in cleanShares" + e.getMessage(), e);
            }
        }
    }

    // TODO shares null?
    public void saveShares(List<OCShare> shares) {
        cleanShares();
        ArrayList<ContentProviderOperation> operations = new ArrayList<>(shares.size());

        // prepare operations to insert or update files to save in the given folder
        for (OCShare share : shares) {
            ContentValues contentValues = createContentValueForShare(share);

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
            @SuppressWarnings("unused")
            ContentProviderResult[] results = null;
            Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size()));
            try {
                if (getContentResolver() != null) {
                    results = getContentResolver().applyBatch(MainApp.getAuthority(),
                                                              operations);
                } else {
                    results = getContentProviderClient().applyBatch(operations);
                }

            } catch (OperationApplicationException | RemoteException e) {
                Log_OC.e(TAG, EXCEPTION_MSG + e.getMessage(), e);
            }
        }
    }

    public void updateSharedFiles(Collection<OCFile> sharedFiles) {
        resetShareFlagsInAllFiles();

        if (sharedFiles != null) {
            ArrayList<ContentProviderOperation> operations = new ArrayList<>(sharedFiles.size());

            // prepare operations to insert or update files to save in the given folder
            for (OCFile file : sharedFiles) {
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
                cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
                cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
                if (!file.isFolder()) {
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
                }
                cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, user.getAccountName());
                cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
                cv.put(
                    ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA,
                    file.getLastSyncDateForData()
                );
                cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
                cv.put(ProviderTableMeta.FILE_ETAG_ON_SERVER, file.getEtagOnServer());
                cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, file.isSharedViaLink() ? 1 : 0);
                cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, file.isSharedWithSharee() ? 1 : 0);
                cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.getPermissions());
                cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.getRemoteId());
                cv.put(ProviderTableMeta.FILE_FAVORITE, file.isFavorite());
                cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.isUpdateThumbnailNeeded() ? 1 : 0);
                cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading() ? 1 : 0);
                cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, file.getEtagInConflict());

                boolean existsByPath = fileExists(file.getRemotePath());
                if (existsByPath || fileExists(file.getFileId())) {
                    // updating an existing file
                    operations.add(
                        ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                            withValues(cv).
                            withSelection(ProviderTableMeta._ID + "=?",
                                          new String[]{String.valueOf(file.getFileId())})
                            .build());

                } else {
                    // adding a new file
                    operations.add(
                        ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI).
                            withValues(cv).
                            build()
                    );
                }
            }

            // apply operations in batch
            if (operations.size() > 0) {
                @SuppressWarnings("unused")
                ContentProviderResult[] results = null;
                Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size()));
                try {
                    if (getContentResolver() != null) {
                        results = getContentResolver().applyBatch(MainApp.getAuthority(), operations);
                    } else {
                        results = getContentProviderClient().applyBatch(operations);
                    }

                } catch (OperationApplicationException | RemoteException e) {
                    Log_OC.e(TAG, EXCEPTION_MSG + e.getMessage(), e);
                }
            }
        }

    }

    public void removeShare(OCShare share) {
        Uri contentUriShare = ProviderTableMeta.CONTENT_URI_SHARE;
        String where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + AND +
            ProviderTableMeta._ID + " = ?";
        String[] whereArgs = {user.getAccountName(), Long.toString(share.getId())};

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

    // TOOD check if shares can be null
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
        Iterable<OCShare> shares, ArrayList<ContentProviderOperation> operations) {

        ContentValues contentValues;
        // prepare operations to insert or update files to save in the given folder
        for (OCShare share : shares) {
            contentValues = createContentValueForShare(share);

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
            String where = ProviderTableMeta.OCSHARES_PATH + AND
                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?";
            String[] whereArgs = new String[]{"", user.getAccountName()};

            List<OCFile> files = getFolderContent(folder, false);

            for (OCFile file : files) {
                whereArgs[0] = file.getRemotePath();
                preparedOperations.add(
                    ContentProviderOperation.newDelete(ProviderTableMeta.CONTENT_URI_SHARE).
                        withSelection(where, whereArgs).
                        build()
                );
            }
        }
        return preparedOperations;
    }

    private ArrayList<ContentProviderOperation> prepareRemoveSharesInFile(
        String filePath, ArrayList<ContentProviderOperation> preparedOperations) {

        String where = ProviderTableMeta.OCSHARES_PATH + AND
            + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = ?";
        String[] whereArgs = new String[]{filePath, user.getAccountName()};

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
            + ProviderTableMeta.OCSHARES_SHARE_TYPE + " = ? OR "
            + ProviderTableMeta.OCSHARES_SHARE_TYPE + " = ? ) ";
        String[] selectionArgs = new String[]{filePath, accountName,
            Integer.toString(ShareType.USER.getValue()),
            Integer.toString(ShareType.GROUP.getValue()),
            Integer.toString(ShareType.EMAIL.getValue()),
            Integer.toString(ShareType.FEDERATED.getValue()),
            Integer.toString(ShareType.ROOM.getValue()),
            Integer.toString(ShareType.CIRCLE.getValue())
        };

        Cursor cursor = null;
        if (getContentResolver() != null) {
            cursor = getContentResolver().query(ProviderTableMeta.CONTENT_URI_SHARE,
                                                null,
                                                selection,
                                                selectionArgs,
                                                null);
        } else {
            try {
                cursor = getContentProviderClient().query(ProviderTableMeta.CONTENT_URI_SHARE,
                                                          null,
                                                          selection,
                                                          selectionArgs,
                                                          null);

            } catch (RemoteException e) {
                Log_OC.e(TAG, "Could not get list of shares with: " + e.getMessage(), e);
                cursor = null;
            }
        }
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
        triggerMediaScan(path, null);
    }

    public static void triggerMediaScan(String path, OCFile file) {
        if (path != null && !TextUtils.isEmpty(path)) {
            ContentValues values = new ContentValues();
            ContentResolver contentResolver = MainApp.getAppContext().getContentResolver();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
                if (file != null) {
                    values.put(MediaStore.Images.Media.MIME_TYPE, file.getMimeType());
                    values.put(MediaStore.Images.Media.TITLE, file.getFileName());
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getFileName());
                }
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
                values.put(MediaStore.Images.Media.RELATIVE_PATH, path);
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                try {
                    contentResolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                                           values);
                } catch (IllegalArgumentException e) {
                    Log_OC.e("MediaScanner", "Adding image to media scanner failed: " + e);
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(new File(path)));
                MainApp.getAppContext().sendBroadcast(intent);
            }
        }
    }

    public void deleteFileInMediaScan(String path) {
        String mimetypeString = FileStorageUtils.getMimeTypeFromName(path);
        ContentResolver contentResolver = getContentResolver();

        if (contentResolver != null) {
            if (MimeTypeUtil.isImage(mimetypeString)) {
                // Images
                contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                       MediaStore.Images.Media.DATA + "=?", new String[]{path});
            } else if (MimeTypeUtil.isAudio(mimetypeString)) {
                // Audio
                contentResolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                       MediaStore.Audio.Media.DATA + "=?", new String[]{path});
            } else if (MimeTypeUtil.isVideo(mimetypeString)) {
                // Video
                contentResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                       MediaStore.Video.Media.DATA + "=?", new String[]{path});
            }
        } else {
            ContentProviderClient contentProviderClient = getContentProviderClient();
            try {
                if (MimeTypeUtil.isImage(mimetypeString)) {
                    // Images
                    contentProviderClient.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                 MediaStore.Images.Media.DATA + "=?", new String[]{path});
                } else if (MimeTypeUtil.isAudio(mimetypeString)) {
                    // Audio
                    contentProviderClient.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                 MediaStore.Audio.Media.DATA + "=?", new String[]{path});
                } else if (MimeTypeUtil.isVideo(mimetypeString)) {
                    // Video
                    contentProviderClient.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                 MediaStore.Video.Media.DATA + "=?", new String[]{path});
                }
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception deleting media file in MediaStore " + e.getMessage(), e);
            }
        }
    }

    public void saveConflict(OCFile ocFile, String etagInConflict) {
        ContentValues cv = new ContentValues();
        if (!ocFile.isDown()) {
            cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, (String) null);
        } else {
            cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, etagInConflict);
        }

        int updated = 0;
        if (getContentResolver() != null) {
            updated = getContentResolver().update(
                ProviderTableMeta.CONTENT_URI_FILE,
                cv,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(ocFile.getFileId())}
            );
        } else {
            try {
                updated = getContentProviderClient().update(
                    ProviderTableMeta.CONTENT_URI_FILE,
                    cv,
                    ProviderTableMeta._ID + "=?",
                    new String[]{String.valueOf(ocFile.getFileId())}
                );
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Failed saving conflict in database " + e.getMessage(), e);
            }
        }

        Log_OC.d(TAG, "Number of files updated with CONFLICT: " + updated);

        if (updated > 0) {
            if (etagInConflict != null && ocFile.isDown()) {
                /// set conflict in all ancestor folders

                long parentId = ocFile.getParentId();
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
                        updated = getContentResolver().update(
                            ProviderTableMeta.CONTENT_URI_FILE,
                            cv,
                            stringBuilder.toString(),
                            ancestorIds.toArray(new String[]{})
                        );
                    } else {
                        try {
                            updated = getContentProviderClient().update(
                                ProviderTableMeta.CONTENT_URI_FILE,
                                cv,
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
                String parentPath = ocFile.getRemotePath();
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
                    Cursor descendentsInConflict = null;
                    if (getContentResolver() != null) {
                        descendentsInConflict = getContentResolver().query(
                            ProviderTableMeta.CONTENT_URI_FILE,
                            projection,
                            whereForDescencentsInConflict,
                            new String[]{user.getAccountName(), parentPath + "%"},
                            null
                        );
                    } else {
                        try {
                            descendentsInConflict = getContentProviderClient().query(
                                ProviderTableMeta.CONTENT_URI_FILE,
                                projection,
                                whereForDescencentsInConflict,
                                new String[]{user.getAccountName(), parentPath + "%"},
                                null
                            );
                        } catch (RemoteException e) {
                            Log_OC.e(TAG, "Failed querying for descendents in conflict " + e.getMessage(), e);
                        }
                    }

                    if (descendentsInConflict == null || descendentsInConflict.getCount() == 0) {
                        Log_OC.d(TAG, "NO MORE conflicts in " + parentPath);
                        if (getContentResolver() != null) {
                            updated = getContentResolver().update(
                                ProviderTableMeta.CONTENT_URI_FILE,
                                cv,
                                ProviderTableMeta.FILE_ACCOUNT_OWNER + AND +
                                    ProviderTableMeta.FILE_PATH + "=?",
                                new String[]{user.getAccountName(), parentPath}
                            );
                        } else {
                            try {
                                updated = getContentProviderClient().update(
                                    ProviderTableMeta.CONTENT_URI_FILE,
                                    cv,
                                    ProviderTableMeta.FILE_ACCOUNT_OWNER + AND +
                                        ProviderTableMeta.FILE_PATH + "=?"
                                    , new String[]{user.getAccountName(), parentPath}
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
        // Prepare capabilities data
        ContentValues contentValues = createContentValues(user.getAccountName(), capability);

        if (capabilityExists(user.getAccountName())) {
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI_CAPABILITIES, contentValues,
                                            ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=?",
                                            new String[]{user.getAccountName()});
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI_CAPABILITIES,
                                                      contentValues, ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=?",
                                                      new String[]{user.getAccountName()});
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }
        } else {
            Uri result_uri = null;
            if (getContentResolver() != null) {
                result_uri = getContentResolver().insert(
                    ProviderTableMeta.CONTENT_URI_CAPABILITIES, contentValues);
            } else {
                try {
                    result_uri = getContentProviderClient().insert(
                        ProviderTableMeta.CONTENT_URI_CAPABILITIES, contentValues);
                } catch (RemoteException e) {
                    Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
                }
            }
            if (result_uri != null) {
                long new_id = Long.parseLong(result_uri.getPathSegments()
                                                 .get(1));
                capability.setId(new_id);
                capability.setAccountName(user.getAccountName());
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
        contentValues.put(ProviderTableMeta.CAPABILITIES_SERVER_LOGO,
                          capability.getServerLogo());
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
        contentValues.put(ProviderTableMeta.CAPABILITIES_ETAG, capability.getEtag());
        contentValues.put(ProviderTableMeta.CAPABILITIES_USER_STATUS, capability.getUserStatus().getValue());
        contentValues.put(ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI,
                          capability.getUserStatusSupportsEmoji().getValue());

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
        Cursor cursor = null;
        if (getContentResolver() != null) {
            cursor = getContentResolver()
                .query(ProviderTableMeta.CONTENT_URI_CAPABILITIES,
                       null,
                       ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=? ",
                       new String[]{accountName}, null);
        } else {
            try {
                cursor = getContentProviderClient().query(
                    ProviderTableMeta.CONTENT_URI_CAPABILITIES,
                    null,
                    ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=? ",
                    new String[]{accountName}, null);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Couldn't determine capability existence, assuming non existance: " + e.getMessage(), e);
            }
        }

        return cursor;
    }

    @NonNull
    public OCCapability getCapability(User user) {
        return getCapability(user.getAccountName());
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

    public boolean capabilityExistsForAccount(String accountName) {
        Cursor cursor = getCapabilityCursorForAccount(accountName);

        boolean exists = cursor.moveToFirst();
        cursor.close();

        return exists;
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
            capability.setFilesSharingApiEnabled(getBoolean(cursor,
                                                            ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED));
            capability.setFilesSharingPublicEnabled(getBoolean(cursor,
                                                               ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED));
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
            capability.setFilesSharingPublicUpload(getBoolean(cursor,
                                                              ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD));
            capability.setFilesSharingUserSendMail(getBoolean(cursor,
                                                              ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL));
            capability.setFilesSharingResharing(getBoolean(cursor,
                                                           ProviderTableMeta.CAPABILITIES_SHARING_RESHARING));
            capability.setFilesSharingFederationOutgoing(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING));
            capability.setFilesSharingFederationIncoming(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING));
            capability.setFilesBigFileChunking(getBoolean(cursor,
                                                          ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING));
            capability.setFilesUndelete(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_FILES_UNDELETE));
            capability.setFilesVersioning(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_FILES_VERSIONING));
            capability.setExternalLinks(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_EXTERNAL_LINKS));
            capability.setServerName(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_NAME));
            capability.setServerColor(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_COLOR));
            capability.setServerTextColor(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR));
            capability.setServerElementColor(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR));
            capability.setServerBackground(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL));
            capability.setServerSlogan(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN));
            capability.setServerLogo(getString(cursor, ProviderTableMeta.CAPABILITIES_SERVER_LOGO));
            capability.setEndToEndEncryption(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION));
            capability.setServerBackgroundDefault(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT));
            capability.setServerBackgroundPlain(getBoolean(cursor,
                                                           ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN));
            capability.setActivity(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_ACTIVITY));
            capability.setRichDocuments(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT));
            capability.setRichDocumentsDirectEditing(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING));
            capability.setRichDocumentsTemplatesAvailable(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES));
            String mimetypes = getString(cursor, ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST);
            if (mimetypes == null) {
                mimetypes = "";
            }
            capability.setRichDocumentsMimeTypeList(Arrays.asList(mimetypes.split(",")));

            String optionalMimetypes = getString(cursor,
                                                 ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST);
            if (optionalMimetypes == null) {
                optionalMimetypes = "";
            }
            capability.setRichDocumentsOptionalMimeTypeList(Arrays.asList(optionalMimetypes.split(",")));
            capability.setRichDocumentsProductName(getString(cursor,
                                                             ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME));
            capability.setDirectEditingEtag(getString(cursor, ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG));
            capability.setEtag(getString(cursor, ProviderTableMeta.CAPABILITIES_ETAG));
            capability.setUserStatus(getBoolean(cursor, ProviderTableMeta.CAPABILITIES_USER_STATUS));
            capability.setUserStatusSupportsEmoji(
                getBoolean(cursor, ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI));
        }
        return capability;
    }

    public void deleteVirtuals(VirtualFolderType type) {
        if (getContentResolver() != null) {
            getContentResolver().delete(ProviderTableMeta.CONTENT_URI_VIRTUAL,
                                        ProviderTableMeta.VIRTUAL_TYPE + "=?", new String[]{String.valueOf(type)});
        } else {
            try {
                getContentProviderClient().delete(ProviderTableMeta.CONTENT_URI_VIRTUAL,
                                                  ProviderTableMeta.VIRTUAL_TYPE + "=?",
                                                  new String[]{String.valueOf(type)});
            } catch (RemoteException e) {
                Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
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

    public void saveVirtual(VirtualFolderType type, OCFile file) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.VIRTUAL_TYPE, type.toString());
        cv.put(ProviderTableMeta.VIRTUAL_OCFILE_ID, file.getFileId());

        if (getContentResolver() != null) {
            getContentResolver().insert(ProviderTableMeta.CONTENT_URI_VIRTUAL, cv);
        } else {
            try {
                getContentProviderClient().insert(ProviderTableMeta.CONTENT_URI_VIRTUAL, cv);
            } catch (RemoteException e) {
                Log_OC.e(TAG, FAILED_TO_INSERT_MSG + e.getMessage(), e);
            }
        }
    }

    public List<OCFile> getVirtualFolderContent(VirtualFolderType type, boolean onlyImages) {
        List<OCFile> ocFiles = new ArrayList<>();
        Uri req_uri = ProviderTableMeta.CONTENT_URI_VIRTUAL;
        Cursor c;

        if (getContentProviderClient() != null) {
            try {
                c = getContentProviderClient().query(
                    req_uri,
                    null,
                    ProviderTableMeta.VIRTUAL_TYPE + "=?",
                    new String[]{String.valueOf(type)},
                    null
                );
            } catch (RemoteException e) {
                Log_OC.e(TAG, e.getMessage(), e);
                return ocFiles;
            }
        } else {
            c = getContentResolver().query(
                req_uri,
                null,
                ProviderTableMeta.VIRTUAL_TYPE + "=?",
                new String[]{String.valueOf(type)},
                null
            );
        }

        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    OCFile child = createFileInstanceFromVirtual(c);

                    if (child != null) {
                        ocFiles.add(child);
                    }
                } while (c.moveToNext());
            }

            c.close();
        }

        if (onlyImages) {
            List<OCFile> temp = new ArrayList<>();

            for (OCFile file : ocFiles) {
                if (MimeTypeUtil.isImage(file)) {
                    temp.add(file);
                }
            }
            ocFiles = temp;
        }

        if (ocFiles.size() > 0) {
            Collections.sort(ocFiles);
        }

        return ocFiles;
    }

    public void deleteAllFiles() {
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "= ? AND " +
            ProviderTableMeta.FILE_PATH + "= ?";
        String[] whereArgs = new String[]{user.getAccountName(), OCFile.ROOT_PATH};

        if (getContentResolver() != null) {
            getContentResolver().delete(ProviderTableMeta.CONTENT_URI_DIR, where, whereArgs);
        } else {
            try {
                getContentProviderClient().delete(ProviderTableMeta.CONTENT_URI_DIR, where, whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in deleteAllFiles for account " + user.getAccountName() + ": " + e.getMessage(), e);
            }
        }
    }

    public List<OCFile> getAllFiles() {
        String selection = ProviderTableMeta.FILE_ACCOUNT_OWNER + "= ? ";
        String[] selectionArgs = new String[]{user.getAccountName()};

        List<OCFile> folderContent = new ArrayList<>();

        Uri requestURI = ProviderTableMeta.CONTENT_URI_DIR;
        Cursor cursor;

        if (getContentProviderClient() != null) {
            try {
                cursor = getContentProviderClient().query(requestURI, null, selection, selectionArgs, null);
            } catch (RemoteException e) {
                Log_OC.e(TAG, e.getMessage(), e);
                return folderContent;
            }
        } else {
            cursor = getContentResolver().query(requestURI, null, selection, selectionArgs, null);
        }

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    folderContent.add(createFileInstance(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        return folderContent;
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

    public ContentResolver getContentResolver() {
        return this.contentResolver;
    }

    public ContentProviderClient getContentProviderClient() {
        return this.contentProviderClient;
    }

    @Deprecated
    public Account getAccount() {
        return this.user.toPlatformAccount();
    }
}
