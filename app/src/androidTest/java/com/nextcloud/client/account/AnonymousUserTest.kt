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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnonymousUserTest {
    @Test
    fun anonymousUserImplementsParcelable() {
        // GIVEN
        //      anonymous user instance
        val original = AnonymousUser("test_account")

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
        Assert.assertNotSame(original, retrieved)
        Assert.assertTrue(retrieved is AnonymousUser)
        Assert.assertEquals(original, retrieved)
    }
}
