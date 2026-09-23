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
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.appcompat.app.ActionBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.nextcloud.ui.behavior.OnScrollBehavior
import com.owncloud.android.lib.common.utils.Log_OC

fun View.addNavigationBarInsetToBottomMargin() {
    val baseBottomMargin = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: return
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = baseBottomMargin + insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        }
        insets
    }
}

fun View.fitBetweenActionBarAndNavigationBar(visibleActionBar: ActionBar?) {
    val container = parent as? View ?: return
    val navigationBarHeight = visibleActionBar?.let {
        ViewCompat.getRootWindowInsets(this)
            ?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            ?.bottom
    } ?: 0
    val containerTop = IntArray(2).also(container::getLocationInWindow)[1]
    val containerBottom = containerTop + container.height
    val top = ((visibleActionBar?.height ?: 0) - containerTop).coerceAtLeast(0)
    val bottom = (navigationBarHeight - (rootView.height - containerBottom)).coerceAtLeast(0)
    val params = layoutParams as? ViewGroup.MarginLayoutParams

    if (params != null && (params.topMargin != top || params.bottomMargin != bottom)) {
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = top
            bottomMargin = bottom
        }
    }
}

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

fun View?.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
    if (this == null) {
        return
    }

    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val param = layoutParams as ViewGroup.MarginLayoutParams
        param.setMargins(left, top, right, bottom)
        requestLayout()
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

@Suppress("UNCHECKED_CAST", "ReturnCount", "TooGenericExceptionCaught")
fun <T : View?> T.slideHideBottomBehavior(visible: Boolean) {
    this ?: return
    val params = layoutParams as? CoordinatorLayout.LayoutParams ?: return
    val behavior = params.behavior as? OnScrollBehavior<T> ?: return
    post {
        try {
            if (visible) {
                behavior.slideIn(this)
            } else {
                behavior.slideOut(this)
            }
        } catch (e: Exception) {
            Log_OC.e("slideHideBottomBehavior", e.message)
        }
    }
}
