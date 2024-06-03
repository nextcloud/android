/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.graphics.drawable.DrawableCompat

class DrawableUtil {

    fun changeColor(source: Drawable, color: Int): Drawable {
        val drawable = DrawableCompat.wrap(source)
        DrawableCompat.setTint(drawable, color)
        return drawable
    }

    fun addDrawableAsOverlay(backgroundDrawable: Drawable, overlayDrawable: Drawable): LayerDrawable {
        val overlayBounds = Rect()
        val overlayIconSize = backgroundDrawable.intrinsicWidth / 2
        val topMargin = overlayIconSize.div(2)
        overlayBounds.set(overlayIconSize, overlayIconSize + topMargin, overlayIconSize, overlayIconSize)

        val layerDrawable = LayerDrawable(arrayOf(backgroundDrawable, overlayDrawable))
        layerDrawable.setLayerInset(1, overlayBounds.left, overlayBounds.top, overlayBounds.right, overlayBounds.bottom)
        return layerDrawable
    }
}
