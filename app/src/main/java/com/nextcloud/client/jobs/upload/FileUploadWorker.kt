/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        private const val MAX_PROGRESS: Int = 100
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
            result
        } catch (t: Throwable) {
            Log_OC.e(TAG, "Error caught at FileUploadWorker " + t.localizedMessage)
            Result.failure()
        }
    }

    override fun onStopped() {
        Log_OC.e(TAG, "FileUploadWorker stopped")

        setIdleWorkerState()
        currentUploadFileOperation?.cancel(null)
        notificationManager.dismissWorkerNotifications()

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
        var currentPage = uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(-1, accountName)

        notificationManager.dismissWorkerNotifications()

        while (currentPage.isNotEmpty() && !isStopped) {
            if (preferences.isGlobalUploadPaused) {
                Log_OC.d(TAG, "Upload is paused, skip uploading files!")
                notificationManager.notifyPaused(
                    intents.notificationStartIntent(null)
                )
                return Result.success()
            }

            Log_OC.d(TAG, "Handling ${currentPage.size} uploads for account $accountName")
            val lastId = currentPage.last().uploadId
            uploadFiles(currentPage, accountName)
            currentPage =
                uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(lastId, accountName)
        }

        if (isStopped) {
            Log_OC.d(TAG, "FileUploadWorker for account $accountName was stopped")
        } else {
            Log_OC.d(TAG, "No more pending uploads for account $accountName, stopping work")
        }
        return Result.success()
    }

    private fun uploadFiles(uploads: List<OCUpload>, accountName: String) {
        val user = userAccountManager.getUser(accountName)
        setWorkerState(user.get(), uploads)

        for (upload in uploads) {
            if (isStopped) {
                break
            }

            if (user.isPresent) {
                val operation = createUploadFileOperation(upload, user.get())

                currentUploadFileOperation = operation
                val result = upload(operation, user.get())
                currentUploadFileOperation = null

                fileUploaderDelegate.sendBroadcastUploadFinished(
                    operation,
                    result,
                    operation.oldFile?.storagePath,
                    context,
                    localBroadcastManager
                )
            } else {
                uploadsStorageManager.removeUpload(upload.uploadId)
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
    private fun upload(operation: UploadFileOperation, user: User): RemoteOperationResult<Any?> {
        lateinit var result: RemoteOperationResult<Any?>

        notificationManager.prepareForStart(
            operation,
            intents.startIntent(operation),
            intents.notificationStartIntent(operation)
        )

        try {
            val storageManager = operation.storageManager
            val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
            val uploadClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)
            result = operation.execute(uploadClient)

            val task = ThumbnailsCacheManager.ThumbnailGenerationTask(storageManager, user)
            val file = File(operation.originalStoragePath)
            val remoteId: String? = operation.file.remoteId
            task.execute(ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, remoteId))
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error uploading", e)
            result = RemoteOperationResult<Any?>(e)
        } finally {
            cleanupUploadProcess(result, operation)
        }

        return result
    }

    private fun cleanupUploadProcess(result: RemoteOperationResult<Any?>, operation: UploadFileOperation) {
        if (!(isStopped && result.isCancelled)) {
            uploadsStorageManager.updateDatabaseUploadResult(result, operation)
            notifyUploadResult(operation, result)
            notificationManager.dismissWorkerNotifications()
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
                uploadFileOperation.user, File(uploadFileOperation.storagePath), uploadFileOperation.remotePath, context
            )
        ) {
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

            // FIXME SYNC_CONFLICT passes wrong OCFile, check ConflictsResolveActivity.createIntent usage
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
            notifyForFailedResult(uploadResult.code, conflictResolveIntent, credentialIntent, errorMessage)
            showNewNotification(uploadFileOperation)
        }
    }

    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        fileAbsoluteName: String
    ) {
        val percent = (MAX_PROGRESS * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()

        if (percent != lastPercent) {
            notificationManager.run {
                updateUploadProgress(fileAbsoluteName, percent, currentUploadFileOperation)

                val accountName = currentUploadFileOperation?.user?.accountName
                val remotePath = currentUploadFileOperation?.remotePath

                if (accountName != null && remotePath != null) {
                    val key: String =
                        FileUploadHelper.buildRemoteName(accountName, remotePath)
                    val boundListener = FileUploadHelper.mBoundListeners[key]

                    boundListener?.onTransferProgress(
                        progressRate,
                        totalTransferredSoFar,
                        totalToTransfer,
                        fileAbsoluteName
                    )
                }

                dismissOldErrorNotification(currentUploadFileOperation)
            }
        }

        lastPercent = percent
    }
}
