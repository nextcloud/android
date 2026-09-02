/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.util

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import com.owncloud.android.R

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

    fun setupStatusBar(@ColorRes backgroundColorRes: Int) {
        val backgroundColor = ContextCompat.getColor(context, backgroundColorRes)
        insetsController.isAppearanceLightStatusBars = isLightColor(backgroundColor)
        drawSystemBarBackground(
            R.id.player_status_bar_background,
            Gravity.TOP,
            backgroundColor,
            systemBarInsets(WindowInsetsCompat.Type.statusBars()).top
        )
    }

    fun setupNavigationBar(@ColorRes backgroundColorRes: Int) {
        val backgroundColor = ContextCompat.getColor(context, backgroundColorRes)
        insetsController.isAppearanceLightNavigationBars = isLightColor(backgroundColor)
        drawSystemBarBackground(
            R.id.player_navigation_bar_background,
            Gravity.BOTTOM,
            backgroundColor,
            systemBarInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        )
    }

    private fun systemBarInsets(typeMask: Int): Insets =
        ViewCompat.getRootWindowInsets(window.decorView)?.getInsets(typeMask) ?: Insets.NONE

    private fun drawSystemBarBackground(@IdRes id: Int, gravity: Int, @ColorInt color: Int, height: Int) {
        val content = window.findViewById<ViewGroup>(android.R.id.content) ?: return
        val background = content.findViewById<View>(id) ?: View(context).apply {
            this.id = id
            content.addView(this, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height, gravity))
        }

        background.setBackgroundColor(color)
        background.updateLayoutParams { this.height = height }
    }

    private fun isLightColor(@ColorInt color: Int): Boolean = ColorUtils.calculateLuminance(color) > LUMINANCE_THRESHOLD
}
