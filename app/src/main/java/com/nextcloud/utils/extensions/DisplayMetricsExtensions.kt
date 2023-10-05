package com.nextcloud.utils.extensions

import android.util.DisplayMetrics

fun DisplayMetrics.isLowDensityScreen(): Boolean {
    return this.density < 2
}
