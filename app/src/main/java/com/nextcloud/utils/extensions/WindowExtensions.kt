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
import androidx.core.view.updatePadding

fun Window?.setNoLimitLayout() {
    val flag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    this?.setFlags(flag, flag)
}

fun Window?.addSystemBarPaddings() {
    if (this == null) {
        return
    }

    ViewCompat.setOnApplyWindowInsetsListener(decorView) { v: View, insets: WindowInsetsCompat ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.updatePadding(
            left = bars.left,
            top = bars.top,
            right = bars.right,
            bottom = bars.bottom
        )

        WindowInsetsCompat.CONSUMED
    }
}
