/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.core.graphics.drawable.DrawableCompat
import com.ionos.annotation.IonosCustomization

object DrawableUtil {

    fun changeColor(source: Drawable, color: Int): Drawable {
        val drawable = DrawableCompat.wrap(source)
        DrawableCompat.setTint(drawable, color)
        return drawable
    }

    @IonosCustomization
    fun addDrawableAsOverlay(backgroundDrawable: Drawable, overlayDrawable: Drawable): LayerDrawable {
        return LayerDrawable(arrayOf(backgroundDrawable, overlayDrawable)).apply {
            setLayerSize(1, overlayDrawable.intrinsicWidth, overlayDrawable.intrinsicHeight)
            setLayerGravity(1, Gravity.CENTER)
        }
    }
}
