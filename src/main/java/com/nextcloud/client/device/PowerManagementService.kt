package com.nextcloud.client.device

/**
 * This service provides all device power management
 * functions.
 */
interface PowerManagementService {

    /**
     * Checks if power saving mode is enabled on this device.
     * On platforms that do not support power saving mode it
     * evaluates to false.
     *
     * @see android.os.PowerManager.isPowerSaveMode
     */
    val isPowerSavingEnabled: Boolean

    /**
     * Checks if the device vendor requires power saving
     * exclusion workaround.
     *
     * @return true if workaround is required, false otherwise
     */
    val isPowerSavingExclusionAvailable: Boolean

}
