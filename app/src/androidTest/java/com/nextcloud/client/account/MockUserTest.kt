/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Chris Narkiewicz
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
package com.nextcloud.client.account

import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MockUserTest {

    private companion object {
        const val ACCOUNT_NAME = "test_account_name"
        const val ACCOUNT_TYPE = "test_account_type"
    }

    @Test
    fun mock_user_is_parcelable() {
        // GIVEN
        //      mock user instance
        val original = MockUser(ACCOUNT_NAME, ACCOUNT_TYPE)

        // WHEN
        //      instance is serialized into Parcel
        //      instance is retrieved from Parcel
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)
        parcel.writeParcelable(original, 0)
        parcel.setDataPosition(0)
        val retrieved = parcel.readParcelable<User>(User::class.java.classLoader)

        // THEN
        //      retrieved instance in distinct
        //      instances are equal
        assertNotSame(original, retrieved)
        assertTrue(retrieved is MockUser)
        assertEquals(original, retrieved)
    }

    @Test
    fun mock_user_has_platform_account() {
        // GIVEN
        //      mock user instance
        val mock = MockUser(ACCOUNT_NAME, ACCOUNT_TYPE)

        // THEN
        //      can convert to platform account
        val account = mock.toPlatformAccount()
        assertEquals(ACCOUNT_NAME, account.name)
        assertEquals(ACCOUNT_TYPE, account.type)
    }
}
