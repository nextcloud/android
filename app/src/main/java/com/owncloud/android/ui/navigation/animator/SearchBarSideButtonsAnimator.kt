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
    private var progress = VISIBLE_PROGRESS

    fun hide() = animateTo(HIDDEN_PROGRESS)

    fun show() = animateTo(VISIBLE_PROGRESS)

    private fun animateTo(target: Float) {
        animator?.cancel()
        applyProgress(progress)
        setButtonsVisible(true)

        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = durationMillis
            interpolator = this@SearchBarSideButtonsAnimator.interpolator
            addUpdateListener {
                progress = it.animatedValue as Float
                applyProgress(progress)
            }
            doOnEnd { if (progress == HIDDEN_PROGRESS) settleHidden() }
            start()
        }
    }

    private fun applyProgress(fraction: Float) {
        val leadingCurrentWidth = lerp(leadingWidth, 0, fraction)
        leadingButton.updateLayoutParams<MarginLayoutParams> {
            width = leadingCurrentWidth
            marginStart = lerp(leadingIdleMarginStart, expandedMargin, fraction)
            marginEnd = lerp(leadingIdleMarginEnd, 0, fraction)
        }
        leadingButton.scrollX = -(leadingWidth - leadingCurrentWidth) / 2

        val trailingCurrentWidth = lerp(trailingWidth, 0, fraction)
        trailingButton.updateLayoutParams<MarginLayoutParams> {
            width = trailingCurrentWidth
            marginEnd = lerp(trailingIdleMarginEnd, expandedMargin, fraction)
        }
        trailingButton.scrollX = (trailingWidth - trailingCurrentWidth) / 2

        searchCard.updateLayoutParams<MarginLayoutParams> {
            marginStart = cardIdleMarginStart
            marginEnd = lerp(cardIdleMarginEnd, 0, fraction)
        }

        val alpha = VISIBLE_ALPHA - fraction
        leadingButton.alpha = alpha
        trailingButton.alpha = alpha
    }

    private fun settleHidden() {
        applyProgress(VISIBLE_PROGRESS)
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

    private fun lerp(start: Int, end: Int, fraction: Float): Int = (start + (end - start) * fraction).roundToInt()

    private companion object {
        const val VISIBLE_PROGRESS = 0f
        const val HIDDEN_PROGRESS = 1f
        const val VISIBLE_ALPHA = 1f
    }
}
