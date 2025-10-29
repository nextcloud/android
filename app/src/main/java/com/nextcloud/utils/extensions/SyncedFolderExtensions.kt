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
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File

private const val TAG = "SyncedFolderExtensions"

/**
 * Determines whether a file should be skipped during auto-upload based on folder settings.
 */
fun SyncedFolder.shouldSkipFile(file: File, lastModified: Long, creationTime: Long?): Boolean {
    if (isExcludeHidden && file.isHidden) {
        Log_OC.d(TAG, "Skipping hidden: ${file.absolutePath}")
        return true
    }

    if (lastModified < lastScanTimestampMs) {
        Log_OC.d(TAG, "Skipping old file (last scan > modified): ${file.absolutePath}")
        return true
    }

    if (lastModified < enabledTimestampMs && lastScanTimestampMs != -1L) {
        Log_OC.d(TAG, "Skipping file older than enabled time: ${file.absolutePath}")
        return true
    }

    if (!isExisting && creationTime != null && creationTime < enabledTimestampMs) {
        Log_OC.d(TAG, "Skipping pre-existing file (creation < enabled): ${file.absolutePath}")
        return true
    }

    return false
}

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
