/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.nextcloud.utils.extensions.getBitmapSize
import com.nextcloud.utils.extensions.getExifSize
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.MimeTypeUtil

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

    fun getMediaPlaceholder(file: OCFile, imageDimension: Pair<Int, Int>): BitmapDrawable {
        val context = MainApp.getAppContext()

        val drawableId = if (MimeTypeUtil.isImage(file)) {
            R.drawable.file_image
        } else if (MimeTypeUtil.isVideo(file)) {
            R.drawable.file_movie
        } else {
            R.drawable.file
        }

        val drawable = ContextCompat.getDrawable(context, drawableId)
            ?: return Color.GRAY.toDrawable().toBitmap(imageDimension.first, imageDimension.second)
                .toDrawable(context.resources)

        val bitmap = BitmapUtils.drawableToBitmap(
            drawable,
            imageDimension.first,
            imageDimension.second
        )

        return bitmap.toDrawable(context.resources)
    }
}
