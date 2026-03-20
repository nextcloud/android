/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.nextcloud.client.account.User
import com.nextcloud.client.core.Clock
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.dao.SyncedFolderDao
import com.nextcloud.client.database.entity.toSyncedFolder
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.datamodel.MediaFolderType.Companion.getById
import com.owncloud.android.datamodel.SyncedFolderObserver.start
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import java.util.Observable

class SyncedFolderProvider(
    contentResolver: ContentResolver,
    @JvmField val preferences: AppPreferences,
    private val clock: Clock
) : Observable() {

    companion object {
        private val TAG: String = SyncedFolderProvider::class.java.simpleName
    }

    private val resolver: ContentResolver = contentResolver
    private val dao: SyncedFolderDao = NextcloudDatabase.instance().syncedFolderDao()

    init {
        start(dao)
    }

    fun storeSyncedFolder(syncedFolder: SyncedFolder): Long {
        Log_OC.v(TAG, "Inserting ${syncedFolder.localPath} with enabled=${syncedFolder.isEnabled}")
        return resolver.insert(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            createContentValuesFromSyncedFolder(syncedFolder)
        )
            ?.pathSegments?.get(1)?.toLong()
            ?: run {
                Log_OC.e(TAG, "Failed to insert item ${syncedFolder.localPath} into folder sync db.")
                -1L
            }
    }

    fun countEnabledSyncedFolders(): Int = resolver.query(
        ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
        null,
        "${ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED} = ?",
        arrayOf("1"),
        null
    )?.use { it.count } ?: 0

    val syncedFolders: MutableList<SyncedFolder>
        get() = resolver.query(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            ArrayList<SyncedFolder>(cursor.count).also {
                while (cursor.moveToNext()) it.add(createSyncedFolderFromCursor(cursor))
            }
        } ?: run {
            Log_OC.e(TAG, "DB error creating read all cursor for synced folders.")
            ArrayList(0)
        }

    fun updateSyncedFolderEnabled(id: Long, enabled: Boolean): Int {
        Log_OC.v(TAG, "Storing synced folder id$id with enabled=$enabled")
        return resolver.query(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            null,
            "${ProviderMeta.ProviderTableMeta._ID}=?",
            arrayOf(id.toString()),
            null
        )?.use { cursor ->
            if (cursor.count == 1 && cursor.moveToNext()) {
                val syncedFolder = createSyncedFolderFromCursor(cursor)
                syncedFolder.setEnabled(enabled, clock.currentTime)
                updateSyncFolder(syncedFolder)
            } else {
                Log_OC.e(
                    TAG,
                    "${cursor.count} items for id=$id available in sync folder database. " +
                        "Expected 1. Failed to update sync folder db."
                )
                0
            }
        } ?: run {
            Log_OC.e(TAG, "Sync folder db cursor for ID=$id in NULL.")
            0
        }
    }

    fun findByLocalPathAndAccount(localPath: String, user: User): SyncedFolder? =
        dao.findByLocalPathAndAccount(localPath, user.accountName)?.toSyncedFolder()

    fun getSyncedFolderByID(syncedFolderID: Long): SyncedFolder? = resolver.query(
        ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
        null,
        "${ProviderMeta.ProviderTableMeta._ID} =?",
        arrayOf(syncedFolderID.toString()),
        null
    )?.use { cursor ->
        if (cursor.count == 1 && cursor.moveToFirst()) createSyncedFolderFromCursor(cursor) else null
    }

    fun deleteSyncFoldersForAccount(user: User): Int = resolver.delete(
        ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
        "${ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT} = ?",
        arrayOf(user.accountName)
    )

    private fun deleteSyncFolderWithId(id: Long) {
        resolver.delete(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            "${ProviderMeta.ProviderTableMeta._ID} = ?",
            arrayOf(id.toString())
        )
    }

    fun updateAutoUploadPaths(context: Context?) {
        for (syncedFolder in syncedFolders) {
            if (!File(syncedFolder.localPath).exists()) {
                var localPath = syncedFolder.localPath
                if (localPath.endsWith(OCFile.PATH_SEPARATOR)) {
                    localPath = localPath.substring(0, localPath.lastIndexOf('/'))
                }
                localPath = localPath.substring(0, localPath.lastIndexOf('/'))

                if (File(localPath).exists()) {
                    syncedFolder.localPath = localPath
                    updateSyncFolder(syncedFolder)
                } else {
                    deleteSyncFolderWithId(syncedFolder.id)
                }
            }
        }

        context?.let { AppPreferencesImpl.fromContext(it).setAutoUploadPathsUpdateEnabled(true) }
    }

    fun deleteSyncedFoldersNotInList(ids: MutableList<Long?>?): Int = resolver.delete(
        ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
        "${ProviderMeta.ProviderTableMeta._ID} NOT IN (?)",
        arrayOf(ids.toString())
    ).also { if (it > 0) preferences.setLegacyClean(true) }

    fun deleteSyncedFolder(id: Long): Int = resolver.delete(
        ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
        "${ProviderMeta.ProviderTableMeta._ID} = ?",
        arrayOf(id.toString())
    )

    fun updateSyncFolder(syncedFolder: SyncedFolder): Int {
        Log_OC.v(TAG, "Updating ${syncedFolder.localPath} with enabled=${syncedFolder.isEnabled}")
        return resolver.update(
            ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
            createContentValuesFromSyncedFolder(syncedFolder),
            "${ProviderMeta.ProviderTableMeta._ID}=?",
            arrayOf(syncedFolder.id.toString())
        )
    }

    private fun createSyncedFolderFromCursor(cursor: Cursor): SyncedFolder {
        fun str(col: String) = cursor.getString(cursor.getColumnIndexOrThrow(col))
        fun int(col: String) = cursor.getInt(cursor.getColumnIndexOrThrow(col))
        fun long(col: String) = cursor.getLong(cursor.getColumnIndexOrThrow(col))
        fun bool(col: String) = int(col) == 1

        return SyncedFolder(
            long(ProviderMeta.ProviderTableMeta._ID),
            str(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH),
            str(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH),
            bool(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY),
            bool(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY),
            bool(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXISTING),
            bool(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE),
            str(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT),
            int(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION),
            int(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY),
            bool(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED),
            long(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS),
            getById(int(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_TYPE)),
            bool(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_HIDDEN),
            SubFolderRule.entries[int(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_RULE)],
            bool(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXCLUDE_HIDDEN),
            long(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LAST_SCAN_TIMESTAMP_MS)
        )
    }

    private fun createContentValuesFromSyncedFolder(syncedFolder: SyncedFolder): ContentValues = ContentValues().apply {
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH, syncedFolder.localPath)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH, syncedFolder.remotePath)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY, syncedFolder.isWifiOnly)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY, syncedFolder.isChargingOnly)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXISTING, syncedFolder.isExisting)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED, syncedFolder.isEnabled)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS, syncedFolder.enabledTimestampMs)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE, syncedFolder.isSubfolderByDate)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT, syncedFolder.account)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION, syncedFolder.uploadAction)
        put(
            ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY,
            syncedFolder.nameCollisionPolicyInt
        )
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_TYPE, syncedFolder.type.id)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_HIDDEN, syncedFolder.isHidden)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_RULE, syncedFolder.subfolderRule.ordinal)
        put(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXCLUDE_HIDDEN, syncedFolder.isExcludeHidden)
        put(
            ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LAST_SCAN_TIMESTAMP_MS,
            syncedFolder.lastScanTimestampMs
        )
    }
}
