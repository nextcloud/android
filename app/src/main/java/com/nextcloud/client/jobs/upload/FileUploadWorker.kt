/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.app.PendingIntent
import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
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
import com.nextcloud.utils.extensions.getPercent
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

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
) : Worker(context, params), OnDatatransferProgressListener {

    companion object {
        val TAG: String = FileUploadWorker::class.java.simpleName

        const val NOTIFICATION_ERROR_ID: Int = 413
        const val ACCOUNT = "data_account"
        var currentUploadFileOperation: UploadFileOperation? = null

        private const val UPLOADS_ADDED_MESSAGE = "UPLOADS_ADDED"
        private const val UPLOAD_START_MESSAGE = "UPLOAD_START"
        private const val UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH"

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

        fun getUploadsAddedMessage(): String {
            return FileUploadWorker::class.java.name + UPLOADS_ADDED_MESSAGE
        }

        fun getUploadStartMessage(): String {
            return FileUploadWorker::class.java.name + UPLOAD_START_MESSAGE
        }

        fun getUploadFinishMessage(): String {
            return FileUploadWorker::class.java.name + UPLOAD_FINISH_MESSAGE
        }
    }

    private var lastPercent = 0
    private val notificationManager = UploadNotificationManager(context, viewThemeUtils)
    private val intents = FileUploaderIntents(context)
    private val fileUploaderDelegate = FileUploaderDelegate()

    @Suppress("TooGenericExceptionCaught")
    override fun doWork(): Result {
        return try {
            backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class))
            val result = retrievePagesBySortingUploadsByID()
            backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
            notificationManager.dismissNotification()
            result
        } catch (t: Throwable) {
            Log_OC.e(TAG, "Error caught at FileUploadWorker $t")
            Result.failure()
        }
    }

    override fun onStopped() {
        Log_OC.e(TAG, "FileUploadWorker stopped")

        setIdleWorkerState()
        currentUploadFileOperation?.cancel(null)
        notificationManager.dismissNotification()

        super.onStopped()
    }

    private fun setWorkerState(user: User?, uploads: List<OCUpload>) {
        WorkerStateLiveData.instance().setWorkState(WorkerState.Upload(user, uploads))
    }

    private fun setIdleWorkerState() {
        WorkerStateLiveData.instance().setWorkState(WorkerState.Idle)
    }

    @Suppress("ReturnCount")
    private fun retrievePagesBySortingUploadsByID(): Result {
        val accountName = inputData.getString(ACCOUNT) ?: return Result.failure()
        var uploadsPerPage = uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(-1, accountName)
        val totalUploadSize = uploadsStorageManager.getTotalUploadSize(accountName)

        Log_OC.d(TAG, "Total upload size: $totalUploadSize")

        while (uploadsPerPage.isNotEmpty() && !isStopped) {
            if (preferences.isGlobalUploadPaused) {
                Log_OC.d(TAG, "Upload is paused, skip uploading files!")
                notificationManager.notifyPaused(
                    intents.notificationStartIntent(null)
                )
                return Result.success()
            }

            Log_OC.d(TAG, "Handling ${uploadsPerPage.size} uploads for account $accountName")
            val lastId = uploadsPerPage.last().uploadId
            uploadFiles(totalUploadSize, uploadsPerPage, accountName)
            uploadsPerPage =
                uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(lastId, accountName)
        }

        if (isStopped) {
            Log_OC.d(TAG, "FileUploadWorker for account $accountName was stopped")
        } else {
            Log_OC.d(TAG, "No more pending uploads for account $accountName, stopping work")
        }
        return Result.success()
    }

    private fun uploadFiles(totalUploadSize: Int, uploadsPerPage: List<OCUpload>, accountName: String) {
        val user = userAccountManager.getUser(accountName)
        setWorkerState(user.get(), uploadsPerPage)

        run uploads@{
            uploadsPerPage.forEachIndexed { currentUploadIndex, upload ->
                if (isStopped) {
                    return@uploads
                }

                if (user.isPresent) {
                    val uploadFileOperation = createUploadFileOperation(upload, user.get())

                    currentUploadFileOperation = uploadFileOperation

                    notificationManager.prepareForStart(
                        uploadFileOperation,
                        cancelPendingIntent = intents.startIntent(uploadFileOperation),
                        startIntent = intents.notificationStartIntent(uploadFileOperation),
                        currentUploadIndex = currentUploadIndex + 1,
                        totalUploadSize = totalUploadSize
                    )

                    val result = upload(uploadFileOperation, user.get())

                    currentUploadFileOperation = null

                    fileUploaderDelegate.sendBroadcastUploadFinished(
                        uploadFileOperation,
                        result,
                        uploadFileOperation.oldFile?.storagePath,
                        context,
                        localBroadcastManager
                    )
                } else {
                    uploadsStorageManager.removeUpload(upload.uploadId)
                }
            }
        }
    }

    private fun createUploadFileOperation(upload: OCUpload, user: User): UploadFileOperation {
        return UploadFileOperation(
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
    }

    @Suppress("TooGenericExceptionCaught", "DEPRECATION")
    private fun upload(uploadFileOperation: UploadFileOperation, user: User): RemoteOperationResult<Any?> {
        lateinit var result: RemoteOperationResult<Any?>

        try {
            val storageManager = uploadFileOperation.storageManager
            val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
            val uploadClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)
            result = uploadFileOperation.execute(uploadClient)

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

    @Suppress("ReturnCount")
    private fun notifyUploadResult(
        uploadFileOperation: UploadFileOperation,
        uploadResult: RemoteOperationResult<Any?>
    ) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.code)

        if (uploadResult.isSuccess) {
            notificationManager.dismissOldErrorNotification(uploadFileOperation)
            return
        }

        if (uploadResult.isCancelled) {
            return
        }

        // Only notify if it is not same file on remote that causes conflict
        if (uploadResult.code == ResultCode.SYNC_CONFLICT && FileUploadHelper().isSameFileOnRemote(
                uploadFileOperation.user,
                File(uploadFileOperation.storagePath),
                uploadFileOperation.remotePath,
                context
            )
        ) {
            context.showToast(R.string.file_upload_worker_same_file_already_exists)
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

            notifyForFailedResult(
                uploadFileOperation,
                uploadResult.code,
                conflictResolveIntent,
                credentialIntent,
                errorMessage
            )
        }
    }

    @Suppress("MagicNumber")
    private val minProgressUpdateInterval = 750
    private var lastUpdateTime = 0L

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
