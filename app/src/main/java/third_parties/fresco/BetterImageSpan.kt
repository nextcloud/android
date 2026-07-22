/*
 * SPDX-FileCopyrightText: 2015-present, Facebook, Inc. and its affiliates.
 * SPDX-License-Identifier: MIT
 */

package third_parties.fresco

import android.graphics.Canvas
import android.graphics.Paint
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
    @param:BetterImageSpanAlignment private val alignment: Int = ALIGN_BASELINE
) : ReplacementSpan() {
    @Suppress("Detekt.SpreadOperator")
    @IntDef(*[ALIGN_BASELINE, ALIGN_BOTTOM, ALIGN_CENTER])
    @Retention(AnnotationRetention.SOURCE)
    annotation class BetterImageSpanAlignment

    private var width = 0
    private var height = 0
    private val fontMetricsInt = Paint.FontMetricsInt()

    init {
        updateBounds()
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fontMetrics: Paint.FontMetricsInt?
    ): Int {
        updateBounds()
        if (fontMetrics == null) {
            return width
        }

        val offsetAbove = getOffsetAboveBaseline(fontMetrics)
        val offsetBelow = height + offsetAbove
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
        return width
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
        paint.getFontMetricsInt(fontMetricsInt)
        val iconTop = y + getOffsetAboveBaseline(fontMetricsInt)
        canvas.translate(x, iconTop.toFloat())
        drawable.draw(canvas)
        canvas.translate(-x, -iconTop.toFloat())
    }

    private fun updateBounds() {
        val bounds = drawable.bounds
        width = bounds.width()
        height = bounds.height()
    }

    private fun getOffsetAboveBaseline(fm: Paint.FontMetricsInt): Int = when (alignment) {
        ALIGN_BOTTOM -> fm.descent - height

        ALIGN_CENTER -> {
            val textHeight = fm.descent - fm.ascent
            val offset = (textHeight - height) / 2
            fm.ascent + offset
        }

        else -> -height
    }

    companion object {
        const val ALIGN_BOTTOM = 0
        const val ALIGN_BASELINE = 1
        const val ALIGN_CENTER = 2
    }
}
