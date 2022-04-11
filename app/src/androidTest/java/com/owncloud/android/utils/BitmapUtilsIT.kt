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
package com.owncloud.android.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BitmapUtilsIT {
    @Test
    @Suppress("MagicNumber")
    fun usernameToColor() {
        assertEquals(BitmapUtils.Color(0, 0, 0), BitmapUtils.Color(0, 0, 0))
        assertEquals(BitmapUtils.Color(221, 203, 85), BitmapUtils.usernameToColor("User"))
        assertEquals(BitmapUtils.Color(208, 158, 109), BitmapUtils.usernameToColor("Admin"))
        assertEquals(BitmapUtils.Color(0, 130, 201), BitmapUtils.usernameToColor(""))
        assertEquals(BitmapUtils.Color(201, 136, 121), BitmapUtils.usernameToColor("68b329da9893e34099c7d8ad5cb9c940"))

        // tests from server
        assertEquals(BitmapUtils.Color(208, 158, 109), BitmapUtils.usernameToColor("Alishia Ann Lowry"))
        assertEquals(BitmapUtils.Color(0, 130, 201), BitmapUtils.usernameToColor("Arham Johnson"))
        assertEquals(BitmapUtils.Color(208, 158, 109), BitmapUtils.usernameToColor("Brayden Truong"))
        assertEquals(BitmapUtils.Color(151, 80, 164), BitmapUtils.usernameToColor("Daphne Roy"))
        assertEquals(BitmapUtils.Color(195, 114, 133), BitmapUtils.usernameToColor("Ellena Wright Frederic Conway"))
        assertEquals(BitmapUtils.Color(214, 180, 97), BitmapUtils.usernameToColor("Gianluca Hills"))
        assertEquals(BitmapUtils.Color(214, 180, 97), BitmapUtils.usernameToColor("Haseeb Stephens"))
        assertEquals(BitmapUtils.Color(151, 80, 164), BitmapUtils.usernameToColor("Idris Mac"))
        assertEquals(BitmapUtils.Color(0, 130, 201), BitmapUtils.usernameToColor("Kristi Fisher"))
        assertEquals(BitmapUtils.Color(188, 92, 145), BitmapUtils.usernameToColor("Lillian Wall"))
        assertEquals(BitmapUtils.Color(221, 203, 85), BitmapUtils.usernameToColor("Lorelai Taylor"))
        assertEquals(BitmapUtils.Color(151, 80, 164), BitmapUtils.usernameToColor("Madina Knight"))
        assertEquals(BitmapUtils.Color(121, 90, 171), BitmapUtils.usernameToColor("Rae Hope"))
        assertEquals(BitmapUtils.Color(188, 92, 145), BitmapUtils.usernameToColor("Santiago Singleton"))
        assertEquals(BitmapUtils.Color(208, 158, 109), BitmapUtils.usernameToColor("Sid Combs"))
        assertEquals(BitmapUtils.Color(30, 120, 193), BitmapUtils.usernameToColor("Vivienne Jacobs"))
        assertEquals(BitmapUtils.Color(110, 166, 143), BitmapUtils.usernameToColor("Zaki Cortes"))
        assertEquals(BitmapUtils.Color(91, 100, 179), BitmapUtils.usernameToColor("a user"))
        assertEquals(BitmapUtils.Color(208, 158, 109), BitmapUtils.usernameToColor("admin"))
        assertEquals(BitmapUtils.Color(151, 80, 164), BitmapUtils.usernameToColor("admin@cloud.example.com"))
        assertEquals(BitmapUtils.Color(221, 203, 85), BitmapUtils.usernameToColor("another user"))
        assertEquals(BitmapUtils.Color(36, 142, 181), BitmapUtils.usernameToColor("asd"))
        assertEquals(BitmapUtils.Color(0, 130, 201), BitmapUtils.usernameToColor("bar"))
        assertEquals(BitmapUtils.Color(208, 158, 109), BitmapUtils.usernameToColor("foo"))
        assertEquals(BitmapUtils.Color(182, 70, 157), BitmapUtils.usernameToColor("wasd"))
    }

    @Test
    @Suppress("MagicNumber")
    fun checkEqual() {
        assertEquals(BitmapUtils.Color(208, 158, 109), BitmapUtils.Color(208, 158, 109))
        assertNotEquals(BitmapUtils.Color(208, 158, 109), BitmapUtils.Color(208, 158, 100))
    }

    @Test
    @Suppress("MagicNumber")
    fun checkHashCode() {
        assertEquals(BitmapUtils.Color(208, 158, 109).hashCode(), BitmapUtils.Color(208, 158, 109).hashCode())
        assertNotEquals(BitmapUtils.Color(208, 158, 109).hashCode(), BitmapUtils.Color(208, 158, 100).hashCode())
    }
}
