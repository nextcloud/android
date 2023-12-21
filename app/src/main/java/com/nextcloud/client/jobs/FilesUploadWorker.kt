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

import android.accounts.Account
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.utils.FileUploaderDelegate
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.ui.activity.UploadListActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FilesUploadHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import java.security.SecureRandom

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
    private val notificationBuilder: NotificationCompat.Builder =
        NotificationUtils.newNotificationBuilder(context, viewThemeUtils)
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
        return result // user account is needed
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

        // start notification
        createNotification(uploadFileOperation)

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

                // cancel notification
                notificationManager.cancel(FOREGROUND_SERVICE_ID)
            }
        }

        return uploadResult
    }

    /**
     * adapted from [com.owncloud.android.files.services.FileUploader.notifyUploadStart]
     */
    private fun createNotification(uploadFileOperation: UploadFileOperation) {
        val notificationActionIntent = Intent(context, FileUploader.UploadNotificationActionReceiver::class.java)
        notificationActionIntent.putExtra(FileUploader.EXTRA_ACCOUNT_NAME, uploadFileOperation.user.accountName)
        notificationActionIntent.putExtra(FileUploader.EXTRA_REMOTE_PATH, uploadFileOperation.remotePath)
        notificationActionIntent.action = FileUploader.ACTION_CANCEL_BROADCAST

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SecureRandom().nextInt(),
            notificationActionIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder
            .setOngoing(true)
            .setSmallIcon(R.drawable.notification_icon)
            .setTicker(context.getString(R.string.uploader_upload_in_progress_ticker))
            .setProgress(MAX_PROGRESS, 0, false)
            .setContentText(
                String.format(
                    context.getString(R.string.uploader_upload_in_progress_content),
                    0,
                    uploadFileOperation.fileName
                )
            )
            .clearActions() // to make sure there is only one action
            .addAction(R.drawable.ic_action_cancel_grey, context.getString(R.string.common_cancel), pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
        }

        // includes a pending intent in the notification showing the details
        val intent = UploadListActivity.createIntent(
            uploadFileOperation.file,
            uploadFileOperation.user,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            context
        )
        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        if (!uploadFileOperation.isInstantPicture && !uploadFileOperation.isInstantVideo) {
            notificationManager.notify(FOREGROUND_SERVICE_ID, notificationBuilder.build())
        } // else wait until the upload really start (onTransferProgress is called), so that if it's discarded

        // due to lack of Wifi, no notification is shown
        // TODO generalize for automated uploads
    }

    private fun createConflictResolveAction(context: Context, uploadFileOperation: UploadFileOperation): PendingIntent {
        val intent = ConflictsResolveActivity.createIntent(
            uploadFileOperation.file,
            uploadFileOperation.user,
            uploadFileOperation.ocUploadId,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            context
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun addConflictResolveActionToNotification(uploadFileOperation: UploadFileOperation) {
        val intent: PendingIntent = createConflictResolveAction(context, uploadFileOperation)

        notificationBuilder.addAction(
            R.drawable.ic_cloud_upload,
            context.getString(R.string.upload_list_resolve_conflict),
            intent
        )
    }

    private fun addUploadListContentIntent(uploadFileOperation: UploadFileOperation) {
        val uploadListIntent = createUploadListIntent(uploadFileOperation)

        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                uploadListIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
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
            cancelOldErrorNotification(uploadFileOperation)
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
            notificationBuilder
                .setTicker(context.getString(tickerId))
                .setContentTitle(context.getString(tickerId))
                .setAutoCancel(true)
                .setOngoing(false)
                .setProgress(0, 0, false)
                .clearActions()

            val content = ErrorMessageAdapter.getErrorCauseMessage(uploadResult, uploadFileOperation, context.resources)

            addUploadListContentIntent(uploadFileOperation)

            if (uploadResult.code == ResultCode.SYNC_CONFLICT) {
                addConflictResolveActionToNotification(uploadFileOperation)
            }

            if (needsToUpdateCredentials) {
                createUpdateCredentialsNotification(uploadFileOperation.user.toPlatformAccount())
            }

            notificationBuilder.setContentText(content)

            notificationManager.notify(
                NotificationUtils.createUploadNotificationTag(uploadFileOperation.file),
                NOTIFICATION_ERROR_ID,
                notificationBuilder.build()
            )
        }
    }

    private fun createUploadListIntent(uploadFileOperation: UploadFileOperation): Intent {
        return UploadListActivity.createIntent(
            uploadFileOperation.file,
            uploadFileOperation.user,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            context
        )
    }

    private fun createUpdateCredentialsNotification(account: Account) {
        // let the user update credentials with one click
        val updateAccountCredentials = Intent(context, AuthenticatorActivity::class.java)
        updateAccountCredentials.putExtra(
            AuthenticatorActivity.EXTRA_ACCOUNT,
            account
        )
        updateAccountCredentials.putExtra(
            AuthenticatorActivity.EXTRA_ACTION,
            AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN
        )
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        updateAccountCredentials.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        updateAccountCredentials.addFlags(Intent.FLAG_FROM_BACKGROUND)
        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                updateAccountCredentials,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        )
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
            notificationBuilder.setProgress(MAX_PROGRESS, percent, false)
            val fileName: String =
                fileAbsoluteName.substring(fileAbsoluteName.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1)
            val text = String.format(context.getString(R.string.uploader_upload_in_progress_content), percent, fileName)
            notificationBuilder.setContentText(text)
            notificationManager.notify(FOREGROUND_SERVICE_ID, notificationBuilder.build())
            FilesUploadHelper.onTransferProgress(
                currentUploadFileOperation?.user?.accountName,
                currentUploadFileOperation?.remotePath,
                progressRate,
                totalTransferredSoFar,
                totalToTransfer,
                fileAbsoluteName
            )
            currentUploadFileOperation?.let { cancelOldErrorNotification(it) }
        }
        lastPercent = percent
    }

    private fun cancelOldErrorNotification(uploadFileOperation: UploadFileOperation) {
        // cancel for old file because of file conflicts
        if (uploadFileOperation.oldFile != null) {
            notificationManager.cancel(
                NotificationUtils.createUploadNotificationTag(uploadFileOperation.oldFile),
                NOTIFICATION_ERROR_ID
            )
        }
        notificationManager.cancel(
            NotificationUtils.createUploadNotificationTag(uploadFileOperation.file),
            NOTIFICATION_ERROR_ID
        )
    }

    override fun onStopped() {
        super.onStopped()
        currentUploadFileOperation?.cancel(null)
        notificationManager.cancel(FOREGROUND_SERVICE_ID)
    }

    companion object {
        val TAG: String = FilesUploadWorker::class.java.simpleName
        private const val FOREGROUND_SERVICE_ID: Int = 412
        const val NOTIFICATION_ERROR_ID: Int = 413
        private const val MAX_PROGRESS: Int = 100
        const val ACCOUNT = "data_account"
        var currentUploadFileOperation: UploadFileOperation? = null
    }
}
