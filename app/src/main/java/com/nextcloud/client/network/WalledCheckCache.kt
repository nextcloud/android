/*
 * Nextcloud - Android Client
 *
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

    private var cachedEntry: Pair<Long, Boolean>? = null

    @Synchronized
    fun isExpired(): Boolean = when (val timestamp = cachedEntry?.first) {
        null -> true
        else -> {
            val diff = clock.currentTime - timestamp
            diff >= CACHE_TIME_MS
        }
    }

    @Synchronized
    fun setValue(isWalled: Boolean) {
        this.cachedEntry = Pair(clock.currentTime, isWalled)
    }

    @Synchronized
    fun getValue(): Boolean? = when (isExpired()) {
        true -> null
        else -> cachedEntry?.second
    }

    @Synchronized
    fun clear() {
        cachedEntry = null
    }

    companion object {
        // 10 minutes
        private const val CACHE_TIME_MS = 10 * 60 * 1000
    }
}
