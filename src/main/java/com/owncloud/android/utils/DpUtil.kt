package com.owncloud.android.utils

import android.content.res.Resources
import android.util.TypedValue

object DpUtil {

    fun sp2px(spVal: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            spVal, Resources.getSystem().displayMetrics
        )
    }

    fun dp2px(dpVal: Float): Int {
        return (TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpVal, Resources.getSystem().displayMetrics
        ) + 0.5).toInt()
    }
}
