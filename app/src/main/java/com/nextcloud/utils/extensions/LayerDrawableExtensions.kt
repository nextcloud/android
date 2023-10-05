package com.nextcloud.utils.extensions

import android.graphics.drawable.LayerDrawable

fun LayerDrawable.setLayerSizeWithInsetTop(index: Int, size: Int, margin: Int) {
    this.apply {
        setLayerSize(index, size, size)
        setLayerInsetTop(index, margin)
    }
}
