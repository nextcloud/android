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
import com.nextcloud.client.database.entity.AssistantEntity
import com.owncloud.android.db.ProviderMeta

@Dao
interface AssistantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssistantTask(task: AssistantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssistantTasks(tasks: List<AssistantEntity>)

    @Update
    suspend fun updateAssistantTask(task: AssistantEntity)

    @Delete
    suspend fun deleteAssistantTask(task: AssistantEntity)

    @Query(
        """
    SELECT * FROM ${ProviderMeta.ProviderTableMeta.ASSISTANT_TABLE_NAME}
    WHERE accountName = :accountName
    ORDER BY lastUpdated DESC
"""
    )
    suspend fun getAssistantTasksByAccount(accountName: String): List<AssistantEntity>
}
