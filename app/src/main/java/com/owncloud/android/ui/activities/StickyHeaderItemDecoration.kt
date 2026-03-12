/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activities

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.withTranslation

@Suppress("ReturnCount")
class StickyHeaderItemDecoration(private val adapter: StickyHeaderAdapter) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)

        val topChild = parent.getChildAt(0) ?: return
        val position = parent.getChildAdapterPosition(topChild)
        if (position == RecyclerView.NO_POSITION) return

        val header = getHeaderViewForItem(position, parent)
        fixLayoutSize(parent, header)

        val contactPoint = header.bottom
        val childInContact = getChildInContact(parent, contactPoint) ?: return

        if (adapter.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(canvas, header, childInContact)
        } else {
            drawHeader(canvas, header)
        }
    }

    private fun drawHeader(canvas: Canvas, header: View) = canvas.withTranslation(0f, 0f) { header.draw(this) }

    private fun moveHeader(canvas: Canvas, currentHeader: View, nextHeader: View) =
        canvas.withTranslation(0f, (nextHeader.top - currentHeader.height).toFloat()) {
            currentHeader.draw(this)
        }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? = (0 until parent.childCount)
        .map { parent.getChildAt(it) }
        .firstOrNull { it.bottom > contactPoint && it.top <= contactPoint }

    private fun getHeaderViewForItem(position: Int, parent: RecyclerView): View {
        val headerPos = adapter.getHeaderPositionForItem(position)
        val layoutId = adapter.getHeaderLayout(position)

        return LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
            .apply { adapter.bindHeaderData(this, headerPos) }
    }

    private fun fixLayoutSize(parent: ViewGroup, view: View) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        val childWidthSpec = ViewGroup.getChildMeasureSpec(
            widthSpec,
            parent.paddingLeft + parent.paddingRight,
            view.layoutParams.width
        )

        val childHeightSpec = ViewGroup.getChildMeasureSpec(
            heightSpec,
            parent.paddingTop + parent.paddingBottom,
            view.layoutParams.height
        )

        view.measure(childWidthSpec, childHeightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }
}
