/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Outline
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import com.google.android.material.behavior.HideViewOnScrollBehavior

fun View?.setVisibleIf(condition: Boolean) {
    if (this == null) return
    visibility = if (condition) View.VISIBLE else View.GONE
}

fun View?.setVisibilityWithAnimation(condition: Boolean, duration: Long = 200L) {
    this ?: return

    if (condition) {
        this.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(null)
        }
    } else {
        animate()
            .alpha(0f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
    }
}

fun View?.makeRounded(context: Context, cornerRadius: Float) {
    this?.let {
        it.apply {
            outlineProvider = createRoundedOutline(context, cornerRadius)
            clipToOutline = true
        }
    }
}

fun createRoundedOutline(context: Context, cornerRadiusValue: Float): ViewOutlineProvider =
    object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val left = 0
            val top = 0
            val right = view.width
            val bottom = view.height
            val cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                cornerRadiusValue,
                context.resources.displayMetrics
            ).toInt()

            outline.setRoundRect(left, top, right, bottom, cornerRadius.toFloat())
        }
    }

fun View?.slideHideBottomBehavior(visible: Boolean) {
    val view = this ?: return
    val params = view.layoutParams as? CoordinatorLayout.LayoutParams
    if (params?.behavior !is HideViewOnScrollBehavior<*>) return

    val behavior = HideViewOnScrollBehavior.from(view)
    view.doOnLayout {
        if (visible) behavior.slideIn(view) else behavior.slideOut(view)
    }
}

fun View.addPaddingForNavBar() {
    addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        val insets = ViewCompat.getRootWindowInsets(view)
            ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            ?: return@addOnLayoutChangeListener
        view.updatePadding(left = insets.left, right = insets.right, bottom = insets.bottom)
    }
}
