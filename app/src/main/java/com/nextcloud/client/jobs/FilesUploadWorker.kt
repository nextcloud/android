/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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

package com.nextcloud.client.jobs

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.files.uploader.FileUploaderIntents
import com.nextcloud.client.files.uploader.UploadNotificationManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.utils.FileUploaderDelegate
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
import com.owncloud.android.utils.FilesUploadHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

@Suppress("LongParameterList")
class FilesUploadWorker(
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
            return result // user account is needed
        }

        /*
         * As pages are retrieved by sorting uploads by ID, if new uploads are added while uploading the current ones,
         * they will be present in the pages that follow.
         */
        var currentPage = uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(-1, accountName)
        while (currentPage.isNotEmpty() && !isStopped) {
            Log_OC.d(TAG, "Handling ${currentPage.size} uploads for account $accountName")
            val lastId = currentPage.last().uploadId
            handlePendingUploads(currentPage, accountName)
            currentPage =
                uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(lastId, accountName)
        }

        Log_OC.d(TAG, "No more pending uploads for account $accountName, stopping work")
        val result = Result.success()
        backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
        return result
    }

    private fun handlePendingUploads(uploads: List<OCUpload>, accountName: String) {
        val user = userAccountManager.getUser(accountName)

        for (upload in uploads) {
            if (isStopped) {
                break
            }
            // create upload file operation
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
                // user not present anymore, remove upload
                uploadsStorageManager.removeUpload(upload.uploadId)
            }
        }
    }

    /**
     * from @{link FileUploader#retryUploads()}
     */
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
            addDataTransferProgressListener(this@FilesUploadWorker)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun upload(uploadFileOperation: UploadFileOperation, user: User): RemoteOperationResult<Any?> {
        lateinit var uploadResult: RemoteOperationResult<Any?>

        notificationManager.notifyForStart(uploadFileOperation, intents.startIntent(uploadFileOperation))

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
            // only update db if operation finished and worker didn't get canceled
            if (!(isStopped && uploadResult.isCancelled)) {
                uploadsStorageManager.updateDatabaseUploadResult(uploadResult, uploadFileOperation)

                // / notify result
                notifyUploadResult(uploadFileOperation, uploadResult)

                notificationManager.dismissWorkerNotifications()
            }
        }

        return uploadResult
    }

    /**
     * adapted from [com.owncloud.android.files.services.FileUploader.notifyUploadResult]
     */
    private fun notifyUploadResult(
        uploadFileOperation: UploadFileOperation,
        uploadResult: RemoteOperationResult<Any?>
    ) {
        Log_OC.d(TAG, "NotifyUploadResult with resultCode: " + uploadResult.code)

        if (uploadResult.isSuccess) {
            notificationManager.dismissOldErrorNotification(uploadFileOperation)
            return
        }

        // Only notify if the upload fails
        if (uploadResult.isCancelled) {
            return
        }

        val notDelayed = uploadResult.code != ResultCode.DELAYED_FOR_WIFI &&
            uploadResult.code != ResultCode.DELAYED_FOR_CHARGING &&
            uploadResult.code != ResultCode.DELAYED_IN_POWER_SAVE_MODE

        if (notDelayed &&
            uploadResult.code != ResultCode.LOCAL_FILE_NOT_FOUND &&
            uploadResult.code != ResultCode.LOCK_FAILED
        ) {
            var tickerId = R.string.uploader_upload_failed_ticker

            // check credentials error
            val needsToUpdateCredentials = uploadResult.code == ResultCode.UNAUTHORIZED
            if (needsToUpdateCredentials) {
                tickerId = R.string.uploader_upload_failed_credentials_error
            } else if (uploadResult.code == ResultCode.SYNC_CONFLICT) {
                // check file conflict
                tickerId = R.string.uploader_upload_failed_sync_conflict_error
            }
            notificationManager.notifyForResult(tickerId)

            val content = ErrorMessageAdapter.getErrorCauseMessage(uploadResult, uploadFileOperation, context.resources)
            notificationManager.setContentIntent(intents.resultIntent(ResultCode.OK, uploadFileOperation))

            if (uploadResult.code == ResultCode.SYNC_CONFLICT) {
                notificationManager.addAction(
                    R.drawable.ic_cloud_upload,
                    R.string.upload_list_resolve_conflict,
                    intents.conflictResolveActionIntents(context, uploadFileOperation)
                )
            }

            if (needsToUpdateCredentials) {
                notificationManager.setContentIntent(intents.credentialIntent(uploadFileOperation))
            }

            notificationManager.setContentText(content)
            notificationManager.showNotificationTag(uploadFileOperation)
        }
    }

    /**
     * see [com.owncloud.android.files.services.FileUploader.onTransferProgress]
     */
    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        fileAbsoluteName: String
    ) {
        val percent = (MAX_PROGRESS * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()
        if (percent != lastPercent) {
            notificationManager.updateUploadProgressNotification(fileAbsoluteName, percent, currentUploadFileOperation)
            FilesUploadHelper.onTransferProgress(
                currentUploadFileOperation?.user?.accountName,
                currentUploadFileOperation?.remotePath,
                progressRate,
                totalTransferredSoFar,
                totalToTransfer,
                fileAbsoluteName
            )
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
        val TAG: String = FilesUploadWorker::class.java.simpleName
        const val NOTIFICATION_ERROR_ID: Int = 413
        private const val MAX_PROGRESS: Int = 100
        const val ACCOUNT = "data_account"
        var currentUploadFileOperation: UploadFileOperation? = null

        const val UPLOADS_ADDED_MESSAGE = "UPLOADS_ADDED"
        const val UPLOAD_START_MESSAGE = "UPLOAD_START"
        const val UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH"
        const val EXTRA_UPLOAD_RESULT = "RESULT"
        const val EXTRA_REMOTE_PATH = "REMOTE_PATH"
        const val EXTRA_OLD_REMOTE_PATH = "OLD_REMOTE_PATH"
        const val EXTRA_OLD_FILE_PATH = "OLD_FILE_PATH"
        const val EXTRA_LINKED_TO_PATH = "LINKED_TO"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"
        const val EXTRA_ACCOUNT_NAME = "ACCOUNT_NAME"
        const val ACTION_CANCEL_BROADCAST = "CANCEL"
        const val ACTION_PAUSE_BROADCAST = "PAUSE"
        const val KEY_FILE = "FILE"
        const val KEY_LOCAL_FILE = "LOCAL_FILE"
        const val KEY_REMOTE_FILE = "REMOTE_FILE"
        const val KEY_MIME_TYPE = "MIME_TYPE"
        const val KEY_RETRY = "KEY_RETRY"
        const val KEY_RETRY_UPLOAD = "KEY_RETRY_UPLOAD"
        const val KEY_ACCOUNT = "ACCOUNT"
        const val KEY_USER = "USER"
        const val KEY_NAME_COLLISION_POLICY = "KEY_NAME_COLLISION_POLICY"
        const val KEY_CREATE_REMOTE_FOLDER = "CREATE_REMOTE_FOLDER"
        const val KEY_CREATED_BY = "CREATED_BY"
        const val KEY_WHILE_ON_WIFI_ONLY = "KEY_ON_WIFI_ONLY"
        const val KEY_WHILE_CHARGING_ONLY = "KEY_WHILE_CHARGING_ONLY"
        const val KEY_LOCAL_BEHAVIOUR = "BEHAVIOUR"
        const val KEY_DISABLE_RETRIES = "DISABLE_RETRIES"
        const val LOCAL_BEHAVIOUR_COPY = 0
        const val LOCAL_BEHAVIOUR_MOVE = 1
        const val LOCAL_BEHAVIOUR_FORGET = 2
        const val LOCAL_BEHAVIOUR_DELETE = 3
    }
}
