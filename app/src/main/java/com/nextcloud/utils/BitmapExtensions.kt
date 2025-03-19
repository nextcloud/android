/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.graphics.Bitmap
import androidx.core.graphics.scale

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
