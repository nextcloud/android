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
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nextcloud.client.database.entity.OfflineOperationEntity

@Dao
interface OfflineOperationDao {
    @Query("SELECT * FROM offline_operations")
    fun getAll(): List<OfflineOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg entity: OfflineOperationEntity)

    @Update
    fun update(entity: OfflineOperationEntity)

    @Delete
    fun delete(entity: OfflineOperationEntity)

    @Query("DELETE FROM offline_operations WHERE offline_operations_path = :path")
    fun deleteByPath(path: String)

    @Query("SELECT * FROM offline_operations WHERE offline_operations_path = :path LIMIT 1")
    fun getByPath(path: String): OfflineOperationEntity?

    @Query("SELECT * FROM offline_operations WHERE offline_operations_parent_oc_file_id = :parentOCFileId")
    fun getSubEntitiesByParentOCFileId(parentOCFileId: Long): List<OfflineOperationEntity>

    @Query("DELETE FROM offline_operations")
    fun clearTable()

    @Query("DELETE FROM offline_operations WHERE _id = :id")
    fun deleteById(id: Int)
}
