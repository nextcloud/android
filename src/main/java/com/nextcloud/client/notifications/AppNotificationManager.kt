package com.nextcloud.client.notifications

import android.app.Notification
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile

/**
 * Application-specific notification manager interface.
 * Contrary to the platform [android.app.NotificationManager],
 * it offer high-level, use-case oriented API.
 */
interface AppNotificationManager {

    companion object {
        const val TRANSFER_NOTIFICATION_ID = 1_000_000
    }

    /**
     * Builds notification to be set when downloader starts in foreground.
     *
     * @return foreground downloader service notification
     */
    fun buildDownloadServiceForegroundNotification(): Notification

    /**
     * Post download transfer progress notification. Subsequent calls will update
     * currently displayed transfer notification.
     *
     * @param fileOwner User owning the downloaded file
     * @param file File being downloaded
     * @param progress Progress as percentage (0-100)
     * @param allowPreview if true, pending intent with preview action is added to the notification
     */
    fun postDownloadTransferProgress(fileOwner: User, file: OCFile, progress: Int, allowPreview: Boolean = true)

    /**
     * Post upload transfer progress notification. Subsequent calls will update
     * currently displayed transfer notification.
     *
     * @param fileOwner User owning the downloaded file
     * @param file File being downloaded
     * @param progress Progress as percentage (0-100)
     */
    fun postUploadTransferProgress(fileOwner: User, file: OCFile, progress: Int)

    /**
     * Removes download or upload progress notification.
     */
    fun cancelTransferNotification()
}
