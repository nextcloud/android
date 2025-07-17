/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils
import androidx.exifinterface.media.ExifInterface
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import java.io.File

object OCFileUtils {
    private const val TAG = "OCFileUtils"

    @Suppress("ReturnCount")
    fun getImageSize(ocFile: OCFile, defaultThumbnailSize: Float): Pair<Int, Int> {
        val storagePath = ocFile.storagePath ?: return fallbackSize(ocFile, defaultThumbnailSize)

        if (!File(storagePath).exists()) {
            Log_OC.d(TAG, "File not downloaded. Using imageDimension or default size.")
            return fallbackSize(ocFile, defaultThumbnailSize)
        }

        val exif = ExifInterface(storagePath)
        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

        return if (width > 0 && height > 0) {
            Log_OC.d(TAG, "Exif used to determine width and height")
            width to height
        } else {
            Log_OC.d(TAG, "Exif data missing. Using fallback size.")
            fallbackSize(ocFile, defaultThumbnailSize)
        }
    }

    private fun fallbackSize(file: OCFile, defaultSize: Float): Pair<Int, Int> {
        val path = file.storagePath
        if (path != null) {
            val (w, h) = BitmapUtils.getImageResolution(path).let { it[0] to it[1] }
            if (w > 0 && h > 0) return w to h
        }
        val width = (file.imageDimension?.width ?: defaultSize).toInt()
        val height = (file.imageDimension?.height ?: defaultSize).toInt()
        return width to height
    }
}
