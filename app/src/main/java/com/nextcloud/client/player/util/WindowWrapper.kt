/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.util

import android.os.Build
import android.view.Window
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private const val LUMINANCE_THRESHOLD = 0.5

class WindowWrapper(private val window: Window) {
    private val context = window.context
    private val insetsController = WindowCompat.getInsetsController(window, window.decorView)

    fun showSystemBars() {
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    fun hideSystemBars() {
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun setupStatusBar(@ColorRes backgroundColorRes: Int, contrastEnforced: Boolean) {
        val backgroundColor = ContextCompat.getColor(context, backgroundColorRes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(contrastEnforced)
        }
        insetsController.isAppearanceLightStatusBars = isLightColor(backgroundColor)
    }

    fun setupNavigationBar(@ColorRes backgroundColorRes: Int, contrastEnforced: Boolean) {
        val backgroundColor = ContextCompat.getColor(context, backgroundColorRes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(contrastEnforced)
        }
        window.navigationBarColor = backgroundColor
        insetsController.isAppearanceLightNavigationBars = isLightColor(backgroundColor)
    }

    private fun isLightColor(@ColorInt color: Int): Boolean = ColorUtils.calculateLuminance(color) > LUMINANCE_THRESHOLD
}
