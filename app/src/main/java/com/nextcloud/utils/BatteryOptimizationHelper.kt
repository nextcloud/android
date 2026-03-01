/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Helper for checking and requesting Android battery optimization exemptions.
 *
 * Methods allow checking whether the app is impacted by battery optimizations and
 * guide the user to the appropriate settings screen to grant exemption.
 */
object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptimizationHelper"

    /**
     * Returns true if battery optimization is currently enabled for the app.
     */
    fun isBatteryOptimizationEnabled(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Guides the user to disable battery optimization for this app.
     * Returns true if a settings screen was opened, false if not possible.
     */
    @Suppress("TooGenericExceptionCaught")
    @SuppressLint("BatteryLife")
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return runCatching {
            tryOpenIgnoreBatteryOptimizationsScreen(context)
            || tryOpenGeneralBatteryOptimizationSettings(context)
        }.getOrElse { e ->
            Log_OC.w(TAG, "Failed to open battery optimization settings", e)
            false
        }
    }

    private fun tryOpenIgnoreBatteryOptimizationsScreen(context: Context): Boolean {
        return try {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "package:${context.packageName}".toUri()
            )

            // Add ONLY if non-Activity context (for best navigation UX)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: ActivityNotFoundException) {
            Log_OC.w(TAG, "Direct battery optimization exemption screen not found", e)
            false
        }
    }

    private fun tryOpenGeneralBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

            // Add ONLY if non-Activity context (for best navigation UX)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: ActivityNotFoundException) {
            Log_OC.e(TAG, "General battery optimization settings screen not found", e)
            false
        }
    }
}
