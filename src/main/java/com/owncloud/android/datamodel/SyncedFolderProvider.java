/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud.
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

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Database provider for handling the persistence aspects of {@link SyncedFolder}s.
 */
public class SyncedFolderProvider extends Observable {
    static private final String TAG = SyncedFolderProvider.class.getSimpleName();

    private ContentResolver mContentResolver;

    /**
     * constructor.
     *
     * @param contentResolver the ContentResolver to work with.
     */
    public SyncedFolderProvider(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        mContentResolver = contentResolver;
    }

    /**
     * Stores a synced folder object in database.
     *
     * @param syncedFolder synced folder to store
     * @return synced folder id, -1 if the insert process fails.
     */
    public long storeSyncedFolder(SyncedFolder syncedFolder) {
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
     * get all synced folder entries.
     *
     * @return all synced folder entries, empty if none have been found
     */
    public List<SyncedFolder> getSyncedFolders() {
        Cursor cursor = mContentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                null,
                "1=1",
                null,
                null
        );

        if (cursor != null) {
            List<SyncedFolder> list = new ArrayList<>(cursor.getCount());
            if (cursor.moveToFirst()) {
                do {
                    SyncedFolder syncedFolder = createSyncedFolderFromCursor(cursor);
                    if (syncedFolder == null) {
                        Log_OC.e(TAG, "SyncedFolder could not be created from cursor");
                    } else {
                        list.add(cursor.getPosition(), syncedFolder);
                    }
                } while (cursor.moveToNext());

            }
            cursor.close();
            return list;
        } else {
            Log_OC.e(TAG, "DB error creating read all cursor for synced folders.");
        }

        return new ArrayList<>(0);
    }

