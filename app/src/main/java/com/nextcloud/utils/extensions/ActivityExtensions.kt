/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.app.Activity
import android.os.Build
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

fun AppCompatActivity.isDialogFragmentReady(fragment: Fragment): Boolean = isActive() && !fragment.isStateSaved

fun AppCompatActivity.isActive(): Boolean = !isFinishing && !isDestroyed

fun AppCompatActivity.fragments(): List<Fragment> = supportFragmentManager.fragments

fun AppCompatActivity.lastFragment(): Fragment = fragments().last()

fun Activity.hasNavigationBar(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val insets = window.decorView.rootWindowInsets
        insets?.isVisible(WindowInsets.Type.navigationBars()) == true
    } else {
        val insets = ViewCompat.getRootWindowInsets(window.decorView)
        insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) == true
    }
}

fun Activity.navBarHeight(windowInsetsCompat: WindowInsetsCompat): Int {
    val typeMask = WindowInsetsCompat.Type.navigationBars()
    val insets = windowInsetsCompat.getInsets(typeMask)

    return if (insets.bottom != 0 && hasNavigationBar()) {
        insets.bottom
    } else {
        0
    }
}
