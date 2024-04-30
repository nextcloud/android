/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(AuthenticatorUrlUtilsTest.NormalizeScheme.class)
public class AuthenticatorUrlUtilsTest {

    public static class NormalizeScheme {
        @Test
        public void noScheme() {
            // GIVEN
            //      input URL has no scheme
            String url = "host.net/index.php/apps/ABC/def/?";

            // WHEN
            //      scheme is normalized
            String normalized = AuthenticatorUrlUtils.INSTANCE.normalizeScheme(url);

            // THEN
            //      input is returned unchanged
            Assert.assertSame(url, normalized);
        }

        @Test
        public void lowercaseScheme() {
            // GIVEN
            //      input URL has scheme
            //      scheme is lowercase
            String url = "https://host.net/index.php/ABC/def/?";

            // WHEN
            //      scheme is normalized
            String normalized = AuthenticatorUrlUtils.INSTANCE.normalizeScheme(url);

            // THEN
            //      output is equal
            Assert.assertEquals(url, normalized);
        }

        @Test
        public void uppercaseScheme() {
            // GIVEN
            //      input URL has scheme
            //      scheme has uppercase characters
            String mixedCaseUrl = "HTtps://host.net/index.php/ABC/def/?";

            // WHEN
            //      scheme is normalized
            String normalized = AuthenticatorUrlUtils.INSTANCE.normalizeScheme(mixedCaseUrl);

            // THEN
            //      scheme has been lower-cased
            //      remaining URL part is left unchanged
            String expectedUrl = "https://host.net/index.php/ABC/def/?";
            Assert.assertEquals(expectedUrl, normalized);
        }

        @Test
        public void emptyInput() {
            // GIVEN
            //      input URL is empty
            String emptyUrl = "";

            // WHEN
            //      scheme is normalized
            String normalized = AuthenticatorUrlUtils.INSTANCE.normalizeScheme(emptyUrl);

            // THEN
            //      output is empty
            Assert.assertEquals("", normalized);
        }


        @Test
        public void ipAddress() {
            // GIVEN
            //      input URL is an IP address
            String url = "127.0.0.1";

            // WHEN
            //      scheme is normalized
            String normalized = AuthenticatorUrlUtils.INSTANCE.normalizeScheme(url);

            // THEN
            //      output is equal
            Assert.assertEquals(url, normalized);
        }


        @Test
        public void withPort() {
            // GIVEN
            //      input URL has a port
            String url = "host.net:8080/index.php/apps/ABC/def/?";

            // WHEN
            //      scheme is normalized
            String normalized = AuthenticatorUrlUtils.INSTANCE.normalizeScheme(url);

            // THEN
            //      output is equal
            Assert.assertEquals(url, normalized);
        }


        @Test
        public void ipAddressWithPort() {
            // GIVEN
            //      input URL is an IP address
            //      input URL has a port
            String url = "127.0.0.1:8080/index.php/apps/ABC/def/?";

            // WHEN
            //      scheme is normalized
            String normalized = AuthenticatorUrlUtils.INSTANCE.normalizeScheme(url);

            // THEN
            //      output is equal
            Assert.assertEquals(url, normalized);
        }
    }
}
