/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.nextcloud.client.database.entity.ArbitraryDataEntity

@Dao
interface ArbitraryDataDao {
    @Query("INSERT INTO arbitrary_data(cloud_id, `key`, value) VALUES(:accountName, :key, :value)")
    fun insertValue(accountName: String, key: String, value: String?)

    @Query("SELECT * FROM arbitrary_data WHERE cloud_id = :accountName AND `key` = :key LIMIT 1")
    fun getByAccountAndKey(accountName: String, key: String): ArbitraryDataEntity?

    @Query("UPDATE arbitrary_data SET value = :value WHERE cloud_id = :accountName AND `key` = :key ")
    fun updateValue(accountName: String, key: String, value: String?)

    @Query("DELETE FROM arbitrary_data WHERE cloud_id = :accountName AND `key` = :key")
    fun deleteValue(accountName: String, key: String)
}
