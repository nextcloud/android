/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.R
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import java.io.File
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.TimeZone

class SyncFolderHelper(private val context: Context) {

    companion object {
        private const val TAG = "SyncFolderHelper"
    }

    fun getAutoUploadRemotePath(syncedFolder: SyncedFolder, file: File): String {
        val resources = context.resources
        val isLightVersion = resources.getBoolean(R.bool.syncedFolder_light)
        val lastModificationTime = calculateLastModificationTime(file, syncedFolder)

        val remoteFolder: String
        val useSubfolders: Boolean
        val subFolderRule: SubFolderRule

        if (isLightVersion) {
            remoteFolder = resources.getString(R.string.syncedFolder_remote_folder)
            useSubfolders = resources.getBoolean(R.bool.syncedFolder_light_use_subfolders)
            subFolderRule = SubFolderRule.YEAR_MONTH
        } else {
            remoteFolder = syncedFolder.remotePath
            useSubfolders = syncedFolder.isSubfolderByDate
            subFolderRule = syncedFolder.subfolderRule
        }

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            resources.configuration.locales[0],
            remoteFolder,
            syncedFolder.localPath,
            lastModificationTime,
            useSubfolders,
            subFolderRule
        )

        Log_OC.d(TAG, "auto upload remote path: $result")

        return result
    }

    @Suppress("NestedBlockDepth")
    private fun calculateLastModificationTime(file: File, syncedFolder: SyncedFolder): Long {
        val resources = context.resources
        val currentLocale = resources.configuration.locales[0]
        val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", currentLocale).apply {
            timeZone = TimeZone.getTimeZone(TimeZone.getDefault().id)
        }
        var lastModificationTime = file.lastModified()
        if (MediaFolderType.IMAGE == syncedFolder.type && hasExif(file)) {
            Log_OC.d(TAG, "calculateLastModificationTime exif found")

            @Suppress("TooGenericExceptionCaught")
            try {
                val exifInterface = ExifInterface(file.absolutePath)
                val exifDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                if (!exifDate.isNullOrBlank()) {
                    val pos = ParsePosition(0)
                    val dateTime = formatter.parse(exifDate, pos)
                    if (dateTime != null) {
                        lastModificationTime = dateTime.time
                        Log_OC.w(
                            TAG,
                            "calculateLastModificationTime calculatedTime is: $lastModificationTime"
                        )
                    } else {
                        Log_OC.w(TAG, "calculateLastModificationTime dateTime is empty")
                    }
                } else {
                    Log_OC.w(TAG, "calculateLastModificationTime exifDate is empty")
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "Failed to get the proper time " + e.localizedMessage)
            }
        }
        return lastModificationTime
    }

    private fun hasExif(file: File): Boolean {
        val mimeType = FileStorageUtils.getMimeTypeFromName(file.absolutePath)
        return mimeType.equals(MimeType.JPEG, ignoreCase = true) ||
            mimeType.equals(MimeType.TIFF, ignoreCase = true)
    }
}
