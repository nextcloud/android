/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network

import com.nextcloud.client.core.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalledCheckCache @Inject constructor(private val clock: Clock) {
    private var connectivityCache = mutableMapOf<ConnectivityKey, Connectivity>()
    private val walledStatusCache = mutableMapOf<ConnectivityKey, Pair<Long, Boolean>>()

    @Synchronized
    fun setValue(key: ConnectivityKey, isWalled: Boolean) {
        walledStatusCache[key] = Pair(clock.currentTime, isWalled)
    }

    @Synchronized
    fun clear(key: ConnectivityKey) {
        walledStatusCache.remove(key)
    }

    @Synchronized
    fun getValue(key: ConnectivityKey): Boolean? {
        val entry = walledStatusCache[key] ?: return null
        val isExpired = (clock.currentTime - entry.first) >= CACHE_TIME_MS
        return if (isExpired) null else entry.second
    }

    @Synchronized
    fun putConnectivityValue(key: ConnectivityKey, connectivity: Connectivity) {
        connectivityCache[key] = connectivity
    }

    @Synchronized
    fun getConnectivity(key: ConnectivityKey): Connectivity? = connectivityCache[key]

    companion object {
        private const val CACHE_TIME_MS = 10 * 60 * 1000
    }
}
