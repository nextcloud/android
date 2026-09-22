/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.view.View
import android.view.Window
import androidx.appcompat.app.ActionBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun ActionBar.toggle(show: Boolean) {
    if (show) {
        show()
    } else {
        hide()
    }
}

fun Window.showSystemBar(show: Boolean, view: View) {
    WindowCompat.getInsetsController(this, view).run {
        val types = WindowInsetsCompat.Type.systemBars()
        if (show) {
            show(types)
        } else {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(types)
        }
    }
}

fun Window.showNavigationBar(show: Boolean, view: View) {
    WindowCompat.getInsetsController(this, view).run {
        val types = WindowInsetsCompat.Type.navigationBars()
        if (show) {
            show(types)
        } else {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(types)
        }
    }
}
