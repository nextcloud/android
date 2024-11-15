/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.graphics.drawable.DrawableCompat

object DrawableUtil {

    fun changeColor(source: Drawable, color: Int): Drawable {
        val drawable = DrawableCompat.wrap(source)
        DrawableCompat.setTint(drawable, color)
        return drawable
    }

    fun addDrawableAsOverlay(backgroundDrawable: Drawable, overlayDrawable: Drawable): LayerDrawable {
        val overlaySizeFraction = 0.1f
        val baseWidth = backgroundDrawable.intrinsicWidth
        val baseHeight = backgroundDrawable.intrinsicHeight
        val overlayWidth = (baseWidth * overlaySizeFraction).toInt()
        val overlayHeight = (baseHeight * overlaySizeFraction).toInt()

        val insetLeft = (baseWidth - overlayWidth) / 2
        val insetTop = (baseHeight - overlayHeight) / 2

        val insetOverlay = InsetDrawable(overlayDrawable, insetLeft, overlayHeight + insetTop, insetLeft, insetTop)
        return LayerDrawable(arrayOf(backgroundDrawable, insetOverlay))
    }
}
