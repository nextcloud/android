/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

fun AppCompatActivity.isDialogFragmentReady(fragment: Fragment): Boolean = isActive() && !fragment.isStateSaved

fun AppCompatActivity.isActive(): Boolean = !isFinishing && !isDestroyed

fun AppCompatActivity.fragments(): List<Fragment> = supportFragmentManager.fragments

fun AppCompatActivity.lastFragment(): Fragment = fragments().last()

// TODO move it to the WindowExtensions
fun Window?.setNavBarColor(@ColorInt color: Int) {
    if (this == null) {
        return
    }

    ViewCompat.setOnApplyWindowInsetsListener(decorView) { v: View, insets: WindowInsetsCompat ->
        val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        v.setPadding(
            v.paddingLeft,
            v.top,
            v.paddingRight,
            navigationBarInsets.bottom,
        )
        insets
    }

    decorView.setBackgroundColor(color)
}
