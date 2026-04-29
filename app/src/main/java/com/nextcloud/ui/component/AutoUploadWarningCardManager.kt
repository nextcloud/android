/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.view.View
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.databinding.AutoUploadBatterySaverWarningBannerBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class AutoUploadWarningCardManager(
    private val powerManagementService: PowerManagementService,
    private val viewThemeUtils: ViewThemeUtils
) {
    fun bind(binding: AutoUploadBatterySaverWarningBannerBinding) {
        val isBatterySaver = powerManagementService.isPowerSavingEnabled
        val isIgnoringOptimization = powerManagementService.isIgnoringOptimization

        binding.root.setVisibleIf(isBatterySaver || isIgnoringOptimization)

        if (isBatterySaver && isIgnoringOptimization) {
            binding.title.visibility = View.VISIBLE
            binding.batterySaverReason.visibility = View.VISIBLE
            binding.batteryOptimizationReason.visibility = View.VISIBLE
        } else if (isBatterySaver) {
            binding.title.visibility = View.VISIBLE
            binding.batterySaverReason.visibility = View.VISIBLE
        } else if (isIgnoringOptimization) {
            binding.title.visibility = View.VISIBLE
            binding.batteryOptimizationReason.visibility = View.VISIBLE
        }

        viewThemeUtils.material.themeCardView(binding.root)
    }

    // region listen power mode changes
    private var binding: AutoUploadBatterySaverWarningBannerBinding? = null

    private val batterySaverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                binding?.let { bind(it) }
            }
        }
    }

    fun register(context: Context, binding: AutoUploadBatterySaverWarningBannerBinding) {
        this.binding = binding
        bind(binding)
        context.registerReceiver(batterySaverReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(batterySaverReceiver)
        binding = null
    }
    // endregion
}
