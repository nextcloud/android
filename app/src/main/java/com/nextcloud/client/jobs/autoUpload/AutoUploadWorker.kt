/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.ForegroundServiceHelper
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ForegroundServiceType
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
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

@Suppress("LongParameterList", "TooManyFunctions")
class AutoUploadWorker(
    private val context: Context,
    params: WorkerParameters,
    private val userAccountManager: UserAccountManager,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager,
    private val repository: FileSystemRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "🔄📤" + "AutoUpload"
        const val OVERRIDE_POWER_SAVING = "overridePowerSaving"
        const val CHANGED_FILES = "changedFiles"
        const val SYNCED_FOLDER_ID = "syncedFolderId"
        private const val CHANNEL_ID = NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD

        private const val NOTIFICATION_ID = 266
    }

    private lateinit var syncedFolder: SyncedFolder
    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun doWork(): Result {
        return try {
            val syncFolderId = inputData.getLong(SYNCED_FOLDER_ID, -1)
            syncedFolder = syncedFolderProvider.getSyncedFolderByID(syncFolderId)
                ?.takeIf { it.isEnabled } ?: return Result.failure()

            // initial notification
            val notification = createNotification(context.getString(R.string.upload_files))
            updateForegroundInfo(notification)

            val changedFiles = inputData.getStringArray(CHANGED_FILES)

            if (canExitEarly(changedFiles, syncFolderId)) {
                return Result.retry()
            }

            collectFileChangesFromContentObserverWork(changedFiles)
            updateNotification()
            uploadFiles(syncedFolder)

            Log_OC.d(TAG, "✅ ${syncedFolder.remotePath} finished checking files.")
            Result.success()
        } catch (e: Exception) {
            Log_OC.e(TAG, "❌ failed: ${e.message}")
            Result.failure()
        }
    }

    private fun updateNotification() {
        getStartNotificationTitle()?.let { (localFolderName, remoteFolderName) ->
            val startNotification = createNotification(
                context.getString(
                    R.string.auto_upload_worker_start_text,
                    localFolderName,
                    remoteFolderName
                )
            )

            notificationManager.notify(NOTIFICATION_ID, startNotification)
        }
    }

    private suspend fun updateForegroundInfo(notification: Notification) {
        val foregroundInfo = ForegroundServiceHelper.createWorkerForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ForegroundServiceType.DataSync
        )
        setForeground(foregroundInfo)
    }

    private fun createNotification(title: String): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setSmallIcon(R.drawable.uploads)
        .setOngoing(true)
        .setSound(null)
        .setVibrate(null)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .build()

    @Suppress("TooGenericExceptionCaught")
    private fun getStartNotificationTitle(): Pair<String, String>? = try {
        val localPath = syncedFolder.localPath
        val remotePath = syncedFolder.remotePath
        if (localPath.isBlank() || remotePath.isBlank()) {
            null
        } else {
            try {
                File(localPath).name to File(remotePath).name
            } catch (_: Exception) {
                null
            }
        }
    } catch (_: Exception) {
        null
    }

    @Suppress("ReturnCount")
    private fun canExitEarly(changedFiles: Array<String>?, syncedFolderID: Long): Boolean {
        val overridePowerSaving = inputData.getBoolean(OVERRIDE_POWER_SAVING, false)
        if ((powerManagementService.isPowerSavingEnabled && !overridePowerSaving)) {
            Log_OC.w(TAG, "⚡ Skipping: device is in power saving mode")
            return true
        }

        if (syncedFolderID < 0) {
            Log_OC.e(TAG, "invalid sync folder id")
            return true
        }

        if (backgroundJobManager.bothFilesSyncJobsRunning(syncedFolderID)) {
            Log_OC.w(TAG, "🚧 another worker is already running for $syncedFolderID")
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

        if (!passedScanInterval && changedFiles.isNullOrEmpty() && !overridePowerSaving) {
            Log_OC.w(
                TAG,
                "skipped since started before scan interval and nothing todo: " + syncedFolder.localPath
            )
            return true
        }

        return false
    }

    @Suppress("MagicNumber", "TooGenericExceptionCaught")
    private suspend fun collectFileChangesFromContentObserverWork(changedFiles: Array<String>?) = try {
        withContext(Dispatchers.IO) {
            if (changedFiles.isNullOrEmpty()) {
                // Check every file in synced folder for changes and update
                // filesystemDataProvider database (potentially needs a long time)
                FilesSyncHelper.insertAllDBEntriesForSyncedFolder(syncedFolder)
            } else {
                FilesSyncHelper.insertChangedEntries(syncedFolder, changedFiles)
            }
            syncedFolder.lastScanTimestampMs = System.currentTimeMillis()
            syncedFolderProvider.updateSyncFolder(syncedFolder)
        }
    } catch (e: Exception) {
        Log_OC.d(TAG, "Exception collectFileChangesFromContentObserverWork: $e")
    }

    private fun prepareDateFormat(): SimpleDateFormat {
        val currentLocale = context.resources.configuration.locales[0]
        return SimpleDateFormat("yyyy:MM:dd HH:mm:ss", currentLocale).apply {
            timeZone = TimeZone.getTimeZone(TimeZone.getDefault().id)
        }
    }

    private fun getUserOrReturn(syncedFolder: SyncedFolder): User? {
        val optionalUser = userAccountManager.getUser(syncedFolder.account)
        if (!optionalUser.isPresent) {
            Log_OC.w(TAG, "uploadFilesFromFolder skipped user not present")
            return null
        }
        return optionalUser.get()
    }

    private fun buildPathsAndMimes(
        paths: Set<String>,
        syncedFolder: SyncedFolder,
        dateFormat: SimpleDateFormat
    ): List<Triple<String, String, String>> {
        val lightVersion = context.resources.getBoolean(R.bool.syncedFolder_light)
        val currentLocale = context.resources.configuration.locales[0]

        return paths.map { path ->
            val file = File(path)
            val localPath = file.absolutePath
            val remotePath =
                getRemotePath(file, syncedFolder, dateFormat, lightVersion, context.resources, currentLocale)
            val mimeType = MimeTypeUtil.getBestMimeTypeByFilename(localPath)
            Triple(localPath, remotePath, mimeType)
        }
    }

    @Suppress("DEPRECATION")
    private fun getUploadSettings(syncedFolder: SyncedFolder): Triple<Boolean, Boolean, Int> {
        val lightVersion = context.resources.getBoolean(R.bool.syncedFolder_light)
        val accountName = syncedFolder.account

        return if (lightVersion) {
            Log_OC.d(TAG, "uploadFilesFromFolder light version is used")
            val arbitraryDataProvider = ArbitraryDataProviderImpl(context)
            val needsCharging = context.resources.getBoolean(R.bool.syncedFolder_light_on_charging)
            val needsWifi = arbitraryDataProvider.getBooleanValue(
                accountName,
                SettingsActivity.SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI
            )
            val uploadActionString = context.resources.getString(R.string.syncedFolder_light_upload_behaviour)
            val uploadAction = getUploadAction(uploadActionString)
            Log_OC.d(TAG, "upload action is: $uploadAction")
            Triple(needsCharging, needsWifi, uploadAction)
        } else {
            Log_OC.d(TAG, "getUploadSettings not light version is used")
            Triple(syncedFolder.isChargingOnly, syncedFolder.isWifiOnly, syncedFolder.uploadAction)
        }
    }

    @Suppress("LongMethod", "DEPRECATION", "TooGenericExceptionCaught")
    private suspend fun uploadFiles(syncedFolder: SyncedFolder) = withContext(Dispatchers.IO) {
        val dateFormat = prepareDateFormat()
        val user = getUserOrReturn(syncedFolder) ?: return@withContext
        val paths = repository.getAutoUploadFiles(syncedFolder)
        if (paths.isEmpty()) {
            Log_OC.w(TAG, "uploadFiles skipped paths is empty")
            return@withContext
        }

        val pathsAndMimes = buildPathsAndMimes(paths, syncedFolder, dateFormat)
        val (needsCharging, needsWifi, uploadAction) = getUploadSettings(syncedFolder)

        val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(ocAccount, context)

        pathsAndMimes.forEach { (localPath, remotePath, _) ->
            try {
                Log_OC.d(TAG, "creating oc upload for ${user.accountName}")
                val upload = OCUpload(localPath, remotePath, user.accountName).apply {
                    nameCollisionPolicy = syncedFolder.nameCollisionPolicy
                    isUseWifiOnly = needsWifi
                    isWhileChargingOnly = needsCharging
                    uploadStatus = UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS
                    createdBy = UploadFileOperation.CREATED_AS_INSTANT_PICTURE
                    isCreateRemoteFolder = true
                    localAction = uploadAction
                }

                uploadsStorageManager.storeUpload(upload)

                val operation = createUploadFileOperation(upload, user)
                Log_OC.d(TAG, "🕒 uploading: $localPath")

                val result = operation.execute(client)
                if (result.isSuccess) {
                    repository.markFileAsUploaded(localPath, syncedFolder)
                    Log_OC.d(TAG, "✅ auto upload completed: $localPath")
                } else {
                    Log_OC.e(TAG, "❌ auto upload failed: $localPath")
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Exception uploadFiles, localPath: $localPath, remotePath: $remotePath, exception: $e")
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

        val (remoteFolder, useSubfolders, subFolderRule) = if (lightVersion) {
            Triple(
                resources.getString(R.string.syncedFolder_remote_folder),
                resources.getBoolean(R.bool.syncedFolder_light_use_subfolders),
                SubFolderRule.YEAR_MONTH
            )
        } else {
            Triple(
                syncedFolder.remotePath,
                syncedFolder.isSubfolderByDate,
                syncedFolder.subfolderRule
            )
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

    @Suppress("NestedBlockDepth")
    private fun calculateLastModificationTime(
        file: File,
        syncedFolder: SyncedFolder,
        formatter: SimpleDateFormat
    ): Long {
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
                        Log_OC.w(TAG, "calculateLastModificationTime calculatedTime is: $lastModificationTime")
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

    private fun getUploadAction(action: String): Int = when (action) {
        "LOCAL_BEHAVIOUR_FORGET" -> FileUploadWorker.Companion.LOCAL_BEHAVIOUR_FORGET
        "LOCAL_BEHAVIOUR_MOVE" -> FileUploadWorker.Companion.LOCAL_BEHAVIOUR_MOVE
        "LOCAL_BEHAVIOUR_DELETE" -> FileUploadWorker.Companion.LOCAL_BEHAVIOUR_DELETE
        else -> FileUploadWorker.Companion.LOCAL_BEHAVIOUR_FORGET
    }
}
