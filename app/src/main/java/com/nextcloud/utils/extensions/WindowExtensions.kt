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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding

fun ActionBar.toggle(show: Boolean) {
    if (show) {
        show()
    } else {
        hide()
    }
}

fun Window.extendBarsBehindSystemBars(appBar: View, bottomBar: View?) {
    ViewCompat.setOnApplyWindowInsetsListener(decorView) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val isBottomBarBehindNavigationBar = bottomBar?.isVisible == true && ime.bottom == 0
        val bottomInset = if (isBottomBarBehindNavigationBar) 0 else maxOf(systemBars.bottom, ime.bottom)

        view.updatePadding(left = systemBars.left, top = 0, right = systemBars.right, bottom = bottomInset)
        appBar.updatePadding(top = systemBars.top)

        insets.inset(systemBars.left, 0, systemBars.right, bottomInset)
    }
    ViewCompat.requestApplyInsets(decorView)
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
