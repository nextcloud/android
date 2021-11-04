/*
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author David A. Velasco
 * @author masensio
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Observable;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Database helper for storing list of files to be uploaded, including status
 * information for each file.
 */
public class UploadsStorageManager extends Observable {
    private static final String TAG = UploadsStorageManager.class.getSimpleName();

    private static final String AND = " AND ";
    private static final int SINGLE_RESULT = 1;

    private final ContentResolver contentResolver;
    private final CurrentAccountProvider currentAccountProvider;

    public UploadsStorageManager(
        CurrentAccountProvider currentAccountProvider,
        ContentResolver contentResolver
    ) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        this.contentResolver = contentResolver;
        this.currentAccountProvider = currentAccountProvider;
    }

    /**
     * Stores an upload object in DB.
     *
     * @param ocUpload Upload object to store
     * @return upload id, -1 if the insert process fails.
     */
    public long storeUpload(OCUpload ocUpload) {
        Log_OC.v(TAG, "Inserting " + ocUpload.getLocalPath() + " with status=" + ocUpload.getUploadStatus());

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_PATH, ocUpload.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_REMOTE_PATH, ocUpload.getRemotePath());
        cv.put(ProviderTableMeta.UPLOADS_ACCOUNT_NAME, ocUpload.getAccountName());
        cv.put(ProviderTableMeta.UPLOADS_FILE_SIZE, ocUpload.getFileSize());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.getUploadStatus().value);
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR, ocUpload.getLocalAction());
        cv.put(ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY, ocUpload.getNameCollisionPolicy().serialize());
        cv.put(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER, ocUpload.isCreateRemoteFolder() ? 1 : 0);
        cv.put(ProviderTableMeta.UPLOADS_LAST_RESULT, ocUpload.getLastResult().getValue());
        cv.put(ProviderTableMeta.UPLOADS_CREATED_BY, ocUpload.getCreatedBy());
        cv.put(ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY, ocUpload.isWhileChargingOnly() ? 1 : 0);
        cv.put(ProviderTableMeta.UPLOADS_IS_WIFI_ONLY, ocUpload.isUseWifiOnly() ? 1 : 0);
        cv.put(ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN, ocUpload.getFolderUnlockToken());
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
     * @param ocUpload Upload object with state to update
     * @return num of updated uploads.
     */
    public int updateUpload(OCUpload ocUpload) {
        Log_OC.v(TAG, "Updating " + ocUpload.getLocalPath() + " with status=" + ocUpload.getUploadStatus());

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_LOCAL_PATH, ocUpload.getLocalPath());
        cv.put(ProviderTableMeta.UPLOADS_REMOTE_PATH, ocUpload.getRemotePath());
        cv.put(ProviderTableMeta.UPLOADS_ACCOUNT_NAME, ocUpload.getAccountName());
        cv.put(ProviderTableMeta.UPLOADS_STATUS, ocUpload.getUploadStatus().value);
        cv.put(ProviderTableMeta.UPLOADS_LAST_RESULT, ocUpload.getLastResult().getValue());
        cv.put(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP, ocUpload.getUploadEndTimestamp());
        cv.put(ProviderTableMeta.UPLOADS_FILE_SIZE, ocUpload.getFileSize());
        cv.put(ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN, ocUpload.getFolderUnlockToken());

        int result = getDB().update(ProviderTableMeta.CONTENT_URI_UPLOADS,
                cv,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(ocUpload.getUploadId())}
        );

        Log_OC.d(TAG, "updateUpload returns with: " + result + " for file: " + ocUpload.getLocalPath());
        if (result != SINGLE_RESULT) {
            Log_OC.e(TAG, "Failed to update item " + ocUpload.getLocalPath() + " into upload db.");
        } else {
            notifyObserversNow();
        }

        return result;
    }

    private int updateUploadInternal(Cursor c, UploadStatus status, UploadResult result, String remotePath,
                                     String localPath) {

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
            if (localPath != null) {
                upload.setLocalPath(localPath);
            }
            if (status == UploadStatus.UPLOAD_SUCCEEDED) {
                upload.setUploadEndTimestamp(Calendar.getInstance().getTimeInMillis());
            }

            // store update upload object to db
            r = updateUpload(upload);

        }

        return r;
    }

    /**
     * Update upload status of file uniquely referenced by id.
     *
     * @param id         upload id.
     * @param status     new status.
     * @param result     new result of upload operation
     * @param remotePath path of the file to upload in the ownCloud storage
     * @param localPath  path of the file to upload in the device storage
     * @return 1 if file status was updated, else 0.
     */
    private int updateUploadStatus(long id, UploadStatus status, UploadResult result, String remotePath,
                                   String localPath) {
        //Log_OC.v(TAG, "Updating "+filepath+" with uploadStatus="+status +" and result="+result);

        int returnValue = 0;
        Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (c != null) {
            if (c.getCount() != SINGLE_RESULT) {
                Log_OC.e(TAG, c.getCount() + " items for id=" + id
                        + " available in UploadDb. Expected 1. Failed to update upload db.");
            } else {
                returnValue = updateUploadInternal(c, status, result, remotePath, localPath);
            }
            c.close();
        } else {
            Log_OC.e(TAG, "Cursor is null");
        }

        return returnValue;
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
     * Remove an upload from the uploads list, known its target account and remote path.
     *
     * @param upload Upload instance to remove from persisted storage.
     * @return true when the upload was stored and could be removed.
     */
    public int removeUpload(@Nullable OCUpload upload) {
        if (upload == null) {
            return 0;
        } else {
            return removeUpload(upload.getUploadId());
        }
    }

    /**
     * Remove an upload from the uploads list, known its target account and remote path.
     *
     * @param id to remove from persisted storage.
     * @return true when the upload was stored and could be removed.
     */
    public int removeUpload(long id) {
        int result = getDB().delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta._ID + "=?",
            new String[]{Long.toString(id)}
                                   );
        Log_OC.d(TAG, "delete returns " + result + " for upload with id " + id);
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    /**
     * Remove an upload from the uploads list, known its target account and remote path.
     *
     * @param accountName Name of the OC account target of the upload to remove.
     * @param remotePath  Absolute path in the OC account target of the upload to remove.
     * @return true when one or more upload entries were removed
     */
    public int removeUpload(String accountName, String remotePath) {
        int result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=? AND " + ProviderTableMeta.UPLOADS_REMOTE_PATH + "=?",
                new String[]{accountName, remotePath}
        );
        Log_OC.d(TAG, "delete returns " + result + " for file " + remotePath + " in " + accountName);
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    /**
     * Remove all the uploads of a given account from the uploads list.
     *
     * @param accountName Name of the OC account target of the uploads to remove.
     * @return true when one or more upload entries were removed
     */
    public int removeUploads(String accountName) {
        int result = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=?",
                new String[]{accountName}
        );
        Log_OC.d(TAG, "delete returns " + result + " for uploads in " + accountName);
        if (result > 0) {
            notifyObserversNow();
        }
        return result;
    }

    public OCUpload[] getAllStoredUploads() {
        return getUploads(null, (String[]) null);
    }

    public @Nullable
    OCUpload getUploadById(long id) {
        OCUpload result = null;
        Cursor cursor = getDB().query(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            null,
            ProviderTableMeta._ID + "=?",
            new String[]{Long.toString(id)},
            "_id ASC");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = createOCUploadFromCursor(cursor);
            }
        }
        Log_OC.d(TAG, "Retrieve job " + result + " for id " + id);
        return result;
    }

    private OCUpload[] getUploads(@Nullable String selection, @Nullable String... selectionArgs) {
        ArrayList<OCUpload> uploads = new ArrayList<>();
        final long pageSize = 100;
        long page = 0;
        long rowsRead;
        long rowsTotal = 0;
        long lastRowID = -1;

        do {
            String pageSelection = selection;
            String[] pageSelectionArgs = selectionArgs;
            if (page > 0 && lastRowID >= 0) {
                if (selection != null) {
                    pageSelection = "(" + selection + ") AND _id < ?";
                } else {
                    pageSelection = "_id < ?";
                }
                if (selectionArgs != null) {
                    pageSelectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
                } else {
                    pageSelectionArgs = new String[1];
                }
                pageSelectionArgs[pageSelectionArgs.length - 1] = String.valueOf(lastRowID);
                Log_OC.d(TAG, String.format(Locale.ENGLISH, "QUERY: %s ROWID: %d", pageSelection, lastRowID));
            } else {
                Log_OC.d(TAG, String.format(Locale.ENGLISH, "QUERY: %s ROWID: %d", selection, lastRowID));
            }
            rowsRead = 0;

            Cursor c = getDB().query(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                null,
                pageSelection,
                pageSelectionArgs,
                String.format(Locale.ENGLISH, "_id DESC LIMIT %d", pageSize)
                                    );

            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        rowsRead++;
                        rowsTotal++;
                        lastRowID = c.getLong(c.getColumnIndex(ProviderTableMeta._ID));
                        OCUpload upload = createOCUploadFromCursor(c);
                        if (upload == null) {
                            Log_OC.e(TAG, "OCUpload could not be created from cursor");
                        } else {
                            uploads.add(upload);
                        }
                    } while (c.moveToNext() && !c.isAfterLast());
                }
                c.close();
                Log_OC.v(TAG, String.format(Locale.ENGLISH,
                                            "getUploads() got %d rows from page %d, %d rows total so far, last ID %d",
                                            rowsRead,
                                            page,
                                            rowsTotal,
                                            lastRowID
                                           ));
                page += 1;
            } else {
                break;
            }
        } while (rowsRead > 0);

        Log_OC.v(TAG, String.format(Locale.ENGLISH,
                                    "getUploads() returning %d (%d) rows after reading %d pages",
                                    rowsTotal,
                                    uploads.size(),
                                    page
                                   ));

        return uploads.toArray(new OCUpload[0]);
    }

    private OCUpload createOCUploadFromCursor(Cursor c) {
        OCUpload upload = null;
        if (c != null) {
            String localPath = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_LOCAL_PATH));
            String remotePath = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_REMOTE_PATH));
            String accountName = c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_ACCOUNT_NAME));
            upload = new OCUpload(localPath, remotePath, accountName);

            upload.setFileSize(c.getLong(c.getColumnIndex(ProviderTableMeta.UPLOADS_FILE_SIZE)));
            upload.setUploadId(c.getLong(c.getColumnIndex(ProviderTableMeta._ID)));
            upload.setUploadStatus(
                    UploadStatus.fromValue(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_STATUS)))
            );
            upload.setLocalAction(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR)));
            upload.setNameCollisionPolicy(NameCollisionPolicy.deserialize(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY))));
            upload.setCreateRemoteFolder(c.getInt(
                    c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER)) == 1);
            upload.setUploadEndTimestamp(c.getLong(c.getColumnIndex(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP)));
            upload.setLastResult(UploadResult.fromValue(
                    c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_LAST_RESULT))));
            upload.setCreatedBy(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_CREATED_BY)));
            upload.setUseWifiOnly(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_WIFI_ONLY)) == 1);
            upload.setWhileChargingOnly(c.getInt(c.getColumnIndex(ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY))
                    == 1);
            upload.setFolderUnlockToken(c.getString(c.getColumnIndex(ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN)));
        }
        return upload;
    }

    public OCUpload[] getCurrentAndPendingUploadsForCurrentAccount() {
        User user = currentAccountProvider.getUser();

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_IN_PROGRESS.value +
                        " OR " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "==" + UploadResult.DELAYED_FOR_WIFI.getValue() +
                        " OR " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "==" + UploadResult.LOCK_FAILED.getValue() +
                        " OR " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "==" + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                        " OR " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "==" + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue() +
                        " AND " + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "== ?",
                        user.getAccountName());
    }

    /**
     * Get all failed uploads.
     */
    public OCUpload[] getFailedUploads() {
        return getUploads("(" + ProviderTableMeta.UPLOADS_STATUS + "== ?" +
                " OR " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "==" + UploadResult.DELAYED_FOR_WIFI.getValue() +
                        " OR " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "==" + UploadResult.LOCK_FAILED.getValue() +
                        " OR " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "==" + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                        " OR " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "==" + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue() +
                        " ) AND " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "!= " + UploadResult.VIRUS_DETECTED.getValue()
            , String.valueOf(UploadStatus.UPLOAD_FAILED.value));
    }

    public OCUpload[] getFinishedUploadsForCurrentAccount() {
        User user = currentAccountProvider.getUser();

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_SUCCEEDED.value + AND +
                              ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "== ?", user.getAccountName());
    }

    /**
     * Get all uploads which where successfully completed.
     */
    public OCUpload[] getFinishedUploads() {

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_SUCCEEDED.value, (String[]) null);
    }

    public OCUpload[] getFailedButNotDelayedUploadsForCurrentAccount() {
        User user = currentAccountProvider.getUser();

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_FAILED.value +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_FOR_WIFI.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.LOCK_FAILED.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue() +
                        AND + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "== ?",
                        user.getAccountName());
    }

    /**
     * Get all failed uploads, except for those that were not performed due to lack of Wifi connection.
     *
     * @return Array of failed uploads, except for those that were not performed due to lack of Wifi connection.
     */
    public OCUpload[] getFailedButNotDelayedUploads() {

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_FAILED.value + AND +
                        ProviderTableMeta.UPLOADS_LAST_RESULT + "<>" + UploadResult.LOCK_FAILED.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_FOR_WIFI.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue(),
                (String[]) null
        );
    }

    private ContentResolver getDB() {
        return contentResolver;
    }

    public long clearFailedButNotDelayedUploads() {
        User user = currentAccountProvider.getUser();
        final long deleted = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_FAILED.value +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.LOCK_FAILED.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_FOR_WIFI.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                        AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                        "<>" + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue() +
                        AND + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "== ?",
                new String[]{user.getAccountName()}
        );
        Log_OC.d(TAG, "delete all failed uploads but those delayed for Wifi");
        if (deleted > 0) {
            notifyObserversNow();
        }
        return deleted;
    }

    public long clearSuccessfulUploads() {
        User user = currentAccountProvider.getUser();
        final long deleted = getDB().delete(
                ProviderTableMeta.CONTENT_URI_UPLOADS,
                ProviderTableMeta.UPLOADS_STATUS + "==" + UploadStatus.UPLOAD_SUCCEEDED.value + AND +
                        ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "== ?", new String[]{user.getAccountName()}
        );

        Log_OC.d(TAG, "delete all successful uploads");
        if (deleted > 0) {
            notifyObserversNow();
        }
        return deleted;
    }

    /**
     * Updates the persistent upload database with upload result.
     */
    public void updateDatabaseUploadResult(RemoteOperationResult uploadResult, UploadFileOperation upload) {
        // result: success or fail notification
        Log_OC.d(TAG, "updateDatabaseUploadResult uploadResult: " + uploadResult + " upload: " + upload);

        if (uploadResult.isCancelled()) {
            removeUpload(
                    upload.getAccount().name,
                    upload.getRemotePath()
            );
        } else {
            String localPath = (FileUploader.LOCAL_BEHAVIOUR_MOVE == upload.getLocalBehaviour())
                    ? upload.getStoragePath() : null;

            if (uploadResult.isSuccess()) {
                updateUploadStatus(
                        upload.getOCUploadId(),
                        UploadStatus.UPLOAD_SUCCEEDED,
                        UploadResult.UPLOADED,
                        upload.getRemotePath(),
                        localPath
                );
            } else {
                updateUploadStatus(
                        upload.getOCUploadId(),
                        UploadStatus.UPLOAD_FAILED,
                        UploadResult.fromOperationResult(uploadResult),
                        upload.getRemotePath(),
                        localPath
                );
            }
        }
    }

    /**
     * Updates the persistent upload database with an upload now in progress.
     */
    public void updateDatabaseUploadStart(UploadFileOperation upload) {
        String localPath = (FileUploader.LOCAL_BEHAVIOUR_MOVE == upload.getLocalBehaviour())
                ? upload.getStoragePath() : null;

        updateUploadStatus(
                upload.getOCUploadId(),
                UploadStatus.UPLOAD_IN_PROGRESS,
                UploadResult.UNKNOWN,
                upload.getRemotePath(),
                localPath
        );
    }

    /**
     * Changes the status of any in progress upload from UploadStatus.UPLOAD_IN_PROGRESS
     * to UploadStatus.UPLOAD_FAILED
     *
     * @return Number of uploads which status was changed.
     */
    public int failInProgressUploads(UploadResult fail) {
        Log_OC.v(TAG, "Updating state of any killed upload");

        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.UPLOADS_STATUS, UploadStatus.UPLOAD_FAILED.getValue());
        cv.put(
            ProviderTableMeta.UPLOADS_LAST_RESULT,
            fail != null ? fail.getValue() : UploadResult.UNKNOWN.getValue()
              );
        cv.put(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP, Calendar.getInstance().getTimeInMillis());

        int result = getDB().update(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            cv,
            ProviderTableMeta.UPLOADS_STATUS + "=?",
            new String[]{String.valueOf(UploadStatus.UPLOAD_IN_PROGRESS.getValue())}
                                   );

        if (result == 0) {
            Log_OC.v(TAG, "No upload was killed");
        } else {
            Log_OC.w(TAG, Integer.toString(result) + " uploads where abruptly interrupted");
            notifyObserversNow();
        }

        return result;
    }

    @VisibleForTesting
    public int removeAllUploads() {
        Log_OC.v(TAG, "Delete all uploads!");
        return getDB().delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            "",
            new String[]{});
    }

    public int removeUserUploads(User user) {
        Log_OC.v(TAG, "Delete all uploads for account " + user.getAccountName());
        return getDB().delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=?",
            new String[]{user.getAccountName()});
    }

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

        public int getValue() {
            return value;
        }

    }
}
