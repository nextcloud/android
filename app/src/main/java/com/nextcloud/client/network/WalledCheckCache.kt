/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.client.network

import com.nextcloud.client.core.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalledCheckCache @Inject constructor(private val clock: Clock) {

    private var cachedEntry: Pair<Long, Boolean>? = null

    @Synchronized
    fun isExpired(): Boolean {
        return when (val timestamp = cachedEntry?.first) {
            null -> true
            else -> {
                val diff = clock.currentTime - timestamp
                diff >= CACHE_TIME_MS
            }
        }
    }

    @Synchronized
    fun setValue(isWalled: Boolean) {
        this.cachedEntry = Pair(clock.currentTime, isWalled)
    }

    @Synchronized
    fun getValue(): Boolean? {
        return when (isExpired()) {
            true -> null
            else -> cachedEntry?.second
        }
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
