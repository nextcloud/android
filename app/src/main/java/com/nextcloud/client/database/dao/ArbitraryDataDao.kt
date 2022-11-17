/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
