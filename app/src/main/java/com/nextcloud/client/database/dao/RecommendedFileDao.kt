/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nextcloud.client.database.entity.RecommendedFileEntity
import com.owncloud.android.db.ProviderMeta

@Dao
interface RecommendedFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recommendedFile: RecommendedFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recommendedFiles: List<RecommendedFileEntity>)

    @Update
    suspend fun update(recommendedFile: RecommendedFileEntity)

    @Delete
    suspend fun delete(recommendedFile: RecommendedFileEntity)

    @Query("SELECT * FROM ${ProviderMeta.ProviderTableMeta.RECOMMENDED_FILE_TABLE_NAME}")
    suspend fun getAll(): List<RecommendedFileEntity>

    @Query("DELETE FROM ${ProviderMeta.ProviderTableMeta.RECOMMENDED_FILE_TABLE_NAME}")
    suspend fun deleteAll()
}
