/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.nextcloud.client.database.entity.SyncedFolderEntity
import com.owncloud.android.db.ProviderMeta

@Dao
interface SyncedFolderDao {
    @Query(
        """
        SELECT * FROM ${ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME}
        WHERE ${ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH} = :localPath
          AND ${ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT} = :account
        LIMIT 1
        """
    )
    fun findByLocalPathAndAccount(localPath: String, account: String): SyncedFolderEntity?
}
