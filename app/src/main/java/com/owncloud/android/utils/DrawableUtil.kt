/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
