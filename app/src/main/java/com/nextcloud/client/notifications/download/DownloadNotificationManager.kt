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

package com.nextcloud.client.notifications.download

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import java.security.SecureRandom

class DownloadNotificationManager(private val context: Context, private val viewThemeUtils: ViewThemeUtils) {

    private var notification: Notification? = null
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun init() {
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils)
            .setContentTitle(context.resources.getString(R.string.app_name))
            .setContentText(context.resources.getString(R.string.foreground_service_download))
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
        }

        notification = notificationBuilder.build()
    }

    @Suppress("MagicNumber")
    fun notifyForStart(operation: DownloadFileOperation) {
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils)
            .setSmallIcon(R.drawable.notification_icon)
            .setTicker(context.getString(R.string.downloader_download_in_progress_ticker))
            .setContentTitle(context.getString(R.string.downloader_download_in_progress_ticker))
            .setOngoing(true)
            .setProgress(100, 0, operation.size < 0)
            .setContentText(
                String.format(
                    context.getString(R.string.downloader_download_in_progress_content), 0,
                    File(operation.savePath).name
                )
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
        }
    }

    @Suppress("MagicNumber")
    fun notifyForResult(result: RemoteOperationResult<*>, download: DownloadFileOperation) {
        dismissDownloadInProgressNotification()

        val tickerId = getTickerId(result.isSuccess, null)
        val notifyId = SecureRandom().nextInt()

        val contentText = if (result.isSuccess) {
            download.file.fileName
        } else {
            ErrorMessageAdapter.getErrorCauseMessage(
                result,
                download,
                context.resources
            )
        }

        notificationBuilder.run {
            setTicker(context.getString(tickerId))
            setContentText(contentText)
            notificationManager.notify(notifyId, this.build())
        }

        NotificationUtils.cancelWithDelay(
            notificationManager,
            notifyId,
            2000
        )
    }

    private fun getTickerId(isSuccess: Boolean, needsToUpdateCredentials: Boolean?): Int {
        return if (needsToUpdateCredentials == true) {
            R.string.downloader_download_failed_credentials_error
        } else {
            if (isSuccess) {
                R.string.downloader_download_succeeded_ticker
            } else {
                R.string.downloader_download_failed_ticker
            }
        }
    }

    fun prepareForResult(
        downloadResult: RemoteOperationResult<*>,
        needsToUpdateCredentials: Boolean
    ) {
        val tickerId = getTickerId(downloadResult.isSuccess, needsToUpdateCredentials)

        notificationBuilder
            .setTicker(context.getString(tickerId))
            .setContentTitle(context.getString(tickerId))
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)
    }

    @Suppress("MagicNumber")
    fun updateDownloadProgressNotification(filePath: String, percent: Int, totalToTransfer: Long) {
        notificationBuilder.setProgress(100, percent, totalToTransfer < 0)
        val fileName: String = filePath.substring(filePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1)
        val text =
            String.format(context.getString(R.string.downloader_download_in_progress_content), percent, fileName)
        notificationBuilder.setContentText(text)
    }

    fun showDownloadInProgressNotification() {
        notificationManager.notify(
            R.string.downloader_download_in_progress_ticker,
            notificationBuilder.build()
        )
    }

    fun dismissDownloadInProgressNotification() {
        notificationManager.cancel(R.string.downloader_download_in_progress_ticker)
    }

    fun setCredentialContentIntent(user: User) {
        val intent = Intent(context, AuthenticatorActivity::class.java).apply {
            putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, user.toPlatformAccount())
            putExtra(
                AuthenticatorActivity.EXTRA_ACTION,
                AuthenticatorActivity.ACTION_UPDATE_EXPIRED_TOKEN
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_FROM_BACKGROUND)
        }

        setContentIntent(intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun setContentIntent(intent: Intent, flag: Int) {
        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                flag
            )
        )
    }
}
