/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun Window?.makeStatusBarTransparent() {
    val flag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    this?.setFlags(flag, flag)
}

fun Window?.addStatusBarPadding() {
    if (this == null) {
        return
    }

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
}
