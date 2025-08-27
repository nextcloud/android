/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.nextcloud.client.database.entity.UploadEntity
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Dao
interface UploadDao {
    @Query(
        "SELECT _id FROM " + ProviderTableMeta.UPLOADS_TABLE_NAME +
            " WHERE " + ProviderTableMeta.UPLOADS_STATUS + " = :status AND " +
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + " = :accountName AND _id IS NOT NULL"
    )
    fun getAllIds(status: Int, accountName: String): List<Int>

    @Query(
        "SELECT * FROM " + ProviderTableMeta.UPLOADS_TABLE_NAME +
            " WHERE " + ProviderTableMeta._ID + " IN (:ids) AND " +
            ProviderTableMeta.UPLOADS_ACCOUNT_NAME + " = :accountName"
    )
    fun getUploadsByIds(ids: LongArray, accountName: String): List<UploadEntity>
}
