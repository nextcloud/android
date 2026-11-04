/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network.local

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkAddressTest {

    private fun isLocal(serverUrl: String): Boolean = runBlocking { LocalNetworkAddress.isLocalServer(serverUrl) }

    @Test
    fun `private class A IPv4 address is local`() {
        assertTrue(isLocal("https://10.0.0.5"))
    }

    @Test
    fun `private class B IPv4 address is local`() {
        assertTrue(isLocal("https://172.16.0.1"))
    }

    @Test
    fun `private class C IPv4 address is local`() {
        assertTrue(isLocal("http://192.168.1.10"))
    }

    @Test
    fun `private IPv4 address with port and path is local`() {
        assertTrue(isLocal("https://192.168.1.10:8443/nextcloud"))
    }

    @Test
    fun `link local IPv4 address is local`() {
        assertTrue(isLocal("http://169.254.10.20"))
    }

    @Test
    fun `link local IPv6 address is local`() {
        assertTrue(isLocal("http://[fe80::1]"))
    }

    @Test
    fun `unique local IPv6 address is local`() {
        assertTrue(isLocal("https://[fd12:3456:789a::1]"))
    }

    @Test
    fun `mdns host name is local`() {
        assertTrue(isLocal("https://nextcloud.local"))
    }

    @Test
    fun `mdns host name is local regardless of case and trailing dot`() {
        assertTrue(isLocal("https://NextCloud.LOCAL."))
    }

    @Test
    fun `private IPv4 address without scheme is local`() {
        assertTrue(isLocal("192.168.1.10"))
    }

    @Test
    fun `private IPv4 address with port and without scheme is local`() {
        assertTrue(isLocal("10.0.2.2:55000"))
    }

    @Test
    fun `unique local IPv6 address without scheme is local`() {
        assertTrue(isLocal("[fd12:3456:789a::1]:443"))
    }

    @Test
    fun `mdns host name without scheme is local`() {
        assertTrue(isLocal("nextcloud.local:8080/nextcloud"))
    }

    @Test
    fun `public IPv4 address without scheme is not local`() {
        assertFalse(isLocal("8.8.8.8:443"))
    }

    @Test
    fun `IPv4 loopback address is not local`() {
        assertFalse(isLocal("http://127.0.0.1"))
    }

    @Test
    fun `IPv6 loopback address is not local`() {
        assertFalse(isLocal("http://[::1]"))
    }

    @Test
    fun `public IPv4 address is not local`() {
        assertFalse(isLocal("https://8.8.8.8"))
    }

    @Test
    fun `public IPv6 address is not local`() {
        assertFalse(isLocal("https://[2001:4860:4860::8888]"))
    }

    @Test
    fun `IPv4 address next to private range is not local`() {
        assertFalse(isLocal("https://172.32.0.1"))
    }

    @Test
    fun `url without host is not local`() {
        assertFalse(isLocal("file:///storage/emulated/0"))
    }

    @Test
    fun `malformed url is not local`() {
        assertFalse(isLocal("https://not a valid url"))
    }

    @Test
    fun `empty url is not local`() {
        assertFalse(isLocal(""))
    }
}
