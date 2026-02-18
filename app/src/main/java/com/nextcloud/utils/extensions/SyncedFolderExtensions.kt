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
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File

private const val TAG = "SyncedFolderExtensions"

/**
 * Determines whether a file should be skipped during auto-upload based on folder settings.
 */
@Suppress("ReturnCount")
fun SyncedFolder.shouldSkipFile(
    file: File,
    lastModified: Long,
    creationTime: Long?,
    fileSentForUpload: Boolean
): Boolean {
    Log_OC.d(TAG, "Checking file: ${file.name}, lastModified=$lastModified, lastScan=$lastScanTimestampMs")

    if (isExcludeHidden && file.isHidden) {
        Log_OC.d(TAG, "Skipping hidden: ${file.absolutePath}")
        return true
    }

    // If "upload existing files" is DISABLED, only upload files created after enabled time
    if (!isExisting) {
        if (creationTime != null) {
            if (creationTime < enabledTimestampMs) {
                Log_OC.d(TAG, "Skipping pre-existing file (creation < enabled): ${file.absolutePath}")
                return true
            }
        } else {
            Log_OC.w(TAG, "file will be inserted to db - cannot determine creation time: ${file.absolutePath}")
            return false
        }
    }

    // Skip files that haven't changed since last scan ONLY if they were sent for upload
    // AND only if this is not the first scan
    if (fileSentForUpload && lastScanTimestampMs != -1L && lastModified < lastScanTimestampMs) {
        Log_OC.d(
            TAG,
            "Skipping unchanged file that was already sent for upload (last modified < last scan): " +
                "${file.absolutePath}"
        )
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

/**
 * Builds a structured debug string of the SyncedFolder configuration.
 *
 * Important developer notes:
 *
 * uploadAction:
 *     Represents the UI option:
 *     👉 "Original file will be..."
 *     (e.g., kept, deleted, moved after upload)
 *
 * nameCollisionPolicy:
 *     Represents the UI option:
 *     👉 "What to do if the file already exists?"
 *     (e.g., rename, overwrite, skip)
 *
 * subfolderByDate:
 *     Represents the UI toggle:
 *     👉 "Use subfolders"
 *
 * existing:
 *     Represents the UI option:
 *     👉 "Also upload existing files"
 *     If false → only files created AFTER enabling are uploaded.
 */
fun SyncedFolder.getLog(): String {
    val mediaType = when (type) {
        MediaFolderType.IMAGE -> "🖼️ Images"
        MediaFolderType.VIDEO -> "🎬 Videos"
        MediaFolderType.CUSTOM -> "📁 Custom"
    }

    return """
        📦 Synced Folder
        ─────────────────────────
        🆔 ID: $id
        👤 Account: $account
        
        📂 Local:  $localPath
        ☁️ Remote: $remotePath
        
        $mediaType
        📅 Subfolder rule: ${subfolderRule ?: "None"}
        🗂️ By date: $isSubfolderByDate
        🙈 Exclude hidden: $isExcludeHidden
        👀 Hidden config: $isHidden
        
        📶 Wi-Fi only: $isWifiOnly
        🔌 Charging only: $isChargingOnly
        
        📤 Upload existing files: $isExisting
        ⚙️ Upload action: $uploadAction
        🧩 Name collision: $nameCollisionPolicy
        
        ✅ Enabled: $isEnabled
        🕒 Enabled at: $enabledTimestampMs
        🔍 Last scan: $lastScanTimestampMs
        ─────────────────────────
    """.trimIndent()
}
