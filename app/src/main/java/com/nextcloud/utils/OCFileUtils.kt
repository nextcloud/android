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

object OCFileUtils {
    private const val TAG = "OCFileUtils"

    @Suppress("ReturnCount")
    fun getImageSize(ocFile: OCFile, defaultThumbnailSize: Float): Pair<Int, Int> {
        try {
            Log_OC.d(TAG, "Getting image size for: ${ocFile.fileName}")

            if (!ocFile.exists()) {
                Log_OC.d(TAG, "File not downloaded, using fallbackSize.")
                return fallbackSize(ocFile, defaultThumbnailSize)
            }

            val exif = ExifInterface(ocFile.storagePath)
            val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

            return if (width > 0 && height > 0) {
                Log_OC.d(TAG, "Exif used width: $width and height: $height")
                width to height
            } else {
                Log_OC.d(TAG, "Exif data missing using fallbackSize.")
                fallbackSize(ocFile, defaultThumbnailSize)
            }
        } finally {
            Log_OC.d(TAG, "-----------------------------")
        }
    }

    private fun fallbackSize(file: OCFile, defaultSize: Float): Pair<Int, Int> {
        if (file.exists()) {
            val (width, height) = BitmapUtils.getImageResolution(file.storagePath).let { it[0] to it[1] }
            if (width > 0 && height > 0) {
                Log_OC.d(TAG, "BitmapUtils.getImageResolution used width: $width and height: $height")
                return width to height
            }
        }

        file.imageDimension?.width?.let { width ->
            file.imageDimension?.height?.let { height ->
                Log_OC.d(TAG, "Image dimension used width: $width and height: $height")
                return width.toInt() to height.toInt()
            }
        }

        Log_OC.d(TAG, "Default size used width: $defaultSize and height: $defaultSize")
        val defaultSize = defaultSize.toInt()
        return defaultSize to defaultSize
    }
}
