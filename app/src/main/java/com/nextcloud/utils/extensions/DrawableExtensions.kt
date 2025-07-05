/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import androidx.core.graphics.createBitmap

fun PictureDrawable.toBitmap(): Bitmap {
    val bitmap = createBitmap(picture.getWidth(), picture.getHeight())
    val canvas = Canvas(bitmap)
    picture.draw(canvas)
    return bitmap
}
