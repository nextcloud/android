package com.owncloud.android.utils

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import androidx.core.graphics.drawable.DrawableCompat

class DrawableUtil {

    fun changeColor(source: Drawable, color: Int): Drawable {
        val drawable = DrawableCompat.wrap(source)
        DrawableCompat.setTint(drawable, color)
        return drawable
    }

    fun addDrawableAsOverlay(backgroundDrawable: Drawable, overlayDrawable: Drawable, topMargin: Int = 3): LayerDrawable {
        val layerDrawable = LayerDrawable(arrayOf(backgroundDrawable, overlayDrawable))
        layerDrawable.setLayerGravity(1, Gravity.CENTER)
        layerDrawable.setLayerInsetTop(1, topMargin)
        return layerDrawable
    }
}