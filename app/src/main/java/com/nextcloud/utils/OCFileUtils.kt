/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.nextcloud.utils.extensions.getBitmapSize
import com.nextcloud.utils.extensions.getExifSize
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("TooGenericExceptionCaught", "ReturnCount")
object OCFileUtils {
    private const val TAG = "OCFileUtils"

    fun getImageSize(ocFile: OCFile, defaultThumbnailSize: Float): Pair<Int, Int> {
        val fallback = defaultThumbnailSize.toInt().coerceAtLeast(1)
        val fallbackPair = fallback to fallback

        try {
            Log_OC.d(TAG, "Getting image size for: ${ocFile.fileName}")

            // Server-provided
            ocFile.imageDimension?.let { dim ->
                val w = dim.width.toInt().coerceAtLeast(1)
                val h = dim.height.toInt().coerceAtLeast(1)
                Log_OC.d(TAG, "Using server-provided imageDimension: $w x $h")
                return w to h
            }

            // Local file
            val path = ocFile.storagePath
            if (!path.isNullOrEmpty() && ocFile.exists()) {
                path.getExifSize()?.let { return it }
                path.getBitmapSize()?.let { return it }
            }

            // 3 Fallback
            Log_OC.d(TAG, "Fallback to default size: $fallback x $fallback")
            return fallbackPair
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error getting image size for ${ocFile.fileName}", e)
        }

        return fallbackPair
    }

    fun getMediaPlaceholder(file: OCFile, viewThemeUtils: ViewThemeUtils): Drawable {
        val context = MainApp.getAppContext()

        return MimeTypeUtil.getFileTypeIcon(file.mimeType, file.fileName, context, viewThemeUtils)
            ?: Color.GRAY.toDrawable()
    }
}
