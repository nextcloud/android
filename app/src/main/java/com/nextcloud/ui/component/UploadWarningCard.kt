/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component

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
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.databinding.UploadWarningCardBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
class UploadWarningCard(
    private val context: Context,
    private val powerManagementService: PowerManagementService,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager,
    private val scope: CoroutineScope,
    private val viewThemeUtils: ViewThemeUtils
) {
    fun bind(binding: UploadWarningCardBinding) {
        val isBatterySaver = powerManagementService.isPowerSavingEnabled
        val isIgnoringOptimization = powerManagementService.isIgnoringOptimization

        binding.root.setVisibleIf(isBatterySaver || !isIgnoringOptimization)

        if (isBatterySaver) {
            viewThemeUtils.material.run {
                themeCardView(binding.batterySaverLayout)
                colorMaterialButtonPrimaryBorderless(binding.batterySaverButton)
                colorMaterialButtonPrimaryBorderless(binding.syncNowButton)
            }

            binding.batterySaverLayout.visibility = View.VISIBLE
            binding.batterySaverButton.setOnClickListener {
                openBatterySaverPage()
            }
            binding.syncNowButton.setOnClickListener {
                startAutoUploadIgnoringBatterySaver(it)
            }
        } else {
            binding.batterySaverLayout.visibility = View.GONE
        }

        if (!isIgnoringOptimization) {
            viewThemeUtils.material.themeCardView(binding.backgroundActivityLimitedLayout)
            binding.backgroundActivityLimitedLayout.visibility = View.VISIBLE
            binding.backgroundActivityLimitedButton.setOnClickListener {
                showIgnoreBatteryOptimizationDialog()
            }
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(binding.backgroundActivityLimitedButton)
        } else {
            binding.backgroundActivityLimitedLayout.visibility = View.GONE
        }
    }

    // region listen power mode changes
    private var binding: UploadWarningCardBinding? = null

    private val batterySaverReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                binding?.let { bind(it) }
            }
        }
    }

    fun register(context: Context, binding: UploadWarningCardBinding) {
        this.binding = binding
        bind(binding)
        context.registerReceiver(batterySaverReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(batterySaverReceiver)
        binding = null
    }
    // endregion

    /**
     * Opens page for OS's battery saver screen.
     */
    private fun openBatterySaverPage() {
        context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
    }

    /**
     * Shows dialog to allow background usage for app.
     */
    @SuppressLint("BatteryLife")
    private fun showIgnoreBatteryOptimizationDialog() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = "package:${context.packageName}".toUri()
        context.startActivity(intent)
    }

    private fun startAutoUploadIgnoringBatterySaver(view: View) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                FilesSyncHelper.startAutoUploadIgnoringPowerSaving(syncedFolderProvider, backgroundJobManager)
            }

            DisplayUtils.showSnackMessage(view, result.messageId)
        }
    }
}
