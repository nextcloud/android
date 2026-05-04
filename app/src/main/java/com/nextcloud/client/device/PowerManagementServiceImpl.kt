/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2026 Josh Richards <josh.t.richards@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import com.nextcloud.utils.extensions.registerBroadcastReceiver
import com.owncloud.android.datamodel.ReceiverFlag
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Implementation of [PowerManagementService] that reports device power-saving mode and battery status.
 */
internal class PowerManagementServiceImpl(
    private val context: Context,
    private val platformPowerManager: PowerManager
) : PowerManagementService {

    companion object {
        private const val TAG = "PowerManagementServiceImpl"
        private const val PERCENT = 100

        /**
         * Convenient factory to create [PowerManagementServiceImpl] from [Context].
         */
        @JvmStatic
        fun fromContext(context: Context): PowerManagementServiceImpl {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return PowerManagementServiceImpl(context, powerManager)
        }
    }

    /**
     * True if the device's power saving (battery saver) mode is currently active.
     */
    override val isPowerSavingEnabled: Boolean
        get() = platformPowerManager.isPowerSaveMode

    /**
     * Returns the current [BatteryStatus], including charging status and a calculated charge
     * percentage (0-100).
     *
     * Charging logic combines reported charging status and plugged-in state for robustness.
     * Logs discrepancies between Android-reported plugged state and charging status for diagnostics.
     * If battery information is unavailable, returns safe defaults (0% and not charging).
     */
    override val battery: BatteryStatus
        get() {
            val intent = context.registerBroadcastReceiver(
                receiver = null,
                filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                flag = ReceiverFlag.NotExported
            ) ?: return BatteryStatus(isCharging = false, level = 0)

            // Defensive calculation of battery percentage: only computes if valid, else returns 0.
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val chargePercent = if (level >= 0 && scale > 0) {
                ((level * PERCENT) / scale.toFloat()).toInt()
            } else {
                Log_OC.w(TAG, "Invalid battery info: level=$level, scale=$scale")
                0 // Unavailable data
            }

            // Robust charging determination: first use status, then plugged fallback for buggy/missing cases.
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

            val pluggedIn = plugged in setOf(
                BatteryManager.BATTERY_PLUGGED_USB,
                BatteryManager.BATTERY_PLUGGED_AC,
                BatteryManager.BATTERY_PLUGGED_WIRELESS
            )
            val statusChargingOrFull =
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            val isCharging = when {
                statusChargingOrFull -> true   // Reliable for 99% of devices
                status == -1 && pluggedIn -> true // Status missing but plugged in? Likely charging (fallback)
                else -> false
            }

            // Log disagreements between plugged-in and charging status for device/ROM diagnostics.
            if (pluggedIn != statusChargingOrFull) {
                Log_OC.w(TAG, "BatteryManager disagrees: status=$status, plugged=$plugged")
            }

            return BatteryStatus(isCharging, chargePercent)
        }
}
