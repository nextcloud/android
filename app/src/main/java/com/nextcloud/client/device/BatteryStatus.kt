/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.device

/**
 * This class exposes battery status information
 * in platform-independent way.
 *
 * @param isCharging true if device battery is charging
 * @param level Battery level, from 0 to 100%
 *
 * @see [android.os.BatteryManager]
 */
data class BatteryStatus(val isCharging: Boolean = false, val level: Int = 0) {

    companion object {
        const val BATTERY_FULL = 100
    }

    /**
     * True if battery is fully loaded, false otherwise.
     * Some dodgy devices can report battery charging
     * status as "battery full".
     */
    val isFull: Boolean get() = level >= BATTERY_FULL
}
