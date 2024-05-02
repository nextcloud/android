/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import com.nextcloud.client.account.User

@Suppress("Detekt.TooManyFunctions") // legacy interface, will get rid of `accountName` methods in the future
interface ArbitraryDataProvider {
    fun deleteKeyForAccount(account: String, key: String)

    fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: Long)

    fun incrementValue(accountName: String, key: String)
    fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: Boolean)
    fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: String)
    fun storeOrUpdateKeyValue(user: User, key: String, newValue: String)

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
        const val PUBLIC_KEY = "PUBLIC_KEY_"
        const val E2E_ERRORS = "E2E_ERRORS"
        const val E2E_ERRORS_TIMESTAMP = "E2E_ERRORS_TIMESTAMP"
    }
}
