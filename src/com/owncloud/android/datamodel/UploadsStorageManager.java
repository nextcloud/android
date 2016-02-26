/**
 *  ownCloud Android client application
 *
 *  @author LukeOwncloud
 *  @author David A. Velasco
 *  @author masensio
 *  Copyright (C) 2016 ownCloud Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;

import java.util.Calendar;
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
         * Upload currently in progress or scheduled to be executed.
         */
        UPLOAD_IN_PROGRESS(0),

        /**
         * Last upload failed.
         */
        UPLOAD_FAILED(1),

        /**
         * Upload was successful.
         */
        UPLOAD_SUCCEEDED(2);

        private final int value;

        UploadStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static UploadStatus fromValue(int value) {
            switch (value) {
                case 0:
                    return UPLOAD_IN_PROGRESS;
                case 1:
                    return UPLOAD_FAILED;
                case 2:
                    return UPLOAD_SUCCEEDED;
            }
            return null;
        }

    }

    ;

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
     * @return upload id, -1 if the insert process fails.
     */
    public long storeUpload(OCUpload ocUpload) {
        Log_OC.e(TAG, "Inserting " + ocUpload.getLocalPath() + " with status=" + ocUpload.getUploadStatus());

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_PATH, ocUpload.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_REMOTE_PATH, ocUpload.getRemotePath());
        cv.put(ProviderTableMeta.UPLOADS_ACCOUNT_NAME, ocUpload.getAccountName());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.getUploadStatus().value);
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR, ocUpload.getLocalAction());
        cv.put(ProviderTableMeta.UPLOADS_FORCE_OVERWRITE, ocUpload.isForceOverwrite() ? 1 : 0);
        cv.put(ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY, ocUpload.isWhileChargingOnly() ? 1 : 0);
        cv.put(ProviderTableMeta.UPLOADS_IS_WIFI_ONLY, ocUpload.isUseWifiOnly() ? 1 : 0);
        cv.put(ProviderTableMeta.UPLOADS_LAST_RESULT, ocUpload.getLastResult().getValue());
        cv.put(ProviderTableMeta.UPLOADS_CREATED_BY, ocUpload.getCreadtedBy());

        Uri result = getDB().insert(ProviderTableMeta.CONTENT_URI_UPLOADS, cv);

        Log_OC.d(TAG, "storeUpload returns with: " + result + " for file: " + ocUpload.getLocalPath());
        if (result == null) {
            Log_OC.e(TAG, "Failed to insert item " + ocUpload.getLocalPath() + " into upload db.");
            return -1;
        } else {
            long new_id = Long.parseLong(result.getPathSegments().get(1));
            ocUpload.setUploadId(new_id);
            notifyObserversNow();
            return new_id;
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
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_PATH, ocUpload.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_REMOTE_PATH, ocUpload.getRemotePath());
        cv.put(ProviderTableMeta.UPLOADS_ACCOUNT_NAME, ocUpload.getAccountName());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.getUploadStatus().value);
        cv.put(ProviderTableMeta.UPLOADS_LAST_RESULT, ocUpload.getLastResult().getValue());
        cv.put(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP, ocUpload.getUploadEndTimestamp());

        int result = getDB().update(ProviderTableMeta.CONTENT_URI_UPLOADS,
                cv,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(ocUpload.getUploadId())}
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
        return updateUploadStatus(ocUpload.getUploadId(), ocUpload.getUploadStatus(),
                ocUpload.getLastResult(), ocUpload.getRemotePath());
    }

    private int updateUploadInternal(Cursor c, UploadStatus status, UploadResult result, String remotePath) {

        int r = 0;
        while (c.moveToNext()) {
            // read upload object and update
            OCUpload upload = createOCUploadFromCursor(c);

            String path = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_LOCAL_PATH));
            Log_OC.v(
                    TAG,
                    "Updating " + path + " with status:" + status + " and result:"
                            + (result == null ? "null" : result.toString()) + " (old:"
                            + upload.toFormattedString() + ")");

            upload.setUploadStatus(status);
            upload.setLastResult(result);
            upload.setRemotePath(remotePath);
            if (status == UploadStatus.UPLOAD_SUCCEEDED) {
                upload.setUploadEndTimestamp(Calendar.getInstance().getTimeInMillis());
            }

            // store update upload object to db
            r = updateUpload(upload);

        }

        c.close();
        return r;
    }

    /**
     * Update upload status of file uniquely referenced by id.
     *
     * @param id     upload id.
     * @param status new status.
     * @param result new result of upload operation
     * @return 1 if file status was updated, else 0.
     */
    public int updateUploadStatus(long id, UploadStatus status, UploadResult result, String remotePath) {
        //Log_OC.e(TAG, "Updating "+filepath+" with uploadStatus="+status +" and result="+result);

        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (c.getCount() != 1) {
            Log_OC.e(TAG, c.getCount() + " items for id=" + id
                    + " available in UploadDb. Expected 1. Failed to update upload db.");

            c.close();
            return 0;
        }
        return updateUploadInternal(c, status, result, remotePath);
    }

    /*
    public int updateFileIdUpload(long uploadId, long fileId) {
        Log_OC.e(TAG, "Updating " + uploadId + " with fileId= " + fileId);

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_FILE_ID, fileId);

        int result = getDB().update(ProviderTableMeta.CONTENT_URI_UPLOADS,
                cv,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(uploadId)}
        );

        Log_OC.d(TAG, "updateUpload returns with: " + result + " for fileId: " + fileId);
        if (result != 1) {
            Log_OC.e(TAG, "Failed to update item " + uploadId + " into upload db.");
        } else {
            notifyObserversNow();
        }

        return result;
    }
    */

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
     * Remove an upload from the uploads list, known its target account and remote path.
     *
     * @param upload            Upload instance to remove from persisted storage.
     *
     * @return true when the upload was stored and could be removed.
     */
    public int removeUpload(OCUpload upload) {
        int result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta._ID + "=?" ,
                new String[]{Long.toString(upload.getUploadId())}
        );
        Log_OC.d(TAG, "delete returns " + result + " for upload " + upload);
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }


    /**
     * Remove an upload from the uploads list, known its target account and remote path.
     *
     * @param accountName       Name of the OC account target of the upload to remove.
     * @param remotePath        Absolute path in the OC account target of the upload to remove.
     * @return true when one or more upload entries were removed
     */
    public int removeUpload(String accountName, String remotePath) {
        int result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=? AND " + ProviderTableMeta.UPLOADS_REMOTE_PATH + "=?" ,
                new String[]{accountName, remotePath}
        );
        Log_OC.d(TAG, "delete returns " + result + " for file " + remotePath + " in " + accountName);
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public OCUpload[] getAllStoredUploads() {
        return getUploads(null, null);
    }

    public OCUpload[] getUploadByLocalPath(String localPath) {
        return getUploads(ProviderTableMeta.UPLOADS_LOCAL_PATH + "=?", new String[]{localPath});
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
                OCUpload upload = createOCUploadFromCursor(c);
                if (upload == null) {
                    Log_OC.e(TAG, "OCUpload could not be created from cursor");
                } else {
                    list[c.getPosition()] = upload;
                }
            } while (c.moveToNext());

            c.close();
        }

        return list;
    }

    /*
    private OCFile getUploadFile(long id) {
        OCFile file = null;
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_FILE,
                null,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (c.moveToFirst()) {
            file = createFileInstance(c);
            c.close();
        }

        return file;
    }
    */


    private OCUpload createOCUploadFromCursor(Cursor c) {
        OCUpload upload = null;
        if (c != null) {
            String localPath = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_LOCAL_PATH));
            String remotePath = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_REMOTE_PATH));
            String accountName = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_ACCOUNT_NAME));
            upload = new OCUpload(localPath, remotePath, accountName);

            upload.setUploadId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
            upload.setUploadStatus(
                    UploadStatus.fromValue(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_STATUS)))
            );
            upload.setLocalAction(c.getInt(c.getColumnIndex((ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR))));
            upload.setForceOverwrite(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_FORCE_OVERWRITE)) == 1);
            upload.setCreateRemoteFolder(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER)) == 1);
            upload.setWhileChargingOnly(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY)) == 1);
            upload.setUseWifiOnly(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_WIFI_ONLY)) == 1);
            upload.setUploadEndTimestamp(c.getLong(c.getColumnIndex(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP)));
            upload.setLastResult(UploadResult.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_LAST_RESULT))));
            upload.setCreatedBy(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_CREATED_BY)));
        }
        return upload;
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
        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_IN_PROGRESS.value, null);
    }

    /**
     * Get all unrecoverably failed. Upload of these should/must/will not be
     * retried.
     */
    public OCUpload[] getFailedUploads() {
        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_FAILED.value, null);
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
        long result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_FAILED.value, null
        );
        Log_OC.d(TAG, "delete all failed uploads");
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public long clearFinishedUploads() {
        long result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "=="+ UploadStatus.UPLOAD_SUCCEEDED.value, null
        );
        Log_OC.d(TAG, "delete all finished uploads");
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public long clearAllUploads() {
        String[] whereArgs = new String[3];
        whereArgs[0] = String.valueOf(UploadStatus.UPLOAD_SUCCEEDED.value);
        whereArgs[1] = String.valueOf(UploadStatus.UPLOAD_FAILED.value);
        whereArgs[2] = String.valueOf(UploadStatus.UPLOAD_IN_PROGRESS.value);
        long result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "=? OR " + ProviderTableMeta.UPLOADS_STATUS + "=? OR " +
                        ProviderTableMeta.UPLOADS_STATUS + "=?",
                whereArgs
        );
        Log_OC.d(TAG, "delete all uploads");
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

