package com.nextcloud.client.device

import android.annotation.TargetApi
import android.os.Build
import android.os.PowerManager

internal class PowerManagementServiceImpl(
    private val powerManager: PowerManager,
    private val deviceInfo: DeviceInfo = DeviceInfo()
) : PowerManagementService {

    companion object {
        /**
         * Vendors on this list use aggressive power saving methods that might
         * break application experience.
         */
        val OVERLY_AGGRESSIVE_POWER_SAVING_VENDORS = setOf("samsung", "huawei", "xiaomi")
    }

    override val isPowerSavingEnabled: Boolean
        get() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            if (deviceInfo.apiLevel >= Build.VERSION_CODES.LOLLIPOP) {
                return powerManager.isPowerSaveMode
            }
            // For older versions, we just say that device is not in power save mode
            return false
        }

    override val isPowerSavingExclusionAvailable: Boolean
        get() = deviceInfo.vendor in OVERLY_AGGRESSIVE_POWER_SAVING_VENDORS
}
