/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;

import com.owncloud.android.MainApp;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.CapabilityBooleanType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class FileDataStorageManager {

    public static final int ROOT_PARENT_ID = 0;

    private ContentResolver mContentResolver;
    private ContentProviderClient mContentProviderClient;
    private Account mAccount;

    private static String TAG = FileDataStorageManager.class.getSimpleName();


    public FileDataStorageManager(Account account, ContentResolver cr) {
        mContentProviderClient = null;
        mContentResolver = cr;
        mAccount = account;
    }

    public FileDataStorageManager(Account account, ContentProviderClient cp) {
        mContentProviderClient = cp;
        mContentResolver = null;
        mAccount = account;
    }


    public void setAccount(Account account) {
        mAccount = account;
    }

    public Account getAccount() {
        return mAccount;
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    public ContentProviderClient getContentProviderClient() {
        return mContentProviderClient;
    }


    public OCFile getFileByPath(String path) {
        Cursor c = getFileCursorForValue(ProviderTableMeta.FILE_PATH, path);
        OCFile file = null;
        if (c.moveToFirst()) {
            file = createFileInstance(c);
        }
        c.close();
        if (file == null && OCFile.ROOT_PATH.equals(path)) {
            return createRootDir(); // root should always exist
        }
        return file;
    }


    public OCFile getFileById(long id) {
        Cursor c = getFileCursorForValue(ProviderTableMeta._ID, String.valueOf(id));
        OCFile file = null;
        if (c.moveToFirst()) {
            file = createFileInstance(c);
        }
        c.close();
        return file;
    }

    public OCFile getFileByLocalPath(String path) {
        Cursor c = getFileCursorForValue(ProviderTableMeta.FILE_STORAGE_PATH, path);
        OCFile file = null;
        if (c.moveToFirst()) {
            file = createFileInstance(c);
        }
        c.close();
        return file;
    }

    public boolean fileExists(long id) {
        return fileExists(ProviderTableMeta._ID, String.valueOf(id));
    }

    public boolean fileExists(String path) {
        return fileExists(ProviderTableMeta.FILE_PATH, path);
    }


    public Vector<OCFile> getFolderContent(OCFile f/*, boolean onlyOnDevice*/) {
        if (f != null && f.isFolder() && f.getFileId() != -1) {
            // TODO Enable when "On Device" is recovered ?
            return getFolderContent(f.getFileId()/*, onlyOnDevice*/);

        } else {
            return new Vector<OCFile>();
        }
    }


    public Vector<OCFile> getFolderImages(OCFile folder/*, boolean onlyOnDevice*/) {
        Vector<OCFile> ret = new Vector<OCFile>();
        if (folder != null) {
            // TODO better implementation, filtering in the access to database instead of here
            // TODO Enable when "On Device" is recovered ?
            Vector<OCFile> tmp = getFolderContent(folder/*, onlyOnDevice*/);
            OCFile current = null;
            for (int i=0; i<tmp.size(); i++) {
                current = tmp.get(i);
                if (current.isImage()) {
                    ret.add(current);
                }
            }
        }
        return ret;
    }

    public boolean saveFile(OCFile file) {
        boolean overriden = false;
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_MODIFIED, file.getModificationTimestamp());
        cv.put(
                ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
                file.getModificationTimestampAtLastSyncForData()
        );
        cv.put(ProviderTableMeta.FILE_CREATION, file.getCreationTimestamp());
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.getFileLength());
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.getMimetype());
        cv.put(ProviderTableMeta.FILE_NAME, file.getFileName());
        cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
        cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
        if (!file.isFolder())
            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.isFavorite() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, file.isSharedViaLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, file.isSharedWithSharee() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, file.getPublicLink());
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.getPermissions());
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.getRemoteId());
        cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.needsUpdateThumbnail());
        cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading());
        cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, file.getEtagInConflict());

        boolean sameRemotePath = fileExists(file.getRemotePath());
        if (sameRemotePath ||
                fileExists(file.getFileId())) {  // for renamed files; no more delete and create

            OCFile oldFile;
            if (sameRemotePath) {
                oldFile = getFileByPath(file.getRemotePath());
                file.setFileId(oldFile.getFileId());
            } else {
                oldFile = getFileById(file.getFileId());
            }

            overriden = true;
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv,
                        ProviderTableMeta._ID + "=?",
                        new String[]{String.valueOf(file.getFileId())});
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI,
                            cv, ProviderTableMeta._ID + "=?",
                            new String[]{String.valueOf(file.getFileId())});
                } catch (RemoteException e) {
                    Log_OC.e(TAG,
                            "Fail to insert insert file to database "
                                    + e.getMessage());
                }
            }
        } else {
            Uri result_uri = null;
            if (getContentResolver() != null) {
                result_uri = getContentResolver().insert(
                        ProviderTableMeta.CONTENT_URI_FILE, cv);
            } else {
                try {
                    result_uri = getContentProviderClient().insert(
                            ProviderTableMeta.CONTENT_URI_FILE, cv);
                } catch (RemoteException e) {
                    Log_OC.e(TAG,
                            "Fail to insert insert file to database "
                                    + e.getMessage());
                }
            }
            if (result_uri != null) {
                long new_id = Long.parseLong(result_uri.getPathSegments()
                        .get(1));
                file.setFileId(new_id);
            }
        }

        return overriden;
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
     * <p/>
     * CALLER IS THE RESPONSIBLE FOR GRANTING RIGHT UPDATE OF INFORMATION, NOT THIS METHOD.
     * HERE ONLY DATA CONSISTENCY SHOULD BE GRANTED
     *
     * @param folder
     * @param updatedFiles
     * @param filesToRemove
     */
    public void saveFolder(
            OCFile folder, Collection<OCFile> updatedFiles, Collection<OCFile> filesToRemove
    ) {

        Log_OC.d(TAG,  "Saving folder " + folder.getRemotePath() + " with " + updatedFiles.size()
                + " children and " + filesToRemove.size() + " files to remove");

        ArrayList<ContentProviderOperation> operations =
                new ArrayList<ContentProviderOperation>(updatedFiles.size());

        // prepare operations to insert or update files to save in the given folder
        for (OCFile file : updatedFiles) {
            ContentValues cv = new ContentValues();
            cv.put(ProviderTableMeta.FILE_MODIFIED, file.getModificationTimestamp());
            cv.put(
                    ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
                    file.getModificationTimestampAtLastSyncForData()
            );
            cv.put(ProviderTableMeta.FILE_CREATION, file.getCreationTimestamp());
            cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.getFileLength());
            cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.getMimetype());
            cv.put(ProviderTableMeta.FILE_NAME, file.getFileName());
            //cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
            cv.put(ProviderTableMeta.FILE_PARENT, folder.getFileId());
            cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
            if (!file.isFolder()) {
                cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
            }
            cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
            cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
            cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.getLastSyncDateForData());
            cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.isFavorite() ? 1 : 0);
            cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
            cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, file.isSharedViaLink() ? 1 : 0);
            cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, file.isSharedWithSharee() ? 1 : 0);
            cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, file.getPublicLink());
            cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.getPermissions());
            cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.getRemoteId());
            cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.needsUpdateThumbnail());
            cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading());
            cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, file.getEtagInConflict());

            boolean existsByPath = fileExists(file.getRemotePath());
            if (existsByPath || fileExists(file.getFileId())) {
                // updating an existing file
                operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                        withValues(cv).
                        withSelection(ProviderTableMeta._ID + "=?",
                                new String[]{String.valueOf(file.getFileId())})
                        .build());

            } else {
                // adding a new file
                operations.add(ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI).
                        withValues(cv).build());
            }
        }

        // prepare operations to remove files in the given folder
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?" + " AND " +
                ProviderTableMeta.FILE_PATH + "=?";
        String [] whereArgs = null;
        for (OCFile file : filesToRemove) {
            if (file.getParentId() == folder.getFileId()) {
                whereArgs = new String[]{mAccount.name, file.getRemotePath()};
                if (file.isFolder()) {
                    operations.add(ContentProviderOperation.newDelete(
                            ContentUris.withAppendedId(
                                    ProviderTableMeta.CONTENT_URI_DIR, file.getFileId()
                            )
                    ).withSelection(where, whereArgs).build());

                    File localFolder =
                            new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
                    if (localFolder.exists()) {
                        removeLocalFolder(localFolder);
                    }
                } else {
                    operations.add(ContentProviderOperation.newDelete(
                            ContentUris.withAppendedId(
                                    ProviderTableMeta.CONTENT_URI_FILE, file.getFileId()
                            )
                    ).withSelection(where, whereArgs).build());

                    if (file.isDown()) {
                        String path = file.getStoragePath();
                        new File(path).delete();
                        triggerMediaScan(path); // notify MediaScanner about removed file
                    }
                }
            }
        }

        // update metadata of folder
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_MODIFIED, folder.getModificationTimestamp());
        cv.put(
                ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
                folder.getModificationTimestampAtLastSyncForData()
        );
        cv.put(ProviderTableMeta.FILE_CREATION, folder.getCreationTimestamp());
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, 0);
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, folder.getMimetype());
        cv.put(ProviderTableMeta.FILE_NAME, folder.getFileName());
        cv.put(ProviderTableMeta.FILE_PARENT, folder.getParentId());
        cv.put(ProviderTableMeta.FILE_PATH, folder.getRemotePath());
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, folder.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, folder.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, folder.isFavorite() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_ETAG, folder.getEtag());
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, folder.isSharedViaLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, folder.isSharedWithSharee() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, folder.getPublicLink());
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, folder.getPermissions());
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, folder.getRemoteId());

        operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                withValues(cv).
                withSelection(ProviderTableMeta._ID + "=?",
                        new String[]{String.valueOf(folder.getFileId())})
                .build());

        // apply operations in batch
        ContentProviderResult[] results = null;
        Log_OC.d(TAG, "Sending " + operations.size() + " operations to FileContentProvider");
        try {
            if (getContentResolver() != null) {
                results = getContentResolver().applyBatch(MainApp.getAuthority(), operations);

            } else {
                results = getContentProviderClient().applyBatch(operations);
            }

        } catch (OperationApplicationException e) {
            Log_OC.e(TAG, "Exception in batch of operations " + e.getMessage());

        } catch (RemoteException e) {
            Log_OC.e(TAG, "Exception in batch of operations  " + e.getMessage());
        }

        // update new id in file objects for insertions
        if (results != null) {
            long newId;
            Iterator<OCFile> filesIt = updatedFiles.iterator();
            OCFile file = null;
            for (int i = 0; i < results.length; i++) {
                if (filesIt.hasNext()) {
                    file = filesIt.next();
                } else {
                    file = null;
                }
                if (results[i].uri != null) {
                    newId = Long.parseLong(results[i].uri.getPathSegments().get(1));
                    //updatedFiles.get(i).setFileId(newId);
                    if (file != null) {
                        file.setFileId(newId);
                    }
                }
            }
        }

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
                    Uri file_uri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE,
                            file.getFileId());
                    String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?" + " AND " +
                            ProviderTableMeta.FILE_PATH + "=?";
                    String[] whereArgs = new String[]{mAccount.name, file.getRemotePath()};
                    int deleted = 0;
                    if (getContentProviderClient() != null) {
                        try {
                            deleted = getContentProviderClient().delete(file_uri, where, whereArgs);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else {
                        deleted = getContentResolver().delete(file_uri, where, whereArgs);
                    }
                    success &= (deleted > 0);
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
            success = false;
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
        }
        return success;
    }

    private boolean removeFolderInDb(OCFile folder) {
        Uri folder_uri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, "" +
                folder.getFileId());   // URI for recursive deletion
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?" + " AND " +
                ProviderTableMeta.FILE_PATH + "=?";
        String [] whereArgs = new String[]{mAccount.name, folder.getRemotePath()};
        int deleted = 0;
        if (getContentProviderClient() != null) {
            try {
                deleted = getContentProviderClient().delete(folder_uri, where, whereArgs);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            deleted = getContentResolver().delete(folder_uri, where, whereArgs);
        }
        return deleted > 0;
    }

    private boolean removeLocalFolder(OCFile folder) {
        boolean success = true;
        String localFolderPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, folder);
        File localFolder = new File(localFolderPath);
        if (localFolder.exists()) {
            // stage 1: remove the local files already registered in the files database
            // TODO Enable when "On Device" is recovered ?
            Vector<OCFile> files = getFolderContent(folder.getFileId()/*, false*/);
            if (files != null) {
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
                    String path = localFile.getAbsolutePath();
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
     * TODO explore better (faster) implementations
     * TODO throw exceptions up !
     */
    public void moveLocalFile(OCFile file, String targetPath, String targetParentPath) {

        if (file != null && file.fileExists() && !OCFile.ROOT_PATH.equals(file.getFileName())) {

            OCFile targetParent = getFileByPath(targetParentPath);
            if (targetParent == null) {
                throw new IllegalStateException(
                        "Parent folder of the target path does not exist!!");
            }

            /// 1. get all the descendants of the moved element in a single QUERY
            Cursor c = null;
            if (getContentProviderClient() != null) {
                try {
                    c = getContentProviderClient().query(
                            ProviderTableMeta.CONTENT_URI,
                            null,
                            ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                                    ProviderTableMeta.FILE_PATH + " LIKE ? ",
                            new String[]{
                                    mAccount.name,
                                    file.getRemotePath() + "%"
                            },
                            ProviderTableMeta.FILE_PATH + " ASC "
                    );
                } catch (RemoteException e) {
                    Log_OC.e(TAG, e.getMessage());
                }

            } else {
                c = getContentResolver().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                                ProviderTableMeta.FILE_PATH + " LIKE ? ",
                        new String[]{
                                mAccount.name,
                                file.getRemotePath() + "%"
                        },
                        ProviderTableMeta.FILE_PATH + " ASC "
                );
            }

            /// 2. prepare a batch of update operations to change all the descendants
            ArrayList<ContentProviderOperation> operations =
                    new ArrayList<ContentProviderOperation>(c.getCount());
            String defaultSavePath = FileStorageUtils.getSavePath(mAccount.name);
            List<String> originalPathsToTriggerMediaScan = new ArrayList<String>();
            List<String> newPathsToTriggerMediaScan = new ArrayList<String>();
            if (c.moveToFirst()) {
                int lengthOfOldPath = file.getRemotePath().length();
                int lengthOfOldStoragePath = defaultSavePath.length() + lengthOfOldPath;
                do {
                    ContentValues cv = new ContentValues(); // keep construction in the loop
                    OCFile child = createFileInstance(c);
                    cv.put(
                            ProviderTableMeta.FILE_PATH,
                            targetPath + child.getRemotePath().substring(lengthOfOldPath)
                    );
                    if (child.getStoragePath() != null &&
                            child.getStoragePath().startsWith(defaultSavePath)) {
                        // update link to downloaded content - but local move is not done here!
                        String targetLocalPath = defaultSavePath + targetPath +
                                child.getStoragePath().substring(lengthOfOldStoragePath);

                        cv.put(ProviderTableMeta.FILE_STORAGE_PATH, targetLocalPath);

                        originalPathsToTriggerMediaScan.add(child.getStoragePath());
                        newPathsToTriggerMediaScan.add(targetLocalPath);

                    }
                    if (child.getRemotePath().equals(file.getRemotePath())) {
                        cv.put(
                                ProviderTableMeta.FILE_PARENT,
                                targetParent.getFileId()
                        );
                    }
                    operations.add(
                            ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                                    withValues(cv).
                                    withSelection(
                                            ProviderTableMeta._ID + "=?",
                                            new String[]{String.valueOf(child.getFileId())}
                                    )
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
                Log_OC.e(TAG, "Fail to update " + file.getFileId() + " and descendants in database",
                        e);
            }

            /// 4. move in local file system 
            String originalLocalPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, file);
            String targetLocalPath = defaultSavePath + targetPath;
            File localFile = new File(originalLocalPath);
            boolean renamed = false;
            if (localFile.exists()) {
                File targetFile = new File(targetLocalPath);
                File targetFolder = targetFile.getParentFile();
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs();
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
            String localPath = FileStorageUtils.getDefaultSavePathFor(mAccount.name, file);
            File localFile = new File(localPath);
            boolean copied = false;
            String defaultSavePath = FileStorageUtils.getSavePath(mAccount.name);
            if (localFile.exists()) {
                File targetFile = new File(defaultSavePath + targetPath);
                File targetFolder = targetFile.getParentFile();
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs();
                }
                copied = copyFile(localFile, targetFile);
            }
            Log_OC.d(TAG, "Local file COPIED : " + copied);
        }
    }

    private boolean copyFile(File src, File target) {
        boolean ret = true;

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(target);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException ex) {
            ret = false;
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }

        return ret;
    }


    private Vector<OCFile> getFolderContent(long parentId/*, boolean onlyOnDevice*/) {

        Vector<OCFile> ret = new Vector<OCFile>();

        Uri req_uri = Uri.withAppendedPath(
                ProviderTableMeta.CONTENT_URI_DIR,
                String.valueOf(parentId));
        Cursor c = null;

        if (getContentProviderClient() != null) {
            try {
                c = getContentProviderClient().query(req_uri, null,
                        ProviderTableMeta.FILE_PARENT + "=?",
                        new String[]{String.valueOf(parentId)}, null);
            } catch (RemoteException e) {
                Log_OC.e(TAG, e.getMessage());
                return ret;
            }
        } else {
            c = getContentResolver().query(req_uri, null,
                    ProviderTableMeta.FILE_PARENT + "=?",
                    new String[]{String.valueOf(parentId)}, null);
        }

        if (c.moveToFirst()) {
            do {
                OCFile child = createFileInstance(c);
                // TODO Enable when "On Device" is recovered ?
                // if (child.isFolder() || !onlyOnDevice || onlyOnDevice && child.isDown()){
                ret.add(child);
                // }
            } while (c.moveToNext());
        }

        c.close();

        Collections.sort(ret);

        return ret;
    }


    private OCFile createRootDir() {
        OCFile file = new OCFile(OCFile.ROOT_PATH);
        file.setMimetype("DIR");
        file.setParentId(FileDataStorageManager.ROOT_PARENT_ID);
        saveFile(file);
        return file;
    }

    private boolean fileExists(String cmp_key, String value) {
        Cursor c;
        if (getContentResolver() != null) {
            c = getContentResolver()
                    .query(ProviderTableMeta.CONTENT_URI,
                            null,
                            cmp_key + "=? AND "
                                    + ProviderTableMeta.FILE_ACCOUNT_OWNER
                                    + "=?",
                            new String[]{value, mAccount.name}, null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        cmp_key + "=? AND "
                                + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                        new String[]{value, mAccount.name}, null);
            } catch (RemoteException e) {
                Log_OC.e(TAG,
                        "Couldn't determine file existance, assuming non existance: "
                                + e.getMessage());
                return false;
            }
        }
        boolean retval = c.moveToFirst();
        c.close();
        return retval;
    }

    private Cursor getFileCursorForValue(String key, String value) {
        Cursor c = null;
        if (getContentResolver() != null) {
            c = getContentResolver()
                    .query(ProviderTableMeta.CONTENT_URI,
                            null,
                            key + "=? AND "
                                    + ProviderTableMeta.FILE_ACCOUNT_OWNER
                                    + "=?",
                            new String[]{value, mAccount.name}, null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        key + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER
                                + "=?", new String[]{value, mAccount.name},
                        null);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Could not get file details: " + e.getMessage());
                c = null;
            }
        }
        return c;
    }


    private OCFile createFileInstance(Cursor c) {
        OCFile file = null;
        if (c != null) {
            file = new OCFile(c.getString(c
                    .getColumnIndex(ProviderTableMeta.FILE_PATH)));
            file.setFileId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
            file.setParentId(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.FILE_PARENT)));
            file.setMimetype(c.getString(c
                    .getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)));
            if (!file.isFolder()) {
                file.setStoragePath(c.getString(c
                        .getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)));
                if (file.getStoragePath() == null) {
                    // try to find existing file and bind it with current account; 
                    // with the current update of SynchronizeFolderOperation, this won't be 
                    // necessary anymore after a full synchronization of the account
                    File f = new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
                    if (f.exists()) {
                        file.setStoragePath(f.getAbsolutePath());
                        file.setLastSyncDateForData(f.lastModified());
                    }
                }
            }
            file.setFileLength(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH)));
            file.setCreationTimestamp(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.FILE_CREATION)));
            file.setModificationTimestamp(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.FILE_MODIFIED)));
            file.setModificationTimestampAtLastSyncForData(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA)));
            file.setLastSyncDateForProperties(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.FILE_LAST_SYNC_DATE)));
            file.setLastSyncDateForData(c.getLong(c.
                    getColumnIndex(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA)));
            file.setFavorite(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_KEEP_IN_SYNC)) == 1 ? true : false);
            file.setEtag(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG)));
            file.setShareViaLink(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_SHARED_VIA_LINK)) == 1 ? true : false);
            file.setShareWithSharee(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_SHARED_WITH_SHAREE)) == 1 ? true : false);
            file.setPublicLink(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PUBLIC_LINK)));
            file.setPermissions(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PERMISSIONS)));
            file.setRemoteId(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID)));
            file.setNeedsUpdateThumbnail(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_UPDATE_THUMBNAIL)) == 1 ? true : false);
            file.setDownloading(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_IS_DOWNLOADING)) == 1 ? true : false);
            file.setEtagInConflict(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG_IN_CONFLICT)));

        }
        return file;
    }

    // Methods for Shares
    public boolean saveShare(OCShare share) {
        boolean overriden = false;
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, share.getFileSource());
        cv.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, share.getItemSource());
        cv.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, share.getShareType().getValue());
        cv.put(ProviderTableMeta.OCSHARES_SHARE_WITH, share.getShareWith());
        cv.put(ProviderTableMeta.OCSHARES_PATH, share.getPath());
        cv.put(ProviderTableMeta.OCSHARES_PERMISSIONS, share.getPermissions());
        cv.put(ProviderTableMeta.OCSHARES_SHARED_DATE, share.getSharedDate());
        cv.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, share.getExpirationDate());
        cv.put(ProviderTableMeta.OCSHARES_TOKEN, share.getToken());
        cv.put(
                ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME,
                share.getSharedWithDisplayName()
        );
        cv.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, share.isFolder() ? 1 : 0);
        cv.put(ProviderTableMeta.OCSHARES_USER_ID, share.getUserId());
        cv.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getRemoteId());
        cv.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, mAccount.name);

        if (shareExistsForRemoteId(share.getRemoteId())) {// for renamed files; no more delete and create
            overriden = true;
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI_SHARE, cv,
                        ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                        new String[]{String.valueOf(share.getRemoteId())});
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI_SHARE,
                            cv, ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                            new String[]{String.valueOf(share.getRemoteId())});
                } catch (RemoteException e) {
                    Log_OC.e(TAG,
                            "Fail to insert insert file to database "
                                    + e.getMessage());
                }
            }
        } else {
            Uri result_uri = null;
            if (getContentResolver() != null) {
                result_uri = getContentResolver().insert(
                        ProviderTableMeta.CONTENT_URI_SHARE, cv);
            } else {
                try {
                    result_uri = getContentProviderClient().insert(
                            ProviderTableMeta.CONTENT_URI_SHARE, cv);
                } catch (RemoteException e) {
                    Log_OC.e(TAG,
                            "Fail to insert insert file to database "
                                    + e.getMessage());
                }
            }
            if (result_uri != null) {
                long new_id = Long.parseLong(result_uri.getPathSegments()
                        .get(1));
                share.setId(new_id);
            }
        }

        return overriden;
    }

    /**
     * Retrieves an stored {@link OCShare} given its id.
     *
     * @param id    Identifier.
     * @return      Stored {@link OCShare} given its id.
     */
    public OCShare getShareById(long id) {
        OCShare share = null;
        Cursor c = getShareCursorForValue(
                ProviderTableMeta._ID,
                String.valueOf(id)
        );
        if (c != null) {
            if (c.moveToFirst()) {
                share = createShareInstance(c);
            }
            c.close();
        }
        return share;
    }


    /**
     * Checks the existance of an stored {@link OCShare} matching the given remote id (not to be confused with
     * the local id) in the current account.
     *
     * @param remoteId      Remote of the share in the server.
     * @return              'True' if a matching {@link OCShare} is stored in the current account.
     */
    private boolean shareExistsForRemoteId(long remoteId) {
        return shareExistsForValue(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, String.valueOf(remoteId));
    }


    /**
     * Checks the existance of an stored {@link OCShare} in the current account
     * matching a given column and a value for that column
     *
     * @param key           Name of the column to match.
     * @param value         Value of the column to match.
     * @return              'True' if a matching {@link OCShare} is stored in the current account.
     */
    private boolean shareExistsForValue(String key, String value) {
        Cursor c = getShareCursorForValue(key, value);
        boolean retval = c.moveToFirst();
        c.close();
        return retval;
    }


    /**
     * Gets a {@link Cursor} for an stored {@link OCShare} in the current account
     * matching a given column and a value for that column
     *
     * @param key           Name of the column to match.
     * @param value         Value of the column to match.
     * @return              'True' if a matching {@link OCShare} is stored in the current account.
     */
    private Cursor getShareCursorForValue(String key, String value) {
        Cursor c;
        if (getContentResolver() != null) {
            c = getContentResolver()
                    .query(ProviderTableMeta.CONTENT_URI_SHARE,
                            null,
                            key + "=? AND "
                                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                            new String[]{value, mAccount.name},
                            null
                    );
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI_SHARE,
                        null,
                        key + "=? AND "
                                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                        new String[]{value, mAccount.name},
                        null
                );
            } catch (RemoteException e) {
                Log_OC.w(TAG,
                        "Could not get details, assuming share does not exist: "+ e.getMessage());
                c = null;
            }
        }
        return c;
    }



    /**
     * Get first share bound to a file with a known path and given {@link ShareType}.
     *
     * @param path          Path of the file.
     * @param type          Type of the share to get
     * @param shareWith     Target of the share. Ignored in type is {@link ShareType#PUBLIC_LINK}
     * @return              First {@OCShare} instance found in DB bound to the file in 'path'
     */
    public OCShare getFirstShareByPathAndType(String path, ShareType type, String shareWith) {
        Cursor c = null;
        if (shareWith == null) {
            shareWith = "";
        }

        String selection = ProviderTableMeta.OCSHARES_PATH + "=? AND "
                + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? AND "
                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?" ;
        if (!ShareType.PUBLIC_LINK.equals(type)) {
            selection += " AND " + ProviderTableMeta.OCSHARES_SHARE_WITH + "=?";
        }

        String [] selectionArgs;
        if (ShareType.PUBLIC_LINK.equals(type)) {
            selectionArgs = new String[]{
                    path,
                    Integer.toString(type.getValue()),
                    mAccount.name
            };
        } else {
            selectionArgs = new String[]{
                    path,
                    Integer.toString(type.getValue()),
                    mAccount.name,
                    shareWith
            };
        }

        if (getContentResolver() != null) {
            c = getContentResolver().query(
                    ProviderTableMeta.CONTENT_URI_SHARE,
                    null,
                    selection, selectionArgs,
                    null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI_SHARE,
                        null,
                        selection, selectionArgs,
                        null);

            } catch (RemoteException e) {
                Log_OC.e(TAG, "Could not get file details: " + e.getMessage());
                c = null;
            }
        }
        OCShare share = null;
        if (c.moveToFirst()) {
            share = createShareInstance(c);
        }
        c.close();
        return share;
    }

    private OCShare createShareInstance(Cursor c) {
        OCShare share = null;
        if (c != null) {
            share = new OCShare(c.getString(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_PATH)));
            share.setId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
            share.setFileSource(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_ITEM_SOURCE)));
            share.setShareType(ShareType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_TYPE))));
            share.setShareWith(c.getString(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH)));
            share.setPermissions(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_PERMISSIONS)));
            share.setSharedDate(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_SHARED_DATE)));
            share.setExpirationDate(c.getLong(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_EXPIRATION_DATE)));
            share.setToken(c.getString(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_TOKEN)));
            share.setSharedWithDisplayName(c.getString(c
                    .getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME)));
            share.setIsFolder(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.OCSHARES_IS_DIRECTORY)) == 1);
            share.setUserId(c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_USER_ID)));
            share.setIdRemoteShared(c.getLong(
                    c.getColumnIndex(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED)));
        }
        return share;
    }

    private void resetShareFlagsInAllFiles() {
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, false);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, false);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, "");
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{mAccount.name};

        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs);

        } else {
            try {
                getContentProviderClient().update(ProviderTableMeta.CONTENT_URI, cv, where,
                        whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInAllFiles" + e.getMessage());
            }
        }
    }

    private void resetShareFlagsInFolder(OCFile folder) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, false);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, false);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, "");
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                ProviderTableMeta.FILE_PARENT + "=?";
        String [] whereArgs = new String[] { mAccount.name , String.valueOf(folder.getFileId()) };

        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs);

        } else {
            try {
                getContentProviderClient().update(ProviderTableMeta.CONTENT_URI, cv, where,
                        whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInFolder " + e.getMessage());
            }
        }
    }

    private void resetShareFlagInAFile(String filePath){
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, false);
        cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, false);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, "");
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                ProviderTableMeta.FILE_PATH+ "=?";
        String [] whereArgs = new String[] { mAccount.name , filePath };

        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs);

        } else {
            try {
                getContentProviderClient().update(ProviderTableMeta.CONTENT_URI, cv, where,
                        whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in resetShareFlagsInFolder " + e.getMessage());
            }
        }
    }

    private void cleanShares() {
        String where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{mAccount.name};

        if (getContentResolver() != null) {
            getContentResolver().delete(ProviderTableMeta.CONTENT_URI_SHARE, where, whereArgs);

        } else {
            try {
                getContentProviderClient().delete(ProviderTableMeta.CONTENT_URI_SHARE, where,
                        whereArgs);
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in cleanShares" + e.getMessage());
            }
        }
    }

    public void saveShares(Collection<OCShare> shares) {
        cleanShares();
        if (shares != null) {
            ArrayList<ContentProviderOperation> operations =
                    new ArrayList<ContentProviderOperation>(shares.size());

            // prepare operations to insert or update files to save in the given folder
            for (OCShare share : shares) {
                ContentValues cv = new ContentValues();
                cv.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, share.getFileSource());
                cv.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, share.getItemSource());
                cv.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, share.getShareType().getValue());
                cv.put(ProviderTableMeta.OCSHARES_SHARE_WITH, share.getShareWith());
                cv.put(ProviderTableMeta.OCSHARES_PATH, share.getPath());
                cv.put(ProviderTableMeta.OCSHARES_PERMISSIONS, share.getPermissions());
                cv.put(ProviderTableMeta.OCSHARES_SHARED_DATE, share.getSharedDate());
                cv.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, share.getExpirationDate());
                cv.put(ProviderTableMeta.OCSHARES_TOKEN, share.getToken());
                cv.put(
                        ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME,
                        share.getSharedWithDisplayName()
                );
                cv.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, share.isFolder() ? 1 : 0);
                cv.put(ProviderTableMeta.OCSHARES_USER_ID, share.getUserId());
                cv.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getRemoteId());
                cv.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, mAccount.name);

                if (shareExistsForRemoteId(share.getRemoteId())) {
                    // updating an existing file
                    operations.add(
                            ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI_SHARE).
                                    withValues(cv).
                                    withSelection(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                                            new String[]{String.valueOf(share.getRemoteId())})
                                    .build());
                } else {
                    // adding a new file
                    operations.add(
                            ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI_SHARE).
                                    withValues(cv).
                                    build()
                    );
                }
            }

            // apply operations in batch
            if (operations.size() > 0) {
                @SuppressWarnings("unused")
                ContentProviderResult[] results = null;
                Log_OC.d(TAG, "Sending " + operations.size() +
                        " operations to FileContentProvider");
                try {
                    if (getContentResolver() != null) {
                        results = getContentResolver().applyBatch(MainApp.getAuthority(),
                                operations);
                    } else {
                        results = getContentProviderClient().applyBatch(operations);
                    }

                } catch (OperationApplicationException e) {
                    Log_OC.e(TAG, "Exception in batch of operations " + e.getMessage());

                } catch (RemoteException e) {
                    Log_OC.e(TAG, "Exception in batch of operations  " + e.getMessage());
                }
            }
        }

    }

    public void updateSharedFiles(Collection<OCFile> sharedFiles) {
        resetShareFlagsInAllFiles();

        if (sharedFiles != null) {
            ArrayList<ContentProviderOperation> operations =
                    new ArrayList<ContentProviderOperation>(sharedFiles.size());

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
                cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.getMimetype());
                cv.put(ProviderTableMeta.FILE_NAME, file.getFileName());
                cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
                cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
                if (!file.isFolder()) {
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
                }
                cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
                cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
                cv.put(
                        ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA,
                        file.getLastSyncDateForData()
                );
                cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.isFavorite() ? 1 : 0);
                cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
                cv.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, file.isSharedViaLink() ? 1 : 0);
                cv.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, file.isSharedWithSharee() ? 1 : 0);
                cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, file.getPublicLink());
                cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.getPermissions());
                cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.getRemoteId());
                cv.put(
                        ProviderTableMeta.FILE_UPDATE_THUMBNAIL,
                        file.needsUpdateThumbnail() ? 1 : 0
                );
                cv.put(
                        ProviderTableMeta.FILE_IS_DOWNLOADING,
                        file.isDownloading() ? 1 : 0
                );
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
                Log_OC.d(TAG, "Sending " + operations.size() +
                        " operations to FileContentProvider");
                try {
                    if (getContentResolver() != null) {
                        results = getContentResolver().applyBatch(MainApp.getAuthority(), operations);
                    } else {
                        results = getContentProviderClient().applyBatch(operations);
                    }

                } catch (OperationApplicationException e) {
                    Log_OC.e(TAG, "Exception in batch of operations " + e.getMessage());

                } catch (RemoteException e) {
                    Log_OC.e(TAG, "Exception in batch of operations  " + e.getMessage());
                }
            }
        }

    }

    public void removeShare(OCShare share) {
        Uri share_uri = ProviderTableMeta.CONTENT_URI_SHARE;
        String where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?" + " AND " +
                ProviderTableMeta._ID + "=?";
        String [] whereArgs = new String[]{mAccount.name, Long.toString(share.getId())};
        if (getContentProviderClient() != null) {
            try {
                getContentProviderClient().delete(share_uri, where, whereArgs);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            getContentResolver().delete(share_uri, where, whereArgs);
        }
    }

    public void saveSharesDB(ArrayList<OCShare> shares) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        // Reset flags & Remove shares for this files
        String filePath = "";
        for (OCShare share: shares) {
            if (filePath != share.getPath()){
                filePath = share.getPath();
                resetShareFlagInAFile(filePath);
                operations = prepareRemoveSharesInFile(filePath, operations);
            }
        }

       // Add operations to insert shares
       operations = prepareInsertShares(shares, operations);

        // apply operations in batch
        if (operations.size() > 0) {
            Log_OC.d(TAG, "Sending " + operations.size() + " operations to FileContentProvider");
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);

                } else {
                    getContentProviderClient().applyBatch(operations);
                }

            } catch (OperationApplicationException e) {
                Log_OC.e(TAG, "Exception in batch of operations " + e.getMessage());

            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in batch of operations  " + e.getMessage());
            }
        }

