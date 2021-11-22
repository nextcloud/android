/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 * Copyright (C) 2020 Chris Narkiewicz
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.jobs

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.PowerManager.WakeLock
import android.text.TextUtils
import androidx.exifinterface.media.ExifInterface
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FilesystemDataProvider
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Suppress("LongParameterList") // legacy code
class FilesSyncWork(
    private val context: Context,
    params: WorkerParameters,
    private val resources: Resources,
    private val contentResolver: ContentResolver,
    private val userAccountManager: UserAccountManager,
    private val preferences: AppPreferences,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val clock: Clock
) : Worker(context, params) {

    companion object {
        const val TAG = "FilesSyncJob"
        const val SKIP_CUSTOM = "skipCustom"
        const val OVERRIDE_POWER_SAVING = "overridePowerSaving"
        private const val WAKELOCK_TAG_SEPARATION = ":"
        private const val WAKELOCK_ACQUIRE_TIMEOUT_MS = 10L * 60L * 1000L
    }

    override fun doWork(): Result {
        val wakeLock: WakeLock? = null
        val overridePowerSaving = inputData.getBoolean(OVERRIDE_POWER_SAVING, false)
        // If we are in power save mode, better to postpone upload
        if (powerManagementService.isPowerSavingEnabled && !overridePowerSaving) {
            wakeLock?.release()
            return Result.success()
        }
        val resources = context.resources
        val lightVersion = resources.getBoolean(R.bool.syncedFolder_light)
        val skipCustom = inputData.getBoolean(SKIP_CUSTOM, false)
        FilesSyncHelper.restartJobsIfNeeded(
            uploadsStorageManager,
            userAccountManager,
            connectivityService,
            powerManagementService
        )
        FilesSyncHelper.insertAllDBEntries(preferences, clock, skipCustom)
        // Create all the providers we'll needq
        val filesystemDataProvider = FilesystemDataProvider(contentResolver)
        val syncedFolderProvider = SyncedFolderProvider(contentResolver, preferences, clock)
        val currentLocale = resources.configuration.locale
        val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", currentLocale)
        dateFormat.timeZone = TimeZone.getTimeZone(TimeZone.getDefault().id)
        for (syncedFolder in syncedFolderProvider.syncedFolders) {
            if (syncedFolder.isEnabled && (!skipCustom || MediaFolderType.CUSTOM != syncedFolder.type)) {
                syncFolder(
                    context,
                    resources,
                    lightVersion,
                    filesystemDataProvider,
                    currentLocale,
                    dateFormat,
                    syncedFolder
                )
            }
        }
        wakeLock?.release()
        return Result.success()
    }

    @Suppress("LongMethod") // legacy code
    private fun syncFolder(
        context: Context,
        resources: Resources,
        lightVersion: Boolean,
        filesystemDataProvider: FilesystemDataProvider,
        currentLocale: Locale,
        sFormatter: SimpleDateFormat,
        syncedFolder: SyncedFolder
    ) {
        var remotePath: String?
        var subfolderByDate: Boolean
        var uploadAction: Int?
        var needsCharging: Boolean
        var needsWifi: Boolean
        var file: File
        val accountName = syncedFolder.account
        val optionalUser = userAccountManager.getUser(accountName)
        if (!optionalUser.isPresent) {
            return
        }
        val user = optionalUser.get()
        val arbitraryDataProvider = if (lightVersion) {
            ArbitraryDataProvider(contentResolver)
        } else {
            null
        }
        val paths = filesystemDataProvider.getFilesForUpload(
            syncedFolder.localPath,
            java.lang.Long.toString(syncedFolder.id)
        )
        for (path in paths) {
            file = File(path)
            val lastModificationTime = calculateLastModificationTime(file, syncedFolder, sFormatter)
            val mimeType = MimeTypeUtil.getBestMimeTypeByFilename(file.absolutePath)
            if (lightVersion) {
                needsCharging = resources.getBoolean(R.bool.syncedFolder_light_on_charging)
                needsWifi = arbitraryDataProvider!!.getBooleanValue(
                    accountName,
                    SettingsActivity.SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI
                )
                val uploadActionString = resources.getString(R.string.syncedFolder_light_upload_behaviour)
                uploadAction = getUploadAction(uploadActionString)
                subfolderByDate = resources.getBoolean(R.bool.syncedFolder_light_use_subfolders)
                remotePath = resources.getString(R.string.syncedFolder_remote_folder)
            } else {
                needsCharging = syncedFolder.isChargingOnly
                needsWifi = syncedFolder.isWifiOnly
                uploadAction = syncedFolder.uploadAction
                subfolderByDate = syncedFolder.isSubfolderByDate
                remotePath = syncedFolder.remotePath
            }
            FileUploader.uploadNewFile(
                context,
                user.toPlatformAccount(),
                file.absolutePath,
                FileStorageUtils.getInstantUploadFilePath(
                    file,
                    currentLocale,
                    remotePath,
                    syncedFolder.localPath,
                    lastModificationTime,
                    subfolderByDate
                ),
                uploadAction!!,
                mimeType,
                true, // create parent folder if not existent
                UploadFileOperation.CREATED_AS_INSTANT_PICTURE,
                needsWifi,
                needsCharging,
                syncedFolder.nameCollisionPolicy
            )
            filesystemDataProvider.updateFilesystemFileAsSentForUpload(
                path,
                java.lang.Long.toString(syncedFolder.id)
            )
        }
    }

    private fun hasExif(file: File): Boolean {
        val mimeType = FileStorageUtils.getMimeTypeFromName(file.absolutePath)
        return MimeType.JPEG.equals(mimeType, ignoreCase = true) || MimeType.TIFF.equals(mimeType, ignoreCase = true)
    }

    private fun calculateLastModificationTime(
        file: File,
        syncedFolder: SyncedFolder,
        formatter: SimpleDateFormat
    ): Long {
        var lastModificationTime = file.lastModified()
        if (MediaFolderType.IMAGE == syncedFolder.type && hasExif(file)) {
            @Suppress("TooGenericExceptionCaught") // legacy code
            try {
                val exifInterface = ExifInterface(file.absolutePath)
                val exifDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                if (!TextUtils.isEmpty(exifDate)) {
                    val pos = ParsePosition(0)
                    val dateTime = formatter.parse(exifDate, pos)
                    lastModificationTime = dateTime.time
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "Failed to get the proper time " + e.localizedMessage)
            }
        }
        return lastModificationTime
    }

    private fun getUploadAction(action: String): Int? {
        return when (action) {
            "LOCAL_BEHAVIOUR_FORGET" -> FileUploader.LOCAL_BEHAVIOUR_FORGET
            "LOCAL_BEHAVIOUR_MOVE" -> FileUploader.LOCAL_BEHAVIOUR_MOVE
            "LOCAL_BEHAVIOUR_DELETE" -> FileUploader.LOCAL_BEHAVIOUR_DELETE
            else -> FileUploader.LOCAL_BEHAVIOUR_FORGET
        }
    }
}
