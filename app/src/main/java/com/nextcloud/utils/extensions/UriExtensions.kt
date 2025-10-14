/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Returns absolute filesystem path to the media item on disk. I/O errors that could occur. From Android 11 onwards,
 * this column is read-only for apps that target R and higher.
 *
 * [More Info](https://developer.android.com/reference/android/provider/MediaStore.MediaColumns#DATA)
 */
@Suppress("ReturnCount", "TooGenericExceptionCaught")
fun Uri.toFilePath(context: Context): String? {
    try {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)

        val resolver = context.contentResolver

        resolver.query(this, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }

            val dataIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val data = if (dataIdx != -1) cursor.getString(dataIdx) else null
            return data
        }

        return null
    } catch (e: Exception) {
        Log_OC.e("UriExtensions", "exception, toFilePath: $e")
        return null
    }
}
