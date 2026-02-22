/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.app.Notification
import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.database.entity.toOCUpload
import com.nextcloud.client.database.entity.toUploadEntity
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadBroadcastManager
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.jobs.utils.UploadErrorNotificationManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.extensions.getLog
import com.nextcloud.utils.extensions.isNonRetryable
import com.nextcloud.utils.extensions.updateStatus
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Suppress("LongParameterList", "TooManyFunctions", "TooGenericExceptionCaught")
class AutoUploadWorker(
    private val context: Context,
    params: WorkerParameters,
    private val userAccountManager: UserAccountManager,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager,
    private val repository: FileSystemRepository,
    val viewThemeUtils: ViewThemeUtils,
    localBroadcastManager: LocalBroadcastManager
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "🔄📤" + "AutoUpload"
        const val OVERRIDE_POWER_SAVING = "overridePowerSaving"
        const val CONTENT_URIS = "content_uris"
        const val SYNCED_FOLDER_ID = "syncedFolderId"
        const val NOTIFICATION_ID = 266
    }

    private val helper = AutoUploadHelper()
    private val syncFolderHelper = SyncFolderHelper(context)
    private val fileUploadBroadcastManager = FileUploadBroadcastManager(localBroadcastManager)
    private lateinit var syncedFolder: SyncedFolder
    private val notificationManager = AutoUploadNotificationManager(context, viewThemeUtils, NOTIFICATION_ID)

    @Suppress("ReturnCount")
    override suspend fun doWork(): Result {
        return try {
            val syncFolderId = inputData.getLong(SYNCED_FOLDER_ID, -1)
            syncedFolder = syncedFolderProvider.getSyncedFolderByID(syncFolderId)
                ?.takeIf { it.isEnabled } ?: return Result.failure()

            Log_OC.d(TAG, syncedFolder.getLog())

            /**
             * Receives from [com.nextcloud.client.jobs.ContentObserverWork.checkAndTriggerAutoUpload]
             */
            val contentUris = inputData.getStringArray(CONTENT_URIS)

            if (canExitEarly(contentUris, syncFolderId)) {
                return Result.retry()
            }

            if (powerManagementService.isPowerSavingEnabled) {
                Log_OC.w(TAG, "power saving mode enabled")
            }

            collectFileChangesFromContentObserverWork(contentUris)
            uploadFiles(syncedFolder)

            // only update last scan time after uploading files
            syncedFolder.lastScanTimestampMs = System.currentTimeMillis()
            syncedFolderProvider.updateSyncFolder(syncedFolder)

            Log_OC.d(TAG, "✅ ${syncedFolder.remotePath} completed")
            Result.success()
        } catch (e: Exception) {
            Log_OC.e(TAG, "❌ failed: ${e.message}")
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createNotification(context.getString(R.string.upload_files))
        return notificationManager.getForegroundInfo(notification)
    }

    private suspend fun updateNotification() {
        getStartNotificationTitle()?.let { (localFolderName, remoteFolderName) ->
            try {
                val startNotification = createNotification(
                    context.getString(
                        R.string.auto_upload_worker_start_text,
                        localFolderName,
                        remoteFolderName
                    )
                )

                setForeground(notificationManager.getForegroundInfo(startNotification))
            } catch (e: Exception) {
                Log_OC.w(TAG, "⚠️ Could not update notification: ${e.message}")
            }
        }
    }

    private suspend fun trySetForeground() {
        try {
            val notification = createNotification(context.getString(R.string.upload_files))
            setForeground(notificationManager.getForegroundInfo(notification))
        } catch (e: Exception) {
            Log_OC.w(TAG, "⚠️ Could not set foreground service: ${e.message}")
        }
    }

    private fun createNotification(title: String): Notification = notificationManager.notificationBuilder
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
    private fun canExitEarly(contentUris: Array<String>?, syncedFolderID: Long): Boolean {
        val overridePowerSaving = inputData.getBoolean(OVERRIDE_POWER_SAVING, false)
        if ((powerManagementService.isPowerSavingEnabled && !overridePowerSaving)) {
            Log_OC.w(TAG, "⚡ Skipping: device is in power saving mode")
            return true
        }

        if (syncedFolderID < 0) {
            Log_OC.e(TAG, "invalid sync folder id")
            return true
        }

        if (backgroundJobManager.isAutoUploadWorkerRunning(syncedFolderID)) {
            Log_OC.w(TAG, "🚧 another worker is already running for $syncedFolderID")
            return true
        }

        val totalScanInterval = syncedFolder.getTotalScanInterval(connectivityService, powerManagementService)
        val currentTime = System.currentTimeMillis()
        val passedScanInterval = totalScanInterval <= currentTime

        if (!passedScanInterval && contentUris.isNullOrEmpty() && !overridePowerSaving) {
            Log_OC.w(
                TAG,
                "skipped since started before scan interval and nothing todo: " + syncedFolder.localPath
            )
            return true
        }

        Log_OC.d(TAG, "starting ...")

        return false
    }

    /**
     * Instead of scanning the entire local folder, optional content URIs can be passed to the worker
     * to detect only the relevant changes.
     */
    @Suppress("MagicNumber", "TooGenericExceptionCaught")
    private suspend fun collectFileChangesFromContentObserverWork(contentUris: Array<String>?) = try {
        Log_OC.d(TAG, "collecting file changes")

        withContext(Dispatchers.IO) {
            if (contentUris.isNullOrEmpty()) {
                Log_OC.d(TAG, "inserting all entries")
                helper.insertEntries(syncedFolder, repository)
            } else {
                Log_OC.d(TAG, "inserting changed entries")
                val isContentUrisStored = helper.insertChangedEntries(syncedFolder, contentUris, repository)
                if (!isContentUrisStored) {
                    Log_OC.w(
                        TAG,
                        "changed content uris not stored, fallback to insert all db entries to not lose files"
                    )

                    helper.insertEntries(syncedFolder, repository)
                }
            }
        }
    } catch (e: Exception) {
        Log_OC.d(TAG, "Exception collectFileChangesFromContentObserverWork: $e")
    }

    private fun getUserOrReturn(syncedFolder: SyncedFolder): User? {
        val optionalUser = userAccountManager.getUser(syncedFolder.account)
        if (!optionalUser.isPresent) {
            Log_OC.w(TAG, "user not present")
            return null
        }
        return optionalUser.get()
    }

    @Suppress("DEPRECATION")
    private fun getUploadSettings(syncedFolder: SyncedFolder): Triple<Boolean, Boolean, Int> {
        val lightVersion = context.resources.getBoolean(R.bool.syncedFolder_light)
        val accountName = syncedFolder.account

        return if (lightVersion) {
            Log_OC.d(TAG, "light version is used")
            val arbitraryDataProvider = ArbitraryDataProviderImpl(context)
            val needsCharging = context.resources.getBoolean(R.bool.syncedFolder_light_on_charging)
            val needsWifi = arbitraryDataProvider.getBooleanValue(
                accountName,
                SettingsActivity.SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI
            )
            val uploadActionString = context.resources.getString(R.string.syncedFolder_light_upload_behaviour)
            val uploadAction = FileUploadWorker.getUploadAction(uploadActionString)
            Log_OC.d(TAG, "upload action is: $uploadAction")
            Triple(needsCharging, needsWifi, uploadAction)
        } else {
            Log_OC.d(TAG, "not light version is used")
            Triple(syncedFolder.isChargingOnly, syncedFolder.isWifiOnly, syncedFolder.uploadAction)
        }
    }

    @Suppress("LongMethod", "DEPRECATION", "TooGenericExceptionCaught")
    private suspend fun uploadFiles(syncedFolder: SyncedFolder) = withContext(Dispatchers.IO) {
        val user = getUserOrReturn(syncedFolder) ?: return@withContext
        val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(ocAccount, context)

        trySetForeground()
        updateNotification()

        var lastId = 0

        while (true) {
            val filePathsWithIds = repository.getFilePathsWithIds(syncedFolder, lastId)

            if (filePathsWithIds.isEmpty()) {
                Log_OC.w(TAG, "no more files to upload at lastId: $lastId")
                break
            }
            Log_OC.d(TAG, "started, processing batch: lastId=$lastId, count=${filePathsWithIds.size}")

            filePathsWithIds.forEachIndexed { batchIndex, (path, id) ->
                val file = File(path)
                val localPath = file.absolutePath
                val remotePath = syncFolderHelper.getAutoUploadRemotePath(syncedFolder, file)

                try {
                    val entityResult = getEntityResult(user, localPath, remotePath)
                    if (entityResult !is AutoUploadEntityResult.Success) {
                        repository.markFileAsHandled(localPath, syncedFolder)
                        Log_OC.d(TAG, "marked file as handled: $localPath")
                        continue
                    }

                    var (uploadEntity, upload) = entityResult.data

                    // if local file deleted, upload process cannot be started or retriable thus needs to be removed
                    if (path.isEmpty() || !file.exists()) {
                        Log_OC.w(TAG, "detected non-existing local file, removing entity")
                        deleteNonExistingFile(path, id, upload)
                        continue
                    }

                    try {
                        // Insert/update to IN_PROGRESS state before starting upload
                        val generatedId = uploadsStorageManager.uploadDao.insertOrReplace(uploadEntity)
                        uploadEntity = uploadEntity.copy(id = generatedId.toInt())
                        upload.uploadId = generatedId

                        fileUploadBroadcastManager.sendAdded(context)
                        val operation = createUploadFileOperation(upload, user)
                        Log_OC.d(TAG, "🕒 uploading: $localPath, id: $generatedId")

                        val result = operation.execute(client)
                        fileUploadBroadcastManager.sendStarted(operation, context)

                        UploadErrorNotificationManager.handleResult(
                            context,
                            notificationManager,
                            operation,
                            result
                        )

                        if (result.isSuccess) {
                            repository.markFileAsHandled(localPath, syncedFolder)
                            Log_OC.d(TAG, "✅ upload completed: $localPath")
                        } else {
                            Log_OC.e(
                                TAG,
                                "❌ upload failed $localPath (${upload.accountName}): ${result.logMessage}"
                            )

                            // Mark CONFLICT files as handled to prevent retries
                            if (result.code == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                                repository.markFileAsHandled(localPath, syncedFolder)
                                Log_OC.w(TAG, "Marked CONFLICT file as handled: $localPath")
                            }
                        }

                        val isLastInBatch = (batchIndex == filePathsWithIds.size - 1)
                        if (isLastInBatch) {
                            sendUploadFinishEvent(operation, result)
                        }
                    } catch (e: Exception) {
                        uploadsStorageManager.updateStatus(
                            uploadEntity,
                            UploadsStorageManager.UploadStatus.UPLOAD_FAILED
                        )
                        Log_OC.e(
                            TAG,
                            "Exception during upload file, localPath: $localPath, remotePath: $remotePath," +
                                " exception: $e"
                        )

                        if (path.isEmpty() || !file.exists()) {
                            Log_OC.w(TAG, "detected non-existing local file, removing entity")
                            deleteNonExistingFile(path, id, upload)
                            continue
                        }
                    }
                } catch (e: Exception) {
                    Log_OC.e(
                        TAG,
                        "Exception uploadFiles during creating entity and upload, localPath: $localPath, " +
                            "remotePath: $remotePath, exception: $e"
                    )
                } finally {
                    // update last id so upload can continue where it left
                    lastId = id
                }
            }
        }
    }

    private suspend fun deleteNonExistingFile(path: String, id: Int, upload: OCUpload) {
        repository.deleteByLocalPathAndId(path, id)
        uploadsStorageManager.removeUpload(upload)
    }

    @Suppress("ReturnCount")
    private fun getEntityResult(user: User, localPath: String, remotePath: String): AutoUploadEntityResult {
        val (needsCharging, needsWifi, uploadAction) = getUploadSettings(syncedFolder)
        Log_OC.d(TAG, "creating oc upload for ${user.accountName}")

        // Get existing upload or create new one
        val uploadEntity = uploadsStorageManager.uploadDao.getUploadByAccountAndPaths(
            localPath = localPath,
            remotePath = remotePath,
            accountName = user.accountName
        )

        val lastUploadResult = uploadEntity?.lastResult?.let { UploadResult.fromValue(it) }
        if (lastUploadResult?.isNonRetryable() == true) {
            Log_OC.w(
                TAG,
                "last upload failed with ${lastUploadResult.value}, skipping auto-upload: $localPath"
            )
            return AutoUploadEntityResult.NonRetryable
        }

        val upload = try {
            uploadEntity?.toOCUpload(null) ?: OCUpload(localPath, remotePath, user.accountName)
        } catch (_: IllegalArgumentException) {
            Log_OC.e(TAG, "cannot construct oc upload")
            return AutoUploadEntityResult.CreationError
        }

        // only valid for skip collision policy other scenarios will be handled in UploadFileOperation.java
        if (upload.lastResult == UploadResult.UPLOADED &&
            syncedFolder.nameCollisionPolicy == NameCollisionPolicy.SKIP
        ) {
            Log_OC.d(TAG, "no need to create and process this entity file is already uploaded")
            return AutoUploadEntityResult.Uploaded
        }

        upload.apply {
            uploadStatus = UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS
            nameCollisionPolicy = syncedFolder.nameCollisionPolicy
            isUseWifiOnly = needsWifi
            isWhileChargingOnly = needsCharging
            localAction = uploadAction

            // Only set these for new uploads
            if (uploadEntity == null) {
                createdBy = UploadFileOperation.CREATED_AS_INSTANT_PICTURE
                isCreateRemoteFolder = true
            }
        }

        return AutoUploadEntityResult.Success(upload.toUploadEntity() to upload)
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

    private fun sendUploadFinishEvent(operation: UploadFileOperation, result: RemoteOperationResult<*>) {
        fileUploadBroadcastManager.sendFinished(
            operation,
            result,
            operation.oldFile?.storagePath,
            context
        )
    }
}
