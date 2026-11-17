/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component.powerSettings

import com.nextcloud.client.device.PowerManagementService

enum class PowerSavingBannerState {
    HIDDEN,
    RESTRICTING_APP,
    IGNORED_BY_APP;

    companion object {
        fun from(powerManagementService: PowerManagementService): PowerSavingBannerState {
            if (!powerManagementService.isPowerSavingEnabled) return HIDDEN
            if (powerManagementService.isAppRestrictedByPowerSaving) return RESTRICTING_APP

            return IGNORED_BY_APP
        }
    }
}
