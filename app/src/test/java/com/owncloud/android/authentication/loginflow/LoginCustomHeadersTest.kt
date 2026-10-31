/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.loginflow

import com.nextcloud.common.CustomHeaders
import com.nextcloud.utils.headers.UriParsingMock
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginCustomHeadersTest {
    private var restoreCalls = 0
    private val loginHeaders = LoginCustomHeaders { restoreCalls++ }

    @Before
    fun setUp() {
        UriParsingMock.install()
    }

    @After
    fun tearDown() {
        CustomHeaders.origins.forEach { CustomHeaders.unregister(it) }
        unmockkAll()
    }

    @Test
    fun `captured headers are registered for the typed https origin`() {
        assertTrue(loginHeaders.capture("https://cloud.example.com/nextcloud", HEADERS))

        assertEquals(HEADERS, CustomHeaders.forOrigin("https://cloud.example.com"))
    }

    @Test
    fun `headers for a server that is not https are refused`() {
        assertFalse(loginHeaders.capture("http://cloud.example.com", HEADERS))

        assertTrue(CustomHeaders.origins.isEmpty())
    }

    @Test
    fun `no headers is accepted without registering anything`() {
        assertTrue(loginHeaders.capture("http://cloud.example.com", emptyMap()))

        assertTrue(CustomHeaders.origins.isEmpty())
    }

    @Test
    fun `headers are only handed to an account on the captured origin`() {
        loginHeaders.capture("https://cloud.example.com", HEADERS)

        assertEquals(CustomHeaders.toJson(HEADERS), loginHeaders.jsonFor("https://cloud.example.com/index.php"))
        assertNull(loginHeaders.jsonFor("https://other.example.com"))
    }

    @Test
    fun `discard unregisters the origin and restores account headers`() {
        loginHeaders.capture("https://cloud.example.com", HEADERS)

        loginHeaders.discard()

        assertTrue(CustomHeaders.origins.isEmpty())
        assertNull(loginHeaders.jsonFor("https://cloud.example.com"))
        assertEquals(1, restoreCalls)
    }

    @Test
    fun `capturing for another origin discards the previous one`() {
        loginHeaders.capture("https://one.example.com", HEADERS)

        loginHeaders.capture("https://two.example.com", HEADERS)

        assertEquals(setOf("https://two.example.com"), CustomHeaders.origins)
        assertEquals(1, restoreCalls)
    }

    companion object {
        private val HEADERS = mapOf("CF-Access-Client-Id" to "id")
    }
}
