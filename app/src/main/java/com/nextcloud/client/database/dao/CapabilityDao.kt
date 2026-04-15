/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.nextcloud.client.database.entity.CapabilityEntity

@Dao
interface CapabilityDao {

    @Query("SELECT * FROM capabilities WHERE account = :accountName LIMIT 1")
    suspend fun getByAccountName(accountName: String): CapabilityEntity?
}
