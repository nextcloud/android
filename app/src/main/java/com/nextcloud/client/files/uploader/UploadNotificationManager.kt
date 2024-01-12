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

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.owncloud.android.R
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.security.SecureRandom

class UploadNotificationManager(private val context: Context, private val viewThemeUtils: ViewThemeUtils) {

    companion object {
        private const val WORKER_ID = 411
    }

    private var notification: Notification? = null
    private val secureRandomGenerator = SecureRandom()
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        initNotificationBuilder()
    }

    private fun initNotificationBuilder() {
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils).apply {
            setContentTitle(context.resources.getString(R.string.app_name))
            setContentText(context.resources.getString(R.string.foreground_service_upload))
            setSmallIcon(R.drawable.notification_icon)
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
            setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
        }

        notification = notificationBuilder.build()
    }

    @Suppress("MagicNumber")
    fun notifyForStart(upload: UploadFileOperation, pendingIntent: PendingIntent, startIntent: PendingIntent) {
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils).apply {
            setOngoing(true)
            setSmallIcon(R.drawable.notification_icon)
            setTicker(context.getString(R.string.uploader_upload_in_progress_ticker))
            setContentTitle(context.getString(R.string.uploader_upload_in_progress_ticker))
            setProgress(100, 0, false)
            setContentText(
                String.format(
                    context.getString(R.string.uploader_upload_in_progress_content),
                    0,
                    upload.fileName
                )
            )
            clearActions()
            addAction(
                R.drawable.ic_action_cancel_grey,
                context.getString(R.string.common_cancel),
                pendingIntent
            )
            setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
            setContentIntent(startIntent)
        }

        if (!upload.isInstantPicture && !upload.isInstantVideo) {
            showWorkerNotification()
        }
    }

    fun notifyForResult(tickerId: Int) {
        notificationBuilder
            .setTicker(context.getString(tickerId))
            .setContentTitle(context.getString(tickerId))
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)
            .clearActions()
    }

    fun setContentIntent(pendingIntent: PendingIntent) {
        notificationBuilder.setContentIntent(pendingIntent)
    }

    fun setContentText(text: String) {
        notificationBuilder.setContentText(text)
    }

    fun addAction(icon: Int, textId: Int, intent: PendingIntent) {
        notificationBuilder.addAction(
            icon,
            context.getString(textId),
            intent
        )
    }

    fun showNotificationTag(operation: UploadFileOperation) {
        notificationManager.notify(
            NotificationUtils.createUploadNotificationTag(operation.file),
            FileUploadWorker.NOTIFICATION_ERROR_ID,
            notificationBuilder.build()
        )
    }

    fun showRandomNotification() {
        notificationManager.notify(secureRandomGenerator.nextInt(), notificationBuilder.build())
    }

    private fun showWorkerNotification() {
        notificationManager.notify(WORKER_ID, notificationBuilder.build())
    }

    @Suppress("MagicNumber")
    fun updateUploadProgressNotification(filePath: String, percent: Int, currentOperation: UploadFileOperation?) {
        notificationBuilder.setProgress(100, percent, false)

        val fileName = filePath.substring(filePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1)
        val text = String.format(context.getString(R.string.uploader_upload_in_progress_content), percent, fileName)

        notificationBuilder.setContentText(text)
        showWorkerNotification()
        dismissOldErrorNotification(currentOperation)
    }

    fun dismissOldErrorNotification(operation: UploadFileOperation?) {
        if (operation == null) {
            return
        }

        notificationManager.cancel(
            NotificationUtils.createUploadNotificationTag(operation.file),
            FileUploadWorker.NOTIFICATION_ERROR_ID
        )

        operation.oldFile?.let {
            notificationManager.cancel(
                NotificationUtils.createUploadNotificationTag(it),
                FileUploadWorker.NOTIFICATION_ERROR_ID
            )
        }
    }

    fun dismissWorkerNotifications() {
        notificationManager.cancel(WORKER_ID)
    }
}
