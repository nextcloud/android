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
import com.owncloud.android.datamodel.AutoUploadFolder
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import java.util.Locale

private const val TAG = "AutoUploadFolderExtensions"

/**
 * Determines whether a file should be skipped during auto-upload based on folder settings.
 */
fun AutoUploadFolder.shouldSkipFile(
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
    if (!alsoUploadExistingFiles) {
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

fun List<AutoUploadFolder>.filterEnabledOrWithoutEnabledParent(): List<AutoUploadFolder> = filter {
    it.isEnabled || !hasEnabledParent(it.localPath)
}

fun List<AutoUploadFolder>.hasEnabledParent(localPath: String?): Boolean {
    localPath ?: return false

    val localFile = File(localPath).takeIf { it.exists() } ?: return false
    val parent = localFile.parentFile ?: return false

    return any { it.isEnabled && File(it.localPath).exists() && File(it.localPath) == parent } ||
        hasEnabledParent(parent.absolutePath)
}

@Suppress("MagicNumber")
fun AutoUploadFolder.calculateScanInterval(
    connectivityService: ConnectivityService,
    powerManagementService: PowerManagementService
): Pair<Long, Int?> {
    val defaultIntervalMillis = BackgroundJobManagerImpl.DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES * 60_000L

    if (!connectivityService.isConnected || connectivityService.isInternetWalled()) {
        return defaultIntervalMillis * 2 to null
    }

    if (isWifiOnly && !connectivityService.connectivity.isWifi) {
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
 * Builds a structured debug string of the AutoUploadFolder configuration.
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
fun AutoUploadFolder.getLog(): String {
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
        📅 Subfolder rule: $subfolderRule
        🗂️ By date: $isSubfolderByDate
        🙈 Exclude hidden: $isExcludeHidden
        👀 Hidden config: $isHidden
        
        📶 Wi-Fi only: $isWifiOnly
        🔌 Charging only: $isChargingOnly
        
        📤 Upload existing files: $alsoUploadExistingFiles
        ⚙️ Upload action: $uploadAction
        🧩 Name collision: $nameCollisionPolicy
        
        ✅ Enabled: $isEnabled
        🕒 Enabled at: $enabledTimestampMs
        🔍 Last scan: $lastScanTimestampMs
        ─────────────────────────
    """.trimIndent()
}

private val prioritizedFolders = listOf("Camera", "Screenshots")

fun List<AutoUploadFolder?>.sortAutoUploadFolderItems(): List<AutoUploadFolder?> = sortedWith(::compareItems)

private fun compareItems(f1: AutoUploadFolder?, f2: AutoUploadFolder?): Int = when {
    f1 == null && f2 == null -> 0
    f1 == null -> -1
    f2 == null -> 1
    f1.isEnabled && f2.isEnabled -> compareEnabledItems(f1.folderName, f2.folderName)
    else -> compareMixedItems(f1, f2)
}

private fun compareEnabledItems(name1: String?, name2: String?): Int = when {
    name1 == null -> -1
    name2 == null -> 1
    else -> compareNames(name1, name2)
}

private fun compareMixedItems(f1: AutoUploadFolder, f2: AutoUploadFolder): Int {
    val name1 = f1.folderName
    val name2 = f2.folderName
    return when {
        name1 == null && name2 == null -> 0
        f1.isEnabled -> -1
        f2.isEnabled -> 1
        name1 == null -> -1
        name2 == null -> 1
        else -> comparePrioritizedNames(name1, name2)
    }
}

private fun comparePrioritizedNames(name1: String, name2: String): Int {
    val prioritizedFolder = prioritizedFolders.firstOrNull { it == name1 || it == name2 }
    return when {
        prioritizedFolder == null -> compareNames(name1, name2)
        prioritizedFolder == name1 && prioritizedFolder == name2 -> 0
        prioritizedFolder == name1 -> -1
        else -> 1
    }
}

private fun compareNames(name1: String, name2: String): Int =
    name1.lowercase(Locale.getDefault()).compareTo(name2.lowercase(Locale.getDefault()))
