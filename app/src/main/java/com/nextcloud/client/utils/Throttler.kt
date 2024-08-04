/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
