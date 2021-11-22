/**
 * ownCloud Android client application
 *
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
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

    private fun getDisplayNameFromContentResolver(uri: Uri, context: Context): String? {
        var displayName: String? = null
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null) {
            val displayNameColumn: String = when {
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
            try {
                context.contentResolver.query(
                    uri, arrayOf(displayNameColumn),
                    null,
                    null,
                    null
                ).use { cursor ->
                    if (cursor != null) {
                        cursor.moveToFirst()
                        displayName = cursor.getString(cursor.getColumnIndex(displayNameColumn))
                    }
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Could not retrieve display name for $uri")
                // nothing else, displayName keeps null
            }
        }
        return displayName
    }
}
