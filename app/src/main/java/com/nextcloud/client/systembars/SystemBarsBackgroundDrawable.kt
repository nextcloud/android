/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.systembars

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SystemBarsBackgroundDrawable(
    private val decorView: View,
    @ColorInt statusBarColor: Int,
    @ColorInt navigationBarColor: Int
) : Drawable() {

    private val statusBarPaint = Paint().apply { color = statusBarColor }
    private val navigationBarPaint = Paint().apply { color = navigationBarColor }

    fun setColors(@ColorInt statusBarColor: Int, @ColorInt navigationBarColor: Int) {
        if (statusBarPaint.color == statusBarColor && navigationBarPaint.color == navigationBarColor) {
            return
        }
        statusBarPaint.color = statusBarColor
        navigationBarPaint.color = navigationBarColor
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val insets = ViewCompat.getRootWindowInsets(decorView) ?: return
        drawStatusBar(canvas, insets.getInsets(WindowInsetsCompat.Type.statusBars()))
        drawNavigationBar(canvas, insets.getInsets(WindowInsetsCompat.Type.navigationBars()))
    }

    private fun drawStatusBar(canvas: Canvas, insets: Insets) {
        if (insets.top == 0) {
            return
        }
        canvas.drawRect(left, top, right, top + insets.top, statusBarPaint)
    }

    private fun drawNavigationBar(canvas: Canvas, insets: Insets) {
        if (insets.bottom > 0) {
            canvas.drawRect(left, bottom - insets.bottom, right, bottom, navigationBarPaint)
        }
        if (insets.left > 0) {
            canvas.drawRect(left, top, left + insets.left, bottom, navigationBarPaint)
        }
        if (insets.right > 0) {
            canvas.drawRect(right - insets.right, top, right, bottom, navigationBarPaint)
        }
    }

    private val left: Float get() = bounds.left.toFloat()
    private val top: Float get() = bounds.top.toFloat()
    private val right: Float get() = bounds.right.toFloat()
    private val bottom: Float get() = bounds.bottom.toFloat()

    override fun setAlpha(alpha: Int) {
        statusBarPaint.alpha = alpha
        navigationBarPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        statusBarPaint.colorFilter = colorFilter
        navigationBarPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
