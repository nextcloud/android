/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.owncloud.android.lib.common.utils.Log_OC

@Suppress("MagicNumber")
fun Bitmap.allocationKilobyte(): Int = allocationByteCount.div(1024)

/**
 * Recursively scales down the Bitmap until its size allocation is within the specified size.
 *
 * This function checks if the current Bitmap's size (in kilobytes) is already within
 * the target size. If not, it scales the Bitmap down by a factor of `1.5` in both width and height
 * and calls itself recursively until the size condition is met.
 *
 * @receiver Bitmap The original Bitmap to be resized.
 * @param targetKB The target size in kilobytes (KB) that the Bitmap should be reduced to.
 * @return A scaled-down Bitmap that meets the size allocation requirement.
 */
@Suppress("MagicNumber")
fun Bitmap.scaleUntil(targetKB: Int): Bitmap {
    if (allocationKilobyte() <= targetKB) {
        return this
    }

    // 1.5 is used to gradually scale down while minimizing distortion
    val scaleRatio = 1.5
    val width = width.div(scaleRatio).toInt()
    val height = height.div(scaleRatio).toInt()

    val scaledBitmap = scale(width, height)
    return scaledBitmap.scaleUntil(targetKB)
}

/**
 * Rotates and/or flips a [Bitmap] according to an EXIF orientation constant.
 *
 * Needed because loading bitmaps directly may ignore EXIF metadata with some devices,
 * resulting in incorrectly displayed images.
 *
 * This function uses a [Matrix] transformation to adjust the image so that it
 * appears upright when displayed. It supports all standard EXIF orientations,
 * including mirrored and rotated cases.
 *
 * The original bitmap will be recycled if a new one is successfully created.
 * If the device runs out of memory during the transformation, the original bitmap
 * is returned unchanged.
 *
 * @receiver The [Bitmap] to rotate or flip. Can be `null`.
 * @param orientation One of the [ExifInterface] orientation constants, such as
 * [ExifInterface.ORIENTATION_ROTATE_90] or [ExifInterface.ORIENTATION_FLIP_HORIZONTAL].
 * @return The correctly oriented [Bitmap], or `null` if the receiver was `null`.
 *
 * @see ExifInterface
 * @see Matrix
 */
@Suppress("MagicNumber", "ReturnCount")
fun Bitmap?.rotateBitmapViaExif(orientation: Int): Bitmap? {
    if (this == null) {
        return null
    }

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_NORMAL -> return this
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return this
    }

    return try {
        val rotated = Bitmap.createBitmap(
            this,
            0,
            0,
            this.width,
            this.height,
            matrix,
            true
        )

        // release original if a new one was created
        if (rotated != this) {
            this.recycle()
        }

        rotated
    } catch (_: OutOfMemoryError) {
        Log_OC.e("BitmapExtension", "rotating bitmap, out of memory exception")
        this
    }
}
