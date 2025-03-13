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
