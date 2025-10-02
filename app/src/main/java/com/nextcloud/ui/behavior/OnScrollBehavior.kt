/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.behavior.HideViewOnScrollBehavior

class OnScrollBehavior<V : View> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    HideViewOnScrollBehavior<V>(context, attrs) {

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (dyConsumed > 0) {
            slideOut(child)
        } else if (dyConsumed < 0 || dyUnconsumed < 0) {
            slideIn(child)
        }
    }
}
