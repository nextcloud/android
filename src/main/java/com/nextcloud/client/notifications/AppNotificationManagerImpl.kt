package com.nextcloud.client.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.utils.theme.ThemeColorUtils
import javax.inject.Inject

class AppNotificationManagerImpl @Inject constructor(
    private val context: Context,
    private val resources: Resources,
    private val platformNotificationsManager: NotificationManager
) : AppNotificationManager {

    companion object {
        const val PROGRESS_PERCENTAGE_MAX = 100
        const val PROGRESS_PERCENTAGE_MIN = 0
    }

    private fun builder(channelId: String): NotificationCompat.Builder {
        val color = ThemeColorUtils.primaryColor(context, true)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, channelId).setColor(color)
        } else {
            NotificationCompat.Builder(context).setColor(color)
        }
    }

    override fun buildDownloadServiceForegroundNotification(): Notification {
        val icon = BitmapFactory.decodeResource(resources, R.drawable.notification_icon)
        return builder(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(resources.getString(R.string.foreground_service_download))
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(icon)
            .build()
    }

    override fun postDownloadProgress(fileOwner: User, file: OCFile, progress: Int, allowPreview: Boolean) {
        val builder = builder(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
        val content = resources.getString(
            R.string.downloader_download_in_progress_content,
            progress,
            file.fileName
        )
        builder
            .setSmallIcon(R.drawable.notification_icon)
            .setTicker(resources.getString(R.string.downloader_download_in_progress_ticker))
            .setContentTitle(resources.getString(R.string.downloader_download_in_progress_ticker))
            .setOngoing(true)
            .setProgress(PROGRESS_PERCENTAGE_MAX, progress, progress <= PROGRESS_PERCENTAGE_MIN)
            .setContentText(content)

        if (allowPreview) {
            val openFileIntent = if (PreviewImageFragment.canBePreviewed(file)) {
                PreviewImageActivity.previewFileIntent(context, fileOwner, file)
            } else {
                FileDisplayActivity.openFileIntent(context, fileOwner, file)
            }
            openFileIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            val pendingOpenFileIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                openFileIntent,
                0
            )
            builder.setContentIntent(pendingOpenFileIntent)
        }
        platformNotificationsManager.notify(AppNotificationManager.DOWNLOAD_NOTIFICATION_ID, builder.build())
    }

    override fun cancelDownloadProgress() {
        platformNotificationsManager.cancel(AppNotificationManager.DOWNLOAD_NOTIFICATION_ID)
    }
}
