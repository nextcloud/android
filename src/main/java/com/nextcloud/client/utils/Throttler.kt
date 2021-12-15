/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2021 Álvaro Brey Vilas
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.utils

import com.nextcloud.client.core.Clock

/**
 * Simple throttler that just discards new calls until interval has passed.
 *
 * @param clock the Clock to provide timestamps
 */
class Throttler(private val clock: Clock) {

    /**
     * The interval, in milliseconds, between accepted calls
     */
    @Suppress("MagicNumber")
    var intervalMillis = 150L
    private val timestamps: MutableMap<String, Long> = mutableMapOf()

    @Synchronized
    fun run(key: String, runnable: Runnable) {
        val time = clock.currentTime
        val lastCallTimestamp = timestamps[key] ?: 0
        if (time - lastCallTimestamp > intervalMillis) {
            runnable.run()
            timestamps[key] = time
        }
    }
}
