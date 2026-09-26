/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network

import com.nextcloud.client.core.ClockImpl
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalledCheckCache @Inject constructor() {
    private val clock = ClockImpl()

    private val connectivityCache = ConcurrentHashMap<ConnectivityKey, Connectivity>()
    private val walledStatusCache = ConcurrentHashMap<ConnectivityKey, Pair<Long, Boolean>>()

    fun setValue(key: ConnectivityKey, isWalled: Boolean) {
        walledStatusCache[key] = Pair(clock.currentTime, isWalled)
    }

    fun clear(key: ConnectivityKey) {
        walledStatusCache.remove(key)
    }

    fun getValue(key: ConnectivityKey): Boolean? {
        val entry = walledStatusCache[key] ?: return null
        val isExpired = (clock.currentTime - entry.first) >= CACHE_TIME_MS
        return if (isExpired) null else entry.second
    }

    fun putConnectivityValue(key: ConnectivityKey, connectivity: Connectivity) {
        connectivityCache[key] = connectivity
    }

    companion object {
        private const val CACHE_TIME_MS = 10 * 60 * 1000
    }
}
