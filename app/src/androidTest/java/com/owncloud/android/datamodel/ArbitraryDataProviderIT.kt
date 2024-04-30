/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import com.owncloud.android.AbstractIT
import org.junit.Assert.assertEquals
import org.junit.Test

class ArbitraryDataProviderIT : AbstractIT() {

    @Test
    fun testEmpty() {
        val key = "DUMMY_KEY"
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, key, "")

        assertEquals("", arbitraryDataProvider.getValue(user.accountName, key))
    }

    @Test
    fun testString() {
        val key = "DUMMY_KEY"
        var value = "123"
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, key, value)
        assertEquals(value, arbitraryDataProvider.getValue(user.accountName, key))

        value = ""
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, key, value)
        assertEquals(value, arbitraryDataProvider.getValue(user.accountName, key))

        value = "-1"
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, key, value)
        assertEquals(value, arbitraryDataProvider.getValue(user.accountName, key))
    }

    @Test
    fun testBoolean() {
        val key = "DUMMY_KEY"
        var value = true
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, key, value)
        assertEquals(value, arbitraryDataProvider.getBooleanValue(user.accountName, key))

        value = false
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, key, value)
        assertEquals(value, arbitraryDataProvider.getBooleanValue(user.accountName, key))
    }

    @Test
    fun testInteger() {
        val key = "DUMMY_KEY"
        var value = 1
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, key, value.toString())
        assertEquals(value, arbitraryDataProvider.getIntegerValue(user.accountName, key))

        value = -1
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, key, value.toString())
        assertEquals(value, arbitraryDataProvider.getIntegerValue(user.accountName, key))
    }

    @Test
    fun testIncrement() {
        val key = "INCREMENT"

        // key does not exist
        assertEquals(-1, arbitraryDataProvider.getIntegerValue(user.accountName, key))

        // increment -> 1
        arbitraryDataProvider.incrementValue(user.accountName, key)
        assertEquals(1, arbitraryDataProvider.getIntegerValue(user.accountName, key))

        // increment -> 2
        arbitraryDataProvider.incrementValue(user.accountName, key)
        assertEquals(2, arbitraryDataProvider.getIntegerValue(user.accountName, key))

        // delete
        arbitraryDataProvider.deleteKeyForAccount(user.accountName, key)
        assertEquals(-1, arbitraryDataProvider.getIntegerValue(user.accountName, key))
    }
}
