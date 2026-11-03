/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication.loginurl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LoginDataUrlParserTest {

    @Test
    fun allDataUrlElements() {
        val info = parse(USER_PART, PASSWORD_PART, SERVER_PART)

        assertEquals(USER, info.loginName)
        assertEquals(PASSWORD, info.appPassword)
        assertEquals(SERVER, info.server)
    }

    @Test
    fun oneElement() {
        assertEquals(USER, parse(USER_PART).loginName)
        assertEquals(PASSWORD, parse(PASSWORD_PART).appPassword)
        assertEquals(SERVER, parse(SERVER_PART).server)
    }

    @Test
    fun twoDataUrlElements() {
        parse(USER_PART, PASSWORD_PART).run {
            assertEquals(USER, loginName)
            assertEquals(PASSWORD, appPassword)
        }

        parse(USER_PART, SERVER_PART).run {
            assertEquals(USER, loginName)
            assertEquals(SERVER, server)
        }

        parse(SERVER_PART, USER_PART).run {
            assertEquals(USER, loginName)
            assertEquals(SERVER, server)
        }

        parse(SERVER_PART, PASSWORD_PART).run {
            assertEquals(PASSWORD, appPassword)
            assertEquals(SERVER, server)
        }

        parse(PASSWORD_PART, USER_PART).run {
            assertEquals(USER, loginName)
            assertEquals(PASSWORD, appPassword)
        }

        parse(PASSWORD_PART, SERVER_PART).run {
            assertEquals(PASSWORD, appPassword)
            assertEquals(SERVER, server)
        }
    }

    @Test
    fun tooManyDataUrlElements() {
        assertThrows(IllegalArgumentException::class.java) {
            parse(USER_PART, PASSWORD_PART, SERVER_PART, UNKNOWN_PART)
        }
    }

    @Test
    fun noDataUrlElements() {
        assertThrows(IllegalArgumentException::class.java) {
            LoginDataUrlParser.parse(SCHEME_URL, "")
        }
    }

    @Test
    fun onlyOneDataUrlElements() {
        assertEquals(USER, parse(USER_PART).loginName)
    }

    @Test
    fun onlyTwoDataUrlElements() {
        val info = parse(USER_PART, SERVER_PART)

        assertEquals(USER, info.loginName)
        assertEquals(SERVER, info.server)
    }

    private fun parse(vararg elements: String): LoginUrlInfo =
        LoginDataUrlParser.parse(SCHEME_URL, SCHEME_URL + elements.joinToString(ELEMENT_SEPARATOR))

    companion object {
        private const val SCHEME_URL = "nc://login/"
        private const val ELEMENT_SEPARATOR = "&"

        private const val USER = "testuser123"
        private const val PASSWORD = "testpassword123"
        private const val SERVER = "testserver123"

        private const val USER_PART = "user:$USER"
        private const val PASSWORD_PART = "password:$PASSWORD"
        private const val SERVER_PART = "server:$SERVER"
        private const val UNKNOWN_PART = "notexist:boom"
    }
}
