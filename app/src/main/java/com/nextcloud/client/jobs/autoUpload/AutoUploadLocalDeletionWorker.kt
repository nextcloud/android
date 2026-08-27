/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.app.Notification
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.upload.DeleteUploadedFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

class AutoUploadLocalDeletionWorker(
    private val context: Context,
    params: WorkerParameters,
    private val userAccountManager: UserAccountManager,
    private val syncedFolderProvider: SyncedFolderProvider,
    val viewThemeUtils: ViewThemeUtils
) : CoroutineWorker(context, params) {

    companion object {
        const val SYNCED_FOLDER_IDS = "synced_folder_IDs"
        const val NOTIFICATION_ID = 267

        private const val TAG = "\uD83D\uDDD1\uFE0F AutoUploadLocalDeletionWorker"
    }

    private val notificationManager = WorkerNotificationManager(
        NOTIFICATION_ID,
        context,
        viewThemeUtils,
        R.string.autoupload_delete_uploaded_notif_ticker,
        NotificationUtils.NOTIFICATION_CHANNEL_BACKGROUND_OPERATIONS
    )

    override suspend fun doWork(): Result {
        showNotification(
            createNotification(context.getString(R.string.autoupload_delete_uploaded_notif_started_title))
        )
        Log_OC.d(TAG, "Started")

        val syncedFolderIDs = inputData.getLongArray(SYNCED_FOLDER_IDS)
            ?: throw IllegalArgumentException("$SYNCED_FOLDER_IDS param is mandatory")
        val syncedFolders = syncedFolderIDs
            .map { syncedFolderProvider.getSyncedFolderByID(it) }

        var filesPreserved = 0L
        var foldersAnalyzed = 0L
        var filesRemoved = 0L
        var spaceFreed = 0L
        syncedFolders
            .filterNotNull()
            .filter { it.isEnabled }
            .filter { FileUtil.isFolderWritable(File(it.localPath)) }
            .forEach {
                val sharedFolderOwner = userAccountManager.getUser(it.account).get()
                val fileDataStorageManager = FileDataStorageManager(sharedFolderOwner, context.contentResolver)
                val op = DeleteUploadedFileOperation(
                    it,
                    context,
                    fileDataStorageManager
                )
                val res = op.run()
                if (res.code != RemoteOperationResult.ResultCode.OK) {
                    Log_OC.d(TAG, "Failed")
                    showNotification(
                        createNotification(context.getString(R.string.autoupload_delete_uploaded_notif_error_title))
                    )
                    return Result.failure()
                }
                foldersAnalyzed++
                filesPreserved += res.resultData.filesPreserved
                filesRemoved += res.resultData.filesRemoved
                spaceFreed += res.resultData.spaceFreed
            }


        showNotification(
            createSuccessNotification(
                foldersAnalyzed,
                filesRemoved,
                filesPreserved,
                spaceFreed
            )
        )
        Log_OC.d(TAG, "Success: foldersAnalyzed=$foldersAnalyzed, filesPreserved=$filesPreserved, " +
            "filesRemoved=$filesRemoved, spaceFreed=$spaceFreed bytes")
        return Result.success()
    }

    private fun createSuccessNotification(
        foldersRemoved: Long,
        filesRemoved: Long,
        filesPreserved: Long,
        spaceFreed: Long
    ): Notification {
        var notificationContent = context.getString(
            R.string.autoupload_delete_uploaded_notif_ended_content,
            DisplayUtils.bytesToHumanReadable(spaceFreed),
            filesRemoved,
            foldersRemoved
        )
        if (filesPreserved > 0) {
            notificationContent +=
                "\n" +
                context.getString(
                    R.string.autoupload_delete_uploaded_notif_ended_content_preserved,
                    filesPreserved
                )
        }
        return createNotification(
            title = context.getString(R.string.autoupload_delete_uploaded_notif_ended_title),
            content = notificationContent
        )
    }

    private fun createNotification(title: String, content: String? = null): Notification =
        notificationManager.notificationBuilder
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_delete)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_BACKGROUND_OPERATIONS)
            .build()

    private fun showNotification(notification: Notification) = notificationManager.showNotification()
}
