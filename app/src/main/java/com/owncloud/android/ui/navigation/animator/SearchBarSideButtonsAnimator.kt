/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation.animator

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AnimationUtils
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import com.owncloud.android.R
import kotlin.math.roundToInt

class SearchBarSideButtonsAnimator(homeToolbar: ViewGroup, private val durationMillis: Long) {

    private val searchCard: View = homeToolbar.findViewById(R.id.home_search_container)
    private val leadingButton: View = homeToolbar.findViewById(R.id.menu_button)
    private val trailingButton: View = homeToolbar.findViewById(R.id.switch_account_button)

    private val leadingWidth = leadingButton.layoutParams.width
    private val leadingIdleMarginStart = leadingButton.marginStart
    private val leadingIdleMarginEnd = leadingButton.marginEnd
    private val trailingWidth = trailingButton.layoutParams.width
    private val trailingIdleMarginEnd = trailingButton.marginEnd
    private val cardIdleMarginStart = searchCard.marginStart
    private val cardIdleMarginEnd = searchCard.marginEnd
    private val expandedMargin = homeToolbar.resources.getDimensionPixelSize(R.dimen.standard_margin)

    private val interpolator =
        AnimationUtils.loadInterpolator(homeToolbar.context, android.R.interpolator.fast_out_slow_in)

    private var animator: ValueAnimator? = null

    private var hiddenFraction = SHOWN

    fun hide() = animateTo(HIDDEN)

    fun show() = animateTo(SHOWN)

    // Animates hiddenFraction from its current value to target over durationMillis.
    // SHOWN (0f) = idle home bar, HIDDEN (1f) = hamburger and avatar completely gone (search open).
    // Example for hide(), avatar width 48 -> 0 (real timings follow the fast_out_slow_in easing):
    //
    // | time   | hiddenFraction | avatar width | avatar alpha |
    // |--------|----------------|--------------|--------------|
    // | 0 ms   | 0.0  (0%)      | 48           | 1.0          |
    // | 100 ms | 0.25 (25%)     | 36           | 0.75         |
    // | 200 ms | 0.5  (50%)     | 24           | 0.5          |
    // | 300 ms | 0.75 (75%)     | 12           | 0.25         |
    // | 400 ms | 1.0  (100%)    | 0            | 0.0          |
    //
    // show() runs the same table backwards (1f -> 0f), so everything returns to its idle size.
    private fun animateTo(target: Float) {
        animator?.cancel()
        applyHiddenFraction(hiddenFraction)
        setButtonsVisible(true)

        animator = ValueAnimator.ofFloat(hiddenFraction, target).apply {
            duration = durationMillis
            interpolator = this@SearchBarSideButtonsAnimator.interpolator
            addUpdateListener {
                // Called on every screen frame so that we can apply transition for each frame
                hiddenFraction = it.animatedValue as Float
                applyHiddenFraction(hiddenFraction)
            }
            doOnEnd { if (hiddenFraction == HIDDEN) settleHidden() }
            start()
        }
    }

    private fun applyHiddenFraction(fraction: Float) {
        val leadingCurrentWidth = interpolate(leadingWidth, 0, fraction)
        leadingButton.updateLayoutParams<MarginLayoutParams> {
            width = leadingCurrentWidth
            marginStart = interpolate(leadingIdleMarginStart, expandedMargin, fraction)
            marginEnd = interpolate(leadingIdleMarginEnd, 0, fraction)
        }
        leadingButton.scrollX = -(leadingWidth - leadingCurrentWidth) / 2

        val trailingCurrentWidth = interpolate(trailingWidth, 0, fraction)
        trailingButton.updateLayoutParams<MarginLayoutParams> {
            width = trailingCurrentWidth
            marginEnd = interpolate(trailingIdleMarginEnd, expandedMargin, fraction)
        }
        trailingButton.scrollX = (trailingWidth - trailingCurrentWidth) / 2

        searchCard.updateLayoutParams<MarginLayoutParams> {
            marginStart = cardIdleMarginStart
            marginEnd = interpolate(cardIdleMarginEnd, 0, fraction)
        }

        val alpha = VISIBLE_ALPHA - fraction
        leadingButton.alpha = alpha
        trailingButton.alpha = alpha
    }

    private fun settleHidden() {
        applyHiddenFraction(SHOWN)
        setButtonsVisible(false)

        searchCard.updateLayoutParams<MarginLayoutParams> {
            marginStart = expandedMargin
            marginEnd = expandedMargin
        }
    }

    private fun setButtonsVisible(visible: Boolean) {
        leadingButton.isVisible = visible
        trailingButton.isVisible = visible
    }

    // Value partway between start and end: fraction 0f gives start, 1f gives end, 0.25f is a quarter of the way,
    // e.g. interpolate(48, 0, 0.25f) = 48 + (0 - 48) * 0.25 = 36
    private fun interpolate(start: Int, end: Int, fraction: Float): Int =
        (start + (end - start) * fraction).roundToInt()

    private companion object {
        const val SHOWN = 0f
        const val HIDDEN = 1f
        const val VISIBLE_ALPHA = 1f
    }
}
