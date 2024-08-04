/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * A helper class for some Uri operations.
 */
object UriUtils {
    private val TAG = UriUtils::class.java.simpleName
    const val URI_CONTENT_SCHEME = "content://"

    @JvmStatic
    fun getDisplayNameForUri(uri: Uri?, context: Context?): String? {
        require(!(uri == null || context == null)) { "Received NULL!" }
        var displayName: String?

        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            // content: URI
            displayName = getDisplayNameFromContentResolver(uri, context)
            if (displayName == null) {
                // last chance to have a name
                displayName = uri.lastPathSegment?.replace("\\s".toRegex(), "")
            }
        } else {
            displayName = uri.lastPathSegment // ready to return
        }

        // Replace path separator characters to avoid inconsistent paths
        return displayName?.replace("/".toRegex(), "-")
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getDisplayNameFromContentResolver(uri: Uri, context: Context): String? {
        val mimeType = context.contentResolver.getType(uri) ?: return null
        val displayNameColumn: String = getDisplayNameColumnForMimeType(mimeType)
        var displayName: String? = null
        try {
            context.contentResolver.query(
                uri,
                arrayOf(displayNameColumn),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor != null) {
                    cursor.moveToFirst()
                    displayName = cursor.getString(cursor.getColumnIndexOrThrow(displayNameColumn))
                }
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Could not retrieve display name for $uri")
            // nothing else, displayName keeps null
        }
        return displayName
    }

    private fun getDisplayNameColumnForMimeType(mimeType: String?) = when {
        MimeTypeUtil.isImage(mimeType) -> {
            MediaStore.Images.ImageColumns.DISPLAY_NAME
        }
        MimeTypeUtil.isVideo(mimeType) -> {
            MediaStore.Video.VideoColumns.DISPLAY_NAME
        }
        MimeTypeUtil.isAudio(mimeType) -> {
            MediaStore.Audio.AudioColumns.DISPLAY_NAME
        }
        else -> {
            MediaStore.Files.FileColumns.DISPLAY_NAME
        }
    }
}
