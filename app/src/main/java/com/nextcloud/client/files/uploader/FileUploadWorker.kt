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

package com.nextcloud.client.files.uploader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.java.util.Optional
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
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
    val context: Context,
    params: WorkerParameters
) : Worker(context, params), OnDatatransferProgressListener {

    private var lastPercent = 0
    private val notificationManager = UploadNotificationManager(context, viewThemeUtils)
    private val intents = FileUploaderIntents(context)
    private val fileUploaderDelegate = FileUploaderDelegate()

    override fun doWork(): Result {
        backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class))

        val accountName = inputData.getString(ACCOUNT)
        if (accountName.isNullOrEmpty()) {
            Log_OC.w(TAG, "User was null for file upload worker")

            val result = Result.failure()
            backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
            return result
        }

        retrievePagesBySortingUploadsByID(accountName)

        val result = Result.success()
        backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
        return result
    }

    private fun retrievePagesBySortingUploadsByID(accountName: String) {
        var currentPage = uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(-1, accountName)
        while (currentPage.isNotEmpty() && !isStopped) {
            Log_OC.d(TAG, "Handling ${currentPage.size} uploads for account $accountName")
            val lastId = currentPage.last().uploadId
            handlePendingUploads(currentPage, accountName)
            currentPage =
                uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(lastId, accountName)
        }

        Log_OC.d(TAG, "No more pending uploads for account $accountName, stopping work")
    }

    private fun handlePendingUploads(uploads: List<OCUpload>, accountName: String) {
        val user = userAccountManager.getUser(accountName)
        for (upload in uploads) {
            if (isStopped) {
                break
            }

            if (user.isPresent) {
                val uploadFileOperation = createUploadFileOperation(upload, user.get())

                currentUploadFileOperation = uploadFileOperation
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

    @Suppress("TooGenericExceptionCaught")
    private fun upload(uploadFileOperation: UploadFileOperation, user: User): RemoteOperationResult<Any?> {
        lateinit var uploadResult: RemoteOperationResult<Any?>

        notificationManager.notifyForStart(
            uploadFileOperation,
            intents.startIntent(uploadFileOperation),
            intents.notificationStartIntent(uploadFileOperation)
        )

        try {
            val storageManager = uploadFileOperation.storageManager

            // always get client from client manager, to get fresh credentials in case of update
            val ocAccount = OwnCloudAccount(user.toPlatformAccount(), context)
            val uploadClient = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)
            uploadResult = uploadFileOperation.execute(uploadClient)

            // generate new Thumbnail
            val task = ThumbnailsCacheManager.ThumbnailGenerationTask(storageManager, user)
            val file = File(uploadFileOperation.originalStoragePath)
            val remoteId: String? = uploadFileOperation.file.remoteId
            task.execute(ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, remoteId))
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error uploading", e)
            uploadResult = RemoteOperationResult<Any?>(e)
        } finally {
            if (!(isStopped && uploadResult.isCancelled)) {
                uploadsStorageManager.updateDatabaseUploadResult(uploadResult, uploadFileOperation)
                notifyUploadResult(uploadFileOperation, uploadResult)
                notificationManager.dismissWorkerNotifications()
            }
        }

        return uploadResult
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

        val (tickerId, needsToUpdateCredentials) = getTickerId(uploadResult.code)
        notificationManager.run {
            notifyForResult(tickerId)
            setContentIntent(intents.resultIntent(ResultCode.OK, uploadFileOperation))

            if (uploadResult.code == ResultCode.SYNC_CONFLICT) {
                addAction(
                    R.drawable.ic_cloud_upload,
                    R.string.upload_list_resolve_conflict,
                    intents.conflictResolveActionIntents(context, uploadFileOperation)
                )
            }

            if (needsToUpdateCredentials) {
                setContentIntent(intents.credentialIntent(uploadFileOperation))
            }

            val content = ErrorMessageAdapter.getErrorCauseMessage(uploadResult, uploadFileOperation, context.resources)
            setContentText(content)

            if (!uploadResult.isSuccess) {
                showRandomNotification()
            }

            showNotificationTag(uploadFileOperation)
        }
    }

    private fun getTickerId(resultCode: ResultCode): Pair<Int, Boolean> {
        var tickerId = R.string.uploader_upload_failed_ticker

        val needsToUpdateCredentials = (resultCode == ResultCode.UNAUTHORIZED)

        if (needsToUpdateCredentials) {
            tickerId = R.string.uploader_upload_failed_credentials_error
        } else if (resultCode == ResultCode.SYNC_CONFLICT) {
            tickerId = R.string.uploader_upload_failed_sync_conflict_error
        }

        return Pair(tickerId, needsToUpdateCredentials)
    }

    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        fileAbsoluteName: String
    ) {
        val percent = (MAX_PROGRESS * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()
        if (percent != lastPercent) {
            notificationManager.updateUploadProgressNotification(fileAbsoluteName, percent, currentUploadFileOperation)

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

            notificationManager.dismissOldErrorNotification(currentUploadFileOperation)
        }
        lastPercent = percent
    }

    override fun onStopped() {
        super.onStopped()
        currentUploadFileOperation?.cancel(null)
        notificationManager.dismissWorkerNotifications()
    }

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

        @Suppress("ComplexCondition")
        fun retryFailedUploads(
            uploadsStorageManager: UploadsStorageManager,
            connectivityService: ConnectivityService,
            accountManager: UserAccountManager,
            powerManagementService: PowerManagementService
        ) {
            val failedUploads = uploadsStorageManager.failedUploads
            if (failedUploads == null || failedUploads.isEmpty()) {
                return
            }

            val (gotNetwork, _, gotWifi) = connectivityService.connectivity
            val batteryStatus = powerManagementService.battery
            val charging = batteryStatus.isCharging || batteryStatus.isFull
            val isPowerSaving = powerManagementService.isPowerSavingEnabled
            var uploadUser = Optional.empty<User>()
            for (failedUpload in failedUploads) {
                // 1. extract failed upload owner account and cache it between loops (expensive query)
                if (!uploadUser.isPresent || !uploadUser.get().nameEquals(failedUpload.accountName)) {
                    uploadUser = accountManager.getUser(failedUpload.accountName)
                }
                val isDeleted = !File(failedUpload.localPath).exists()
                if (isDeleted) {
                    // 2A. for deleted files, mark as permanently failed
                    if (failedUpload.lastResult != UploadResult.FILE_NOT_FOUND) {
                        failedUpload.lastResult = UploadResult.FILE_NOT_FOUND
                        uploadsStorageManager.updateUpload(failedUpload)
                    }
                } else if (!isPowerSaving && gotNetwork &&
                    canUploadBeRetried(failedUpload, gotWifi, charging) && !connectivityService.isInternetWalled
                ) {
                    // 2B. for existing local files, try restarting it if possible
                    FileUploadHelper.instance().retryUpload(failedUpload, uploadUser.get())
                }
            }
        }

        private fun canUploadBeRetried(upload: OCUpload, gotWifi: Boolean, isCharging: Boolean): Boolean {
            val file = File(upload.localPath)
            val needsWifi = upload.isUseWifiOnly
            val needsCharging = upload.isWhileChargingOnly
            return file.exists() && (!needsWifi || gotWifi) && (!needsCharging || isCharging)
        }

        fun getUploadsAddedMessage(): String {
            return FileUploadWorker::class.java.name + UPLOADS_ADDED_MESSAGE
        }

        fun getUploadStartMessage(): String {
            return FileUploadWorker::class.java.name + UPLOAD_START_MESSAGE
        }

        fun getUploadFinishMessage(): String {
            return FileUploadWorker::class.java.name + UPLOAD_FINISH_MESSAGE
        }

        class UploadNotificationActionReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME)
                val remotePath = intent.getStringExtra(EXTRA_REMOTE_PATH)
                val action = intent.action

                if (ACTION_CANCEL_BROADCAST == action) {
                    Log_OC.d(
                        TAG,
                        "Cancel broadcast received for file " + remotePath + " at " + System.currentTimeMillis()
                    )
                    if (accountName == null || remotePath == null) {
                        return
                    }

                    FileUploadHelper.instance().cancelFileUpload(remotePath, accountName)
                }
            }
        }
    }
}
