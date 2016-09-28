/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Database provider for handling the persistence aspects of synced folders.
 */
public class SyncedFolderProvider {
    static private final String TAG = SyncedFolderProvider.class.getSimpleName();

    private ContentResolver mContentResolver;

    public SyncedFolderProvider(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores an media folder sync object in database.
     *
     * @param syncedFolder synced folder to store
     * @return upload id, -1 if the insert process fails.
     */
    public long storeFolderSync(SyncedFolder syncedFolder) {
        Log_OC.v(TAG, "Inserting " + syncedFolder.getLocalPath() + " with enabled=" + syncedFolder.isEnabled());

        ContentValues cv = createContentValuesFromSyncedFolder(syncedFolder);

        Uri result = mContentResolver.insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS, cv);

        if (result != null) {
            return Long.parseLong(result.getPathSegments().get(1));
        } else {
            Log_OC.e(TAG, "Failed to insert item " + syncedFolder.getLocalPath() + " into folder sync db.");
            return -1;
        }
    }

    /**
     * Update upload status of file uniquely referenced by id.
     *
     * @param id      folder sync id.
     * @param enabled new status.
     * @return the number of rows updated.
     */
    public int updateFolderSyncEnabled(long id, Boolean enabled) {
        Log_OC.v(TAG, "Storing sync folder id" + id + " with enabled=" + enabled);

        int result = 0;
        Cursor cursor = mContentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                null,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (cursor == null || cursor.getCount() != 1) {
            Log_OC.e(TAG, cursor.getCount() + " items for id=" + id + " available in UploadDb. Expected 1. Failed to " +
                    "update upload db.");
        } else {
            while (cursor.moveToNext()) {
                // read sync folder object and update
                SyncedFolder syncedFolder = createSyncedFolderFromCursor(cursor);

                syncedFolder.setEnabled(enabled);

                // update sync folder object in db
                result = updateSyncFolder(syncedFolder);
            }
        }
        cursor.close();
        return result;
    }

    public SyncedFolder findByLocalPath(String localPath) {
        Cursor cursor = mContentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                null,
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH + "==" + localPath,
                null,
                null
        );

        if (cursor == null || cursor.getCount() != 1) {
            Log_OC.e(TAG, cursor.getCount() + " items for local path=" + localPath + " available in sync folder db. " +
                    "Expected 1. Failed to update sync folder db.");
            return null;
        } else {
            return createSyncedFolderFromCursor(cursor);
        }
    }

    private int updateSyncFolder(SyncedFolder syncedFolder) {
        Log_OC.v(TAG, "Updating " + syncedFolder.getLocalPath() + " with enabled=" + syncedFolder.isEnabled());

        ContentValues cv = createContentValuesFromSyncedFolder(syncedFolder);

        return mContentResolver.update(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_UPLOADS,
                cv,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(syncedFolder.getId())}
        );
    }

    private SyncedFolder createSyncedFolderFromCursor(Cursor cursor) {
        SyncedFolder syncedFolder = null;
        if (cursor != null) {
            long id = cursor.getLong(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta._ID));
            String localPath = cursor.getString(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH));
            String remotePath = cursor.getString(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH));
            Boolean wifiOnly = cursor.getInt(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY)) == 1;
            Boolean chargingOnly = cursor.getInt(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY)) == 1;
            Boolean subfolderByDate = cursor.getInt(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE)) == 1;
            String accountName = cursor.getString(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT));
            Integer uploadAction = cursor.getInt(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION));
            Boolean enabled = cursor.getInt(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED)) == 1;

            syncedFolder = new SyncedFolder(id, localPath, remotePath, wifiOnly, chargingOnly, subfolderByDate,
                    accountName, uploadAction, enabled);
        }
        return syncedFolder;
    }

    @NonNull
    private ContentValues createContentValuesFromSyncedFolder(SyncedFolder syncedFolder) {
        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH, syncedFolder.getLocalPath());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH, syncedFolder.getRemotePath());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY, syncedFolder.getWifiOnly());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY, syncedFolder.getChargingOnly());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED, syncedFolder.isEnabled());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE, syncedFolder.getSubfolderByDate());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT, syncedFolder.getAccount());
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION, syncedFolder.getUploadAction());
        return cv;
    }
}
