/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.logger.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Cancellable
import com.nextcloud.client.core.Clock
import com.nextcloud.client.logger.LogEntry
import com.owncloud.android.R
import java.io.File
import java.io.FileWriter
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
            file.parentFile.mkdirs()
            val fo = FileWriter(file, false)
            logs.forEach {
                fo.write(it.toString(tz))
                fo.write("\n")
            }
            fo.close()
            return FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file)
        }
    }

    private var task: Cancellable? = null

    fun send(logs: List<LogEntry>) {
        if (task == null) {
            val outFile = File(context.cacheDir, "attachments/logs.txt")
            task = runner.postQuickTask(Task(context, logs, outFile, clock.tz), onResult = { task = null; send(it) })
        }
    }

    fun stop() {
        if (task != null) {
            task?.cancel()
            task = null
        }
    }

    private fun send(uri: Uri?) {
        task = null
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.putExtra(Intent.EXTRA_EMAIL, context.getString(R.string.mail_logger))

        val subject = context.getString(R.string.log_send_mail_subject).format(context.getString(R.string.app_name))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)

        intent.putExtra(Intent.EXTRA_TEXT, getPhoneInfo())

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.type = LOGS_MIME_TYPE
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri))
        try {
            context.startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(context, R.string.log_send_no_mail_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPhoneInfo(): String {
        return "Model: " + Build.MODEL + "\n" +
            "Brand: " + Build.BRAND + "\n" +
            "Product: " + Build.PRODUCT + "\n" +
            "Device: " + Build.DEVICE + "\n" +
            "Version-Codename: " + Build.VERSION.CODENAME + "\n" +
            "Version-Release: " + Build.VERSION.RELEASE
    }
}
