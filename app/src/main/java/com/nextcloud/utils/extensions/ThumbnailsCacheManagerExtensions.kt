/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.core.net.toUri
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Retrieves the orientation of an image file from its EXIF metadata or, as a fallback,
 * from the Android MediaStore.
 *
 * This function first attempts to read the orientation using [ExifInterface.TAG_ORIENTATION]
 * directly from the file at the given [path]. If that fails or returns
 * [ExifInterface.ORIENTATION_UNDEFINED], it then queries the MediaStore for the image's
 * stored orientation in degrees (0, 90, 180, or 270), converting that to an EXIF-compatible
 * orientation constant.
 *
 * @param path Absolute file path or content URI (as string) of the image.
 * @return One of the [ExifInterface] orientation constants, e.g.
 * [ExifInterface.ORIENTATION_ROTATE_90], or [ExifInterface.ORIENTATION_UNDEFINED]
 * if the orientation could not be determined.
 *
 * @see ExifInterface
 * @see MediaStore.Images.Media.ORIENTATION
 */
@Suppress("TooGenericExceptionCaught", "NestedBlockDepth", "MagicNumber")
fun getExifOrientation(path: String): Int {
    val context = MainApp.getAppContext()
    if (context == null || path.isBlank()) {
        return ExifInterface.ORIENTATION_UNDEFINED
    }

    var orientation = ExifInterface.ORIENTATION_UNDEFINED

    try {
        val exif = ExifInterface(path)
        orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
    } catch (e: Exception) {
        Log_OC.e("ThumbnailsCacheManager", "getExifOrientation exception: $e")
    }

    // Fallback: query MediaStore if EXIF is undefined
    if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
        try {
            val uri = path.toUri()
            val projection = arrayOf(MediaStore.Images.Media.ORIENTATION)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val orientationIndex = cursor.getColumnIndexOrThrow(projection[0])
                    val degrees = cursor.getInt(orientationIndex)
                    orientation = when (degrees) {
                        90 -> ExifInterface.ORIENTATION_ROTATE_90
                        180 -> ExifInterface.ORIENTATION_ROTATE_180
                        270 -> ExifInterface.ORIENTATION_ROTATE_270
                        else -> ExifInterface.ORIENTATION_NORMAL
                    }
                }
            }
        } catch (e: Exception) {
            Log_OC.e("ThumbnailsCacheManager", "getExifOrientation exception: $e")
        }
    }

    return orientation
}
