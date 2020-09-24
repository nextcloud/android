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
package com.owncloud.android.authentication

import android.graphics.Color
import org.junit.Assert
import org.junit.Test

class AuthenticatorActivityIT {
    @Test(expected = IndexOutOfBoundsException::class)
    fun testException() {
        Color.parseColor("")
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun tryCatch() {
        val color = try {
            Color.parseColor("1")
        } catch (e: Exception) {
            Color.BLACK
        }

        Assert.assertNotNull(color)
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun tryCatch2() {
        val color = try {
            Color.parseColor("")
        } catch (e: Exception) {
            Color.BLACK
        }

        Assert.assertNotNull(color)
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun tryCatch3() {
        val color = try {
            Color.parseColor(null)
        } catch (e: Exception) {
            Color.BLACK
        }

        Assert.assertNotNull(color)
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun tryCatch4() {
        val color = try {
            Color.parseColor("abc")
        } catch (e: Exception) {
            Color.BLACK
        }

        Assert.assertNotNull(color)
    }
}
