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

import android.accounts.Account
import android.net.Uri
import android.os.Parcel
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudBasicCredentials
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URI

class RegisteredUserTest {

    private companion object {
        fun buildTestUser(accountName: String): RegisteredUser {
            val uri = Uri.parse("https://nextcloud.localhost")
            val credentials = OwnCloudBasicCredentials("user", "pass")
            val account = Account(accountName, "test-type")
            val ownCloudAccount = OwnCloudAccount(uri, credentials)
            val server = Server(
                uri = URI(uri.toString()),
                version = OwnCloudVersion.nextcloud_17
            )
            return RegisteredUser(
                account = account,
                ownCloudAccount = ownCloudAccount,
                server = server
            )
        }
    }

    private lateinit var user: RegisteredUser

    @Before
    fun setUp() {
        user = buildTestUser("test@nextcloud.localhost")
    }

    @Test
    fun registeredUserImplementsParcelable() {
        // GIVEN
        //      registered user instance

        // WHEN
        //      instance is serialized into Parcel
        //      instance is retrieved from Parcel
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)
        parcel.writeParcelable(user, 0)
        parcel.setDataPosition(0)
        val deserialized = parcel.readParcelable<User>(User::class.java.classLoader)

        // THEN
        //      retrieved instance in distinct
        //      instances are equal
        assertNotSame(user, deserialized)
        assertTrue(deserialized is RegisteredUser)
        assertEquals(user, deserialized)
    }

    @Test
    fun accountNamesEquality() {
        // GIVEN
        //      registered user instance with lower-case account name
        //      registered user instance with mixed-case account name
        val user1 = buildTestUser("account_name")
        val user2 = buildTestUser("Account_Name")

        // WHEN
        //      account names are checked for equality
        val equal = user1.nameEquals(user2)

        // THEN
        //      account names are equal
        assertTrue(equal)
    }

    @Test
    fun accountNamesEqualityCheckIsNullSafe() {
        // GIVEN
        //      registered user instance with lower-case account name
        //      null account
        val user1 = buildTestUser("account_name")
        val user2: User? = null

        // WHEN
        //      account names are checked for equality against null
        val equal = user1.nameEquals(user2)

        // THEN
        //      account names are not equal
        assertFalse(equal)
    }
}
