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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
        val (checkedAt, isWalled) = walledStatusCache[key] ?: return null
        val cacheTime = if (isWalled) WALLED_CACHE_TIME_MS else REACHABLE_CACHE_TIME_MS
        val isExpired = (clock.currentTime - checkedAt) >= cacheTime
        return if (isExpired) null else isWalled
    }

    fun putConnectivityValue(key: ConnectivityKey, connectivity: Connectivity) {
        connectivityCache[key] = connectivity
    }

    companion object {
        private val REACHABLE_CACHE_TIME_MS = 10.minutes.inWholeMilliseconds
        private val WALLED_CACHE_TIME_MS = 30.seconds.inWholeMilliseconds
    }
}
