/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Jonas Mayer <jonas.mayer@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.autoUpload.FilesystemRepository
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.ForegroundServiceHelper
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.FilesystemDataProvider
import com.owncloud.android.datamodel.ForegroundServiceType
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Suppress("LongParameterList")
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
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "🔄📤" + "AutoUpload"
        const val OVERRIDE_POWER_SAVING = "overridePowerSaving"
        const val CHANGED_FILES = "changedFiles"
        const val SYNCED_FOLDER_ID = "syncedFolderId"

        private const val NOTIFICATION_ID = 266
    }

    private lateinit var syncedFolder: SyncedFolder

    @Suppress("MagicNumber", "ReturnCount", "LongMethod", "TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        return try {
            val syncFolderId = inputData.getLong(SYNCED_FOLDER_ID, -1)
            val syncedFolder = syncedFolderProvider.getSyncedFolderByID(syncFolderId)
            if (syncedFolder == null || !syncedFolder.isEnabled) {
                Log_OC.e(TAG, "skipped since syncedFolder ($syncFolderId) is not enabled!")
                return Result.failure()
            }
            this.syncedFolder = syncedFolder

            setForeground(createForegroundInfo())

            if (canExitEarly(syncFolderId)) {
                Log_OC.w(TAG, "skipped canExit conditions are met")
                return Result.failure()
            }

            collectFileChangesFromContentObserverWork()
            uploadFiles(syncedFolder)

            Log_OC.d(TAG, "✅ (${syncedFolder.remotePath}) finished checking files.")
            Result.success()
        } catch (e: Exception) {
            Log_OC.e(TAG, "❌ failed: ${e.message}")
            Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD

        val channel = NotificationChannel(
            channelId,
            applicationContext.getString(R.string.notification_channel_upload_name_short),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(context.getString(R.string.upload_files))
            .setSmallIcon(R.drawable.uploads)
            .setOngoing(true)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()

        return ForegroundServiceHelper.createWorkerForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ForegroundServiceType.DataSync
        )
    }

    @Suppress("ReturnCount")
    private fun canExitEarly(syncedFolderID: Long): Boolean {
        // If we are in power save mode better to postpone scan and upload
        val overridePowerSaving = inputData.getBoolean(OVERRIDE_POWER_SAVING, false)
        if ((powerManagementService.isPowerSavingEnabled && !overridePowerSaving)) {
            Log_OC.w(TAG, "skipped powerSaving is enabled!")
            return true
        }

        if (syncedFolderID < 0) {
            Log_OC.w(TAG, "skipped no valid syncedFolderID provided")
            return true
        }

        // or sync worker already running
        if (backgroundJobManager.bothFilesSyncJobsRunning(syncedFolderID)) {
            Log_OC.w(TAG, "skipped another worker instance is running for $syncedFolderID")
            return true
        }

        val calculatedScanInterval =
            FilesSyncHelper.calculateScanInterval(syncedFolder, connectivityService, powerManagementService)
        val totalScanInterval = (syncedFolder.lastScanTimestampMs + calculatedScanInterval)
        val currentTime = System.currentTimeMillis()
        val passedScanInterval = totalScanInterval <= currentTime

        Log_OC.d(TAG, "lastScanTimestampMs: " + syncedFolder.lastScanTimestampMs)
        Log_OC.d(TAG, "calculatedScanInterval: $calculatedScanInterval")
        Log_OC.d(TAG, "totalScanInterval: $totalScanInterval")
        Log_OC.d(TAG, "currentTime: $currentTime")
        Log_OC.d(TAG, "passedScanInterval: $passedScanInterval")

        if (!passedScanInterval && !overridePowerSaving) {
            Log_OC.w(
                TAG,
                "skipped since started before scan interval and nothing todo: " + syncedFolder.localPath
            )
            return true
        }

        if (syncedFolder.isChargingOnly &&
            !powerManagementService.battery.isCharging &&
            !powerManagementService.battery.isFull
        ) {
            Log_OC.w(
                TAG,
                "skipped since phone is not charging: " + syncedFolder.localPath
            )
            return true
        }

        return false
    }

    @Suppress("MagicNumber")
    private suspend fun collectFileChangesFromContentObserverWork() = withContext(Dispatchers.IO) {
        val changedFiles = inputData.getStringArray(CHANGED_FILES)

        if (!changedFiles.isNullOrEmpty()) {
            FilesSyncHelper.insertChangedEntries(syncedFolder, changedFiles)
        } else {
            // Check every file in synced folder for changes and update
            // filesystemDataProvider database (potentially needs a long time)
            FilesSyncHelper.insertAllDBEntriesForSyncedFolder(syncedFolder)
        }
        syncedFolder.lastScanTimestampMs = System.currentTimeMillis()
        syncedFolderProvider.updateSyncFolder(syncedFolder)
    }

    @Suppress("LongMethod")
    private suspend fun uploadFiles(syncedFolder: SyncedFolder) = withContext(Dispatchers.IO) {
        val lightVersion = context.resources.getBoolean(R.bool.syncedFolder_light)
        val filesystemDataProvider = FilesystemDataProvider(contentResolver)
        val currentLocale = context.resources.configuration.locales.get(0)
        val dateFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", currentLocale)
        dateFormat.timeZone = TimeZone.getTimeZone(TimeZone.getDefault().id)

        val uploadAction: Int?
        val needsCharging: Boolean
        val needsWifi: Boolean
        val accountName = syncedFolder.account

        val optionalUser = userAccountManager.getUser(accountName)
        if (!optionalUser.isPresent) {
            Log_OC.w(TAG, "uploadFilesFromFolder skipped user not present")
            return@withContext
        }

        val user = optionalUser.get()
        val arbitraryDataProvider = if (lightVersion) {
            ArbitraryDataProviderImpl(context)
        } else {
            null
        }

        // Ensure only new files are processed for upload.
        // Files that have been previously uploaded cannot be re-uploaded,
        // even if they have been deleted or moved from the target folder,
        // as they are already marked as uploaded in the database.
        val dao = NextcloudDatabase.getInstance(MainApp.getAppContext()).fileSystemDao()
        val repository = FilesystemRepository(dao)
        val paths = repository.getAutoUploadFiles(syncedFolder)
        if (paths.isEmpty()) {
            Log_OC.w(TAG, "uploadFilesFromFolder skipped paths is empty")
            return@withContext
        }

        val pathsAndMimes = paths.map { path ->
            val file = File(path)
            val localPath = file.absolutePath
            val remotePath = getRemotePath(file, syncedFolder, dateFormat, lightVersion, context.resources, currentLocale)
            val mimeType = MimeTypeUtil.getBestMimeTypeByFilename(localPath)

            Log_OC.d(TAG, "pathsAndMimes file.path: ${file.path}")
            Log_OC.d(TAG, "pathsAndMimes localPath: $localPath")
            Log_OC.d(TAG, "pathsAndMimes remotePath: $remotePath")
            Log_OC.d(TAG, "pathsAndMimes mimeType: $mimeType")

            Triple(
                localPath,
                remotePath,
                mimeType
            )
        }

        if (lightVersion) {
            Log_OC.d(TAG, "uploadFilesFromFolder light version is used")

            needsCharging = context.resources.getBoolean(R.bool.syncedFolder_light_on_charging)
            needsWifi = arbitraryDataProvider?.getBooleanValue(
                accountName,
                SettingsActivity.SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI
            ) ?: true

            val uploadActionString = context.resources.getString(R.string.syncedFolder_light_upload_behaviour)
            uploadAction = getUploadAction(uploadActionString)
            Log_OC.d(TAG, "upload action is: $uploadAction")
        } else {
            Log_OC.d(TAG, "uploadFilesFromFolder not light version is used")

            needsCharging = syncedFolder.isChargingOnly
            needsWifi = syncedFolder.isWifiOnly
            uploadAction = syncedFolder.uploadAction
        }

        val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(ocAccount, context)

        pathsAndMimes.forEach { (localPath, remotePath, _) ->
            // create oc upload
            val upload = OCUpload(localPath, remotePath, user.accountName).apply {
                nameCollisionPolicy = syncedFolder.nameCollisionPolicy
                isUseWifiOnly = needsWifi
                isWhileChargingOnly = needsCharging
                uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
                createdBy = UploadFileOperation.CREATED_AS_INSTANT_PICTURE
                isCreateRemoteFolder = true
                localAction = uploadAction
            }

            // store upload
            uploadsStorageManager.storeUpload(upload)

            // upload
            val operation = createUploadFileOperation(upload, user)
            Log_OC.d(TAG, "🕒 uploading: $localPath")

            val result = operation.execute(client)
            if (result.isSuccess) {
                // mark upload as successful
                repository.markFileAsUploaded(localPath, syncedFolder)
                Log_OC.d(TAG, "✅ auto upload completed: $localPath")
            } else {
                Log_OC.e(TAG, "❌ auto upload failed: $localPath")
            }
        }
    }

    private fun createUploadFileOperation(upload: OCUpload, user: User): UploadFileOperation = UploadFileOperation(
        uploadsStorageManager,
        connectivityService,
        powerManagementService,
        user,
        null,
        upload,
        upload.nameCollisionPolicy,
        upload.localAction,
        context,
        upload.isUseWifiOnly,
        upload.isWhileChargingOnly,
        true,
        FileDataStorageManager(user, context.contentResolver)
    )

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
            Log_OC.d(TAG, "getRemotePath light version is used")

            useSubfolders = resources.getBoolean(R.bool.syncedFolder_light_use_subfolders)
            remoteFolder = resources.getString(R.string.syncedFolder_remote_folder)
            subFolderRule = SubFolderRule.YEAR_MONTH
        } else {
            Log_OC.d(TAG, "getRemotePath not light version is used")

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
            Log_OC.d(TAG, "calculateLastModificationTime exif found")

            @Suppress("TooGenericExceptionCaught") // legacy code
            try {
                val exifInterface = ExifInterface(file.absolutePath)
                val exifDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                if (!TextUtils.isEmpty(exifDate)) {
                    val pos = ParsePosition(0)
                    val dateTime = formatter.parse(exifDate, pos)
                    lastModificationTime = dateTime.time
                    Log_OC.w(TAG, "calculateLastModificationTime calculatedTime is: $lastModificationTime")
                } else {
                    Log_OC.w(TAG, "calculateLastModificationTime exifDate is empty")
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "Failed to get the proper time " + e.localizedMessage)
            }
        }
        return lastModificationTime
    }

    private fun getUploadAction(action: String): Int = when (action) {
        "LOCAL_BEHAVIOUR_FORGET" -> FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
        "LOCAL_BEHAVIOUR_MOVE" -> FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
        "LOCAL_BEHAVIOUR_DELETE" -> FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
        else -> FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
    }
}
