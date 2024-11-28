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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.owncloud.android.R

fun AppCompatActivity.isDialogFragmentReady(fragment: Fragment): Boolean = isActive() && !fragment.isStateSaved

fun AppCompatActivity.isActive(): Boolean = !isFinishing && !isDestroyed

fun AppCompatActivity.fragments(): List<Fragment> = supportFragmentManager.fragments

fun AppCompatActivity.lastFragment(): Fragment = fragments().last()


fun Window?.addStatusBarPadding() {
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
}
