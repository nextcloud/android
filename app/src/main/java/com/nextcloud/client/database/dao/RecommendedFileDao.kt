/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nextcloud.client.database.entity.RecommendedFileEntity
import com.owncloud.android.db.ProviderMeta

@Dao
interface RecommendedFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recommendedFiles: List<RecommendedFileEntity>)

    @Query("SELECT * FROM ${ProviderMeta.ProviderTableMeta.RECOMMENDED_FILE_TABLE_NAME} WHERE account_name = :accountName")
    suspend fun getAll(accountName: String): List<RecommendedFileEntity>
}
