/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateLiveData
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
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

@Suppress("LongParameterList")
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

        private const val UPLOADS_ADDED_MESSAGE = "UPLOADS_ADDED"
        private const val UPLOAD_START_MESSAGE = "UPLOAD_START"
        private const val UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH"

        private const val BATCH_SIZE = 100

        const val EXTRA_UPLOAD_RESULT = "RESULT"
        const val EXTRA_REMOTE_PATH = "REMOTE_PATH"
        const val EXTRA_OLD_REMOTE_PATH = "OLD_REMOTE_PATH"
        const val EXTRA_OLD_FILE_PATH = "OLD_FILE_PATH"
        const val EXTRA_LINKED_TO_PATH = "LINKED_TO"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"
        const val EXTRA_ACCOUNT_NAME = "ACCOUNT_NAME"
        const val ACTION_CANCEL_BROADCAST = "CANCEL"
        const val LOCAL_BEHAVIOUR_COPY = 0
        const val LOCAL_BEHAVIOUR_MOVE = 1
        const val LOCAL_BEHAVIOUR_FORGET = 2
        const val LOCAL_BEHAVIOUR_DELETE = 3

        fun getUploadsAddedMessage(): String = FileUploadWorker::class.java.name + UPLOADS_ADDED_MESSAGE

        fun getUploadStartMessage(): String = FileUploadWorker::class.java.name + UPLOAD_START_MESSAGE

        fun getUploadFinishMessage(): String = FileUploadWorker::class.java.name + UPLOAD_FINISH_MESSAGE

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
    }

    private var lastPercent = 0
    private val notificationId = Random.nextInt()
    private val notificationManager = UploadNotificationManager(context, viewThemeUtils, notificationId)
    private val intents = FileUploaderIntents(context)
    private val fileUploaderDelegate = FileUploaderDelegate()

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = try {
        Log_OC.d(TAG, "FileUploadWorker started")
        val workerName = BackgroundJobManagerImpl.formatClassTag(this::class)
        backgroundJobManager.logStartOfWorker(workerName)

        val notificationTitle = notificationManager.currentOperationTitle
            ?: context.getString(R.string.foreground_service_upload)
        val notification = createNotification(notificationTitle)
        updateForegroundInfo(notification)

        val result = uploadFiles()
        backgroundJobManager.logEndOfWorker(workerName, result)
        notificationManager.dismissNotification()
        if (result == Result.success()) {
            setIdleWorkerState()
        }
        result
    } catch (t: Throwable) {
        Log_OC.e(TAG, "Error caught at FileUploadWorker $t")
        cleanup()
        Result.failure()
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

    private fun cleanup() {
        Log_OC.e(TAG, "FileUploadWorker stopped")

        setIdleWorkerState()
        currentUploadFileOperation?.cancel(null)
        notificationManager.dismissNotification()
    }

    private fun setWorkerState(user: User?) {
        WorkerStateLiveData.instance().setWorkState(WorkerState.UploadStarted(user))
    }

    private fun setIdleWorkerState() {
        WorkerStateLiveData.instance().setWorkState(WorkerState.UploadFinished(currentUploadFileOperation?.file))
    }

    @Suppress("ReturnCount", "LongMethod")
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

        for ((index, upload) in uploads.withIndex()) {
            ensureActive()

            if (preferences.isGlobalUploadPaused) {
                Log_OC.d(TAG, "Upload is paused, skip uploading files!")
                notificationManager.notifyPaused(
                    intents.notificationStartIntent(null)
                )
                return@withContext Result.success()
            }

            if (canExitEarly()) {
                notificationManager.showConnectionErrorNotification()
                return@withContext Result.failure()
            }

            setWorkerState(user)
            val operation = createUploadFileOperation(upload, user)
            currentUploadFileOperation = operation

            val currentIndex = (index + 1)
            val currentUploadIndex = (currentIndex + previouslyUploadedFileSize)
            notificationManager.prepareForStart(
                operation,
                cancelPendingIntent = intents.startIntent(operation),
                startIntent = intents.notificationStartIntent(operation),
                currentUploadIndex = currentUploadIndex,
                totalUploadSize = totalUploadSize
            )

            val result = withContext(Dispatchers.IO) {
                upload(operation, user, client)
            }
            val entity = uploadsStorageManager.uploadDao.getUploadById(upload.uploadId, accountName)
            uploadsStorageManager.updateStatus(entity, result.isSuccess)
            currentUploadFileOperation = null

            if (result.code == ResultCode.QUOTA_EXCEEDED) {
                Log_OC.w(TAG, "Quota exceeded, stopping uploads")
                notificationManager.showQuotaExceedNotification(operation, result.code)
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
        val shouldBroadcast =
            (totalUploadSize > BATCH_SIZE && currentUploadIndex > 0) && currentUploadIndex % BATCH_SIZE == 0

        if (shouldBroadcast) {
            // delay broadcast
            fileUploaderDelegate.sendBroadcastUploadFinished(
                operation,
                result,
                operation.oldFile?.storagePath,
                context,
                localBroadcastManager
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
        addDataTransferProgressListener(this@FileUploadWorker)
    }

    @Suppress("TooGenericExceptionCaught", "DEPRECATION")
    private fun upload(
        uploadFileOperation: UploadFileOperation,
        user: User,
        client: OwnCloudClient
    ): RemoteOperationResult<Any?> {
        lateinit var result: RemoteOperationResult<Any?>

        try {
            val storageManager = uploadFileOperation.storageManager
            result = uploadFileOperation.execute(client)
            val task = ThumbnailsCacheManager.ThumbnailGenerationTask(storageManager, user)
            val file = File(uploadFileOperation.originalStoragePath)
            val remoteId: String? = uploadFileOperation.file.remoteId
            task.execute(ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, remoteId))
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error uploading", e)
            result = RemoteOperationResult<Any?>(e)
        } finally {
            cleanupUploadProcess(result, uploadFileOperation)
        }

        return result
    }

    private fun cleanupUploadProcess(result: RemoteOperationResult<Any?>, uploadFileOperation: UploadFileOperation) {
        if (!isStopped || !result.isCancelled) {
            uploadsStorageManager.updateDatabaseUploadResult(result, uploadFileOperation)
            notifyUploadResult(uploadFileOperation, result)
        }
    }

    @Suppress("ReturnCount", "LongMethod")
    private fun notifyUploadResult(
        uploadFileOperation: UploadFileOperation,
        uploadResult: RemoteOperationResult<Any?>
    ) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.code)
        val showSameFileAlreadyExistsNotification =
            inputData.getBoolean(SHOW_SAME_FILE_ALREADY_EXISTS_NOTIFICATION, false)

        if (uploadResult.isSuccess) {
            notificationManager.dismissOldErrorNotification(uploadFileOperation)
            return
        }

        if (uploadResult.isCancelled) {
            return
        }

        // Only notify if it is not same file on remote that causes conflict
        if (uploadResult.code == ResultCode.SYNC_CONFLICT &&
            FileUploadHelper().isSameFileOnRemote(
                uploadFileOperation.user,
                File(uploadFileOperation.storagePath),
                uploadFileOperation.remotePath,
                context
            )
        ) {
            if (showSameFileAlreadyExistsNotification) {
                notificationManager.showSameFileAlreadyExistsNotification(uploadFileOperation.fileName)
            }

            uploadFileOperation.handleLocalBehaviour()
            return
        }

        val notDelayed = uploadResult.code !in setOf(
            ResultCode.DELAYED_FOR_WIFI,
            ResultCode.DELAYED_FOR_CHARGING,
            ResultCode.DELAYED_IN_POWER_SAVE_MODE
        )

        val isValidFile = uploadResult.code !in setOf(
            ResultCode.LOCAL_FILE_NOT_FOUND,
            ResultCode.LOCK_FAILED
        )

        if (!notDelayed || !isValidFile) {
            return
        }

        if (uploadResult.code == ResultCode.USER_CANCELLED) {
            return
        }

        notificationManager.run {
            val errorMessage = ErrorMessageAdapter.getErrorCauseMessage(
                uploadResult,
                uploadFileOperation,
                context.resources
            )

            val conflictResolveIntent = if (uploadResult.code == ResultCode.SYNC_CONFLICT) {
                intents.conflictResolveActionIntents(context, uploadFileOperation)
            } else {
                null
            }

            val credentialIntent: PendingIntent? = if (uploadResult.code == ResultCode.UNAUTHORIZED) {
                intents.credentialIntent(uploadFileOperation)
            } else {
                null
            }

            val cancelUploadActionIntent = if (conflictResolveIntent != null) {
                intents.cancelUploadActionIntent(uploadFileOperation)
            } else {
                null
            }

            notifyForFailedResult(
                uploadFileOperation,
                uploadResult.code,
                conflictResolveIntent,
                cancelUploadActionIntent,
                credentialIntent,
                errorMessage
            )
        }
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
