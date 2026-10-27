/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountNameUtilsTest {

    private val accountNames = mapOf(
        "user@cloud.example.com" to ("user" to "cloud.example.com"),
        "John Doe@cloud.example.com" to ("John Doe" to "cloud.example.com"),
        "user'name@cloud.example.com" to ("user'name" to "cloud.example.com"),
        "john.doe@example.com@cloud.example.com" to ("john.doe@example.com" to "cloud.example.com"),
        "a@b@c@cloud.example.com" to ("a@b@c" to "cloud.example.com"),
        "john.doe+tag@example.com@cloud.example.com" to ("john.doe+tag@example.com" to "cloud.example.com"),
        "CORP\\user@cloud.example.com" to ("CORP\\user" to "cloud.example.com"),
        "ünïcode@cloud.example.com" to ("ünïcode" to "cloud.example.com"),
        "user@cloud.example.com:8443/nextcloud" to ("user" to "cloud.example.com:8443/nextcloud"),
        "user@[2001:db8::1]:8443" to ("user" to "[2001:db8::1]:8443"),
        "user@localhost" to ("user" to "localhost")
    )

    @Test
    fun `user and server parts are split at the last separator`() {
        accountNames.forEach { (accountName, parts) ->
            assertEquals(accountName, parts.first, AccountNameUtils.getUserPart(accountName))
            assertEquals(accountName, parts.second, AccountNameUtils.getServerPart(accountName))
        }
    }

    @Test
    fun `build is reversed by user and server parts`() {
        accountNames.values.forEach { (user, server) ->
            val accountName = AccountNameUtils.build(user, server)
            assertEquals(user, AccountNameUtils.getUserPart(accountName))
            assertEquals(server, AccountNameUtils.getServerPart(accountName))
        }
    }

    @Test
    fun `account name without separator has no server part`() {
        assertEquals("user", AccountNameUtils.getUserPart("user"))
        assertEquals("", AccountNameUtils.getServerPart("user"))
        assertEquals("", AccountNameUtils.getServerPart("user@"))
    }

    @Test
    fun `equivalent server urls are the same server`() {
        listOf(
            "https://server.net" to "https://server.net",
            "https://server.net/" to "https://server.net",
            "https://server.net/index.php" to "https://server.net",
            "https://server.net/nextcloud/" to "https://server.net/nextcloud/index.php",
            "https://Server.NET" to "https://server.net",
            "https://server.net:443" to "https://server.net",
            "http://server.net:80" to "http://server.net",
            "http://server.net" to "https://server.net",
            "https://server.net:8443" to "https://server.net:8443",
            "https://my_host.local" to "https://My_Host.local",
            "https://[2001:DB8:0::1]:8443/nc" to "https://[2001:db8::1]:8443/nc",
            "https://bücher.example" to "https://xn--bcher-kva.example",
            "https://cloud.example.com." to "https://cloud.example.com",
            "http://10.0.2.2:8080" to "http://10.0.2.2:8080",
            "https://localhost" to "https://localhost"
        ).forEach { (first, second) ->
            assertTrue("$first vs $second", AccountNameUtils.isSameServer(first, second))
        }
    }

    @Test
    fun `different server urls are not the same server`() {
        listOf(
            "https://server.net" to "https://other.net",
            "https://server.net" to "https://sub.server.net",
            "https://server.net:8443" to "https://server.net",
            "https://server.net/nextcloud" to "https://server.net",
            "https://server.net/Nextcloud" to "https://server.net/nextcloud",
            "https://[2001:db8::1]:8443" to "https://[2001:db8::1:8443]"
        ).forEach { (first, second) ->
            assertFalse("$first vs $second", AccountNameUtils.isSameServer(first, second))
        }
    }

    @Test
    fun `unparsable server urls are compared as they are`() {
        assertTrue(AccountNameUtils.isSameServer("not a url", "not a url"))
        assertFalse(AccountNameUtils.isSameServer("not a url", "Not a url"))
    }
}
