/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale

object DrawableUtil {

    fun changeColor(source: Drawable, color: Int): Drawable {
        val drawable = DrawableCompat.wrap(source)
        DrawableCompat.setTint(drawable, color)
        return drawable
    }

    fun getResizedDrawable(context: Context, drawable: Drawable, pxSize: Int): Drawable {
        if (drawable is BitmapDrawable) {
            val originalBitmap = drawable.bitmap
            val scaledBitmap = originalBitmap.scale(pxSize, pxSize)
            return scaledBitmap.toDrawable(context.resources)
        }

        val bitmap = createBitmap(pxSize, pxSize)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, pxSize, pxSize)
        drawable.draw(canvas)

        return bitmap.toDrawable(context.resources)
    }

    fun addDrawableAsOverlay(backgroundDrawable: Drawable, overlayDrawable: Drawable): LayerDrawable {
        val containerDrawable = LayerDrawable(arrayOf(backgroundDrawable, overlayDrawable))

        val overlayWidth = overlayDrawable.intrinsicWidth
        val overlayHeight = overlayDrawable.intrinsicHeight
        val backgroundWidth = backgroundDrawable.intrinsicWidth
        val backgroundHeight = backgroundDrawable.intrinsicHeight

        val scaleFactor = 2f / maxOf(overlayWidth, overlayHeight)
        val scaledOverlayWidth = (overlayWidth * scaleFactor).toInt()
        val scaledOverlayHeight = (overlayHeight * scaleFactor).toInt()

        val left = (backgroundWidth - scaledOverlayWidth) / 2
        val top = (backgroundHeight - scaledOverlayHeight) / 2

        // Icons are centered on the folder icon. However, some icons take up more vertical space,
        // so adding a top margin to all icons helps center the overlay icon better.
        val topMargin = 2

        containerDrawable.setLayerInset(1, left, top + topMargin, left, top)
        (overlayDrawable as? BitmapDrawable)?.setBounds(0, 0, scaledOverlayWidth, scaledOverlayHeight)

        return containerDrawable
    }
}
