/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.upload

import android.app.Notification
import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.utils.UploadErrorNotificationManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.ForegroundServiceHelper
import com.nextcloud.utils.extensions.getPercent
import com.nextcloud.utils.extensions.updateStatus
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ForegroundServiceType
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.albums.CopyFileToAlbumOperation
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * this worker is a replica of FileUploadWorker
 * this worker will take care of upload and then copying the uploaded files to selected Album
 */
@Suppress("LongParameterList", "TooGenericExceptionCaught")
class AlbumFileUploadWorker(
    val uploadsStorageManager: UploadsStorageManager,
    val connectivityService: ConnectivityService,
    val powerManagementService: PowerManagementService,
    val userAccountManager: UserAccountManager,
    val viewThemeUtils: ViewThemeUtils,
    val localBroadcastManager: LocalBroadcastManager,
    private val backgroundJobManager: BackgroundJobManager,
    val preferences: AppPreferences,
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params),
    OnDatatransferProgressListener {

    companion object {
        val TAG: String = AlbumFileUploadWorker::class.java.simpleName

        var currentUploadFileOperation: UploadFileOperation? = null

        private const val BATCH_SIZE = 100

        const val ALBUM_NAME = "album_name"

        const val ACCOUNT = "data_account"
        const val UPLOAD_IDS = "uploads_ids"
        const val CURRENT_BATCH_INDEX = "batch_index"
        const val TOTAL_UPLOAD_SIZE = "total_upload_size"
    }

    private var lastPercent = 0
    private val notificationId = Random.nextInt()
    private val notificationManager = UploadNotificationManager(context, viewThemeUtils, notificationId)
    private val intents = FileUploaderIntents(context)
    private val fileUploadBroadcastManager = FileUploadBroadcastManager(localBroadcastManager)

    override suspend fun doWork(): Result = try {
        Log_OC.d(TAG, "AlbumFileUploadWorker started")
        val workerName = BackgroundJobManagerImpl.formatClassTag(this::class)
        backgroundJobManager.logStartOfWorker(workerName)

        trySetForeground()

        val result = uploadFiles()
        backgroundJobManager.logEndOfWorker(workerName, result)
        notificationManager.dismissNotification()
        result
    } catch (t: Throwable) {
        Log_OC.e(TAG, "exception $t")
        currentUploadFileOperation?.cancel(null)
        Result.failure()
    } finally {
        // Ensure all database operations are complete before signaling completion
        uploadsStorageManager.notifyObserversNow()
        notificationManager.dismissNotification()
    }

    private suspend fun trySetForeground() {
        try {
            val notificationTitle = notificationManager.currentOperationTitle
                ?: context.getString(R.string.foreground_service_upload)

            val notification = notificationManager.createSilentNotification(notificationTitle, R.drawable.uploads)
            updateForegroundInfo(notification)
        } catch (e: Exception) {
            // Continue without foreground service - uploads will still work
            Log_OC.w(TAG, "Could not set foreground service: ${e.message}")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationTitle = notificationManager.currentOperationTitle
            ?: context.getString(R.string.foreground_service_upload)
        val notification = notificationManager.createSilentNotification(notificationTitle, R.drawable.uploads)

        return ForegroundServiceHelper.createWorkerForegroundInfo(
            notificationId,
            notification,
            ForegroundServiceType.DataSync
        )
    }

    private suspend fun updateForegroundInfo(notification: Notification) {
        val foregroundInfo = ForegroundServiceHelper.createWorkerForegroundInfo(
            notificationId,
            notification,
            ForegroundServiceType.DataSync
        )
        setForeground(foregroundInfo)
    }

    @Suppress("ReturnCount", "LongMethod", "DEPRECATION")
    private suspend fun uploadFiles(): Result = withContext(Dispatchers.IO) {
        val accountName = inputData.getString(ACCOUNT)
        if (accountName == null) {
            Log_OC.e(TAG, "accountName is null")
            return@withContext Result.failure()
        }

        val uploadIds = inputData.getLongArray(UPLOAD_IDS)
        if (uploadIds == null) {
            Log_OC.e(TAG, "uploadIds is null")
            return@withContext Result.failure()
        }

        val currentBatchIndex = inputData.getInt(CURRENT_BATCH_INDEX, -1)
        if (currentBatchIndex == -1) {
            Log_OC.e(TAG, "currentBatchIndex is -1, cancelling")
            return@withContext Result.failure()
        }

        val totalUploadSize = inputData.getInt(TOTAL_UPLOAD_SIZE, -1)
        if (totalUploadSize == -1) {
            Log_OC.e(TAG, "totalUploadSize is -1, cancelling")
            return@withContext Result.failure()
        }

        // since worker's policy is append or replace and account name comes from there no need check in the loop
        val optionalUser = userAccountManager.getUser(accountName)
        if (!optionalUser.isPresent) {
            Log_OC.e(TAG, "User not found for account: $accountName")
            return@withContext Result.failure()
        }

        val albumName = inputData.getString(ALBUM_NAME)
        if (albumName == null) {
            Log_OC.e(TAG, "album name is null")
            return@withContext Result.failure()
        }

        val user = optionalUser.get()
        val previouslyUploadedFileSize = currentBatchIndex * FileUploadHelper.MAX_FILE_COUNT
        val uploads = uploadsStorageManager.getUploadsByIds(uploadIds, accountName)
        val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
        val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)

        for ((index, upload) in uploads.withIndex()) {
            ensureActive()

            if (preferences.isGlobalUploadPaused) {
                Log_OC.d(TAG, "Upload is paused, skip uploading files!")
                notificationManager.notifyPaused(
                    intents.openUploadListIntent(null)
                )
                return@withContext Result.success()
            }

            if (canExitEarly()) {
                notificationManager.showConnectionErrorNotification()
                return@withContext Result.failure()
            }

            fileUploadBroadcastManager.sendAdded(context)
            val operation = createUploadFileOperation(upload, user)
            currentUploadFileOperation = operation

            val currentIndex = (index + 1)
            val currentUploadIndex = (currentIndex + previouslyUploadedFileSize)
            notificationManager.prepareForStart(
                operation,
                startIntent = intents.openUploadListIntent(operation),
                currentUploadIndex = currentUploadIndex,
                totalUploadSize = totalUploadSize
            )

            val result = withContext(Dispatchers.IO) {
                upload(operation, albumName, user, client)
            }
            val entity = uploadsStorageManager.uploadDao.getUploadById(upload.uploadId, accountName)
            uploadsStorageManager.updateStatus(entity, result.isSuccess)
            currentUploadFileOperation = null

            if (result.code == ResultCode.QUOTA_EXCEEDED) {
                Log_OC.w(TAG, "Quota exceeded, stopping uploads")
                notificationManager.showQuotaExceedNotification(operation)
                break
            }

            sendUploadFinishEvent(totalUploadSize, currentUploadIndex, operation, result)
        }

        return@withContext Result.success()
    }

    private fun sendUploadFinishEvent(
        totalUploadSize: Int,
        currentUploadIndex: Int,
        operation: UploadFileOperation,
        result: RemoteOperationResult<*>
    ) {
        val isLastUpload = currentUploadIndex == totalUploadSize

        val shouldBroadcast =
            (currentUploadIndex % BATCH_SIZE == 0 && totalUploadSize > BATCH_SIZE) ||
                isLastUpload

        if (shouldBroadcast) {
            fileUploadBroadcastManager.sendFinished(
                operation,
                result,
                operation.oldFile?.storagePath,
                context
            )
        }
    }

    private fun canExitEarly(): Boolean {
        val result = !connectivityService.isConnected ||
            connectivityService.isInternetWalled ||
            isStopped

        if (result) {
            Log_OC.d(TAG, "No internet connection, stopping worker.")
        } else {
            notificationManager.dismissErrorNotification()
        }

        return result
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
    ).apply {
        addDataTransferProgressListener(this@AlbumFileUploadWorker)
    }

    @Suppress("TooGenericExceptionCaught", "DEPRECATION")
    private suspend fun upload(
        operation: UploadFileOperation,
        albumName: String,
        user: User,
        client: OwnCloudClient
    ): RemoteOperationResult<Any?> = withContext(Dispatchers.IO) {
        lateinit var result: RemoteOperationResult<Any?>

        try {
            val storageManager = operation.storageManager
            result = operation.execute(client)
            val task = ThumbnailsCacheManager.ThumbnailGenerationTask(storageManager, user)
            val file = File(operation.originalStoragePath)
            val remoteId: String? = operation.file.remoteId
            task.execute(ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, remoteId))
            val copyAlbumFileOperation =
                CopyFileToAlbumOperation(operation.remotePath, albumName, storageManager)
            val copyResult = copyAlbumFileOperation.execute(client)
            if (copyResult.isSuccess) {
                Log_OC.e(TAG, "Successful copied file to Album: $albumName")
            } else {
                Log_OC.e(TAG, "Failed to copy file to Album: $albumName due to ${copyResult.logMessage}")
            }
            fileUploadBroadcastManager.sendStarted(operation, context)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error uploading", e)
            result = RemoteOperationResult(e)
        } finally {
            if (!isStopped) {
                uploadsStorageManager.updateDatabaseUploadResult(result, operation)
                // resolving file conflict will trigger normal file upload and shows two upload process
                // one for normal and one for Album upload
                // as customizing conflict can break normal upload
                // so we are removing the upload if it's a conflict
                // Note: this is fallback logic because default policy while uploading is RENAME
                // if in some case code reach here it will remove the upload
                // so we are checking it first and removing the upload
                if (result.code == ResultCode.SYNC_CONFLICT) {
                    uploadsStorageManager.removeUpload(
                        operation.user.accountName,
                        operation.remotePath
                    )
                } else {
                    UploadErrorNotificationManager.handleResult(
                        context,
                        notificationManager,
                        operation,
                        result,
                        onSameFileConflict = {
                            withContext(Dispatchers.Main) {
                                notificationManager.showSameFileAlreadyExistsNotification(operation.fileName)
                            }
                        }
                    )
                }
            }
        }

        return@withContext result
    }

    @Suppress("MagicNumber")
    private val minProgressUpdateInterval = 750
    private var lastUpdateTime = 0L

    /**
     * Receives from [com.owncloud.android.operations.UploadFileOperation.normalUpload]
     */
    @Suppress("MagicNumber")
    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        fileAbsoluteName: String
    ) {
        val percent = getPercent(totalTransferredSoFar, totalToTransfer)
        val currentTime = System.currentTimeMillis()

        if (percent != lastPercent && (currentTime - lastUpdateTime) >= minProgressUpdateInterval) {
            notificationManager.run {
                val accountName = currentUploadFileOperation?.user?.accountName
                val remotePath = currentUploadFileOperation?.remotePath

                updateUploadProgress(percent, currentUploadFileOperation)

                if (accountName != null && remotePath != null) {
                    val key: String = FileUploadHelper.buildRemoteName(accountName, remotePath)
                    val boundListener = FileUploadHelper.mBoundListeners[key]
                    val filename = currentUploadFileOperation?.fileName ?: ""

                    boundListener?.onTransferProgress(
                        progressRate,
                        totalTransferredSoFar,
                        totalToTransfer,
                        filename
                    )
                }

                dismissOldErrorNotification(currentUploadFileOperation)
            }
            lastUpdateTime = currentTime
        }

        lastPercent = percent
    }
}