    /**
     * Update upload status of file uniquely referenced by id.
     *
     * @param id      synced folder id.
     * @param enabled new status.
     * @return the number of rows updated.
     */
    public int updateSyncedFolderEnabled(long id, Boolean enabled) {
        Log_OC.v(TAG, "Storing synced folder id" + id + " with enabled=" + enabled);

        int result = 0;
        Cursor cursor = mContentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                null,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(id)},
                null
        );

        if (cursor != null && cursor.getCount() == 1) {
            while (cursor.moveToNext()) {
                // read sync folder object and update
                SyncedFolder syncedFolder = createSyncedFolderFromCursor(cursor);

                syncedFolder.setEnabled(enabled);

                // update sync folder object in db
                result = updateSyncFolder(syncedFolder);

            }
        } else {
            if (cursor == null) {
                Log_OC.e(TAG, "Sync folder db cursor for ID=" + id + " in NULL.");
            } else {
                Log_OC.e(TAG, cursor.getCount() + " items for id=" + id + " available in sync folder database. " +
                        "Expected 1. Failed to update sync folder db.");
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    /**
     * find a synced folder by local path.
     *
     * @param localPath the local path of the local folder
     * @return the synced folder if found, else null
     */
    public SyncedFolder findByLocalPath(String localPath) {
        SyncedFolder result = null;
        Cursor cursor = mContentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                null,
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH + "== \"" + localPath + "\"",
                null,
                null
        );

        if (cursor != null && cursor.getCount() == 1) {
            result = createSyncedFolderFromCursor(cursor);
        } else {
            if (cursor == null) {
                Log_OC.e(TAG, "Sync folder db cursor for local path=" + localPath + " in NULL.");
            } else {
                Log_OC.e(TAG, cursor.getCount() + " items for local path=" + localPath
                        + " available in sync folder db. Expected 1. Failed to update sync folder db.");
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    /**
     *  Delete all synced folders for an account
     *
     *  @param account whose synced folders should be deleted
     */
    public int deleteSyncFoldersForAccount(Account account) {
        int result = mContentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT + " = ?",
                new String[]{String.valueOf(account.name)}
        );

        return result;

    }

    /**
     * Delete a synced folder from the db
     *
     * @param id for the synced folder.
     */
    private int deleteSyncFolderWithId(long id) {
        return mContentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                ProviderMeta.ProviderTableMeta._ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }


    /**
     * Try to figure out if a path exists for synced folder, and if not, go one folder back
     * Otherwise, delete the entry
     *
     * @param context the context.
     */
    public void updateAutoUploadPaths(Context context) {
        List<SyncedFolder> syncedFolders = getSyncedFolders();
        for (int i = 0; i < syncedFolders.size(); i++) {
            SyncedFolder syncedFolder = syncedFolders.get(i);
            if (!new File(syncedFolder.getLocalPath()).exists()) {
                String localPath = syncedFolder.getLocalPath();
                if (localPath.endsWith("/")) {
                    localPath = localPath.substring(0, localPath.lastIndexOf("/"));
                }
                localPath = localPath.substring(0, localPath.lastIndexOf("/"));
                if (new File(localPath).exists()) {
                    syncedFolders.get(i).setLocalPath(localPath);
                    updateSyncFolder(syncedFolder);
                } else {
                    deleteSyncFolderWithId(syncedFolder.getId());
                }
            }
        }

        if (context != null) {
            PreferenceManager.setAutoUploadPathsUpdate(context, true);
        }
    }

    /**
     * delete any records of synchronized folders that are not within the given list of ids.
     *
     * @param context the context.
     * @param ids     the list of ids to be excluded from deletion.
     * @return number of deleted records.
     */
    public int deleteSyncedFoldersNotInList(Context context, ArrayList<Long> ids) {
        int result = mContentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                ProviderMeta.ProviderTableMeta._ID + " NOT IN (?)",
                new String[]{String.valueOf(ids)}
        );

        if (result > 0 && context != null) {
            PreferenceManager.setLegacyClean(context, true);
        }

        return result;
    }

    /**
     * delete record of synchronized folder with the given id.
     */
    public int deleteSyncedFolder(long id) {
        return mContentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                ProviderMeta.ProviderTableMeta._ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    /**
     * update given synced folder.
     *
     * @param syncedFolder the synced folder to be updated.
     * @return the number of rows updated.
     */
    public int updateSyncFolder(SyncedFolder syncedFolder) {
        Log_OC.v(TAG, "Updating " + syncedFolder.getLocalPath() + " with enabled=" + syncedFolder.isEnabled());

        ContentValues cv = createContentValuesFromSyncedFolder(syncedFolder);

        int result = mContentResolver.update(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                cv,
                ProviderMeta.ProviderTableMeta._ID + "=?",
                new String[]{String.valueOf(syncedFolder.getId())}
        );

        return result;
    }

    /**
     * maps a cursor into a SyncedFolder object.
     *
     * @param cursor the db cursor
     * @return the mapped SyncedFolder, null if cursor is null
     */
    private SyncedFolder createSyncedFolderFromCursor(Cursor cursor) {
        SyncedFolder syncedFolder = null;
        if (cursor != null) {
            long id = cursor.getLong(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta._ID));
            String localPath = cursor.getString(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH));
            String remotePath = cursor.getString(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH));
            Boolean wifiOnly = cursor.getInt(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY)) == 1;
            Boolean chargingOnly = cursor.getInt(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY)) == 1;
            Boolean subfolderByDate = cursor.getInt(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE)) == 1;
            String accountName = cursor.getString(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT));
            Integer uploadAction = cursor.getInt(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION));
            Boolean enabled = cursor.getInt(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED)) == 1;
            MediaFolderType type = MediaFolderType.getById(cursor.getInt(cursor.getColumnIndex(
                    ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_TYPE)));

            syncedFolder = new SyncedFolder(id, localPath, remotePath, wifiOnly, chargingOnly, subfolderByDate,
                    accountName, uploadAction, enabled, type);
        }
        return syncedFolder;
    }

    /**
     * create ContentValues object based on given SyncedFolder.
     *
     * @param syncedFolder the synced folder
     * @return the corresponding ContentValues object
     */
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
        cv.put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_TYPE, syncedFolder.getType().getId());

        return cv;
    }
}
