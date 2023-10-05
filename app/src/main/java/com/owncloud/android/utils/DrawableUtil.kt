package com.owncloud.android.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.core.graphics.drawable.DrawableCompat
import com.nextcloud.utils.extensions.isLowDensityScreen
import com.nextcloud.utils.extensions.setLayerSizeWithInsetTop

class DrawableUtil {

    fun changeColor(source: Drawable, color: Int): Drawable {
        val drawable = DrawableCompat.wrap(source)
        DrawableCompat.setTint(drawable, color)
        return drawable
    }

    fun addDrawableAsOverlay(context: Context, backgroundDrawable: Drawable, overlayDrawable: Drawable): LayerDrawable {
        val isLowDensityScreen = context.resources.displayMetrics.isLowDensityScreen()

        val overlayIconSize = if (isLowDensityScreen) { 12 } else { 24 }
        val overlayIconTopMargin = if (isLowDensityScreen) { 3 } else { 6 }

        val layerDrawable = LayerDrawable(arrayOf(backgroundDrawable, overlayDrawable))
        layerDrawable.setLayerSizeWithInsetTop(1, overlayIconSize, overlayIconTopMargin)
        layerDrawable.setLayerGravity(1, Gravity.CENTER)

        return layerDrawable
    }
}