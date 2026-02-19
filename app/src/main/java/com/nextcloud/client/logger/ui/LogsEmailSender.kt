/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger.ui

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Cancellable
import com.nextcloud.client.core.Clock
import com.nextcloud.client.logger.LogEntry
import com.owncloud.android.R
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FileExportUtils
import java.io.File
import java.security.SecureRandom
import java.util.TimeZone

class LogsEmailSender(private val context: Context, private val clock: Clock, private val runner: AsyncRunner) {

    private companion object {
        const val LOGS_MIME_TYPE = "text/plain"
    }

    private class Task(
        private val context: Context,
        private val logs: List<LogEntry>,
        private val file: File,
        private val tz: TimeZone
    ) : Function0<Uri?> {

        override fun invoke(): Uri? {
            file.parentFile?.mkdirs()

            file.outputStream().use { outputStream ->
                outputStream.writer(Charsets.UTF_8).buffered().use { writer ->
                    logs.forEach {
                        writer.write(it.toString(tz))
                        writer.newLine()
                    }
                }
            }

            return FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file)
        }
    }

    private var task: Cancellable? = null

    fun send(logs: List<LogEntry>) {
        if (task == null) {
            val outFile = File(context.cacheDir, "attachments/logs.txt")
            task = runner.postQuickTask(Task(context, logs, outFile, clock.tz), onResult = {
                task = null
                send(it)
            })
        }
    }

    fun export(logs: List<LogEntry>) {
        if (task == null) {
            val outFile = File(context.cacheDir, "attachments/logs.txt")
            task = runner.postQuickTask(Task(context, logs, outFile, clock.tz), onResult = {
                task = null
                export(outFile)
            })
        }
    }

    fun stop() {
        if (task != null) {
            task?.cancel()
            task = null
        }
    }

    private fun export(file: File) {
        FileExportUtils().exportFile(
            "Nextcloud Android Files Logs",
            "text/plain",
            context.contentResolver,
            null,
            file
        )
        showSuccessNotification(1)
    }

    fun showSuccessNotification(successfulExports: Int) {
        showNotification(
            context.resources.getQuantityString(
                R.plurals.export_successful,
                successfulExports,
                successfulExports
            )
        )
    }

    private fun showNotification(message: String) {
        val notificationId = SecureRandom().nextInt()

        val notificationBuilder = NotificationCompat.Builder(
            context,
            NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
        )
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(message)
            .setAutoCancel(true)

        val actionIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        val actionPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            actionIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.addAction(
            NotificationCompat.Action(
                null,
                context.getString(R.string.locate_folder),
                actionPendingIntent
            )
        )

        val notificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun send(uri: Uri?) {
        task = null
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.putExtra(Intent.EXTRA_EMAIL, context.getString(R.string.mail_logger))

        val subject = context.getString(R.string.log_send_mail_subject).format(context.getString(R.string.app_name))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)

        intent.putExtra(Intent.EXTRA_TEXT, getPhoneInfo())

        intent.flags = FLAG_ACTIVITY_NEW_TASK
        intent.type = LOGS_MIME_TYPE
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.log_send_no_mail_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPhoneInfo(): String = "Model: " + Build.MODEL + "\n" +
        "Brand: " + Build.BRAND + "\n" +
        "Product: " + Build.PRODUCT + "\n" +
        "Device: " + Build.DEVICE + "\n" +
        "Version-Codename: " + Build.VERSION.CODENAME + "\n" +
        "Version-Release: " + Build.VERSION.RELEASE
}
