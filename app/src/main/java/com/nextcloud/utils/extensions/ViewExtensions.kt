/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider

fun View.disableIf(condition: Boolean) {
    isEnabled = condition
    setBackgroundColor(if (condition) Color.TRANSPARENT else Color.GRAY)
}

fun View.setVisibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

fun createRoundedOutline(context: Context, cornerRadiusValue: Float): ViewOutlineProvider {
    return object : ViewOutlineProvider() {
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
}
