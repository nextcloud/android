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
package com.owncloud.android.datamodel

import com.nextcloud.client.account.User

@Suppress("Detekt.TooManyFunctions") // legacy interface, will get rid of `accountName` methods in the future
interface ArbitraryDataProvider {
    fun deleteKeyForAccount(account: String, key: String)

    fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: Long)
    fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: Boolean)
    fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: String)

    fun getLongValue(accountName: String, key: String): Long
    fun getLongValue(user: User, key: String): Long
    fun getBooleanValue(accountName: String, key: String): Boolean
    fun getBooleanValue(user: User, key: String): Boolean
    fun getIntegerValue(accountName: String, key: String): Int
    fun getValue(user: User?, key: String): String
    fun getValue(accountName: String, key: String): String

    companion object {
        const val DIRECT_EDITING = "DIRECT_EDITING"
        const val DIRECT_EDITING_ETAG = "DIRECT_EDITING_ETAG"
        const val PREDEFINED_STATUS = "PREDEFINED_STATUS"
    }
}
