/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

@file:JvmName("ScreenUtils")

package com.nextcloud.client.player.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.annotation.RequiresApi

fun Context.getDisplayWidth(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    getWindowMetrics().bounds.width()
} else {
    getDisplayMetrics().widthPixels
}

fun Context.getDisplayHeight(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    getWindowMetrics().bounds.height()
} else {
    getDisplayMetrics().heightPixels
}

@RequiresApi(Build.VERSION_CODES.R)
fun Context.getWindowMetrics(): WindowMetrics {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return windowManager.currentWindowMetrics
}

@Suppress("DEPRECATION")
fun Context.getDisplayMetrics(): DisplayMetrics {
    val displayMetrics = DisplayMetrics()
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getRealMetrics(displayMetrics)
    return displayMetrics
}
