/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component.powerSettings

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.core.net.toUri
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.databinding.PowerSettingsBannersBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PowerSettingsBanners(
    private val context: Context,
    private val powerManagementService: PowerManagementService,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager,
    private val scope: CoroutineScope,
    private val viewThemeUtils: ViewThemeUtils
) {
    private var binding: PowerSettingsBannersBinding? = null

    private val powerSavingModeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) return

            binding?.let { bind(it) }
        }
    }

    fun register(context: Context, binding: PowerSettingsBannersBinding) {
        this.binding = binding
        bind(binding)

        context.registerReceiver(
            powerSavingModeChangedReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        )
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(powerSavingModeChangedReceiver)
        binding = null
    }

    fun bind(binding: PowerSettingsBannersBinding) {
        val state = PowerSettingsBannersState.from(powerManagementService)

        binding.root.setVisibleIf(state != PowerSettingsBannersState.UNRESTRICTED)
        bindPowerSavingBanner(binding, state)

        bindBatteryOptimizationBanner(binding, state)
    }

    private fun bindPowerSavingBanner(binding: PowerSettingsBannersBinding, state: PowerSettingsBannersState) {
        val messageId = state.powerSavingMessageId
        binding.powerSavingBanner.setVisibleIf(messageId != null)
        if (messageId == null) return

        viewThemeUtils.material.run {
            themeCardView(binding.powerSavingBanner)
            colorMaterialButtonPrimaryBorderless(binding.powerSavingSettingsButton)
            colorMaterialButtonPrimaryBorderless(binding.powerSavingSyncNowButton)
        }

        binding.powerSavingBannerMessage.setText(messageId)
        binding.powerSavingBannerActions.setVisibleIf(
            state == PowerSettingsBannersState.POWER_SAVING_RESTRICTING_APP
        )
        binding.powerSavingSettingsButton.setOnClickListener { openPowerSavingSettings() }

        binding.powerSavingSyncNowButton.setOnClickListener { startAutoUploadIgnoringPowerSaving(it) }
    }

    private fun bindBatteryOptimizationBanner(binding: PowerSettingsBannersBinding, state: PowerSettingsBannersState) {
        binding.batteryOptimizationBanner.setVisibleIf(state.isBatteryOptimizationBannerVisible)
        if (!state.isBatteryOptimizationBannerVisible) return

        viewThemeUtils.material.run {
            themeCardView(binding.batteryOptimizationBanner)
            colorMaterialButtonPrimaryBorderless(binding.batteryOptimizationExemptButton)
        }

        binding.batteryOptimizationExemptButton.setOnClickListener { requestBatteryOptimizationExemption() }
    }

    private fun openPowerSavingSettings() {
        context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = "package:${context.packageName}".toUri()

        context.startActivity(intent)
    }

    private fun startAutoUploadIgnoringPowerSaving(view: View) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                FilesSyncHelper.startAutoUploadIgnoringPowerSaving(syncedFolderProvider, backgroundJobManager)
            }

            SnackbarUtil.show(view, result.messageId)
        }
    }
}
