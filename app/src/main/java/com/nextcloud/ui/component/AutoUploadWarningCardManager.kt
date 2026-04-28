/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component

import android.content.Context
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.AutoUploadBatterySaverWarningBannerBinding
import com.owncloud.android.utils.theme.ViewThemeUtils

class AutoUploadWarningCardManager(
    private val powerManagementService: PowerManagementService,
    private val viewThemeUtils: ViewThemeUtils,
    private val context: Context
) {

    fun bind(binding: AutoUploadBatterySaverWarningBannerBinding) {
        val isBatterySaver = powerManagementService.isPowerSavingEnabled
        val isIgnoringOptimization = powerManagementService.isIgnoringOptimization

        binding.root.setVisibleIf(isBatterySaver || isIgnoringOptimization)

        val messages = listOfNotNull(
            if (isBatterySaver) context
                .getString(R.string.auto_upload_battery_saver_mode_warning) else null,
            if (isIgnoringOptimization) context
                .getString(R.string.auto_upload_battery_ignore_optimization_mode_warning) else null
        )

        binding.message.text = messages.joinToString("\n")

        viewThemeUtils.material.themeCardView(binding.root)
    }
}
