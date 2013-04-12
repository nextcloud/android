/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.utils.FileStorageUtils;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

public class FileDataStorageManager implements DataStorageManager {

    private ContentResolver mContentResolver;
    private ContentProviderClient mContentProvider;
    private Account mAccount;
    
    private static String TAG = "FileDataStorageManager";
    
    public FileDataStorageManager(Account account, ContentResolver cr) {
        mContentProvider = null;
        mContentResolver = cr;
        mAccount = account;
    }

    public FileDataStorageManager(Account account, ContentProviderClient cp) {
        mContentProvider = cp;
        mContentResolver = null;
        mAccount = account;
    }

    @Override
    public OCFile getFileByPath(String path) {
        Cursor c = getCursorForValue(ProviderTableMeta.FILE_PATH, path);
        OCFile file = null;
        if (c.moveToFirst()) {
            file = createFileInstance(c);
        }
        c.close();
        if (file == null && OCFile.PATH_SEPARATOR.equals(path)) {
            return createRootDir(); // root should always exist
        }
        return file;
    }

    
    private OCFile createRootDir() {
        OCFile file = new OCFile(OCFile.PATH_SEPARATOR);
        file.setMimetype("DIR");
        file.setParentId(DataStorageManager.ROOT_PARENT_ID);
        saveFile(file);
        return file;
    }

    @Override
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

    @Override
    public boolean fileExists(long id) {
        return fileExists(ProviderTableMeta._ID, String.valueOf(id));
    }

    @Override
    public boolean fileExists(String path) {
        return fileExists(ProviderTableMeta.FILE_PATH, path);
    }

