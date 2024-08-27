/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nextcloud.client.database.entity.OfflineOperationEntity

@Dao
interface OfflineOperationDao {
    @Query("SELECT * FROM offline_operations")
    fun getAll(): List<OfflineOperationEntity>

    @Insert
    fun insert(vararg entity: OfflineOperationEntity)

    @Update
    fun update(entity: OfflineOperationEntity)

    @Delete
    fun delete(entity: OfflineOperationEntity)

    @Query("DELETE FROM offline_operations WHERE offline_operations_path = :path")
    fun deleteByPath(path: String)

    @Query("SELECT * FROM offline_operations WHERE offline_operations_path = :path LIMIT 1")
    fun getByPath(path: String): OfflineOperationEntity?

    @Query("SELECT * FROM offline_operations WHERE offline_operations_path LIKE '%' || :path || '%' OR offline_operations_file_name LIKE '%' || :filename || '%'")
    fun getSubDirs(path: String, filename: String): List<OfflineOperationEntity>
}
