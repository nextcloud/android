/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   @author David A. Velasco
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.Observable;

/**
 * Database helper for storing list of files to be uploaded, including status
 * information for each file.
 */
public class UploadsStorageManager extends Observable {

    private ContentResolver mContentResolver;

    static private final String TAG = UploadsStorageManager.class.getSimpleName();

    public enum UploadStatus {
        /**
         * Upload scheduled.
         */
        UPLOAD_LATER(0),
        /**
         * Last upload failed. Will retry.
         */
        UPLOAD_FAILED_RETRY(1),
        /**
         * Upload currently in progress.
         */
        UPLOAD_IN_PROGRESS(2),
        /**
         * Upload paused. Has to be manually resumed by user.
         */
        UPLOAD_PAUSED(3),
        /**
         * Upload was successful.
         */
        UPLOAD_SUCCEEDED(4),
        /**
         * Upload failed with some severe reason. Do not retry.
         */
        UPLOAD_FAILED_GIVE_UP(5),
        /**
         * User has cancelled upload. Do not retry.
         */
        UPLOAD_CANCELLED(6);
        private final int value;

        UploadStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static  UploadStatus fromValue(int value)
        {
            switch (value)
            {
                case 0:
                    return UPLOAD_LATER;
                case 1:
                    return UPLOAD_FAILED_RETRY;
                case 2:
                    return UPLOAD_IN_PROGRESS;
                case 3:
                    return UPLOAD_PAUSED;
                case 4:
                    return UPLOAD_SUCCEEDED;
                case 5:
                    return UPLOAD_FAILED_GIVE_UP;
                case 6:
                    return UPLOAD_CANCELLED;
            }
            return null;
        }

    };

    public UploadsStorageManager(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an upload object in DB.
     * 
     * @param ocUpload
     * @return true on success.
     */
    public boolean storeUpload(OCUpload ocUpload) {
        Log_OC.e(TAG, "Inserting " + ocUpload.getLocalPath() + " with status=" + ocUpload.getUploadStatus());
        
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_PATH, ocUpload.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.getUploadStatus().value);
        cv.put(ProviderTableMeta.UPLOADS_FILE_ID, ocUpload.getOCFile().getFileId());

        Uri result = getDB().insert(ProviderTableMeta.CONTENT_URI_UPLOADS, cv);
        
        Log_OC.d(TAG, "storeUpload returns with: " + result + " for file: " + ocUpload.getLocalPath());
        if (result == null) {
            Log_OC.e(TAG, "Failed to insert item " + ocUpload.getLocalPath() + " into upload db.");
            return false;
        } else {
            notifyObserversNow();
            return true;
        }
    }

    /**
     * Update an upload object in DB.
     *
     * @param ocUpload
     * @return num of updated uploads.
     */
    public int updateUpload(OCUpload ocUpload) {
        Log_OC.e(TAG, "Updating " + ocUpload.getLocalPath() + " with status=" + ocUpload.getUploadStatus());

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_PATH, ocUpload.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.getUploadStatus().value);

        int result = getDB().update(ProviderTableMeta.CONTENT_URI_UPLOADS,
                cv,
                ProviderTableMeta.UPLOADS_FILE_ID + "=?",
                new String[]{ String.valueOf(ocUpload.getOCFile().getFileId()) }
        );

        Log_OC.d(TAG, "updateUpload returns with: " + result + " for file: " + ocUpload.getLocalPath());
        if (result != 1) {
            Log_OC.e(TAG, "Failed to update item " + ocUpload.getLocalPath() + " into upload db.");
        } else {
            notifyObserversNow();
        }

