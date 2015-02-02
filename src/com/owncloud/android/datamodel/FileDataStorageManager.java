/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.owncloud.android.MainApp;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.utils.FileStorageUtils;

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

    public void setContentResolver(ContentResolver cr) {
        mContentResolver = cr;
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    public void setContentProviderClient(ContentProviderClient cp) {
        mContentProviderClient = cp;
    }

    public ContentProviderClient getContentProviderClient() {
        return mContentProviderClient;
    }
    

    public OCFile getFileByPath(String path) {
        Cursor c = getCursorForValue(ProviderTableMeta.FILE_PATH, path);
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
        Cursor c = getCursorForValue(ProviderTableMeta._ID, String.valueOf(id));
        OCFile file = null;
        if (c.moveToFirst()) {
            file = createFileInstance(c);
        }
        c.close();
        return file;
    }

    public OCFile getFileByLocalPath(String path) {
        Cursor c = getCursorForValue(ProviderTableMeta.FILE_STORAGE_PATH, path);
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

    
    public Vector<OCFile> getFolderContent(OCFile f) {
        if (f != null && f.isFolder() && f.getFileId() != -1) {
            return getFolderContent(f.getFileId());

        } else {
            return new Vector<OCFile>();
        }
    }
    
    
    public Vector<OCFile> getFolderImages(OCFile folder) {
        Vector<OCFile> ret = new Vector<OCFile>(); 
        if (folder != null) {
            // TODO better implementation, filtering in the access to database instead of here 
            Vector<OCFile> tmp = getFolderContent(folder);
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
        //if (file.getParentId() != DataStorageManager.ROOT_PARENT_ID)
            cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
        cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
        if (!file.isFolder())
            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.keepInSync() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
        cv.put(ProviderTableMeta.FILE_SHARE_BY_LINK, file.isShareByLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, file.getPublicLink());
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.getPermissions());
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.getRemoteId());
        cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.needsUpdateThumbnail());
        cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading());
        
        boolean sameRemotePath = fileExists(file.getRemotePath());
        if (sameRemotePath ||
                fileExists(file.getFileId())        ) {  // for renamed files

            OCFile oldFile = null;
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
                        new String[] { String.valueOf(file.getFileId()) });
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI,
                            cv, ProviderTableMeta._ID + "=?",
                            new String[] { String.valueOf(file.getFileId()) });
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

//        if (file.isFolder()) {
//            updateFolderSize(file.getFileId());
//        } else {
//            updateFolderSize(file.getParentId());
//        }
        
        return overriden;
    }


    /**
     * Inserts or updates the list of files contained in a given folder.
     * 
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
            cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.keepInSync() ? 1 : 0);
            cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
            cv.put(ProviderTableMeta.FILE_SHARE_BY_LINK, file.isShareByLink() ? 1 : 0);
            cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, file.getPublicLink());
            cv.put(ProviderTableMeta.FILE_PERMISSIONS, file.getPermissions());
            cv.put(ProviderTableMeta.FILE_REMOTE_ID, file.getRemoteId());
            cv.put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.needsUpdateThumbnail());
            cv.put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading());

            boolean existsByPath = fileExists(file.getRemotePath());
            if (existsByPath || fileExists(file.getFileId())) {
                // updating an existing file
                operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                        withValues(cv).
                        withSelection(  ProviderTableMeta._ID + "=?", 
                                new String[] { String.valueOf(file.getFileId()) })
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
                //Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, "" + file.getFileId());
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
        cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, folder.keepInSync() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_ETAG, folder.getEtag());
        cv.put(ProviderTableMeta.FILE_SHARE_BY_LINK, folder.isShareByLink() ? 1 : 0);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, folder.getPublicLink());
        cv.put(ProviderTableMeta.FILE_PERMISSIONS, folder.getPermissions());
        cv.put(ProviderTableMeta.FILE_REMOTE_ID, folder.getRemoteId());
        
        operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                withValues(cv).
                withSelection(  ProviderTableMeta._ID + "=?", 
                        new String[] { String.valueOf(folder.getFileId()) })
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
            for (int i=0; i<results.length; i++) {
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
        
        //updateFolderSize(folder.getFileId());
        
    }


//    /**
//     * 
//     * @param id
//     */
//    private void updateFolderSize(long id) {
//        if (id > FileDataStorageManager.ROOT_PARENT_ID) {
//            Log_OC.d(TAG, "Updating size of " + id);
//            if (getContentResolver() != null) {
//                getContentResolver().update(ProviderTableMeta.CONTENT_URI_DIR, 
//                        new ContentValues(),    
                            // won't be used, but cannot be null; crashes in KLP
