/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun Window?.makeStatusBarTransparent() {
    val flag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    this?.setFlags(flag, flag)
}

fun Window?.changeSystemBar(isDarkModeEnabled: Boolean) {
    if (this == null) {
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val appearanceLightStatusBars = if (isDarkModeEnabled) {
            0
        } else {
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        }

        insetsController?.setSystemBarsAppearance(
            appearanceLightStatusBars,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = if (isDarkModeEnabled) {
            0
        } else {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}

fun Window?.changeStatusBarColor(@ColorInt color: Int) {
    if (this == null) {
        return
    }

    val decorView: View = decorView

    ViewCompat.setOnApplyWindowInsetsListener(decorView) { v: View, insets: WindowInsetsCompat ->
        val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        v.setPadding(
            v.paddingLeft,
            statusBarInsets.top,
            v.paddingRight,
            v.paddingBottom
        )
        insets
    }

    decorView.setBackgroundColor(color)
}
