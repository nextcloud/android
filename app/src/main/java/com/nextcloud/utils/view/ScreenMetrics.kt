/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.view

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics

object ScreenMetrics {

    @JvmStatic
    fun size(activity: Activity?): Point = Point().also { size ->
        @Suppress("DEPRECATION")
        activity?.windowManager?.defaultDisplay?.getSize(size)
    }

    @JvmStatic
    fun dpToPx(dp: Float, context: Context): Int = (dp * (context.densityDpi / DisplayMetrics.DENSITY_DEFAULT)).toInt()

    @JvmStatic
    fun pxToDp(px: Int, context: Context): Float = px * (DisplayMetrics.DENSITY_DEFAULT / context.densityDpi)

    private val Context.densityDpi: Float
        get() = resources.displayMetrics.densityDpi.toFloat()
}
