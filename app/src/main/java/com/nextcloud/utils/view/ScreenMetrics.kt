/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.view

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import com.owncloud.android.MainApp

object ScreenMetrics {

    @JvmStatic
    @JvmOverloads
    fun size(context: Context? = null): Point {
        val displayMetrics = (context ?: MainApp.getAppContext()).displayMetrics
        return Point(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    @JvmStatic
    fun dpToPx(dp: Float, context: Context): Int = (dp * (context.densityDpi / DisplayMetrics.DENSITY_DEFAULT)).toInt()

    @JvmStatic
    fun pxToDp(px: Int, context: Context): Float = px * (DisplayMetrics.DENSITY_DEFAULT / context.densityDpi)

    private val Context.displayMetrics: DisplayMetrics
        get() = resources.displayMetrics

    private val Context.densityDpi: Float
        get() = displayMetrics.densityDpi.toFloat()
}
