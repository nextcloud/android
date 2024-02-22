/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
