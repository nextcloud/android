/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication.server

import com.owncloud.android.authentication.server.AuthenticatorUrlUtils.normalizeScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@RunWith(Suite::class)
@SuiteClasses(AuthenticatorUrlUtilsTest.NormalizeScheme::class)
class AuthenticatorUrlUtilsTest {

    class NormalizeScheme {

        @Test
        fun noScheme() {
            // GIVEN
            //      input URL has no scheme
            val url = "host.net/index.php/apps/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = normalizeScheme(url)

            // THEN
            //      input is returned unchanged
            assertSame(url, normalized)
        }

        @Test
        fun lowercaseScheme() {
            // GIVEN
            //      input URL has scheme
            //      scheme is lowercase
            val url = "https://host.net/index.php/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = normalizeScheme(url)

            // THEN
            //      output is equal
            assertEquals(url, normalized)
        }

        @Test
        fun uppercaseScheme() {
            // GIVEN
            //      input URL has scheme
            //      scheme has uppercase characters
            val mixedCaseUrl = "HTtps://host.net/index.php/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = normalizeScheme(mixedCaseUrl)

            // THEN
            //      scheme has been lower-cased
            //      remaining URL part is left unchanged
            assertEquals("https://host.net/index.php/ABC/def/?", normalized)
        }

        @Test
        fun emptyInput() {
            // GIVEN
            //      input URL is empty
            val emptyUrl = ""

            // WHEN
            //      scheme is normalized
            val normalized = normalizeScheme(emptyUrl)

            // THEN
            //      output is empty
            assertEquals("", normalized)
        }

        @Test
        fun ipAddress() {
            // GIVEN
            //      input URL is an IP address
            val url = "127.0.0.1"

            // WHEN
            //      scheme is normalized
            val normalized = normalizeScheme(url)

            // THEN
            //      output is equal
            assertEquals(url, normalized)
        }

        @Test
        fun withPort() {
            // GIVEN
            //      input URL has a port
            val url = "host.net:8080/index.php/apps/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = normalizeScheme(url)

            // THEN
            //      output is equal
            assertEquals(url, normalized)
        }

        @Test
        fun ipAddressWithPort() {
            // GIVEN
            //      input URL is an IP address
            //      input URL has a port
            val url = "127.0.0.1:8080/index.php/apps/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = normalizeScheme(url)

            // THEN
            //      output is equal
            assertEquals(url, normalized)
        }
    }
}
