package com.owncloud.android.utils

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity

class DrawableUtil {

    fun addDrawableAsOverlay(backgroundDrawable: Drawable, overlayDrawable: Drawable, topMargin: Int = 6): LayerDrawable {
        val layerDrawable = LayerDrawable(arrayOf(backgroundDrawable, overlayDrawable))
        layerDrawable.setLayerGravity(1, Gravity.CENTER)
        layerDrawable.setLayerInsetTop(1, topMargin)
        return layerDrawable
    }
}