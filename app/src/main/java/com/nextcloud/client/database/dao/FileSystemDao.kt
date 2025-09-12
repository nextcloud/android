/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.owncloud.android.db.ProviderMeta

@Dao
interface FileSystemDao {

    @Query("""
        SELECT ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH}
        FROM ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
        WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
          AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD} = 0
          AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER} = 0
    """)
    suspend fun getAutoUploadFiles(syncedFolderId: String): List<String>

    @Query("""
        UPDATE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
        SET ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD} = 1
        WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH} = :localPath
          AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
    """)
    suspend fun markFileAsUploaded(localPath: String, syncedFolderId: String)
}
