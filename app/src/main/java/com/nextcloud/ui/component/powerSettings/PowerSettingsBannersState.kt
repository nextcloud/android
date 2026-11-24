/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component.powerSettings

import androidx.annotation.StringRes
import com.nextcloud.client.device.PowerManagementService
import com.owncloud.android.R

enum class PowerSettingsBannersState(@StringRes val powerSavingMessageId: Int?) {
    UNRESTRICTED(null),
    BATTERY_OPTIMIZED(null),
    POWER_SAVING_RESTRICTING_APP(R.string.power_saving_banner_restricting_app_message),
    POWER_SAVING_IGNORED_BY_APP(R.string.power_saving_banner_ignored_by_app_message);

    val isBatteryOptimizationBannerVisible: Boolean
        get() = this == BATTERY_OPTIMIZED || this == POWER_SAVING_RESTRICTING_APP

    companion object {
        fun from(service: PowerManagementService): PowerSettingsBannersState {
            if (service.isAppRestrictedByPowerSaving) return POWER_SAVING_RESTRICTING_APP
            if (service.isPowerSavingEnabled) return POWER_SAVING_IGNORED_BY_APP
            if (service.isExemptFromBatteryOptimization) return UNRESTRICTED

            return BATTERY_OPTIMIZED
        }
    }
}
