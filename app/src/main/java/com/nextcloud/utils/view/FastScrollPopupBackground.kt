/*
 *  Nextcloud Android Library is available under MIT license
 *
 *  @author Álvaro Brey Vilas
 *  Copyright (C) 2022 Álvaro Brey Vilas
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.nextcloud.utils.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.sqrt

/**
 * Copied over from [me.zhanghai.android.fastscroll.Md2PopupBackground] on 2022/06/15
 * and adapted for color changing
 */
class FastScrollPopupBackground(context: Context, @ColorInt color: Int) : Drawable() {

    private val mPaint: Paint = Paint()
    private val mPaddingStart: Int
    private val mPaddingEnd: Int
    private val mPath = Path()
    private val mTempMatrix = Matrix()

    init {
        mPaint.isAntiAlias = true
        mPaint.color = color
        mPaint.style = Paint.Style.FILL
        val resources = context.resources
        mPaddingStart =
            resources.getDimensionPixelOffset(me.zhanghai.android.fastscroll.R.dimen.afs_md2_popup_padding_start)
        mPaddingEnd =
            resources.getDimensionPixelOffset(me.zhanghai.android.fastscroll.R.dimen.afs_md2_popup_padding_end)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(mPath, mPaint)
    }

    override fun setAlpha(alpha: Int) {
        // noop
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // noop
    }

    override fun isAutoMirrored(): Boolean {
        return true
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    private fun shouldMirrorPath(): Boolean {
        return DrawableCompat.getLayoutDirection(this) == View.LAYOUT_DIRECTION_RTL
    }

    override fun onLayoutDirectionChanged(layoutDirection: Int): Boolean {
        updatePath()
        return true
    }

    override fun onBoundsChange(bounds: Rect) {
        updatePath()
    }

    @Suppress("MagicNumber")
    private fun updatePath() {
        mPath.reset()
        val bounds = bounds
        var width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val r = height / 2
        val sqrt2 = sqrt(2.0).toFloat()
        // Ensure we are convex.
        width = (r + sqrt2 * r).coerceAtLeast(width)
        pathArcTo(mPath, r, r, r, startAngle = 90f, sweepAngle = 180f)
        val o1X = width - sqrt2 * r
        pathArcTo(mPath, o1X, r, r, startAngle = -90f, sweepAngle = 45f)
        val r2 = r / 5
        val o2X = width - sqrt2 * r2
        pathArcTo(mPath, o2X, r, r2, startAngle = -45f, sweepAngle = 90f)
        pathArcTo(mPath, o1X, r, r, startAngle = 45f, sweepAngle = 45f)
        mPath.close()
        if (shouldMirrorPath()) {
            mTempMatrix.setScale(-1f, 1f, width / 2, 0f)
        } else {
            mTempMatrix.reset()
        }
        mTempMatrix.postTranslate(bounds.left.toFloat(), bounds.top.toFloat())
        mPath.transform(mTempMatrix)
    }

    override fun getPadding(padding: Rect): Boolean {
        if (shouldMirrorPath()) {
            padding[mPaddingEnd, 0, mPaddingStart] = 0
        } else {
            padding[mPaddingStart, 0, mPaddingEnd] = 0
        }
        return true
    }

    override fun getOutline(outline: Outline) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !mPath.isConvex) {
            // The outline path must be convex before Q, but we may run into floating point error
            // caused by calculation involving sqrt(2) or OEM implementation difference, so in this
            // case we just omit the shadow instead of crashing.
            super.getOutline(outline)
            return
        }
        outline.setConvexPath(mPath)
    }

    companion object {
        @Suppress("LongParameterList")
        private fun pathArcTo(
            path: Path,
            centerX: Float,
            centerY: Float,
            radius: Float,
            startAngle: Float,
            sweepAngle: Float
        ) {
            path.arcTo(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                startAngle,
                sweepAngle,
                false
            )
        }
    }
}
