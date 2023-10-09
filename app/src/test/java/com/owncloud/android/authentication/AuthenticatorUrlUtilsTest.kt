/*
 *   Nextcloud Android client application
 *
 *   @author Chris Narkiewicz
 *   Copyright (C) 2021 Chris Narkiewicz
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.authentication

import com.owncloud.android.authentication.AuthenticatorUrlUtilsTest.NormalizeScheme
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@RunWith(Suite::class)
@SuiteClasses(NormalizeScheme::class)
class AuthenticatorUrlUtilsTest {
    class NormalizeScheme {
        @Test
        fun noScheme() {
            // GIVEN
            //      input URL has no scheme
            val url = "host.net/index.php/apps/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = AuthenticatorUrlUtils.normalizeScheme(url)

            // THEN
            //      input is returned unchanged
            Assert.assertSame(url, normalized)
        }

        @Test
        fun lowercaseScheme() {
            // GIVEN
            //      input URL has scheme
            //      scheme is lowercase
            val url = "https://host.net/index.php/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = AuthenticatorUrlUtils.normalizeScheme(url)

            // THEN
            //      output is equal
            Assert.assertEquals(url, normalized)
        }

        @Test
        fun uppercaseScheme() {
            // GIVEN
            //      input URL has scheme
            //      scheme has uppercase characters
            val mixedCaseUrl = "HTtps://host.net/index.php/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = AuthenticatorUrlUtils.normalizeScheme(mixedCaseUrl)

            // THEN
            //      scheme has been lower-cased
            //      remaining URL part is left unchanged
            val expectedUrl = "https://host.net/index.php/ABC/def/?"
            Assert.assertEquals(expectedUrl, normalized)
        }

        @Test
        fun emptyInput() {
            // GIVEN
            //      input URL is empty
            val emptyUrl = ""

            // WHEN
            //      scheme is normalized
            val normalized = AuthenticatorUrlUtils.normalizeScheme(emptyUrl)

            // THEN
            //      output is empty
            Assert.assertEquals("", normalized)
        }

        @Test
        fun ipAddress() {
            // GIVEN
            //      input URL is an IP address
            val url = "127.0.0.1"

            // WHEN
            //      scheme is normalized
            val normalized = AuthenticatorUrlUtils.normalizeScheme(url)

            // THEN
            //      output is equal
            Assert.assertEquals(url, normalized)
        }

        @Test
        fun withPort() {
            // GIVEN
            //      input URL has a port
            val url = "host.net:8080/index.php/apps/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = AuthenticatorUrlUtils.normalizeScheme(url)

            // THEN
            //      output is equal
            Assert.assertEquals(url, normalized)
        }

        @Test
        fun ipAddressWithPort() {
            // GIVEN
            //      input URL is an IP address
            //      input URL has a port
            val url = "127.0.0.1:8080/index.php/apps/ABC/def/?"

            // WHEN
            //      scheme is normalized
            val normalized = AuthenticatorUrlUtils.normalizeScheme(url)

            // THEN
            //      output is equal
            Assert.assertEquals(url, normalized)
        }
    }
}