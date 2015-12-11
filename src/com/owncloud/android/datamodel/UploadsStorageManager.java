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

import java.util.Observable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

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
     * @param uploadObject
     * @return true on success.
     */
    public boolean storeUpload(UploadDbObject uploadObject) {
        Log_OC.e(TAG, "Inserting " + uploadObject.getLocalPath() + " with uploadStatus=" + uploadObject.getUploadStatus());
        
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_PATH, uploadObject.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, uploadObject.getUploadStatus().value);
        // TODO - CRITICAL cv.put("uploadObject", uploadObject.toString());

        Uri result = getDB().insert(ProviderTableMeta.CONTENT_URI_UPLOADS, cv);
        
        Log_OC.d(TAG, "storeUpload returns with: " + result + " for file: " + uploadObject.getLocalPath());
        if (result == null) {
            Log_OC.e(TAG, "Failed to insert item " + uploadObject.getLocalPath() + " into upload db.");
            return false;
        } else {
            notifyObserversNow();
            return true;
        }
    }

    /**
     * Update upload status of file in DB.
     * 
     * @return 1 if file status was updated, else 0.
     */
    public int updateUploadStatus(UploadDbObject uploadDbObject) {
        return updateUploadStatus(uploadDbObject.getLocalPath(), uploadDbObject.getUploadStatus(),
                uploadDbObject.getLastResult());
    }

    private int updateUploadInternal(Cursor c, UploadStatus status, RemoteOperationResult result) {
        
        while(c.moveToNext()) {
            // read upload object and update
            String uploadObjectString = c.getString(c.getColumnIndex("uploadObject"));
            UploadDbObject uploadObject = UploadDbObject.fromString(uploadObjectString);
            
            String path = c.getString(c.getColumnIndex("path"));
            Log_OC.v(
                    TAG,
                    "Updating " + path + " with status:" + status + " and result:"
                            + (result == null ? "null" : result.getCode()) + " (old:"
                            + uploadObject.toFormattedString() + ")");

            uploadObject.setUploadStatus(status);
            uploadObject.setLastResult(result);
            uploadObjectString = uploadObject.toString();
            // store update upload object to db
            ContentValues cv = new ContentValues();
            cv.put(ProviderTableMeta.UPLOADS_STATUS, status.value);
            // TODO - CRITICAL cv.put("uploadObject", uploadObjectString);

            int r = getDB().update(
                    ProviderTableMeta.CONTENT_URI_UPLOADS,
                    cv,
                    ProviderTableMeta.UPLOADS_PATH + "=?",
                    new String[] {path}
            );
            
            if (r == 1) {
                notifyObserversNow();
            } else {
                Log_OC.e(TAG, "Failed to update upload db.");
            }
            return r;
        }
        return 0;
    }
    /**
     * Update upload status of file uniquely referenced by filepath.
     * 
     * @param filepath filepath local file path to file. used as identifier.
     * @param status new status.
     * @param result new result of upload operation
     * @return 1 if file status was updated, else 0.
     */
    public int updateUploadStatus(String filepath, UploadStatus status, RemoteOperationResult result) {
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
                ProviderTableMeta.UPLOADS_PATH,
                new String[] { localPath }
        );
        Log_OC.d(TAG, "delete returns with: " + result + " for file: " + localPath);
        if(result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public UploadDbObject[] getAllStoredUploads() {
        return getUploads(null, null);
    }
    
    public UploadDbObject[] getUploadByLocalPath(String localPath) {
        return getUploads("path = ?", new String[] { localPath });
    }


    private UploadDbObject[] getUploads(String selection, String[] selectionArgs) {
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                selection,
                selectionArgs,
                null
        );
        UploadDbObject[] list = new UploadDbObject[c.getCount()];
        if (c.moveToFirst()) {
            do {
                String uploadObjectString = c.getString(c.getColumnIndex("uploadObject"));
                UploadDbObject uploadObject = UploadDbObject.fromString(uploadObjectString);
                if (uploadObject == null) {
                    Log_OC.e(TAG, "Could not deserialize UploadDbObject " + uploadObjectString);
                } else {
                    list[c.getPosition()] = uploadObject;
                }
            } while (c.moveToNext());
        }
        return list;
    }

    /**
     * Get all uploads which are pending, i.e., queued for upload but not
     * currently being uploaded
     * 
     * @return
     */
    public UploadDbObject[] getPendingUploads() {
        return getUploads("uploadStatus==" + UploadStatus.UPLOAD_LATER.value + " OR uploadStatus=="
                + UploadStatus.UPLOAD_FAILED_RETRY.value, null);
    }

    /**
     * Get all uploads which are currently being uploaded. There should only be
     * one. No guarantee though.
     */
    public UploadDbObject[] getCurrentUpload() {
        return getUploads("uploadStatus==" + UploadStatus.UPLOAD_IN_PROGRESS.value, null);
    }

    /**
     * Get all current and pending uploads.
     */
    public UploadDbObject[] getCurrentAndPendingUploads() {
        return getUploads("uploadStatus==" + UploadStatus.UPLOAD_IN_PROGRESS.value + " OR uploadStatus=="
                + UploadStatus.UPLOAD_LATER.value + " OR uploadStatus==" + UploadStatus.UPLOAD_FAILED_RETRY.value
                + " OR uploadStatus==" + UploadStatus.UPLOAD_PAUSED.value, null);
    }

    /**
     * Get all unrecoverably failed. Upload of these should/must/will not be
     * retried.
     */
    public UploadDbObject[] getFailedUploads() {
        return getUploads("uploadStatus==" + UploadStatus.UPLOAD_FAILED_GIVE_UP.value + " OR uploadStatus=="
                + UploadStatus.UPLOAD_CANCELLED.value, null);
    }

    /**
     * Get all uploads which where successfully completed.
     */
    public UploadDbObject[] getFinishedUploads() {
        return getUploads("uploadStatus==" + UploadStatus.UPLOAD_SUCCEEDED.value, null);
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
