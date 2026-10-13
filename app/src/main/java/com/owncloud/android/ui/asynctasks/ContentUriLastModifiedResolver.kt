/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.asynctasks

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import java.util.concurrent.TimeUnit

class ContentUriLastModifiedResolver(private val contentResolver: ContentResolver) {
    companion object {
        private val TIMESTAMP_COLUMNS = listOf(
            DocumentsContract.Document.COLUMN_LAST_MODIFIED to TimeUnit.MILLISECONDS,
            MediaStore.MediaColumns.DATE_MODIFIED to TimeUnit.SECONDS
        )
    }

    fun resolve(uri: Uri): Long = fromMetadata(uri).takeIf { it > 0L } ?: fromFileDescriptor(uri)

    private fun fromMetadata(uri: Uri): Long = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null

            TIMESTAMP_COLUMNS.firstNotNullOfOrNull { (column, unit) ->
                runCatching {
                    cursor.getColumnIndex(column)
                        .takeIf { it >= 0 && !cursor.isNull(it) }
                        ?.let(cursor::getLong)
                        ?.takeIf { it > 0L }
                        ?.let(unit::toMillis)
                }.getOrNull()
            }
        }
    }.getOrNull() ?: 0L

    private fun fromFileDescriptor(uri: Uri): Long = runCatching {
        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            Os.fstat(pfd.fileDescriptor)
                .takeIf { OsConstants.S_ISREG(it.st_mode) }
                ?.lastModifiedMillis()
                ?.takeIf { it > 0L }
        }
    }.getOrNull() ?: 0L

    private fun StructStat.lastModifiedMillis(): Long =
        TimeUnit.SECONDS.toMillis(st_mtim.tv_sec) + TimeUnit.NANOSECONDS.toMillis(st_mtim.tv_nsec)
}
