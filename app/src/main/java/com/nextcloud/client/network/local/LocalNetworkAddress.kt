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
    private const val UNIQUE_LOCAL_MASK = 0xFE
    private const val UNIQUE_LOCAL_PREFIX = 0xFC

    suspend fun isLocalServer(serverUrl: String): Boolean {
        val host = runCatching { URI(serverUrl).host }.getOrNull() ?: return false
        val caseInsensitiveHost = host.lowercase(Locale.ROOT).trimEnd(HOST_LABEL_SEPARATOR)
        return caseInsensitiveHost.endsWith(MDNS_SUFFIX) || withContext(Dispatchers.IO) {
            resolve(host).any { it.isLocalNetworkAddress() }
        }
    }

    private fun resolve(host: String): List<InetAddress> =
        runCatching { InetAddress.getAllByName(host).toList() }.getOrDefault(emptyList())

    private fun InetAddress.isLocalNetworkAddress(): Boolean = when {
        isLoopbackAddress -> false
        isSiteLocalAddress || isLinkLocalAddress -> true
        this is Inet6Address -> address.first().toInt() and UNIQUE_LOCAL_MASK == UNIQUE_LOCAL_PREFIX
        else -> false
    }
}
