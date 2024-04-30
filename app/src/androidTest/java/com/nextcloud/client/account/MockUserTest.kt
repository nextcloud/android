/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
