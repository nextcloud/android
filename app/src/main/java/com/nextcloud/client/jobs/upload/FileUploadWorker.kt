/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.files.FileIndicator
import com.nextcloud.client.files.FileIndicatorManager
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
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

@Suppress("LongParameterList", "TooGenericExceptionCaught")
class FileUploadWorker(
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
        val TAG: String = FileUploadWorker::class.java.simpleName

        const val NOTIFICATION_ERROR_ID: Int = 413

        const val ACCOUNT = "data_account"
        const val UPLOAD_IDS = "uploads_ids"
        const val CURRENT_BATCH_INDEX = "batch_index"
        const val TOTAL_UPLOAD_SIZE = "total_upload_size"
        const val SHOW_SAME_FILE_ALREADY_EXISTS_NOTIFICATION = "show_same_file_already_exists_notification"

        var currentUploadFileOperation: UploadFileOperation? = null

        private const val BATCH_SIZE = 100

        const val EXTRA_UPLOAD_RESULT = "RESULT"
        const val EXTRA_REMOTE_PATH = "REMOTE_PATH"
        const val EXTRA_OLD_REMOTE_PATH = "OLD_REMOTE_PATH"
        const val EXTRA_OLD_FILE_PATH = "OLD_FILE_PATH"
        const val EXTRA_LINKED_TO_PATH = "LINKED_TO"
        const val EXTRA_BEHAVIOUR = "BEHAVIOUR"
        const val EXTRA_ACCOUNT_NAME = "ACCOUNT_NAME"

        const val ACCOUNT_NAME = "ACCOUNT_NAME"
        const val ACTION_CANCEL_BROADCAST = "CANCEL"
        const val LOCAL_BEHAVIOUR_COPY = 0
        const val LOCAL_BEHAVIOUR_MOVE = 1
        const val LOCAL_BEHAVIOUR_FORGET = 2
        const val LOCAL_BEHAVIOUR_DELETE = 3

        fun cancelCurrentUpload(remotePath: String, accountName: String, onCompleted: () -> Unit) {
            currentUploadFileOperation?.let {
                if (it.remotePath == remotePath && it.user.accountName == accountName) {
                    it.cancel(ResultCode.USER_CANCELLED)
                    onCompleted()
                }
            }
        }

        fun isUploading(remotePath: String?, accountName: String?): Boolean {
            currentUploadFileOperation?.let {
                return it.remotePath == remotePath && it.user.accountName == accountName
            }

            return false
        }

        fun getUploadAction(action: String): Int = when (action) {
            "LOCAL_BEHAVIOUR_FORGET" -> LOCAL_BEHAVIOUR_FORGET
            "LOCAL_BEHAVIOUR_MOVE" -> LOCAL_BEHAVIOUR_MOVE
            "LOCAL_BEHAVIOUR_DELETE" -> LOCAL_BEHAVIOUR_DELETE
            else -> LOCAL_BEHAVIOUR_FORGET
        }
    }

    private var lastPercent = 0
    private val notificationId = Random.nextInt()
    private val notificationManager = UploadNotificationManager(context, viewThemeUtils, notificationId)
    private val intents = FileUploaderIntents(context)
    private val fileUploadBroadcastManager = FileUploadBroadcastManager(localBroadcastManager)

    override suspend fun doWork(): Result = try {
        Log_OC.d(TAG, "FileUploadWorker started")
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
            val notification = createNotification(notificationTitle)
            updateForegroundInfo(notification)
        } catch (e: Exception) {
            // Continue without foreground service - uploads will still work
            Log_OC.w(TAG, "Could not set foreground service: ${e.message}")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationTitle = notificationManager.currentOperationTitle
            ?: context.getString(R.string.foreground_service_upload)
        val notification = createNotification(notificationTitle)

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

    private fun createNotification(title: String): Notification =
        NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.uploads)
            .setOngoing(true)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

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

        val user = optionalUser.get()
        val previouslyUploadedFileSize = currentBatchIndex * FileUploadHelper.MAX_FILE_COUNT
        val uploads = uploadsStorageManager.getUploadsByIds(uploadIds, accountName)
        val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
        val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)
        val storageManager = FileDataStorageManager(user, context.contentResolver)

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
            val operation = createUploadFileOperation(upload, user, storageManager)
            currentUploadFileOperation = operation
            val parentFile =
                storageManager.getFileByDecryptedRemotePath(operation.file?.parentRemotePath)

            parentFile?.let {
                FileIndicatorManager.update(it.fileId, FileIndicator.Syncing)
            }

            val currentIndex = (index + 1)
            val currentUploadIndex = (currentIndex + previouslyUploadedFileSize)
            notificationManager.prepareForStart(
                operation,
                startIntent = intents.openUploadListIntent(operation),
                currentUploadIndex = currentUploadIndex,
                totalUploadSize = totalUploadSize
            )

            val result = withContext(Dispatchers.IO) {
                upload(upload, operation, user, client)
            }
            currentUploadFileOperation = null
            parentFile?.let {
                FileIndicatorManager.update(it.fileId, FileIndicator.Idle)
            }

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

    private fun createUploadFileOperation(
        upload: OCUpload,
        user: User,
        storageManager: FileDataStorageManager
    ): UploadFileOperation = UploadFileOperation(
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
        storageManager
    ).apply {
        addDataTransferProgressListener(this@FileUploadWorker)
    }

    @Suppress("TooGenericExceptionCaught", "DEPRECATION")
    private suspend fun upload(
        upload: OCUpload,
        operation: UploadFileOperation,
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
            fileUploadBroadcastManager.sendStarted(operation, context)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error uploading", e)
            uploadsStorageManager.run {
                uploadDao.getUploadById(upload.uploadId, user.accountName)?.let { entity ->
                    updateStatus(
                        entity,
                        UploadsStorageManager.UploadStatus.UPLOAD_FAILED
                    )
                }
            }
            result = RemoteOperationResult(e)
        } finally {
            if (!isStopped) {
                UploadErrorNotificationManager.handleResult(
                    context,
                    notificationManager,
                    operation,
                    result,
                    onSameFileConflict = {
                        withContext(Dispatchers.Main) {
                            val showSameFileAlreadyExistsNotification =
                                inputData.getBoolean(SHOW_SAME_FILE_ALREADY_EXISTS_NOTIFICATION, false)
                            if (showSameFileAlreadyExistsNotification) {
                                notificationManager.showSameFileAlreadyExistsNotification(operation.fileName)
                            }
                        }
                    }
                )
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
