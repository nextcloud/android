/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.device

/**
 * This service provides all device power management
 * functions.
 */
interface PowerManagementService {

    /**
     * Checks if power saving mode is enabled on this device.
     * On platforms that do not support power saving mode it
     * evaluates to false.
     *
     * @see android.os.PowerManager.isPowerSaveMode
     */
    val isPowerSavingEnabled: Boolean

    /**
     * Checks if the device vendor requires power saving
     * exclusion workaround.
     *
     * @return true if workaround is required, false otherwise
     */
    val isPowerSavingExclusionAvailable: Boolean

    /**
     * Checks current battery status using platform [android.os.BatteryManager]
     */
    val battery: BatteryStatus
}
