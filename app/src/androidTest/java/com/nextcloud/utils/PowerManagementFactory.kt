/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService

object PowerManagementFactory {
    @JvmStatic
    val mock: PowerManagementService = object : PowerManagementService {
        override val isIgnoringOptimization: Boolean
            get() = true
        override val isPowerSavingEnabled: Boolean
            get() = false
        override val battery: BatteryStatus
            get() = BatteryStatus(false, 0)
        override val blocksAutoUpload: Boolean
            get() = false
    }

    @JvmStatic
    val mockCharging: PowerManagementService = object : PowerManagementService {
        override val isIgnoringOptimization: Boolean
            get() = true
        override val isPowerSavingEnabled: Boolean
            get() = false
        override val battery: BatteryStatus
            get() = BatteryStatus(true, 100)
        override val blocksAutoUpload: Boolean
            get() = false
    }
}
