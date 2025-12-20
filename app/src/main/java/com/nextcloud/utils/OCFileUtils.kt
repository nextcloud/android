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
import androidx.exifinterface.media.ExifInterface
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
                getExifSize(path)?.let { return it }
                getBitmapSize(path)?.let { return it }
            }

            // 3 Fallback
            Log_OC.d(TAG, "Fallback to default size: $fallback x $fallback")
            return fallbackPair
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error getting image size for ${ocFile.fileName}", e)
        }

        return fallbackPair
    }

    private fun getExifSize(path: String): Pair<Int, Int>? = try {
        val exif = ExifInterface(path)
        var w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        var h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
        ) {
            val tmp = w
            w = h
            h = tmp
        }

        Log_OC.d(TAG, "Using exif imageDimension: $w x $h")
        if (w > 0 && h > 0) w to h else null
    } catch (_: Exception) {
        null
    }

    private fun getBitmapSize(path: String): Pair<Int, Int>? = try {
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, options)
        val w = options.outWidth
        val h = options.outHeight

        Log_OC.d(TAG, "Using bitmap factory imageDimension: $w x $h")
        if (w > 0 && h > 0) w to h else null
    } catch (_: Exception) {
        null
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
