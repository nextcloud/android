/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

fun Context.getDisplayWidth(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    windowManager.currentWindowMetrics.bounds.width()
} else {
    getDisplayMetrics().widthPixels
}

fun Context.getDisplayHeight(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    windowManager.currentWindowMetrics.bounds.height()
} else {
    getDisplayMetrics().heightPixels
}

private val Context.windowManager: WindowManager
    get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

@Suppress("DEPRECATION")
private fun Context.getDisplayMetrics(): DisplayMetrics {
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getRealMetrics(displayMetrics)
    return displayMetrics
}
