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
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.client.jobs.offlineOperations.receiver.OfflineOperationReceiver
import com.nextcloud.utils.extensions.getErrorMessage
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

class OfflineOperationsNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils) :
    WorkerNotificationManager(
        ID,
        context,
        viewThemeUtils,
        tickerId = R.string.offline_operations_worker_notification_manager_ticker,
        channelId = NotificationUtils.NOTIFICATION_CHANNEL_OFFLINE_OPERATIONS
    ) {

    companion object {
        private const val ID = 121
        const val ERROR_ID = 122

        private const val ONE_HUNDRED_PERCENT = 100
    }

    fun start() {
        notificationBuilder.run {
            setContentTitle(context.getString(R.string.offline_operations_worker_notification_start_text))
            setProgress(ONE_HUNDRED_PERCENT, 0, false)
        }

        showNotification()
    }

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

        val progress = (currentOperationIndex * ONE_HUNDRED_PERCENT) / totalOperationSize

        notificationBuilder.run {
            setContentTitle(title)
            setProgress(ONE_HUNDRED_PERCENT, progress, false)
        }

        showNotification()
    }

    fun showNewNotification(id: Int?, result: RemoteOperationResult<*>, operation: RemoteOperation<*>) {
        val reason = (result to operation).getErrorMessage()
        val text = context.getString(R.string.offline_operations_worker_notification_error_text, reason)
        val cancelOfflineOperationAction = id?.let { getCancelOfflineOperationAction(it) }

        notificationBuilder.run {
            cancelOfflineOperationAction?.let {
                addAction(it)
            }
            setContentTitle(text)
            setOngoing(false)
            setProgress(0, 0, false)
            notificationManager.notify(ERROR_ID, this.build())
        }
    }

    fun showConflictNotificationForDeleteOrRemoveOperation(entity: OfflineOperationEntity?) {
        val id = entity?.id
        if (id == null) {
            return
        }

        val title = entity.getConflictText(context)

        notificationBuilder
            .setProgress(0, 0, false)
            .setOngoing(false)
            .clearActions()
            .setContentTitle(title)

        notificationManager.notify(id, notificationBuilder.build())
    }

    fun showConflictResolveNotification(file: OCFile, entity: OfflineOperationEntity?) {
        val path = entity?.path
        val id = entity?.id

        if (path == null || id == null) {
            return
        }

        val resolveConflictAction = getResolveConflictAction(file, id, path)

        val title = entity.getConflictText(context)

        notificationBuilder
            .setProgress(0, 0, false)
            .setOngoing(false)
            .clearActions()
            .setContentTitle(title)
            .setContentIntent(resolveConflictAction.actionIntent)
            .addAction(resolveConflictAction)

        notificationManager.notify(id, notificationBuilder.build())
    }

    private fun getResolveConflictAction(file: OCFile, id: Int, path: String): NotificationCompat.Action {
        val intent = ConflictsResolveActivity.createIntent(file, path, context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action(
            R.drawable.ic_cloud_upload,
            context.getString(R.string.upload_list_resolve_conflict),
            pendingIntent
        )
    }

    private fun getCancelOfflineOperationAction(id: Int): NotificationCompat.Action {
        val intent = Intent(context, OfflineOperationReceiver::class.java).apply {
            putExtra(OfflineOperationReceiver.ID, id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action(
            R.drawable.ic_delete,
            context.getString(R.string.common_cancel),
            pendingIntent
        )
    }

    fun dismissNotification(id: Int?) {
        if (id == null) return
        notificationManager.cancel(id)
    }
}