        return result;
    }

    /**
     * Update upload status of file in DB.
     * 
     * @return 1 if file status was updated, else 0.
     */
    public int updateUploadStatus(OCUpload ocUpload) {
        return updateUploadStatus(ocUpload.getLocalPath(), ocUpload.getUploadStatus(),
                ocUpload.getLastResult());
    }

    private int updateUploadInternal(Cursor c, UploadStatus status, UploadResult result) {

        int r = 0;
        while(c.moveToNext()) {
            // read upload object and update
            OCUpload upload = createOCUploadFromCursor(c);

            String path = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_PATH));
            Log_OC.v(
                    TAG,
                    "Updating " + path + " with status:" + status + " and result:"
                            + (result == null ? "null" : result.toString()) + " (old:"
                            + upload.toFormattedString() + ")");

            upload.setUploadStatus(status);
            upload.setLastResult(result);
            // store update upload object to db
            r = updateUpload(upload);

        }

        c.close();
        return r;
    }

    /**
     * Update upload status of file uniquely referenced by filepath.
     * 
     * @param filepath filepath local file path to file. used as identifier.
     * @param status new status.
     * @param result new result of upload operation
     * @return 1 if file status was updated, else 0.
     */
    public int updateUploadStatus(String filepath, UploadStatus status, UploadResult result) {
        //Log_OC.e(TAG, "Updating "+filepath+" with uploadStatus="+status +" and result="+result);
        
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                ProviderTableMeta.UPLOADS_PATH + "=?",
                new String[] { filepath },
                null
        );

        if (c.getCount() != 1) {
            Log_OC.e(TAG, c.getCount() + " items for path=" + filepath
                    + " available in UploadDb. Expected 1. Failed to update upload db.");

            c.close();
            return 0;
        }
        return updateUploadInternal(c, status, result);
    }
    
    /**
     * Should be called when some value of this DB was changed. All observers
     * are informed.
     */
    public void notifyObserversNow() {
        Log_OC.d(TAG, "notifyObserversNow");
        setChanged();
        notifyObservers();
    }

    /**
     * Remove upload from upload list. Should be called when cleaning up upload
     * list.
     * 
     * @param localPath
     * @return true when one or more upload entries were removed
     */
    public int removeUpload(String localPath) {
        int result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_PATH + "=?",
                new String[]{localPath}
        );
        Log_OC.d(TAG, "delete returns with: " + result + " for file: " + localPath);
        if(result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public OCUpload[] getAllStoredUploads() {
        return getUploads(null, null);
    }
    
    public OCUpload[] getUploadByLocalPath(String localPath) {
        return getUploads(ProviderTableMeta.UPLOADS_PATH +"=?", new String[]{localPath});
    }


    private OCUpload[] getUploads(String selection, String[] selectionArgs) {
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                selection,
                selectionArgs,
                null
        );
        OCUpload[] list = new OCUpload[c.getCount()];
        if (c.moveToFirst()) {
            do {
                long fileUploadId = c.getLong(c.getColumnIndex(ProviderTableMeta.UPLOADS_FILE_ID));
//                // getFile for this fileUploadId
//                OCFile file = getUploadFile(fileUploadId);
                OCUpload upload = createOCUploadFromCursor(c);
                if (upload == null) {
                    Log_OC.e(TAG, "Upload for file id = " + fileUploadId + "not found on DB");
                } else {
                    list[c.getPosition()] = upload;
                }
            } while (c.moveToNext());

            c.close();
        }

        return list;
    }

    private OCFile getUploadFile(long id) {
        OCFile file = null;
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_FILE,
                null,
                ProviderTableMeta._ID + "=?",
                new String[]{ String.valueOf(id) },
                null
        );

        if (c.moveToFirst()) {
            file = createFileInstance(c);
            c.close();
        }

        return file;
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
                    String accountName = c.getString(c
                            .getColumnIndex(ProviderTableMeta.FILE_ACCOUNT_OWNER));
                    File f = new File(FileStorageUtils.getDefaultSavePathFor(accountName, file));
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
                    c.getColumnIndex(ProviderTableMeta.FILE_KEEP_IN_SYNC)) == 1 );
            file.setEtag(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG)));
            file.setShareViaLink(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_SHARED_VIA_LINK)) == 1 );
            file.setShareWithSharee(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_SHARED_WITH_SHAREE)) == 1 );
            file.setPublicLink(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PUBLIC_LINK)));
            file.setPermissions(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PERMISSIONS)));
            file.setRemoteId(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID)));
            file.setNeedsUpdateThumbnail(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_UPDATE_THUMBNAIL)) == 1 );
            file.setDownloading(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.FILE_IS_DOWNLOADING)) == 1 );
            file.setEtagInConflict(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ETAG_IN_CONFLICT)));

        }
        return file;
    }

    private OCUpload createOCUploadFromCursor(Cursor c){
        OCUpload upload = null;
        if (c != null){
            long fileUploadId = c.getLong(c.getColumnIndex(ProviderTableMeta.UPLOADS_FILE_ID));
            //String uploadObjectString = c.getString(c.getColumnIndex("uploadObject"));
            // getFile for this fileUploadId
            OCFile file = getUploadFile(fileUploadId);
            upload = new OCUpload(file);
            upload.setUploadId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
            upload.setUploadStatus(UploadStatus.fromValue(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_STATUS))));
            upload.setAccountName(c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_ACCOUNT_NAME)));
            upload.setLocalAction(c.getInt(c.getColumnIndex((ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR))));
            upload.setForceOverwrite(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_FORCE_OVERWRITE)) == 1 );
            upload.setCreateRemoteFolder(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER)) == 1 );
            upload.setWhileChargingOnly(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY)) == 1 );
            upload.setUseWifiOnly(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_WIFI_ONLY)) == 1 );
            upload.setUploadTimestamp(c.getLong(c.getColumnIndex(ProviderTableMeta.UPLOADS_UPLOAD_TIMESTAMP)));
            upload.setLastResult(UploadResult.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_LAST_RESULT))));
        }
        return upload;
    }

    /**
     * Get all uploads which are pending, i.e., queued for upload but not
     * currently being uploaded
     * 
     * @return
     */
    public OCUpload[] getPendingUploads() {
        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_LATER.value + " OR " +
                ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_FAILED_RETRY.value,
                null);
    }

    /**
     * Get all uploads which are currently being uploaded. There should only be
     * one. No guarantee though.
     */
    public OCUpload[] getCurrentUpload() {
        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_IN_PROGRESS.value, null);
    }

    /**
     * Get all current and pending uploads.
     */
    public OCUpload[] getCurrentAndPendingUploads() {
        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_IN_PROGRESS.value + " OR " +
                ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_LATER.value + " OR " +
                ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_FAILED_RETRY.value + " OR " +
                ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_PAUSED.value, null);
    }

    /**
     * Get all unrecoverably failed. Upload of these should/must/will not be
     * retried.
     */
    public OCUpload[] getFailedUploads() {
        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_FAILED_GIVE_UP.value + " OR " +
                ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_CANCELLED.value, null);
    }

    /**
     * Get all uploads which where successfully completed.
     */
    public OCUpload[] getFinishedUploads() {
        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_SUCCEEDED.value, null);
    }

    private ContentResolver getDB() {
        return mContentResolver;
    }

    public long clearFailedUploads() {
        String[] whereArgs = new String[2];
        whereArgs[0] = String.valueOf(UploadStatus.UPLOAD_CANCELLED.value);
        whereArgs[1] = String.valueOf(UploadStatus.UPLOAD_FAILED_GIVE_UP.value);
        long result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "=? OR " + ProviderTableMeta.UPLOADS_STATUS + "=?",
                whereArgs
        );
        Log_OC.d(TAG, "delete all failed uploads");
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public long clearFinishedUploads() {
        String[] whereArgs = new String[1];
        whereArgs[0] = String.valueOf(UploadStatus.UPLOAD_SUCCEEDED.value);
        long result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "=? ",
                whereArgs
        );
        Log_OC.d(TAG, "delete all finished uploads");
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }
    
    public void setAllCurrentToUploadLater() {
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                ProviderTableMeta.UPLOADS_STATUS + "=? ",
                new String[]{
                        Integer.toString(UploadStatus.UPLOAD_IN_PROGRESS.value)
                },
                null
        );
        updateUploadInternal(c, UploadStatus.UPLOAD_LATER, null);
    }

}
