/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
     * Checks current battery status using platform [android.os.BatteryManager]
     */
    val battery: BatteryStatus
}
