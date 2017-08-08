/**
 * Nextcloud Android client application
 *
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.HashSet;
import java.util.Set;

public class FilesystemDataProvider {

    static private final String TAG = FilesystemDataProvider.class.getSimpleName();

    private ContentResolver contentResolver;

    public FilesystemDataProvider(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        this.contentResolver = contentResolver;
    }

    public int updateFilesInList(Object[] paths) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD, 1);

        String[] stringPaths = new String[paths.length];
        for(int i = 0; i < paths.length; i++) {
            stringPaths[i] = (String) paths[i];
        }

        int result = contentResolver.update(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
                cv,
                ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH + "IN (?)",
                stringPaths
        );

        return result;

    }

    public Object[] getFilesToUploadForPath(String localPath) {
        Set<String> localPathsToUpload = new HashSet<>();

        String likeParam = localPath + "%";

        Cursor cursor = contentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
                null,
                ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH + " LIKE ? and " +
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_BEING_MODIFIED + " = ? and " +
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD + " = ? and " +
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER + " = ?",
                new String[]{likeParam, "0", "0", "0"},
                null);

        if (cursor.moveToFirst()) {
            do {
                String value = cursor.getString(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH));
                if (value == null) {
                    Log_OC.e(TAG, "Cannot get local path");
                } else {
                    localPathsToUpload.add(value);
                }
            } while (cursor.moveToNext());
        }


        cursor.close();
        return localPathsToUpload.toArray();

    }

    public long countFilesThatNeedUploadInFolder(String localPath) {
        String likeParam = localPath + "%";

        Cursor cursor = contentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
                new String[]{"count(*)"},
                ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH + " LIKE ?",
                new String[]{likeParam},
                null);

        if (cursor.getCount() == 0) {
            cursor.close();
            return 0;
        } else {
            cursor.moveToFirst();
            int result = cursor.getInt(0);
            cursor.close();
            return result;
        }
    }

    public void storeOrUpdateFileValue(String localPath, long modifiedAt, boolean isFolder, boolean fileBeingModified) {
        FileSystemDataSet data = getFilesystemDataSet(localPath);

        int isFolderValue = 0;
        if (isFolder) {
            isFolderValue = 1;
        }

        int isBeingModified = 0;
        if (fileBeingModified) {
            isBeingModified = 1;
        }

        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_FOUND_RECENTLY, System.currentTimeMillis());
        cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_BEING_MODIFIED, isBeingModified);

        if (data == null) {

            cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH, localPath);
            cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_MODIFIED, modifiedAt);
            cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER, isFolderValue);
            cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD, 0);


            Uri result = contentResolver.insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM, cv);

            if (result == null) {
                Log_OC.v(TAG, "Failed to insert filesystem data with local path: " + localPath);
            }
        } else {

            if (data.getModifiedAt() != modifiedAt) {
                cv.put(ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD, 0);
            }

            int result = contentResolver.update(
                    ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
                    cv,
                    ProviderMeta.ProviderTableMeta._ID + "=?",
                    new String[]{String.valueOf(data.getId())}
            );

            if (result == 0) {
                Log_OC.v(TAG, "Failed to update filesystem data with local path: " + localPath);
            }
        }
    }

    private FileSystemDataSet getFilesystemDataSet(String localPathParam) {
        Cursor cursor = contentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_FILESYSTEM,
                null,
                ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH + " = ?",
                new String[]{localPathParam},
                null
        );

        FileSystemDataSet dataSet = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta._ID));
                String localPath = cursor.getString(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH));
                long modifiedAt = cursor.getLong(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_MODIFIED));
                boolean isFolder = false;
                if (cursor.getInt(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_MODIFIED)) != 0) {
                    isFolder = true;
                }
                long foundAt = cursor.getLong(cursor.getColumnIndex(ProviderMeta.
                        ProviderTableMeta.FILESYSTEM_FILE_FOUND_RECENTLY));

                boolean isSentForUpload = false;
                if (cursor.getInt(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD)) != 0) {
                    isSentForUpload = true;
                }

                boolean isBeingModified = false;
                if (cursor.getInt(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_BEING_MODIFIED)) != 0) {
                    isBeingModified = true;
                }


                if (id == -1) {
                    Log_OC.e(TAG, "Arbitrary value could not be created from cursor");
                } else {
                    dataSet = new FileSystemDataSet(id, localPath, modifiedAt, isFolder, isSentForUpload, foundAt,
                            isBeingModified);
                }
            }
            cursor.close();
        } else {
            Log_OC.e(TAG, "DB error restoring arbitrary values.");
        }

        return dataSet;
    }
}
