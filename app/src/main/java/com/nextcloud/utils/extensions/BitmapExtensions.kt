/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.graphics.Bitmap

@Suppress("MagicNumber")
fun Bitmap.scaleUntilLessThanEquals512KB(): Bitmap {
    val byteCountInKB = (allocationByteCount / 1024)
    val targetByteCountInKB = 512
    if (byteCountInKB <= targetByteCountInKB) {
        return this
    }

    val newWidth = width / 2
    val newHeight = height / 2
    val scaledBitmap = Bitmap.createScaledBitmap(this, newWidth, newHeight, true)

    return scaledBitmap.scaleUntilLessThanEquals512KB()
}
