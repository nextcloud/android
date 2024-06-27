/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import androidx.exifinterface.media.ExifInterface
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FilesystemDataProvider
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
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
    private val contentResolver: ContentResolver,
    private val userAccountManager: UserAccountManager,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager
) : Worker(context, params) {

    companion object {
        const val TAG = "FilesSyncJob"
        const val OVERRIDE_POWER_SAVING = "overridePowerSaving"
        const val CHANGED_FILES = "changedFiles"
        const val SYNCED_FOLDER_ID = "syncedFolderId"
    }

    private lateinit var syncedFolder: SyncedFolder

    @Suppress("MagicNumber")
    override fun doWork(): Result {
        val syncFolderId = inputData.getLong(SYNCED_FOLDER_ID, -1)
        val changedFiles = inputData.getStringArray(CHANGED_FILES)

        backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class) + "_" + syncFolderId)
        Log_OC.d(TAG, "File-sync worker started for folder ID: $syncFolderId")

        if (canExitEarly(changedFiles, syncFolderId)) {
            val result = Result.success()
            backgroundJobManager.logEndOfWorker(
                BackgroundJobManagerImpl.formatClassTag(this::class) +
                    "_" + syncFolderId,
                result
            )
            return result
        }

        val resources = context.resources
        val lightVersion = resources.getBoolean(R.bool.syncedFolder_light)
        FilesSyncHelper.restartUploadsIfNeeded(
            uploadsStorageManager,
            userAccountManager,
            connectivityService,
            powerManagementService
        )

        // Get changed files from ContentObserverWork (only images and videos) or by scanning filesystem
        Log_OC.d(
            TAG,
            "File-sync worker (${syncedFolder.remotePath}) changed files from observer: " +
                changedFiles.contentToString()
        )
        collectChangedFiles(changedFiles)
        Log_OC.d(TAG, "File-sync worker (${syncedFolder.remotePath}) finished checking files.")

        // Create all the providers we'll need
        val filesystemDataProvider = FilesystemDataProvider(contentResolver)
        val currentLocale = resources.configuration.locale
        val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", currentLocale)
        dateFormat.timeZone = TimeZone.getTimeZone(TimeZone.getDefault().id)

        syncFolder(
            context,
            resources,
            lightVersion,
            filesystemDataProvider,
            currentLocale,
            dateFormat,
            syncedFolder
        )

        Log_OC.d(TAG, "File-sync worker (${syncedFolder.remotePath}) finished")
        val result = Result.success()
        backgroundJobManager.logEndOfWorker(
            BackgroundJobManagerImpl.formatClassTag(this::class) +
                "_" + syncFolderId,
            result
        )
        return result
    }

    private fun setSyncedFolder(syncedFolderID: Long): Boolean {
        val syncedFolderTmp = syncedFolderProvider.getSyncedFolderByID(syncedFolderID)
        if (syncedFolderTmp == null || !syncedFolderTmp.isEnabled || !syncedFolderTmp.isExisting) {
            return false
        }
        syncedFolder = syncedFolderTmp
        return true
    }

    @Suppress("ReturnCount")
    private fun canExitEarly(changedFiles: Array<String>?, syncedFolderID: Long): Boolean {
        // If we are in power save mode better to postpone scan and upload
        val overridePowerSaving = inputData.getBoolean(OVERRIDE_POWER_SAVING, false)
        if ((powerManagementService.isPowerSavingEnabled && !overridePowerSaving)) {
            return true
        }

        if (syncedFolderID < 0) {
            Log_OC.d(TAG, "File-sync kill worker since no valid syncedFolderID provided!")
            return true
        }

        // or sync worker already running and no changed files to be processed
        val alreadyRunning = backgroundJobManager.bothFilesSyncJobsRunning(syncedFolderID)
        if (alreadyRunning && changedFiles.isNullOrEmpty()) {
            Log_OC.d(
                TAG,
                "File-sync kill worker since another instance of the worker " +
                    "($syncedFolderID) seems to be running already!"
            )
            return true
        }

        if (!setSyncedFolder(syncedFolderID)) {
            Log_OC.d(TAG, "File-sync kill worker since syncedFolder ($syncedFolderID) is not enabled!")
            return true
        }

        if (syncedFolder.isChargingOnly &&
            !powerManagementService.battery.isCharging &&
            !powerManagementService.battery.isFull
        ) {
            Log_OC.d(TAG, "File-sync kill worker since phone is not charging (${syncedFolder.localPath})!")
            return true
        }

        return false
    }

    @Suppress("MagicNumber")
    private fun collectChangedFiles(changedFiles: Array<String>?) {
        if (!changedFiles.isNullOrEmpty()) {
            FilesSyncHelper.insertChangedEntries(syncedFolder, changedFiles)
        } else {
            // Check every file in synced folder for changes and update
            // filesystemDataProvider database (potentially needs a long time)
            FilesSyncHelper.insertAllDBEntries(syncedFolder, powerManagementService)
        }
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
        val uploadAction: Int?
        val needsCharging: Boolean
        val needsWifi: Boolean
        var file: File
        val accountName = syncedFolder.account
        val optionalUser = userAccountManager.getUser(accountName)
        if (!optionalUser.isPresent) {
            return
        }
        val user = optionalUser.get()
        val arbitraryDataProvider: ArbitraryDataProvider? = if (lightVersion) {
            ArbitraryDataProviderImpl(context)
        } else {
            null
        }
        val paths = filesystemDataProvider.getFilesForUpload(
            syncedFolder.localPath,
            syncedFolder.id.toString()
        )

        if (paths.size == 0) {
            return
        }

        val pathsAndMimes = paths.map { path ->
            file = File(path)
            val localPath = file.absolutePath
            Triple(
                localPath,
                getRemotePath(file, syncedFolder, sFormatter, lightVersion, resources, currentLocale),
                MimeTypeUtil.getBestMimeTypeByFilename(localPath)
            )
        }
        val localPaths = pathsAndMimes.map { it.first }.toTypedArray()
        val remotePaths = pathsAndMimes.map { it.second }.toTypedArray()

        if (lightVersion) {
            needsCharging = resources.getBoolean(R.bool.syncedFolder_light_on_charging)
            needsWifi = arbitraryDataProvider!!.getBooleanValue(
                accountName,
                SettingsActivity.SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI
            )
            val uploadActionString = resources.getString(R.string.syncedFolder_light_upload_behaviour)
            uploadAction = getUploadAction(uploadActionString)
        } else {
            needsCharging = syncedFolder.isChargingOnly
            needsWifi = syncedFolder.isWifiOnly
            uploadAction = syncedFolder.uploadAction
        }
        FileUploadHelper.instance().uploadNewFiles(
            user,
            localPaths,
            remotePaths,
            uploadAction!!,
            // create parent folder if not existent
            true,
            UploadFileOperation.CREATED_AS_INSTANT_PICTURE,
            needsWifi,
            needsCharging,
            syncedFolder.nameCollisionPolicy
        )

        for (path in paths) {
            // TODO batch update
            filesystemDataProvider.updateFilesystemFileAsSentForUpload(
                path,
                syncedFolder.id.toString()
            )
        }
    }

    private fun getRemotePath(
        file: File,
        syncedFolder: SyncedFolder,
        sFormatter: SimpleDateFormat,
        lightVersion: Boolean,
        resources: Resources,
        currentLocale: Locale
    ): String {
        val lastModificationTime = calculateLastModificationTime(file, syncedFolder, sFormatter)
        val remoteFolder: String
        val useSubfolders: Boolean
        val subFolderRule: SubFolderRule
        if (lightVersion) {
            useSubfolders = resources.getBoolean(R.bool.syncedFolder_light_use_subfolders)
            remoteFolder = resources.getString(R.string.syncedFolder_remote_folder)
            subFolderRule = SubFolderRule.YEAR_MONTH
        } else {
            useSubfolders = syncedFolder.isSubfolderByDate
            remoteFolder = syncedFolder.remotePath
            subFolderRule = syncedFolder.subfolderRule
        }
        return FileStorageUtils.getInstantUploadFilePath(
            file,
            currentLocale,
            remoteFolder,
            syncedFolder.localPath,
            lastModificationTime,
            useSubfolders,
            subFolderRule
        )
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
            "LOCAL_BEHAVIOUR_FORGET" -> FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
            "LOCAL_BEHAVIOUR_MOVE" -> FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
            "LOCAL_BEHAVIOUR_DELETE" -> FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
            else -> FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
        }
    }
}
