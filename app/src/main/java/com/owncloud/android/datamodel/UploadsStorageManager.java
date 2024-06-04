/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Jonas Mayer <jonas.a.mayer@gmx.net>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019-2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016-2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2016 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2014 Luke Owncloud <owncloud@ohrt.org>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.datamodel;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.owncloud.android.MainApp;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Observable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Database helper for storing list of files to be uploaded, including status information for each file.
 */
public class UploadsStorageManager extends Observable {
    private static final String TAG = UploadsStorageManager.class.getSimpleName();

    private static final String IS_EQUAL =  "== ?";
    private static final String EQUAL =  "==";
    private static final String OR =  " OR ";
    private static final String AND = " AND ";
    private static final String ANGLE_BRACKETS = "<>";
    private static final int SINGLE_RESULT = 1;

    private static final long QUERY_PAGE_SIZE = 100;

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
        OCUpload existingUpload = getPendingCurrentOrFailedUpload(ocUpload);
        if (existingUpload != null) {
            Log_OC.v(TAG, "Will update upload in db since " + ocUpload.getLocalPath() + " already exists as " +
                "pending, current or failed upload");
            long existingId = existingUpload.getUploadId();
            ocUpload.setUploadId(existingId);
            updateUpload(ocUpload);
            return existingId;
        }


        Log_OC.v(TAG, "Inserting " + ocUpload.getLocalPath() + " with status=" + ocUpload.getUploadStatus());

        ContentValues cv = getContentValues(ocUpload);
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

    public long[] storeUploads(final List<OCUpload> ocUploads) {
        Log_OC.v(TAG, "Inserting " + ocUploads.size() + " uploads");
        ArrayList<ContentProviderOperation> operations = new ArrayList<>(ocUploads.size());
        for (OCUpload ocUpload : ocUploads) {

            OCUpload existingUpload = getPendingCurrentOrFailedUpload(ocUpload);
            if (existingUpload != null) {
                Log_OC.v(TAG, "Will update upload in db since " + ocUpload.getLocalPath() + " already exists as" +
                    " pending, current or failed upload");
                ocUpload.setUploadId(existingUpload.getUploadId());
                updateUpload(ocUpload);
                continue;
            }

            final ContentProviderOperation operation = ContentProviderOperation
                .newInsert(ProviderTableMeta.CONTENT_URI_UPLOADS)
                .withValues(getContentValues(ocUpload))
                .build();
            operations.add(operation);
        }

        try {
            final ContentProviderResult[] contentProviderResults = getDB().applyBatch(MainApp.getAuthority(), operations);
            final long[] newIds = new long[ocUploads.size()];
            for (int i = 0; i < contentProviderResults.length; i++) {
                final ContentProviderResult result = contentProviderResults[i];
                final long new_id = Long.parseLong(result.uri.getPathSegments().get(1));
                ocUploads.get(i).setUploadId(new_id);
                newIds[i] = new_id;
            }
            notifyObserversNow();
            return newIds;
        } catch (OperationApplicationException | RemoteException e) {
            Log_OC.e(TAG, "Error inserting uploads", e);
        }

        return null;
    }

    @NonNull
    private ContentValues getContentValues(OCUpload ocUpload) {
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
        return cv;
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

            String path = c.getString(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_LOCAL_PATH));
            Log_OC.v(
                TAG,
                "Updating " + path + " with status:" + status + " and result:"
                    + (result == null ? "null" : result.toString()) + " (old:"
                    + upload.toFormattedString() + ')');

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
     * Should be called when some value of this DB was changed. All observers are informed.
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

