/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package thirdparties.fresco

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.style.ReplacementSpan
import androidx.annotation.IntDef

/**
 * A better implementation of image spans that also supports centering images against the text.
 *
 * In order to migrate from ImageSpan, replace `new ImageSpan(drawable, alignment)` with
 * `new BetterImageSpan(drawable, BetterImageSpan.normalizeAlignment(alignment))`.
 *
 * There are 2 main differences between BetterImageSpan and ImageSpan:
 * 1. Pass in ALIGN_CENTER to center images against the text.
 * 2. ALIGN_BOTTOM no longer unnecessarily increases the size of the text:
 * DynamicDrawableSpan (ImageSpan's parent) adjusts sizes as if alignment was ALIGN_BASELINE
 * which can lead to unnecessary whitespace.
 */
open class BetterImageSpan @JvmOverloads constructor(
    val drawable: Drawable,
    @param:BetterImageSpanAlignment private val mAlignment: Int = ALIGN_BASELINE
) : ReplacementSpan() {
    @IntDef(*[ALIGN_BASELINE, ALIGN_BOTTOM, ALIGN_CENTER])
    @Retention(AnnotationRetention.SOURCE)
    annotation class BetterImageSpanAlignment

    private var mWidth = 0
    private var mHeight = 0
    private var mBounds: Rect? = null
    private val mFontMetricsInt = Paint.FontMetricsInt()

    init {
        updateBounds()
    }

    /**
     * Returns the width of the image span and increases the height if font metrics are available.
     */
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fontMetrics: Paint.FontMetricsInt?
    ): Int {
        updateBounds()
        if (fontMetrics == null) {
            return mWidth
        }
        val offsetAbove = getOffsetAboveBaseline(fontMetrics)
        val offsetBelow = mHeight + offsetAbove
        if (offsetAbove < fontMetrics.ascent) {
            fontMetrics.ascent = offsetAbove
        }
        if (offsetAbove < fontMetrics.top) {
            fontMetrics.top = offsetAbove
        }
        if (offsetBelow > fontMetrics.descent) {
            fontMetrics.descent = offsetBelow
        }
        if (offsetBelow > fontMetrics.bottom) {
            fontMetrics.bottom = offsetBelow
        }
        return mWidth
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        paint.getFontMetricsInt(mFontMetricsInt)
        val iconTop = y + getOffsetAboveBaseline(mFontMetricsInt)
        canvas.translate(x, iconTop.toFloat())
        drawable.draw(canvas)
        canvas.translate(-x, -iconTop.toFloat())
    }

    private fun updateBounds() {
        mBounds = drawable.bounds
        mWidth = mBounds!!.width()
        mHeight = mBounds!!.height()
    }

    private fun getOffsetAboveBaseline(fm: Paint.FontMetricsInt): Int {
        return when (mAlignment) {
            ALIGN_BOTTOM -> fm.descent - mHeight
            ALIGN_CENTER -> {
                val textHeight = fm.descent - fm.ascent
                val offset = (textHeight - mHeight) / 2
                fm.ascent + offset
            }

            ALIGN_BASELINE -> -mHeight
            else -> -mHeight
        }
    }

    companion object {
        const val ALIGN_BOTTOM = 0
        const val ALIGN_BASELINE = 1
        const val ALIGN_CENTER = 2
    }
}
