/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.receiver.OfflineOperationActionReceiver
import com.nextcloud.utils.extensions.getErrorMessage
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.utils.theme.ViewThemeUtils

class OfflineOperationsNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils) :
    WorkerNotificationManager(
        ID,
        context,
        viewThemeUtils,
        R.string.offline_operations_worker_notification_manager_ticker
    ) {

    companion object {
        private const val ID = 121
        private const val ERROR_ID = 122
    }

    @Suppress("MagicNumber")
    fun start() {
        notificationBuilder.run {
            setContentTitle(context.getString(R.string.offline_operations_worker_notification_start_text))
            setProgress(100, 0, false)
        }

        showNotification()
    }

    @Suppress("MagicNumber")
    fun update(totalOperationSize: Int, currentOperationIndex: Int, filename: String) {
        val title = if (totalOperationSize > 1) {
            String.format(
                context.getString(R.string.offline_operations_worker_progress_text),
                currentOperationIndex,
                totalOperationSize,
                filename
            )
        } else {
            filename
        }

        val progress = (currentOperationIndex * 100) / totalOperationSize

        notificationBuilder.run {
            setContentTitle(title)
            setProgress(100, progress, false)
        }

        showNotification()
    }

    fun showNewNotification(result: RemoteOperationResult<*>, operation: RemoteOperation<*>) {
        val reason = (result to operation).getErrorMessage()
        val text = context.getString(R.string.offline_operations_worker_notification_error_text, reason)

        notificationBuilder.run {
            setContentTitle(text)
            setOngoing(false)
            notificationManager.notify(ERROR_ID, this.build())
        }
    }

    fun showConflictResolveNotification(file: OCFile, entity: OfflineOperationEntity?, user: User) {
        val path = entity?.path
        val id = entity?.id

        if (path == null || id == null) {
            return
        }

        val resolveConflictIntent = ConflictsResolveActivity.createIntent(file, path, context)
        val resolveConflictPendingIntent = PendingIntent.getActivity(
            context,
            id,
            resolveConflictIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val resolveConflictAction = NotificationCompat.Action(
            R.drawable.ic_cloud_upload,
            context.getString(R.string.upload_list_resolve_conflict),
            resolveConflictPendingIntent
        )

        val deleteIntent = Intent(context, OfflineOperationActionReceiver::class.java).apply {
            putExtra(OfflineOperationActionReceiver.FILE_PATH, path)
            putExtra(OfflineOperationActionReceiver.USER, user)
        }
        val deletePendingIntent =
            PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_IMMUTABLE)
        val deleteAction = NotificationCompat.Action(
            R.drawable.ic_delete,
            context.getString(R.string.offline_operations_worker_notification_delete_offline_folder),
            deletePendingIntent
        )

        val title = context.getString(
            R.string.offline_operations_worker_notification_conflict_text,
            file.fileName
        )

        notificationBuilder
            .clearActions()
            .setContentTitle(title)
            .setContentIntent(resolveConflictPendingIntent)
            .addAction(deleteAction)
            .addAction(resolveConflictAction)

        notificationManager.notify(id, notificationBuilder.build())
    }

    fun dismissNotification(id: Int?) {
        if (id == null) return
        notificationManager.cancel(id)
    }
}