//        // TODO: review if it is needed
//        // Update shared files
//        ArrayList<OCFile> sharedFiles = new ArrayList<OCFile>();
//
//        for (OCShare share : shares) {
//            // Get the path
//            String path = share.getPath();
//            if (share.isFolder()) {
//                path = path + FileUtils.PATH_SEPARATOR;
//            }
//
//            // Update OCFile with data from share: ShareByLink, publicLink and
//            OCFile file = getFileByPath(path);
//            if (file != null) {
//                if (share.getShareType().equals(ShareType.PUBLIC_LINK)) {
//                    file.setShareViaLink(true);
//                    sharedFiles.add(file);
//                }
//            }
//        }
//
//        // TODO: Review
//        updateSharedFiles(sharedFiles);
    }

    public void removeSharesForFile(String remotePath) {
        resetShareFlagInAFile(remotePath);
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        operations = prepareRemoveSharesInFile(remotePath, operations);
        // apply operations in batch
        if (operations.size() > 0) {
            Log_OC.d(TAG, "Sending " + operations.size() + " operations to FileContentProvider");
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);

                } else {
                    getContentProviderClient().applyBatch(operations);
                }

            } catch (OperationApplicationException e) {
                Log_OC.e(TAG, "Exception in batch of operations " + e.getMessage());

            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in batch of operations  " + e.getMessage());
            }
        }
    }


    public void saveSharesInFolder(ArrayList<OCShare> shares, OCFile folder) {
        resetShareFlagsInFolder(folder);
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        operations = prepareRemoveSharesInFolder(folder, operations);

        if (shares != null) {
            // prepare operations to insert or update files to save in the given folder
            operations = prepareInsertShares(shares, operations);
        }

        // apply operations in batch
        if (operations.size() > 0) {
            Log_OC.d(TAG, "Sending " + operations.size() + " operations to FileContentProvider");
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(MainApp.getAuthority(), operations);

                } else {

                    getContentProviderClient().applyBatch(operations);
                }

            } catch (OperationApplicationException e) {
                Log_OC.e(TAG, "Exception in batch of operations " + e.getMessage());

            } catch (RemoteException e) {

            }
        }

    }

    /**
     * Prepare operations to insert or update files to save in the given folder
     * @param shares        List of shares to insert
     * @param operations    List of operations
     * @return
     */
    private ArrayList<ContentProviderOperation> prepareInsertShares(
            ArrayList<OCShare> shares, ArrayList<ContentProviderOperation> operations) {

        if (shares != null) {
            // prepare operations to insert or update files to save in the given folder
            for (OCShare share : shares) {
                ContentValues cv = new ContentValues();
                cv.put(ProviderTableMeta.OCSHARES_FILE_SOURCE, share.getFileSource());
                cv.put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, share.getItemSource());
                cv.put(ProviderTableMeta.OCSHARES_SHARE_TYPE, share.getShareType().getValue());
                cv.put(ProviderTableMeta.OCSHARES_SHARE_WITH, share.getShareWith());
                cv.put(ProviderTableMeta.OCSHARES_PATH, share.getPath());
                cv.put(ProviderTableMeta.OCSHARES_PERMISSIONS, share.getPermissions());
                cv.put(ProviderTableMeta.OCSHARES_SHARED_DATE, share.getSharedDate());
                cv.put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, share.getExpirationDate());
                cv.put(ProviderTableMeta.OCSHARES_TOKEN, share.getToken());
                cv.put(
                        ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME,
                        share.getSharedWithDisplayName()
                );
                cv.put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, share.isFolder() ? 1 : 0);
                cv.put(ProviderTableMeta.OCSHARES_USER_ID, share.getUserId());
                cv.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getRemoteId());
                cv.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, mAccount.name);

                // adding a new share resource
                operations.add(
                        ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI_SHARE).
                                withValues(cv).
                                build()
                );
            }
        }
        return operations;
    }

    private ArrayList<ContentProviderOperation> prepareRemoveSharesInFolder(
            OCFile folder, ArrayList<ContentProviderOperation> preparedOperations) {
        if (folder != null) {
            String where = ProviderTableMeta.OCSHARES_PATH + "=?" + " AND "
                    + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?";
            String [] whereArgs = new String[]{ "", mAccount.name };

            // TODO Enable when "On Device" is recovered ?
            Vector<OCFile> files = getFolderContent(folder /*, false*/);

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

        String where = ProviderTableMeta.OCSHARES_PATH + "=?" + " AND "
                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{filePath, mAccount.name};

        preparedOperations.add(
                ContentProviderOperation.newDelete(ProviderTableMeta.CONTENT_URI_SHARE).
                        withSelection(where, whereArgs).
                        build()
        );

        return preparedOperations;

    }

    public ArrayList<OCShare> getSharesWithForAFile(String filePath, String accountName){
        // Condition
        String where = ProviderTableMeta.OCSHARES_PATH + "=?" + " AND "
                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?"+ "AND"
                + " (" + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? OR "
                + ProviderTableMeta.OCSHARES_SHARE_TYPE +  "=? OR "
                + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? ) ";
        String [] whereArgs = new String[]{ filePath, accountName ,
                Integer.toString(ShareType.USER.getValue()),
                Integer.toString(ShareType.GROUP.getValue()),
                Integer.toString(ShareType.FEDERATED.getValue())};

        Cursor c = null;
        if (getContentResolver() != null) {
            c = getContentResolver().query(
                    ProviderTableMeta.CONTENT_URI_SHARE,
                    null, where, whereArgs, null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI_SHARE,
                        null, where, whereArgs, null);

            } catch (RemoteException e) {
                Log_OC.e(TAG, "Could not get list of shares with: " + e.getMessage());
                c = null;
            }
        }
        ArrayList<OCShare> shares = new ArrayList<OCShare>();
        OCShare share = null;
        if (c.moveToFirst()) {
            do {
                share = createShareInstance(c);
                shares.add(share);
                // }
            } while (c.moveToNext());
        }
        c.close();

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

        if (contentResolver != null) {
            if (mimetypeString.startsWith("image/")) {
                // Images
                contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media.DATA + "=?", new String[]{path});
            } else if (mimetypeString.startsWith("audio/")) {
                // Audio
                contentResolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Audio.Media.DATA + "=?", new String[]{path});
            } else if (mimetypeString.startsWith("video/")) {
                // Video
                contentResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Video.Media.DATA + "=?", new String[]{path});
            }
        } else {
            ContentProviderClient contentProviderClient = getContentProviderClient();
            try {
                if (mimetypeString.startsWith("image/")) {
                    // Images
                    contentProviderClient.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Images.Media.DATA + "=?", new String[]{path});
                } else if (mimetypeString.startsWith("audio/")) {
                    // Audio
                    contentProviderClient.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Audio.Media.DATA + "=?", new String[]{path});
                } else if (mimetypeString.startsWith("video/")) {
                    // Video
                    contentProviderClient.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Video.Media.DATA + "=?", new String[]{path});
                }
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception deleting media file in MediaStore " + e.getMessage());
            }
        }

    }

    public void saveConflict(OCFile file, String etagInConflict) {
        if (!file.isDown()) {
            etagInConflict = null;
        }
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, etagInConflict);
        int updated = 0;
        if (getContentResolver() != null) {
            updated = getContentResolver().update(
                    ProviderTableMeta.CONTENT_URI_FILE,
                    cv,
                    ProviderTableMeta._ID + "=?",
                    new String[] { String.valueOf(file.getFileId())}
            );
        } else {
            try {
                updated = getContentProviderClient().update(
                        ProviderTableMeta.CONTENT_URI_FILE,
                        cv,
                        ProviderTableMeta._ID + "=?",
                        new String[]{String.valueOf(file.getFileId())}
                );
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Failed saving conflict in database " + e.getMessage());
            }
        }

        Log_OC.d(TAG, "Number of files updated with CONFLICT: " + updated);

        if (updated > 0) {
            if (etagInConflict != null) {
                /// set conflict in all ancestor folders

                long parentId = file.getParentId();
                Set<String> ancestorIds = new HashSet<String>();
                while (parentId != FileDataStorageManager.ROOT_PARENT_ID) {
                    ancestorIds.add(Long.toString(parentId));
                    parentId = getFileById(parentId).getParentId();
                }

                if (ancestorIds.size() > 0) {
                    StringBuffer whereBuffer = new StringBuffer();
                    whereBuffer.append(ProviderTableMeta._ID).append(" IN (");
                    for (int i = 0; i < ancestorIds.size() - 1; i++) {
                        whereBuffer.append("?,");
                    }
                    whereBuffer.append("?");
                    whereBuffer.append(")");

                    if (getContentResolver() != null) {
                        updated = getContentResolver().update(
                                ProviderTableMeta.CONTENT_URI_FILE,
                                cv,
                                whereBuffer.toString(),
                                ancestorIds.toArray(new String[]{})
                        );
                    } else {
                        try {
                            updated = getContentProviderClient().update(
                                    ProviderTableMeta.CONTENT_URI_FILE,
                                    cv,
                                    whereBuffer.toString(),
                                    ancestorIds.toArray(new String[]{})
                            );
                        } catch (RemoteException e) {
                            Log_OC.e(TAG, "Failed saving conflict in database " + e.getMessage());
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

                    String whereForDescencentsInConflict =
                            ProviderTableMeta.FILE_ETAG_IN_CONFLICT + " IS NOT NULL AND " +
                                    ProviderTableMeta.FILE_CONTENT_TYPE + " != 'DIR' AND " +
                                    ProviderTableMeta.FILE_ACCOUNT_OWNER + " = ? AND " +
                                    ProviderTableMeta.FILE_PATH + " LIKE ?";
                    Cursor descendentsInConflict = null;
                    if (getContentResolver() != null) {
                        descendentsInConflict = getContentResolver().query(
                                ProviderTableMeta.CONTENT_URI_FILE,
                                new String[]{ProviderTableMeta._ID},
                                whereForDescencentsInConflict,
                                new String[]{mAccount.name, parentPath + "%"},
                                null
                        );
                    } else {
                        try {
                            descendentsInConflict = getContentProviderClient().query(
                                    ProviderTableMeta.CONTENT_URI_FILE,
                                    new String[]{ProviderTableMeta._ID},
                                    whereForDescencentsInConflict,
                                    new String[]{mAccount.name, parentPath + "%"},
                                    null
                            );
                        } catch (RemoteException e) {
                            Log_OC.e(TAG, "Failed querying for descendents in conflict " + e.getMessage());
                        }
                    }
                    if (descendentsInConflict == null || descendentsInConflict.getCount() == 0) {
                        Log_OC.d(TAG, "NO MORE conflicts in " + parentPath);
                        if (getContentResolver() != null) {
                            updated = getContentResolver().update(
                                    ProviderTableMeta.CONTENT_URI_FILE,
                                    cv,
                                    ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                                            ProviderTableMeta.FILE_PATH + "=?",
                                    new String[]{mAccount.name, parentPath}
                            );
                        } else {
                            try {
                                updated = getContentProviderClient().update(
                                        ProviderTableMeta.CONTENT_URI_FILE,
                                        cv,
                                        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
                                                ProviderTableMeta.FILE_PATH + "=?"
                                        , new String[]{mAccount.name, parentPath}
                                );
                            } catch (RemoteException e) {
                                Log_OC.e(TAG, "Failed saving conflict in database " + e.getMessage());
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

    public OCCapability saveCapabilities(OCCapability capability){

        // Prepare capabilities data
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME, mAccount.name);
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR, capability.getVersionMayor());
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_MINOR, capability.getVersionMinor());
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_MICRO, capability.getVersionMicro());
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_STRING, capability.getVersionString());
        cv.put(ProviderTableMeta.CAPABILITIES_VERSION_EDITION, capability.getVersionEdition());
        cv.put(ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL, capability.getCorePollinterval());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED, capability.getFilesSharingApiEnabled().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED,
                capability.getFilesSharingPublicEnabled().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED,
                capability.getFilesSharingPublicPasswordEnforced().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED,
                capability.getFilesSharingPublicExpireDateEnabled().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS,
                capability.getFilesSharingPublicExpireDateDays());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED,
                capability.getFilesSharingPublicExpireDateEnforced().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL,
                capability.getFilesSharingPublicSendMail().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD,
                capability.getFilesSharingPublicUpload().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL,
                capability.getFilesSharingUserSendMail().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_RESHARING, capability.getFilesSharingResharing().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING,
                capability.getFilesSharingFederationOutgoing().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING,
                capability.getFilesSharingFederationIncoming().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING, capability.getFilesBigFileChuncking().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_FILES_UNDELETE, capability.getFilesUndelete().getValue());
        cv.put(ProviderTableMeta.CAPABILITIES_FILES_VERSIONING, capability.getFilesVersioning().getValue());

        if (capabilityExists(mAccount.name)) {
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI_CAPABILITIES, cv,
                        ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=?",
                        new String[]{mAccount.name});
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI_CAPABILITIES,
                            cv, ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=?",
                            new String[]{mAccount.name});
                } catch (RemoteException e) {
                    Log_OC.e(TAG,
                            "Fail to insert insert file to database "
                                    + e.getMessage());
                }
            }
        } else {
            Uri result_uri = null;
            if (getContentResolver() != null) {
                result_uri = getContentResolver().insert(
                        ProviderTableMeta.CONTENT_URI_CAPABILITIES, cv);
            } else {
                try {
                    result_uri = getContentProviderClient().insert(
                            ProviderTableMeta.CONTENT_URI_CAPABILITIES, cv);
                } catch (RemoteException e) {
                    Log_OC.e(TAG,
                            "Fail to insert insert capability to database "
                                    + e.getMessage());
                }
            }
            if (result_uri != null) {
                long new_id = Long.parseLong(result_uri.getPathSegments()
                        .get(1));
                capability.setId(new_id);
                capability.setAccountName(mAccount.name);
            }
        }

        return capability;
    }

    private boolean capabilityExists(String accountName) {
        Cursor c = getCapabilityCursorForAccount(accountName);
        boolean exists = false;
        if (c != null) {
            exists = c.moveToFirst();
            c.close();
        }
        return exists;
    }

    private Cursor getCapabilityCursorForAccount(String accountName){
        Cursor c = null;
        if (getContentResolver() != null) {
            c = getContentResolver()
                    .query(ProviderTableMeta.CONTENT_URI_CAPABILITIES,
                            null,
                            ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=? ",
                            new String[]{accountName}, null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI_CAPABILITIES,
                        null,
                        ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + "=? ",
                        new String[]{accountName}, null);
            } catch (RemoteException e) {
                Log_OC.e(TAG,
                        "Couldn't determine capability existance, assuming non existance: "
                                + e.getMessage());
            }
        }

        return c;

    }
    public OCCapability getCapability(String accountName){
        OCCapability capability = null;
        Cursor c = getCapabilityCursorForAccount(accountName);

        if (c.moveToFirst()) {
            capability = createCapabilityInstance(c);
        } else {
            capability = new OCCapability();    // return default with all UNKNOWN
        }
        c.close();
        return capability;
    }

    private OCCapability createCapabilityInstance(Cursor c) {
        OCCapability capability = null;
        if (c != null) {
            capability = new OCCapability();
            capability.setId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
            capability.setAccountName(c.getString(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME)));
            capability.setVersionMayor(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_MAYOR)));
            capability.setVersionMinor(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_MINOR)));
            capability.setVersionMicro(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_MICRO)));
            capability.setVersionString(c.getString(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_STRING)));
            capability.setVersionEdition(c.getString(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_VERSION_EDITION)));
            capability.setCorePollinterval(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL)));
            capability.setFilesSharingApiEnabled(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED))));
            capability.setFilesSharingPublicEnabled(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED))));
            capability.setFilesSharingPublicPasswordEnforced(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED))));
            capability.setFilesSharingPublicExpireDateEnabled(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED))));
            capability.setFilesSharingPublicExpireDateDays(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)));
            capability.setFilesSharingPublicExpireDateEnforced(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED))));
            capability.setFilesSharingPublicSendMail(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL))));
            capability.setFilesSharingPublicUpload(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD))));
            capability.setFilesSharingUserSendMail(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL))));
            capability.setFilesSharingResharing(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_RESHARING))));
            capability.setFilesSharingFederationOutgoing(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING))));
            capability.setFilesSharingFederationIncoming(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING))));
            capability.setFilesBigFileChuncking(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING))));
            capability.setFilesUndelete(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_FILES_UNDELETE))));
            capability.setFilesVersioning(CapabilityBooleanType.fromValue(c.getInt(c
                    .getColumnIndex(ProviderTableMeta.CAPABILITIES_FILES_VERSIONING))));

        }
        return capability;
    }

}