    @Override
    public boolean saveFile(OCFile file) {
        boolean overriden = false;
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_MODIFIED, file.getModificationTimestamp());
        cv.put(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA, file.getModificationTimestampAtLastSyncForData());
        cv.put(ProviderTableMeta.FILE_CREATION, file.getCreationTimestamp());
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.getFileLength());
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.getMimetype());
        cv.put(ProviderTableMeta.FILE_NAME, file.getFileName());
        if (file.getParentId() != 0)
            cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
        cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
        if (!file.isDirectory())
            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
        cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
        cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.getLastSyncDateForData());
        cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.keepInSync() ? 1 : 0);

        boolean sameRemotePath = fileExists(file.getRemotePath());
        if (sameRemotePath ||
            fileExists(file.getFileId())        ) {           // for renamed files; no more delete and create
            
            if (sameRemotePath) {
                OCFile oldFile = getFileByPath(file.getRemotePath());
                file.setFileId(oldFile.getFileId());
            }

            overriden = true;
            if (getContentResolver() != null) {
                getContentResolver().update(ProviderTableMeta.CONTENT_URI, cv,
                        ProviderTableMeta._ID + "=?",
                        new String[] { String.valueOf(file.getFileId()) });
            } else {
                try {
                    getContentProvider().update(ProviderTableMeta.CONTENT_URI,
                            cv, ProviderTableMeta._ID + "=?",
                            new String[] { String.valueOf(file.getFileId()) });
                } catch (RemoteException e) {
                    Log.e(TAG,
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
                    result_uri = getContentProvider().insert(
                            ProviderTableMeta.CONTENT_URI_FILE, cv);
                } catch (RemoteException e) {
                    Log.e(TAG,
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

        if (file.isDirectory() && file.needsUpdatingWhileSaving())
            for (OCFile f : getDirectoryContent(file))
                saveFile(f);

        return overriden;
    }


    @Override
    public void saveFiles(List<OCFile> files) {
        
        Iterator<OCFile> filesIt = files.iterator();
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(files.size());
        OCFile file = null;

        // prepare operations to perform
        while (filesIt.hasNext()) {
            file = filesIt.next();
            ContentValues cv = new ContentValues();
            cv.put(ProviderTableMeta.FILE_MODIFIED, file.getModificationTimestamp());
            cv.put(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA, file.getModificationTimestampAtLastSyncForData());
            cv.put(ProviderTableMeta.FILE_CREATION, file.getCreationTimestamp());
            cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.getFileLength());
            cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, file.getMimetype());
            cv.put(ProviderTableMeta.FILE_NAME, file.getFileName());
            if (file.getParentId() != 0)
                cv.put(ProviderTableMeta.FILE_PARENT, file.getParentId());
            cv.put(ProviderTableMeta.FILE_PATH, file.getRemotePath());
            if (!file.isDirectory())
                cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());
            cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
            cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE, file.getLastSyncDateForProperties());
            cv.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, file.getLastSyncDateForData());
            cv.put(ProviderTableMeta.FILE_KEEP_IN_SYNC, file.keepInSync() ? 1 : 0);

            if (fileExists(file.getRemotePath())) {
                OCFile oldFile = getFileByPath(file.getRemotePath());
                file.setFileId(oldFile.getFileId());
                operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                        withValues(cv).
                        withSelection(  ProviderTableMeta._ID + "=?", 
                                        new String[] { String.valueOf(file.getFileId()) })
                        .build());
                
            } else if (fileExists(file.getFileId())) {
                    OCFile oldFile = getFileById(file.getFileId());
                    if (file.getStoragePath() == null && oldFile.getStoragePath() != null)
                        file.setStoragePath(oldFile.getStoragePath());
                    if (!file.isDirectory());
                        cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getStoragePath());

                    operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                            withValues(cv).
                            withSelection(  ProviderTableMeta._ID + "=?", 
                                            new String[] { String.valueOf(file.getFileId()) })
                            .build());
                    
            } else {
                operations.add(ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI).withValues(cv).build());
            }
        }
        
        // apply operations in batch
        ContentProviderResult[] results = null;
        try {
            if (getContentResolver() != null) {
                results = getContentResolver().applyBatch(ProviderMeta.AUTHORITY_FILES, operations);
            
            } else {
                results = getContentProvider().applyBatch(operations);
            }
            
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Fail to update/insert list of files to database " + e.getMessage());
            
        } catch (RemoteException e) {
            Log.e(TAG, "Fail to update/insert list of files to database " + e.getMessage());
        }
        
        // update new id in file objects for insertions
        if (results != null) {
            long newId;
            for (int i=0; i<results.length; i++) {
                if (results[i].uri != null) {
                    newId = Long.parseLong(results[i].uri.getPathSegments().get(1));
                    files.get(i).setFileId(newId);
                    //Log.v(TAG, "Found and added id in insertion for " + files.get(i).getRemotePath());
                }
            }
        }

        for (OCFile aFile : files) {
            if (aFile.isDirectory() && aFile.needsUpdatingWhileSaving())
                saveFiles(getDirectoryContent(aFile));
        }

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

    public void setContentProvider(ContentProviderClient cp) {
        mContentProvider = cp;
    }

    public ContentProviderClient getContentProvider() {
        return mContentProvider;
    }

    @Override
    public Vector<OCFile> getDirectoryContent(OCFile f) {
        Vector<OCFile> ret = new Vector<OCFile>();
        if (f != null && f.isDirectory() && f.getFileId() != -1) {

            Uri req_uri = Uri.withAppendedPath(
                    ProviderTableMeta.CONTENT_URI_DIR,
                    String.valueOf(f.getFileId()));
            Cursor c = null;

            if (getContentProvider() != null) {
                try {
                    c = getContentProvider().query(req_uri, null,
                            ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                            new String[] { mAccount.name }, null);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage());
                    return ret;
                }
            } else {
                c = getContentResolver().query(req_uri, null,
                        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                        new String[] { mAccount.name }, null);
            }

            if (c.moveToFirst()) {
                do {
                    OCFile child = createFileInstance(c);
                    ret.add(child);
                } while (c.moveToNext());
            }

            c.close();
            
            Collections.sort(ret);
            
        }
        return ret;
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
                c = getContentProvider().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        cmp_key + "=? AND "
                                + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                        new String[] { value, mAccount.name }, null);
            } catch (RemoteException e) {
                Log.e(TAG,
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
                c = getContentProvider().query(
                        ProviderTableMeta.CONTENT_URI,
                        null,
                        key + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER
                                + "=?", new String[] { value, mAccount.name },
                        null);
            } catch (RemoteException e) {
                Log.e(TAG, "Could not get file details: " + e.getMessage());
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
            if (!file.isDirectory()) {
                file.setStoragePath(c.getString(c
                        .getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)));
                if (file.getStoragePath() == null) {
                    // try to find existing file and bind it with current account; - with the current update of SynchronizeFolderOperation, this won't be necessary anymore after a full synchronization of the account
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
        }
        return file;
    }
    
    @Override
    public void removeFile(OCFile file, boolean removeLocalCopy) {
        Uri file_uri = Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, ""+file.getFileId());
        if (getContentProvider() != null) {
            try {
                getContentProvider().delete(file_uri,
                                            ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
                                            new String[]{mAccount.name});
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            getContentResolver().delete(file_uri,
                                        ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
                                        new String[]{mAccount.name});
        }
        if (file.isDown() && removeLocalCopy) {
            new File(file.getStoragePath()).delete();
        }
        if (file.isDirectory() && removeLocalCopy) {
            File f = new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
            if (f.exists() && f.isDirectory() && (f.list() == null || f.list().length == 0)) {
                f.delete();
            }
        }
    }

    @Override
    public void removeDirectory(OCFile dir, boolean removeDBData, boolean removeLocalContent) {
        // TODO consider possible failures
        if (dir != null && dir.isDirectory() && dir.getFileId() != -1) {
            Vector<OCFile> children = getDirectoryContent(dir);
            if (children.size() > 0) {
                OCFile child = null;
                for (int i=0; i<children.size(); i++) {
                    child = children.get(i);
                    if (child.isDirectory()) {
                        removeDirectory(child, removeDBData, removeLocalContent);
                    } else {
                        if (removeDBData) {
                            removeFile(child, removeLocalContent);
                        } else if (removeLocalContent) {
                            if (child.isDown()) {
                                new File(child.getStoragePath()).delete();
                            }
                        }
                    }
                }
            }
            if (removeDBData) {
                removeFile(dir, true);
            }
        }
    }
    
    
    /**
     * Updates database for a folder that was moved to a different location.
     * 
     * TODO explore better (faster) implementations
     * TODO throw exceptions up !
     */
    @Override
    public void moveDirectory(OCFile dir, String newPath) {
        // TODO check newPath
        
        if (dir != null && dir.isDirectory() && dir.fileExists() && !dir.getFileName().equals(OCFile.PATH_SEPARATOR)) {
            /// 1. get all the descendants of 'dir' in a single QUERY (including 'dir')
            Cursor c = null;
            if (getContentProvider() != null) {
                try {
                    c = getContentProvider().query(ProviderTableMeta.CONTENT_URI, 
                                                    null,
                                                    ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " + ProviderTableMeta.FILE_PATH + " LIKE ?",
                                                    new String[] { mAccount.name, dir.getRemotePath() + "%" }, null);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage());
                }
            } else {
                c = getContentResolver().query(ProviderTableMeta.CONTENT_URI, 
                                                null,
                                                ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " + ProviderTableMeta.FILE_PATH + " LIKE ?",
                                                new String[] { mAccount.name, dir.getRemotePath() + "%" }, null);
            }

            /// 2. prepare a batch of update operations to change all the descendants
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(c.getCount());
            int lengthOfOldPath = dir.getRemotePath().length();
            String defaultSavePath = FileStorageUtils.getSavePath(mAccount.name);
            int lengthOfOldStoragePath = defaultSavePath.length() + lengthOfOldPath;
            if (c.moveToFirst()) {
                do {
                    ContentValues cv = new ContentValues(); // don't take the constructor out of the loop and clear the object
                    OCFile child = createFileInstance(c);
                    cv.put(ProviderTableMeta.FILE_PATH, newPath + child.getRemotePath().substring(lengthOfOldPath));
                    if (child.getStoragePath() != null && child.getStoragePath().startsWith(defaultSavePath)) {
                        cv.put(ProviderTableMeta.FILE_STORAGE_PATH, defaultSavePath + newPath + child.getStoragePath().substring(lengthOfOldStoragePath));
                    }
                    operations.add(ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI).
                                                                        withValues(cv).
                                                                        withSelection(  ProviderTableMeta._ID + "=?", 
                                                                                new String[] { String.valueOf(child.getFileId()) })
                                                                                .build());
                } while (c.moveToNext());
            }
            c.close();
            
            /// 3. apply updates in batch
            try {
                if (getContentResolver() != null) {
                    getContentResolver().applyBatch(ProviderMeta.AUTHORITY_FILES, operations);
                
                } else {
                    getContentProvider().applyBatch(operations);
                }
                
            } catch (OperationApplicationException e) {
                Log.e(TAG, "Fail to update descendants of " + dir.getFileId() + " in database", e);
                
            } catch (RemoteException e) {
                Log.e(TAG, "Fail to update desendants of " + dir.getFileId() + " in database", e);
            }
            
        }
    }

    @Override
    public Vector<OCFile> getDirectoryImages(OCFile directory) {
        Vector<OCFile> ret = new Vector<OCFile>(); 
        if (directory != null) {
            // TODO better implementation, filtering in the access to database (if possible) instead of here 
            Vector<OCFile> tmp = getDirectoryContent(directory);
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

}
