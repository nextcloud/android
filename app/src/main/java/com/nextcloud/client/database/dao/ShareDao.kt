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
import com.nextcloud.client.database.entity.ShareEntity
import com.nextcloud.client.database.entity.model.ShareeKey

@Dao
interface ShareDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shares: List<ShareEntity>)

    @Query("DELETE FROM ocshares WHERE owner_share = :accountName")
    suspend fun clearSharesForAccount(accountName: String)

    @Query(
        "SELECT path, shate_with, share_type FROM ocshares " +
            "WHERE path IN (:paths) AND owner_share = :accountName AND share_type IN (:shareTypes)"
    )
    fun getShareeKeys(paths: List<String>, accountName: String, shareTypes: List<Int>): List<ShareeKey>
}
