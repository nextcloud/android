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

object OCFileUtils {
    private const val TAG = "OCFileUtils"

    @Suppress("ReturnCount", "NestedBlockDepth")
    fun getImageSize(ocFile: OCFile, defaultThumbnailSize: Float): Pair<Int, Int> {
        try {
            Log_OC.d(TAG, "Getting image size for: ${ocFile.fileName}")

            if (!ocFile.exists()) {
                ocFile.imageDimension?.width?.let { w ->
                    ocFile.imageDimension?.height?.let { h ->
                        Log_OC.d(TAG, "Image dimensions are used width: $w and height: $h")
                        return w.toInt() to h.toInt()
                    }
                }

                Log_OC.d(TAG, "Default size is used: $defaultThumbnailSize")
                val size = defaultThumbnailSize.toInt().coerceAtLeast(1)
                return size to size
            }

            val exif = ExifInterface(ocFile.storagePath)
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

            if (width > 0 && height > 0) {
                Log_OC.d(TAG, "Exif used width: $width and height: $height")
                return width to height
            }

            val (bitmapWidth, bitmapHeight) = BitmapUtils.getImageResolution(ocFile.storagePath)
                .let { it[0] to it[1] }

            if (bitmapWidth > 0 && bitmapHeight > 0) {
                Log_OC.d(TAG, "BitmapUtils.getImageResolution used width: $bitmapWidth and height: $bitmapHeight")
                return bitmapWidth to bitmapHeight
            }

            val fallback = defaultThumbnailSize.toInt().coerceAtLeast(1)
            Log_OC.d(TAG, "Default size used width: $fallback and height: $fallback")
            return fallback to fallback
        } finally {
            Log_OC.d(TAG, "-----------------------------")
        }
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
