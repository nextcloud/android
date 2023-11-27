/*
 * Nextcloud Android client application
 *
 *  @author Dariusz Olszewski
 *  Copyright (C) 2023 Dariusz Olszewski
 *  Copyright (C) 2023 Nextcloud GmbH
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
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nextcloud.client.database.entity.ShareEntity

@Dao
interface ShareDao {
    @Query("SELECT * FROM ocshares WHERE id_remote_shared = :remoteId AND owner_share = :shareOwner LIMIT 1")
    fun getShareByRemoteId(remoteId: Long, shareOwner: String): ShareEntity?

    @Insert
    fun insertShare(share: ShareEntity): Long

    @Update
    fun updateShare(share: ShareEntity)

    @Query("DELETE FROM ocshares WHERE _id = :shareId AND owner_share = :shareOwner")
    fun deleteShareById(shareId: Long, shareOwner: String)

    @Query("DELETE FROM ocshares WHERE owner_share = :shareOwner")
    fun deleteShares(shareOwner: String)

    @Query("DELETE FROM ocshares WHERE path = :path AND owner_share = :shareOwner")
    fun deleteSharesByPath(path: String, shareOwner: String)
}
