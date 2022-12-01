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
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.UploadListActivity
import com.owncloud.android.ui.notifications.NotificationUtils
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
        val accountName = inputData.getString(ACCOUNT)
        if (accountName.isNullOrEmpty()) {
            Log_OC.w(TAG, "User was null for file upload worker")
            return Result.failure() // user account is needed
        }

        /*
         * As pages are retrieved by sorting uploads by ID, if new uploads are added while uploading the current ones,
         * they will be present in the pages that follow.
         */
        var currentPage = uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(-1, accountName)
        while (currentPage.isNotEmpty()) {
            Log_OC.d(TAG, "Handling ${currentPage.size} uploads for account $accountName")
            val lastId = currentPage.last().uploadId
            handlePendingUploads(currentPage, accountName)
            currentPage =
                uploadsStorageManager.getCurrentAndPendingUploadsForAccountPageAscById(lastId, accountName)
        }

        Log_OC.d(TAG, "No more pending uploads for account $accountName, stopping work")
        return Result.success()
    }

    private fun handlePendingUploads(uploads: List<OCUpload>, accountName: String) {
        val user = userAccountManager.getUser(accountName)

        for (upload in uploads) {
            // create upload file operation
            if (user.isPresent) {
                val uploadFileOperation = createUploadFileOperation(upload, user.get())

                val result = upload(uploadFileOperation, user.get())

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
            uploadsStorageManager.updateDatabaseUploadResult(uploadResult, uploadFileOperation)

            // cancel notification
            notificationManager.cancel(FOREGROUND_SERVICE_ID)
        }

        return uploadResult
    }

    /**
     * adapted from [com.owncloud.android.files.services.FileUploader.notifyUploadStart]
     */
    private fun createNotification(uploadFileOperation: UploadFileOperation) {
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
        }
        lastPercent = percent
    }

    companion object {
        val TAG: String = FilesUploadWorker::class.java.simpleName
        private const val FOREGROUND_SERVICE_ID: Int = 412
        private const val MAX_PROGRESS: Int = 100
        const val ACCOUNT = "data_account"
    }
}
