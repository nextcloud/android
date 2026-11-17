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
     * True when the user set the app's battery usage to "Unrestricted", which puts the app on Android's power save
     * allowlist. Android then exempts the app from power saving mode as well: its jobs and alarms are not restricted
     * while power saving mode is on (AOSP `AppStateTrackerImpl.areJobsRestricted` and
     * `areAlarmsRestrictedByBatterySaver` return false for allowlisted apps).
     *
     * @see android.os.PowerManager.isIgnoringBatteryOptimizations
     */
    val isExemptFromBatteryOptimization: Boolean

    /**
     * Checks current battery status using platform [android.os.BatteryManager]
     */
    val battery: BatteryStatus

    /**
     * True when power saving mode is on and the app is not exempt from battery optimization. Follows the
     * same rule as Android, so an exempt app keeps uploading in power saving mode.
     */
    val isAppRestrictedByPowerSaving: Boolean
}
