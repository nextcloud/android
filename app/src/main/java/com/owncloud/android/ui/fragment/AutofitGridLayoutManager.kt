/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A [GridLayoutManager] that works out its own column count from the width it is actually given.
 *
 * The width is read during layout rather than from the display metrics, because the metrics are
 * not a reliable stand in: they describe the display rather than the space this list occupies,
 * they are not yet meaningful while the view is being created, and they can still describe the
 * previous orientation while a rotation is being delivered.
 *
 * @param columnWidthProvider target width of a single column in pixels, read on every layout so
 * that a changed preference is picked up without recreating the layout manager.
 */
class AutofitGridLayoutManager(context: Context, private val columnWidthProvider: () -> Int) :
    GridLayoutManager(context, 1) {

    private var lastWidth = 0
    private var lastColumnWidth = 0

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        updateSpanCount()
        super.onLayoutChildren(recycler, state)
    }

    private fun updateSpanCount() {
        val columnWidth = columnWidthProvider()
        if (width <= 0 || columnWidth <= 0) {
            return
        }

        if (width == lastWidth && columnWidth == lastColumnWidth) {
            return
        }

        lastWidth = width
        lastColumnWidth = columnWidth
        spanCount = max(MIN_COLUMN_COUNT, (width.toFloat() / columnWidth).roundToInt())
    }

    companion object {
        private const val MIN_COLUMN_COUNT = 2
    }
}
