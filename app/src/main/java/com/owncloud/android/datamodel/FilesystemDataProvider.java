/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

/**
 * Provider for stored filesystem data.
 */
public class FilesystemDataProvider {

    static private final String TAG = FilesystemDataProvider.class.getSimpleName();

    private final ContentResolver contentResolver;

    public FilesystemDataProvider(ContentResolver contentResolver) {
        if (contentResolver == null) {
            Log_OC.e(TAG, "couldn't be able constructed, contentResolver is null");
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        this.contentResolver = contentResolver;
    }

    public int deleteAllEntriesForSyncedFolder(String syncedFolderId) {
        Log_OC.d(TAG, "deleteAllEntriesForSyncedFolder called, ID: " + syncedFolderId);

        return contentResolver.delete(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
            ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID + " = ?",
            new String[]{syncedFolderId});
    }

    public void storeOrUpdateFileValue(String localPath, long modifiedAt, boolean isFolder, SyncedFolder syncedFolder) {
        Log_OC.d(TAG, "storeOrUpdateFileValue called, localPath: " + localPath + " ID: " + syncedFolder.getId());

        // takes multiple milliseconds to query data from database (around 75% of execution time) (6ms)
        FileSystemDataSet data = getFilesystemDataSet(localPath, syncedFolder);

        int isFolderValue = 0;
        if (isFolder) {
            isFolderValue = 1;
        }

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_FOUND_RECENTLY, System.currentTimeMillis());
        cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_MODIFIED, modifiedAt);

        if (data == null) {
            Log_OC.d(TAG, "storeOrUpdateFileValue data is null");

            cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH, localPath);
            cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER, isFolderValue);
            cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD, Boolean.FALSE);
            cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID, syncedFolder.getId());

            long newCrc32 = getFileChecksum(localPath);
            if (newCrc32 != -1) {
                cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32, Long.toString(newCrc32));
            }

            Uri result = contentResolver.insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM, cv);

            if (result == null) {
                Log_OC.e(TAG, "Failed to insert filesystem data with local path: " + localPath);
            }
        } else {
            Log_OC.d(TAG, "storeOrUpdateFileValue data is not null");

            if (data.getModifiedAt() != modifiedAt) {
                long newCrc32 = getFileChecksum(localPath);
                if (data.getCrc32() == null || (newCrc32 != -1 && !data.getCrc32().equals(Long.toString(newCrc32)))) {
                    cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32, Long.toString(newCrc32));
                    cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD, 0);
                }
            }

            // updating data takes multiple milliseconds (around 25% of exec time) (2 ms)
            int result = contentResolver.update(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
                cv,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(data.getId())}
                                               );

            if (result == 0) {
                Log_OC.e(TAG, "Failed to update filesystem data with local path: " + localPath);
            }
        }
    }

    private FileSystemDataSet getFilesystemDataSet(String localPathParam, SyncedFolder syncedFolder) {
        Log_OC.d(TAG, "getFilesForUpload called, localPath: " + localPathParam + " ID: " + syncedFolder.getId());

        String[] projection = {
            ProviderMeta.ProviderTableMeta._ID,
            ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH,
            ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_MODIFIED,
            ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER,
            ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_FOUND_RECENTLY,
            ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD,
            ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32
        };

        String selection = ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH + " = ? AND " +
            ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID + " = ?";
        String[] selectionArgs = { localPathParam, String.valueOf(syncedFolder.getId()) };

        try (Cursor cursor = contentResolver.query(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
            projection,
            selection,
            selectionArgs,
            null
                                                  )) {
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(ProviderMeta.ProviderTableMeta._ID));
                if (id == -1) {
                    Log_OC.e(TAG, "Arbitrary value could not be created from cursor");
                    return null;
                }

                String localPath = cursor.getString(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH));
                long modifiedAt = cursor.getLong(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_MODIFIED));
                boolean isFolder = cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER)) != 0;
                long foundAt = cursor.getLong(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_FOUND_RECENTLY));
                boolean isSentForUpload = cursor.getInt(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD)) != 0;
                String crc32 = cursor.getString(cursor.getColumnIndexOrThrow(
                    ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32));

                return new FileSystemDataSet(id, localPath, modifiedAt, isFolder, isSentForUpload, foundAt,
                                             syncedFolder.getId(), crc32);
            }
        } catch (Exception e) {
            Log_OC.e(TAG, "DB error restoring arbitrary values.", e);
        }

        return null;
    }

    private long getFileChecksum(String filepath) {

        try (FileInputStream fileInputStream = new FileInputStream(filepath);
            InputStream inputStream = new BufferedInputStream(fileInputStream)) {
            CRC32 crc = new CRC32();
            byte[] buf = new byte[1024 * 64];
            int size;
            while ((size = inputStream.read(buf)) > 0) {
                crc.update(buf, 0, size);
            }

            return crc.getValue();

        } catch (IOException e) {
            return -1;
        }
    }
}