    public OCUpload getPendingCurrentOrFailedUpload(OCUpload upload) {
        try (Cursor cursor = getDB().query(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            null,
            ProviderTableMeta.UPLOADS_REMOTE_PATH + "=? and " +
                ProviderTableMeta.UPLOADS_LOCAL_PATH + "=? and " +
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME + "=? and (" +
                ProviderTableMeta.UPLOADS_STATUS + "=? or " +
                ProviderTableMeta.UPLOADS_STATUS + "=? )",
            new String[]{
                upload.getRemotePath(),
                upload.getLocalPath(),
                upload.getAccountName(),
                String.valueOf(UploadStatus.UPLOAD_IN_PROGRESS.value),
                String.valueOf(UploadStatus.UPLOAD_FAILED.value)
            },
            ProviderTableMeta.UPLOADS_REMOTE_PATH + " ASC")) {

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    return createOCUploadFromCursor(cursor);
                }
            }
        }
        return null;
    }

    public OCUpload getUploadByRemotePath(String remotePath) {
        OCUpload result = null;
        try (Cursor cursor = getDB().query(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            null,
            ProviderTableMeta.UPLOADS_REMOTE_PATH + "=?",
            new String[]{remotePath},
            ProviderTableMeta.UPLOADS_REMOTE_PATH + " ASC")) {

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result = createOCUploadFromCursor(cursor);
                }
            }
        }
        Log_OC.d(TAG, "Retrieve job " + result + " for remote path " + remotePath);
        return result;
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
        final List<OCUpload> uploads = new ArrayList<>();
        long page = 0;
        long rowsRead;
        long rowsTotal = 0;
        long lastRowID = -1;

        do {
            final List<OCUpload> uploadsPage = getUploadPage(lastRowID, selection, selectionArgs);
            rowsRead = uploadsPage.size();
            rowsTotal += rowsRead;
            if (!uploadsPage.isEmpty()) {
                lastRowID = uploadsPage.get(uploadsPage.size() - 1).getUploadId();
            }
            Log_OC.v(TAG, String.format(Locale.ENGLISH,
                                        "getUploads() got %d rows from page %d, %d rows total so far, last ID %d",
                                        rowsRead,
                                        page,
                                        rowsTotal,
                                        lastRowID
                                       ));
            uploads.addAll(uploadsPage);
            page++;
        } while (rowsRead > 0);


        Log_OC.v(TAG, String.format(Locale.ENGLISH,
                                    "getUploads() returning %d (%d) rows after reading %d pages",
                                    rowsTotal,
                                    uploads.size(),
                                    page
                                   ));


        return uploads.toArray(new OCUpload[0]);
    }

    @NonNull
    private List<OCUpload> getUploadPage(final long afterId, @Nullable String selection, @Nullable String... selectionArgs) {
        return getUploadPage(QUERY_PAGE_SIZE, afterId, true, selection, selectionArgs);
    }

    private String getInProgressUploadsSelection() {
        return "( " + ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_IN_PROGRESS.value +
            OR + ProviderTableMeta.UPLOADS_LAST_RESULT +
            EQUAL + UploadResult.DELAYED_FOR_WIFI.getValue() +
            OR + ProviderTableMeta.UPLOADS_LAST_RESULT +
            EQUAL + UploadResult.LOCK_FAILED.getValue() +
            OR + ProviderTableMeta.UPLOADS_LAST_RESULT +
            EQUAL + UploadResult.DELAYED_FOR_CHARGING.getValue() +
            OR + ProviderTableMeta.UPLOADS_LAST_RESULT +
            EQUAL + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue() +
            " ) AND " + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL;
    }

    public int getTotalUploadSize(@Nullable String... selectionArgs) {
        final String selection = getInProgressUploadsSelection();
        int totalSize = 0;

        Cursor cursor = getDB().query(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            new String[]{"COUNT(*) AS count"},
            selection,
            selectionArgs,
            null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                totalSize = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
            }
            cursor.close();
        }

        return totalSize;
    }

    @NonNull
    private List<OCUpload> getUploadPage(long limit, final long afterId, final boolean descending, @Nullable String selection, @Nullable String... selectionArgs) {
        List<OCUpload> uploads = new ArrayList<>();
        String pageSelection = selection;
        String[] pageSelectionArgs = selectionArgs;

        String idComparator;
        String sortDirection;
        if (descending) {
            sortDirection = "DESC";
            idComparator = "<";
        } else {
            sortDirection = "ASC";
            idComparator = ">";
        }

        if (afterId >= 0) {
            if (selection != null) {
                pageSelection = "(" + selection + ") AND _id " + idComparator + " ?";
            } else {
                pageSelection = "_id " + idComparator + " ?";
            }
            if (selectionArgs != null) {
                pageSelectionArgs = Arrays.copyOf(selectionArgs, selectionArgs.length + 1);
            } else {
                pageSelectionArgs = new String[1];
            }
            pageSelectionArgs[pageSelectionArgs.length - 1] = String.valueOf(afterId);
            Log_OC.d(TAG, String.format(Locale.ENGLISH, "QUERY: %s ROWID: %d", pageSelection, afterId));
        } else {
            Log_OC.d(TAG, String.format(Locale.ENGLISH, "QUERY: %s ROWID: %d", selection, afterId));
        }

        String sortOrder;
        if (limit > 0) {
            sortOrder = String.format(Locale.ENGLISH, "_id " + sortDirection + " LIMIT %d", limit);
        } else {
            sortOrder = String.format(Locale.ENGLISH, "_id " + sortDirection);
        }

        Cursor c = getDB().query(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            null,
            pageSelection,
            pageSelectionArgs,
            sortOrder);

        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    OCUpload upload = createOCUploadFromCursor(c);
                    if (upload == null) {
                        Log_OC.e(TAG, "OCUpload could not be created from cursor");
                    } else {
                        uploads.add(upload);
                    }
                } while (c.moveToNext() && !c.isAfterLast());
            }
            c.close();
        }
        return uploads;
    }

    private OCUpload createOCUploadFromCursor(Cursor c) {
        OCUpload upload = null;
        if (c != null) {
            String localPath = c.getString(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_LOCAL_PATH));
            String remotePath = c.getString(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_REMOTE_PATH));
            String accountName = c.getString(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_ACCOUNT_NAME));
            upload = new OCUpload(localPath, remotePath, accountName);

            upload.setFileSize(c.getLong(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_FILE_SIZE)));
            upload.setUploadId(c.getLong(c.getColumnIndexOrThrow(ProviderTableMeta._ID)));
            upload.setUploadStatus(
                UploadStatus.fromValue(c.getInt(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_STATUS)))
                                  );
            upload.setLocalAction(c.getInt(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR)));
            upload.setNameCollisionPolicy(NameCollisionPolicy.deserialize(c.getInt(
                c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY))));
            upload.setCreateRemoteFolder(c.getInt(
                c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER)) == 1);
            upload.setUploadEndTimestamp(c.getLong(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP)));
            upload.setLastResult(UploadResult.fromValue(
                c.getInt(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_LAST_RESULT))));
            upload.setCreatedBy(c.getInt(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_CREATED_BY)));
            upload.setUseWifiOnly(c.getInt(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_IS_WIFI_ONLY)) == 1);
            upload.setWhileChargingOnly(c.getInt(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY))
                                            == 1);
            upload.setFolderUnlockToken(c.getString(c.getColumnIndexOrThrow(ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN)));
        }
        return upload;
    }

    public OCUpload[] getCurrentAndPendingUploadsForCurrentAccount() {
        User user = currentAccountProvider.getUser();

        return getCurrentAndPendingUploadsForAccount(user.getAccountName());
    }

    public OCUpload[] getCurrentAndPendingUploadsForAccount(final @NonNull String accountName) {
        String inProgressUploadsSelection = getInProgressUploadsSelection();
        return getUploads(inProgressUploadsSelection, accountName);
    }

    /**
     * Gets a page of uploads after <code>afterId</code>, where uploads are sorted by ascending upload id.
     * <p>
     * If <code>afterId</code> is -1, returns the first page
     */
    public List<OCUpload> getCurrentAndPendingUploadsForAccountPageAscById(final long afterId, final @NonNull String accountName) {
        final String selection = getInProgressUploadsSelection();
        return getUploadPage(QUERY_PAGE_SIZE, afterId, false, selection, accountName);
    }

    /**
     * Get all failed uploads.
     */
    public OCUpload[] getFailedUploads() {
        return getUploads("(" + ProviderTableMeta.UPLOADS_STATUS + IS_EQUAL +
                              OR + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              EQUAL + UploadResult.DELAYED_FOR_WIFI.getValue() +
                              OR + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              EQUAL + UploadResult.LOCK_FAILED.getValue() +
                              OR + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              EQUAL + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                              OR + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              EQUAL + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue() +
                              " ) AND " + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              "!= " + UploadResult.VIRUS_DETECTED.getValue()
            , String.valueOf(UploadStatus.UPLOAD_FAILED.value));
    }

    public OCUpload[] getUploadsForAccount(final @NonNull String accountName) {
        return getUploads(ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL, accountName);
    }

    public OCUpload[] getFinishedUploadsForCurrentAccount() {
        User user = currentAccountProvider.getUser();

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_SUCCEEDED.value + AND +
                              ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL, user.getAccountName());
    }

    public OCUpload[] getCancelledUploadsForCurrentAccount() {
        User user = currentAccountProvider.getUser();

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_CANCELLED.value + AND +
                              ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL, user.getAccountName());
    }

    /**
     * Get all uploads which where successfully completed.
     */
    public OCUpload[] getFinishedUploads() {
        return getUploads(ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_SUCCEEDED.value, (String[]) null);
    }

    public OCUpload[] getFailedButNotDelayedUploadsForCurrentAccount() {
        User user = currentAccountProvider.getUser();

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_FAILED.value +
                              AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              ANGLE_BRACKETS + UploadResult.DELAYED_FOR_WIFI.getValue() +
                              AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              ANGLE_BRACKETS + UploadResult.LOCK_FAILED.getValue() +
                              AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              ANGLE_BRACKETS + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                              AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              ANGLE_BRACKETS + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue() +
                              AND + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL,
                          user.getAccountName());
    }

    /**
     * Get all failed uploads, except for those that were not performed due to lack of Wifi connection.
     *
     * @return Array of failed uploads, except for those that were not performed due to lack of Wifi connection.
     */
    public OCUpload[] getFailedButNotDelayedUploads() {

        return getUploads(ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_FAILED.value + AND +
                              ProviderTableMeta.UPLOADS_LAST_RESULT + ANGLE_BRACKETS + UploadResult.LOCK_FAILED.getValue() +
                              AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              ANGLE_BRACKETS + UploadResult.DELAYED_FOR_WIFI.getValue() +
                              AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              ANGLE_BRACKETS + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                              AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                              ANGLE_BRACKETS + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue(),
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
            ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_FAILED.value +
                AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                ANGLE_BRACKETS + UploadResult.LOCK_FAILED.getValue() +
                AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                ANGLE_BRACKETS + UploadResult.DELAYED_FOR_WIFI.getValue() +
                AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                ANGLE_BRACKETS + UploadResult.DELAYED_FOR_CHARGING.getValue() +
                AND + ProviderTableMeta.UPLOADS_LAST_RESULT +
                ANGLE_BRACKETS + UploadResult.DELAYED_IN_POWER_SAVE_MODE.getValue() +
                AND + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL,
            new String[]{user.getAccountName()}
                                           );
        Log_OC.d(TAG, "delete all failed uploads but those delayed for Wifi");
        if (deleted > 0) {
            notifyObserversNow();
        }
        return deleted;
    }

    public void clearCancelledUploadsForCurrentAccount() {
        User user = currentAccountProvider.getUser();
        final long deleted = getDB().delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_CANCELLED.value + AND +
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL, new String[]{user.getAccountName()}
                                           );

        Log_OC.d(TAG, "delete all cancelled uploads");
        if (deleted > 0) {
            notifyObserversNow();
        }
    }

    public long clearSuccessfulUploads() {
        User user = currentAccountProvider.getUser();
        final long deleted = getDB().delete(
            ProviderTableMeta.CONTENT_URI_UPLOADS,
            ProviderTableMeta.UPLOADS_STATUS + EQUAL + UploadStatus.UPLOAD_SUCCEEDED.value + AND +
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME + IS_EQUAL, new String[]{user.getAccountName()}
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
                upload.getUser().getAccountName(),
                upload.getRemotePath()
                        );
        } else {
            String localPath = (FileUploadWorker.LOCAL_BEHAVIOUR_MOVE == upload.getLocalBehaviour())
                ? upload.getStoragePath() : null;

            if (uploadResult.isSuccess()) {
                updateUploadStatus(
                    upload.getOCUploadId(),
                    UploadStatus.UPLOAD_SUCCEEDED,
                    UploadResult.UPLOADED,
                    upload.getRemotePath(),
                    localPath
                                  );
            } else if (uploadResult.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT &&
                new FileUploadHelper().isSameFileOnRemote(
                    upload.getUser(), new File(upload.getStoragePath()), upload.getRemotePath(), upload.getContext())) {

                updateUploadStatus(
                    upload.getOCUploadId(),
                    UploadStatus.UPLOAD_SUCCEEDED,
                    UploadResult.SAME_FILE_CONFLICT,
                    upload.getRemotePath(),
                    localPath
                                  );
            } else if (uploadResult.getCode() == RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND) {
                updateUploadStatus(
                    upload.getOCUploadId(),
                    UploadStatus.UPLOAD_SUCCEEDED,
                    UploadResult.FILE_NOT_FOUND,
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
        String localPath = (FileUploadWorker.LOCAL_BEHAVIOUR_MOVE == upload.getLocalBehaviour())
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
     * Changes the status of any in progress upload from UploadStatus.UPLOAD_IN_PROGRESS to UploadStatus.UPLOAD_FAILED
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
        UPLOAD_SUCCEEDED(2),

        /**
         * Upload was cancelled by the user.
         */
        UPLOAD_CANCELLED(3);

        private final int value;

        UploadStatus(int value) {
            this.value = value;
        }

        public static UploadStatus fromValue(int value) {
            return switch (value) {
                case 0 -> UPLOAD_IN_PROGRESS;
                case 1 -> UPLOAD_FAILED;
                case 2 -> UPLOAD_SUCCEEDED;
                case 3 -> UPLOAD_CANCELLED;
                default -> null;
            };
        }

        public int getValue() {
            return value;
        }

    }
}