//    public void setAllCurrentToUploadLater() {
//        Cursor c = getDB().query(
//                ProviderTableMeta.CONTENT_URI_UPLOADS,
//                null,
//                ProviderTableMeta.UPLOADS_STATUS + "=? ",
//                new String[]{
//                        Integer.toString(UploadStatus.UPLOAD_IN_PROGRESS.value)
//                },
//                null
//        );
//        updateUploadInternal(c, UploadStatus.UPLOAD_LATER, UploadResult.UNKNOWN);
//    }


    /**
     * Updates the persistent upload database with upload result.
     */
    public void updateDatabaseUploadResult(RemoteOperationResult uploadResult, UploadFileOperation upload) {
        // result: success or fail notification
        Log_OC.d(TAG, "updateDataseUploadResult uploadResult: " + uploadResult + " upload: " + upload);

        if (uploadResult.isCancelled()) {
            removeUpload(
                    upload.getAccount().name,
                    upload.getRemotePath()
            );
        } else {

            if (uploadResult.isSuccess()) {
                updateUploadStatus(
                        upload.getOCUploadId(),
                        UploadStatus.UPLOAD_SUCCEEDED,
                        UploadResult.UPLOADED,
                        upload.getRemotePath()
                );
            } else {
                // TODO: Disable for testing of menu actions in uploads view
                if (shouldRetryFailedUpload(uploadResult)) {
                    updateUploadStatus(
                            upload.getOCUploadId(), UploadStatus.UPLOAD_FAILED,
                            UploadResult.fromOperationResult(uploadResult), upload.getRemotePath());
                } else {
                    updateUploadStatus(upload.getOCUploadId(),
                            UploadsStorageManager.UploadStatus.UPLOAD_FAILED,
                            UploadResult.fromOperationResult(uploadResult), upload.getRemotePath());
                }
            }
        }
    }

    /**
     * Determines whether with given uploadResult the upload should be retried later.
     * @param uploadResult
     * @return true if upload should be retried later, false if is should be abandoned.
     */
    private boolean shouldRetryFailedUpload(RemoteOperationResult uploadResult) {
        if (uploadResult.isSuccess()) {
            return false;
        }
        switch (uploadResult.getCode()) {
            case HOST_NOT_AVAILABLE:
            case NO_NETWORK_CONNECTION:
            case TIMEOUT:
            case WRONG_CONNECTION: // SocketException
                return true;
            default:
                return false;
        }
    }

    /**
     * Updates the persistent upload database that upload is in progress.
     */
    public void updateDatabaseUploadStart(UploadFileOperation upload) {
        updateUploadStatus(
                upload.getOCUploadId(),
                UploadStatus.UPLOAD_IN_PROGRESS,
                UploadResult.UNKNOWN,
                upload.getRemotePath()
        );
    }



}
