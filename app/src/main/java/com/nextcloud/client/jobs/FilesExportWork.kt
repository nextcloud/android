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

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FileExportUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.security.SecureRandom

class FilesExportWork(
    private val appContext: Context,
    private val user: User,
    private val contentResolver: ContentResolver,
    private val viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : Worker(appContext, params) {

    private lateinit var storageManager: FileDataStorageManager

    override fun doWork(): Result {
        val fileIDs = inputData.getLongArray(FILES_TO_DOWNLOAD) ?: LongArray(0)

        if (fileIDs.isEmpty()) {
            Log_OC.w(this, "File export was started without any file")
            return Result.success()
        }

        storageManager = FileDataStorageManager(user, contentResolver)

        val successfulExports = exportFiles(fileIDs)

        // show notification
        showSuccessNotification(successfulExports)

        return Result.success()
    }

    private fun exportFiles(fileIDs: LongArray): Int {
        var successfulExports = 0
        fileIDs
            .asSequence()
            .map { storageManager.getFileById(it) }
            .filterNotNull()
            .forEach { ocFile ->
                if (!FileStorageUtils.checkIfEnoughSpace(ocFile)) {
                    showErrorNotification(successfulExports)
                    return@forEach
                }

                if (ocFile.isDown) {
                    try {
                        exportFile(ocFile)
                    } catch (e: IllegalStateException) {
                        Log_OC.e(TAG, "Error exporting file", e)
                        showErrorNotification(successfulExports)
                    }
                } else {
                    downloadFile(ocFile)
                }

                successfulExports++
            }
        return successfulExports
    }

    @Throws(IllegalStateException::class)
    private fun exportFile(ocFile: OCFile) {
        FileExportUtils().exportFile(ocFile.fileName, ocFile.mimeType, contentResolver, ocFile, null)
    }

    private fun downloadFile(ocFile: OCFile) {
        val i = Intent(appContext, FileDownloader::class.java)
        i.putExtra(FileDownloader.EXTRA_USER, user)
        i.putExtra(FileDownloader.EXTRA_FILE, ocFile)
        i.putExtra(SendShareDialog.PACKAGE_NAME, "")
        i.putExtra(SendShareDialog.ACTIVITY_NAME, "")
        i.putExtra(FileDownloader.DOWNLOAD_TYPE, DownloadType.EXPORT)
        appContext.startService(i)
    }

    private fun showErrorNotification(successfulExports: Int) {
        if (successfulExports == 0) {
            showNotification(
                appContext.resources.getQuantityString(R.plurals.export_failed, successfulExports, successfulExports)
            )
        } else {
            showNotification(
                appContext.resources.getQuantityString(
                    R.plurals.export_partially_failed,
                    successfulExports,
                    successfulExports
                )
            )
        }
    }

    private fun showSuccessNotification(successfulExports: Int) {
        showNotification(
            appContext.resources.getQuantityString(
                R.plurals.export_successful,
                successfulExports,
                successfulExports
            )
        )
    }

    private fun showNotification(message: String) {
        val notificationId = SecureRandom().nextInt()

        val notificationBuilder = NotificationCompat.Builder(
            appContext,
            NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
        )
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(appContext.resources, R.drawable.notification_icon))
            .setSubText(user.accountName)
            .setContentText(message)
            .setAutoCancel(true)

        viewThemeUtils.androidx.themeNotificationCompatBuilder(appContext, notificationBuilder)

        val actionIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        val actionPendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            actionIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.addAction(
            NotificationCompat.Action(
                null,
                appContext.getString(R.string.locate_folder),
                actionPendingIntent
            )
        )

        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        const val FILES_TO_DOWNLOAD = "files_to_download"
        private val TAG = FilesExportWork::class.simpleName
    }
}
