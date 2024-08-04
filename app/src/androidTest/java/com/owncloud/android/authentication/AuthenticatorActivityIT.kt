/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
