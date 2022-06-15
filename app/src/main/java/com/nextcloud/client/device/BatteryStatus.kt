/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
