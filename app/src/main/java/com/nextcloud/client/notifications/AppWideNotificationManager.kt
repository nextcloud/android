/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nextcloud.client.notifications.action.SyncConflictNotificationBroadcastReceiver
import com.owncloud.android.R
import com.owncloud.android.ui.activity.UploadListActivity
import com.owncloud.android.ui.notifications.NotificationUtils

/**
 * Responsible for showing **app-wide notifications** in the app.
 *
 * This manager provides a centralized place to create and display notifications
 * that are not tied to a specific screen or feature.
 *
 */
object AppWideNotificationManager {

    private const val SYNC_CONFLICT_NOTIFICATION_INTENT_REQ_CODE = 16
    private const val SYNC_CONFLICT_NOTIFICATION_INTENT_ACTION_REQ_CODE = 17

    private const val SYNC_CONFLICT_NOTIFICATION_ID = 112

    fun showSyncConflictNotification(context: Context) {
        val intent = Intent(context, UploadListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            SYNC_CONFLICT_NOTIFICATION_INTENT_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionIntent = Intent(context, SyncConflictNotificationBroadcastReceiver::class.java).apply {
            putExtra(SyncConflictNotificationBroadcastReceiver.NOTIFICATION_ID, SYNC_CONFLICT_NOTIFICATION_ID)
        }

        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            SYNC_CONFLICT_NOTIFICATION_INTENT_ACTION_REQ_CODE,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
            .setSmallIcon(R.drawable.uploads)
            .setContentTitle(context.getString(R.string.uploader_upload_failed_sync_conflict_error))
            .setContentText(context.getString(R.string.upload_conflict_message))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.upload_conflict_message))
            )
            .addAction(
                R.drawable.ic_cloud_upload,
                context.getString(R.string.upload_list_resolve_conflict),
                actionPendingIntent
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(SYNC_CONFLICT_NOTIFICATION_ID, notification)
    }
}
