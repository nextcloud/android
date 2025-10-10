/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.R
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import java.io.File

fun List<SyncedFolderDisplayItem>.filterEnabledOrWithoutEnabledParent(): List<SyncedFolderDisplayItem> = filter {
    it.isEnabled || !hasEnabledParent(it.localPath)
}

@Suppress("ReturnCount")
fun List<SyncedFolder>.hasEnabledParent(localPath: String?): Boolean {
    localPath ?: return false

    val localFile = File(localPath).takeIf { it.exists() } ?: return false
    val parent = localFile.parentFile ?: return false

    return any { it.isEnabled && File(it.localPath).exists() && File(it.localPath) == parent } ||
        hasEnabledParent(parent.absolutePath)
}

@Suppress("MagicNumber", "ReturnCount")
fun SyncedFolder.calculateScanInterval(
    connectivityService: ConnectivityService,
    powerManagementService: PowerManagementService
): Pair<Long, Int?> {
    val defaultIntervalMillis = BackgroundJobManagerImpl.DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES * 60_000L

    if (!connectivityService.isConnected() || connectivityService.isInternetWalled()) {
        return defaultIntervalMillis * 2 to null
    }

    if (isWifiOnly && !connectivityService.getConnectivity().isWifi) {
        return defaultIntervalMillis * 4 to R.string.auto_upload_wifi_only_warning_info
    }

    val batteryLevel = powerManagementService.battery.level
    return when {
        batteryLevel < 20 -> defaultIntervalMillis * 8 to R.string.auto_upload_low_battery_warning_info
        batteryLevel < 50 -> defaultIntervalMillis * 4 to R.string.auto_upload_low_battery_warning_info
        batteryLevel < 80 -> defaultIntervalMillis * 2 to null
        else -> defaultIntervalMillis to null
    }
}
