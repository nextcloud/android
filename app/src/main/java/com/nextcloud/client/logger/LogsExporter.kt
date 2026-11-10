/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.logger

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.nextcloud.client.core.Clock
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

class LogsExporter @Inject constructor(
    private val context: Context,
    private val clock: Clock,
    private val repository: LogsRepository
) {
    suspend fun export(): Boolean = withContext(Dispatchers.IO) {
        val entries = suspendCancellableCoroutine { continuation ->
            repository.load { entries, _ ->
                continuation.resume(entries)
            }
        }
        if (entries.isEmpty()) return@withContext false
        ensureActive()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.ROOT).apply {
            timeZone = clock.tz
        }.format(clock.currentDate)
        val fileName = "Nextcloud-logs-$timestamp.txt"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeToMediaStore(fileName, entries)
        } else {
            writeToPublicDirectory(fileName, entries)
        }
        true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    internal suspend fun writeToMediaStore(fileName: String, entries: List<LogEntry>) {
        val resolver = context.contentResolver
        val uri = createPendingExport(fileName)
        try {
            val stream = resolver.openOutputStream(uri, "w") ?: throw IOException("Could not open log export")
            stream.bufferedWriter(Charsets.UTF_8).use { writeEntries(it, entries) }
            coroutineContext.ensureActive()
            publishExport(uri)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }.onFailure {
                Log_OC.e(TAG, "Could not remove incomplete log export", it)
            }
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createPendingExport(fileName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Nextcloud")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create log export")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publishExport(uri: Uri) {
        val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        if (context.contentResolver.update(uri, published, null, null) != 1) {
            throw IOException("Could not publish log export")
        }
    }

    @Suppress("DEPRECATION")
    internal suspend fun writeToPublicDirectory(fileName: String, entries: List<LogEntry>) {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Nextcloud"
        )
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Could not create log export directory")
        }
        val file = File.createTempFile(fileName.removeSuffix(".txt") + "-", ".txt", directory)
        try {
            file.bufferedWriter(Charsets.UTF_8).use { writeEntries(it, entries) }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    private suspend fun writeEntries(writer: Writer, entries: List<LogEntry>) {
        for (entry in entries) {
            coroutineContext.ensureActive()
            writer.write(entry.toString(clock.tz))
            writer.write("\n")
        }
    }

    companion object {
        private const val TAG = "LogsExporter"
    }
}