//                        ProviderTableMeta._ID + "=?",
//                        new String[] { String.valueOf(id) });
//            } else {
//                try {
//                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI_DIR, 
//                            new ContentValues(),    
                                // won't be used, but cannot be null; crashes in KLP
//                            ProviderTableMeta._ID + "=?",
//                            new String[] { String.valueOf(id) });
//                    
//                } catch (RemoteException e) {
//                    Log_OC.e(
//    TAG, "Exception in update of folder size through compatibility patch " + e.getMessage());
//                }
//            }
//        } else {
//            Log_OC.e(TAG,  "not updating size for folder " + id);
//        }
//    }
    

    public boolean removeFile(OCFile file, boolean removeDBData, boolean removeLocalCopy) {
        boolean success = true;
        if (file != null) {
            if (file.isFolder()) {
                success = removeFolder(file, removeDBData, removeLocalCopy);
                
            } else {
                if (removeDBData) {
                    Uri file_uri = ContentUris.withAppendedId(
                        ProviderTableMeta.CONTENT_URI_FILE, 
                        file.getFileId()
                    );
                    String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?" + " AND " + 
                            ProviderTableMeta.FILE_PATH + "=?";
                    String [] whereArgs = new String[]{mAccount.name, file.getRemotePath()};
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
                    }
                }
            }
        }
        return success;
    }
    

    public boolean removeFolder(OCFile folder, boolean removeDBData, boolean removeLocalContent) {
        boolean success = true;
        if (folder != null && folder.isFolder()) {
            if (removeDBData &&  folder.getFileId() != -1) {
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
            Vector<OCFile> files = getFolderContent(folder.getFileId());
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
                throw new IllegalStateException("Parent folder of the target path does not exist!!");
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
                        new String[] { 
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
                    new String[] { 
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
                                    new String[] { String.valueOf(child.getFileId()) }
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
                Log_OC.e(TAG, "Fail to update " + file.getFileId() + " and descendants in database", e);
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
    
    
    private Vector<OCFile> getFolderContent(long parentId) {

        Vector<OCFile> ret = new Vector<OCFile>();

        Uri req_uri = Uri.withAppendedPath(
                ProviderTableMeta.CONTENT_URI_DIR,
                String.valueOf(parentId));
        Cursor c = null;

        if (getContentProviderClient() != null) {
            try {
                c = getContentProviderClient().query(req_uri, null, 
                        ProviderTableMeta.FILE_PARENT + "=?" ,
                        new String[] { String.valueOf(parentId)}, null);
            } catch (RemoteException e) {
                Log_OC.e(TAG, e.getMessage());
                return ret;
            }
        } else {
            c = getContentResolver().query(req_uri, null, 
                    ProviderTableMeta.FILE_PARENT + "=?" ,
                    new String[] { String.valueOf(parentId)}, null);
        }

        if (c.moveToFirst()) {
            do {
                OCFile child = createFileInstance(c);
                ret.add(child);
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
                                    new String[] { value, mAccount.name }, null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        cmp_key + "=? AND "
                                + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                new String[] { value, mAccount.name }, null);
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

    private Cursor getCursorForValue(String key, String value) {
        Cursor c = null;
        if (getContentResolver() != null) {
            c = getContentResolver()
                    .query(ProviderTableMeta.CONTENT_URI,
                            null,
                            key + "=? AND "
                                    + ProviderTableMeta.FILE_ACCOUNT_OWNER
                                    + "=?",
                                    new String[] { value, mAccount.name }, null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        key + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER
                        + "=?", new String[] { value, mAccount.name },
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
            file.setKeepInSync(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_KEEP_IN_SYNC)) == 1 ? true : false);
            file.setEtag(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG)));
            file.setShareByLink(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_SHARE_BY_LINK)) == 1 ? true : false);
            file.setPublicLink(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PUBLIC_LINK)));
            file.setPermissions(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PERMISSIONS)));
            file.setRemoteId(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID)));
            file.setNeedsUpdateThumbnail(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_UPDATE_THUMBNAIL)) == 1 ? true : false);
            file.setDownloading(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_IS_DOWNLOADING)) == 1 ? true : false);
                    
        }
        return file;
    }
    
    /**
     * Returns if the file/folder is shared by link or not
     * @param path  Path of the file/folder
     * @return
     */
    public boolean isShareByLink(String path) {
        Cursor c = getCursorForValue(ProviderTableMeta.FILE_STORAGE_PATH, path);
        OCFile file = null;
        if (c.moveToFirst()) {
            file = createFileInstance(c);
        }
        c.close();
        return file.isShareByLink();
    }
    
    /**
     * Returns the public link of the file/folder
     * @param path  Path of the file/folder
     * @return
     */
    public String getPublicLink(String path) {
        Cursor c = getCursorForValue(ProviderTableMeta.FILE_STORAGE_PATH, path);
        OCFile file = null;
        if (c.moveToFirst()) {
            file = createFileInstance(c);
        }
        c.close();
        return file.getPublicLink();
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
        cv.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getIdRemoteShared());
        cv.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, mAccount.name);
        
        if (shareExists(share.getIdRemoteShared())) {   // for renamed files

            overriden = true;
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI_SHARE, cv,
                        ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                        new String[] { String.valueOf(share.getIdRemoteShared()) });
            } else {
                try {
                    getContentProviderClient().update(ProviderTableMeta.CONTENT_URI_SHARE,
                            cv, ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?",
                            new String[] { String.valueOf(share.getIdRemoteShared()) });
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


    public OCShare getFirstShareByPathAndType(String path, ShareType type) {
        Cursor c = null;
        if (getContentResolver() != null) {
            c = getContentResolver().query(
                    ProviderTableMeta.CONTENT_URI_SHARE,
                    null,
                    ProviderTableMeta.OCSHARES_PATH + "=? AND "
                            + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? AND "
                            + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                    new String[] { path, Integer.toString(type.getValue()), mAccount.name },
                    null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI_SHARE,
                        null,
                        ProviderTableMeta.OCSHARES_PATH + "=? AND "
                                + ProviderTableMeta.OCSHARES_SHARE_TYPE + "=? AND "
                                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                        new String[] { path, Integer.toString(type.getValue()), mAccount.name }, 
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
                    c.getColumnIndex(ProviderTableMeta.OCSHARES_IS_DIRECTORY)) == 1 ? true : false);
            share.setUserId(c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_USER_ID)));
            share.setIdRemoteShared(
                c.getLong(c.getColumnIndex(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED))
            );
                    
        }
        return share;
    }

    private boolean shareExists(String cmp_key, String value) {
        Cursor c;
        if (getContentResolver() != null) {
            c = getContentResolver()
                    .query(ProviderTableMeta.CONTENT_URI_SHARE,
                            null,
                            cmp_key + "=? AND "
                                    + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER
                                    + "=?",
                                    new String[] { value, mAccount.name }, null);
        } else {
            try {
                c = getContentProviderClient().query(
                        ProviderTableMeta.CONTENT_URI_SHARE,
                        null,
                        cmp_key + "=? AND "
                                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                                new String[] { value, mAccount.name }, null);
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
    
    private boolean shareExists(long remoteId) {
        return shareExists(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, String.valueOf(remoteId));
    }

    private void cleanSharedFiles() {
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_SHARE_BY_LINK, false);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, "");
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
        String [] whereArgs = new String[]{mAccount.name};
        
        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs);

        } else {
            try {
                getContentProviderClient().update(
                        ProviderTableMeta.CONTENT_URI, cv, where, whereArgs
                );
                
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in cleanSharedFiles" + e.getMessage());
            }
        }
    }

    private void cleanSharedFilesInFolder(OCFile folder) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_SHARE_BY_LINK, false);
        cv.put(ProviderTableMeta.FILE_PUBLIC_LINK, "");
        String where = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " + 
                ProviderTableMeta.FILE_PARENT + "=?";
        String [] whereArgs = new String[] { mAccount.name , String.valueOf(folder.getFileId()) };
        
        if (getContentResolver() != null) {
            getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv, where, whereArgs);

        } else {
            try {
                getContentProviderClient().update(
                        ProviderTableMeta.CONTENT_URI, cv, where, whereArgs
                );
                
            } catch (RemoteException e) {
                Log_OC.e(TAG, "Exception in cleanSharedFilesInFolder " + e.getMessage());
            }
        }
    }

    private void cleanShares() {
        String where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?";
        String [] whereArgs = new String[]{mAccount.name};
        
        if (getContentResolver() != null) {
            getContentResolver().delete(ProviderTableMeta.CONTENT_URI_SHARE, where, whereArgs);

        } else {
            try {
                getContentProviderClient().delete(
                        ProviderTableMeta.CONTENT_URI_SHARE, where, whereArgs
                );
                
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
                cv.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getIdRemoteShared());
                cv.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, mAccount.name);

                if (shareExists(share.getIdRemoteShared())) {
                    // updating an existing file
                    operations.add(
                            ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI_SHARE).
                            withValues(cv).
                            withSelection(
                                    ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?", 
                                    new String[] { String.valueOf(share.getIdRemoteShared()) }
                            ).
                            build()
                    );

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
                        results = getContentResolver().applyBatch(
                                MainApp.getAuthority(), operations
                        );
    
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
        cleanSharedFiles();
        
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
                cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.keepInSync() ? 1 : 0);
                cv.put(ProviderTableMeta.FILE_ETAG, file.getEtag());
                cv.put(ProviderTableMeta.FILE_SHARE_BY_LINK, file.isShareByLink() ? 1 : 0);
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

                boolean existsByPath = fileExists(file.getRemotePath());
                if (existsByPath || fileExists(file.getFileId())) {
                    // updating an existing file
                    operations.add(
                            ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                            withValues(cv).
                            withSelection(
                                    ProviderTableMeta._ID + "=?", 
                                    new String[] { String.valueOf(file.getFileId()) }
                            ).build()
                    );

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
                        results = getContentResolver().applyBatch(
                                MainApp.getAuthority(), operations
                        );
    
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
    
    public void removeShare(OCShare share){
        Uri share_uri = ProviderTableMeta.CONTENT_URI_SHARE;
        String where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?" + " AND " + 
                ProviderTableMeta.FILE_PATH + "=?";
        String [] whereArgs = new String[]{mAccount.name, share.getPath()};
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
        saveShares(shares);

        ArrayList<OCFile> sharedFiles = new ArrayList<OCFile>();

        for (OCShare share : shares) {
            // Get the path
            String path = share.getPath();
            if (share.isFolder()) {
                path = path + FileUtils.PATH_SEPARATOR;
            }           

            // Update OCFile with data from share: ShareByLink  and publicLink
            OCFile file = getFileByPath(path);
            if (file != null) {
                if (share.getShareType().equals(ShareType.PUBLIC_LINK)) {
                    file.setShareByLink(true);
                    sharedFiles.add(file);
                }
            } 
        }
        
        updateSharedFiles(sharedFiles);
    }

    
    public void saveSharesInFolder(ArrayList<OCShare> shares, OCFile folder) {
        cleanSharedFilesInFolder(folder);
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        operations = prepareRemoveSharesInFolder(folder, operations);
        
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
                cv.put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.getIdRemoteShared());
                cv.put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, mAccount.name);

                /*
                if (shareExists(share.getIdRemoteShared())) {
                    // updating an existing share resource
                    operations.add(
                            ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI_SHARE).
                            withValues(cv).
                            withSelection(  ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + "=?", 
                                    new String[] { String.valueOf(share.getIdRemoteShared()) })
                                    .build());

                } else {
                */
                // adding a new share resource
                operations.add(
                        ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI_SHARE).
                        withValues(cv).
                        build()
                );
                //}
            }
        }
            
        // apply operations in batch
        if (operations.size() > 0) {
            @SuppressWarnings("unused")
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
        }
        //}
        
    }

    private ArrayList<ContentProviderOperation> prepareRemoveSharesInFolder(
            OCFile folder, ArrayList<ContentProviderOperation> preparedOperations
            ) {
        if (folder != null) {
            String where = ProviderTableMeta.OCSHARES_PATH + "=?" + " AND " 
                    + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?";
            String [] whereArgs = new String[]{ "", mAccount.name };
            
            Vector<OCFile> files = getFolderContent(folder);
            
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
        
        /*
        if (operations.size() > 0) {
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
        */
            
            /*
            if (getContentResolver() != null) {
                
                getContentResolver().delete(ProviderTableMeta.CONTENT_URI_SHARE, 
                                            where,
                                            whereArgs);
            } else {
                try {
                    getContentProviderClient().delete(  ProviderTableMeta.CONTENT_URI_SHARE, 
                                                        where,
                                                        whereArgs);

                } catch (RemoteException e) {
                    Log_OC.e(TAG, "Exception deleting shares in a folder " + e.getMessage());
                }
            }
            */
        //}
    }

    public void triggerMediaScan(String path) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(path)));
        MainApp.getAppContext().sendBroadcast(intent);
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

}
