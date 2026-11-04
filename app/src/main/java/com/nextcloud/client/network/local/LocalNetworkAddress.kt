/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.util.Locale

object LocalNetworkAddress {
    private const val MDNS_SUFFIX = ".local"
    private const val HOST_LABEL_SEPARATOR = '.'

    // Mask for the first byte of a private IPv6 address
    private const val UNIQUE_LOCAL_MASK = 0xFE

    // Private IPv6 addresses start with fc or fd
    private const val UNIQUE_LOCAL_PREFIX = 0xFC

    suspend fun isLocalServer(serverUrl: String): Boolean {
        val host = runCatching { URI(serverUrl).host }.getOrNull() ?: return false
        val caseInsensitiveHost = host.lowercase(Locale.ROOT).trimEnd(HOST_LABEL_SEPARATOR)
        return caseInsensitiveHost.endsWith(MDNS_SUFFIX) || withContext(Dispatchers.IO) {
            resolve(host).any { it.isLocalNetworkAddress() }
        }
    }

    // Looks up all IP addresses of the host name
    private fun resolve(host: String): List<InetAddress> =
        runCatching { InetAddress.getAllByName(host).toList() }.getOrDefault(emptyList())

    private fun InetAddress.isLocalNetworkAddress(): Boolean = when {
        // Loopback (127.0.0.1, ::1) is the device itself, not the local network
        isLoopbackAddress -> false

        // Private LAN addresses (192.168.x.x, 10.x.x.x, 172.16.x.x, 169.254.x.x)
        isSiteLocalAddress || isLinkLocalAddress -> true

        // Private IPv6 addresses (fc00:: to fdff::), not covered by isSiteLocalAddress
        this is Inet6Address -> address.first().toInt() and UNIQUE_LOCAL_MASK == UNIQUE_LOCAL_PREFIX

        // Public internet address
        else -> false
    }
}
