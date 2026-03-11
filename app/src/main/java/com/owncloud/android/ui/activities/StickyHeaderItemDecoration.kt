/*
 * Nextcloud Android client application
 *
 * Copyright (C) 2019 Sevastyan Savanyuk
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.owncloud.android.ui.adapter.StickyHeaderAdapter
import androidx.core.graphics.withTranslation
import androidx.core.view.size

@Suppress("ReturnCount")
class StickyHeaderItemDecoration(private val adapter: StickyHeaderAdapter) : ItemDecoration() {
    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)

        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)

        if (topChildPosition == RecyclerView.NO_POSITION) {
            return
        }
        val currentHeader = getHeaderViewForItem(topChildPosition, parent)
        fixLayoutSize(parent, currentHeader)
        val contactPoint = currentHeader.bottom
        val childInContact = getChildInContact(parent, contactPoint) ?: return

        if (adapter.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(canvas, currentHeader, childInContact)
            return
        }

        drawHeader(canvas, currentHeader)
    }

    private fun drawHeader(canvas: Canvas, header: View) {
        canvas.withTranslation(0f, 0f) {
            header.draw(this)
        }
    }

    private fun moveHeader(canvas: Canvas, currentHeader: View, nextHeader: View) {
        canvas.withTranslation(0f, (nextHeader.top - currentHeader.height).toFloat()) {
            currentHeader.draw(this)
        }
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        var childInContact: View? = null
        for (i in 0..<parent.size) {
            val currentChild = parent.getChildAt(i)
            if (currentChild.bottom > contactPoint && currentChild.top <= contactPoint) {
                childInContact = currentChild
                break
            }
        }
        return childInContact
    }

    private fun getHeaderViewForItem(itemPosition: Int, parent: RecyclerView): View {
        val headerPosition = adapter.getHeaderPositionForItem(itemPosition)
        val layoutId = adapter.getHeaderLayout(itemPosition)
        val header = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        adapter.bindHeaderData(header, headerPosition)
        return header
    }

    private fun fixLayoutSize(parent: ViewGroup, view: View) {
        // Specs for parent (RecyclerView)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        // Specs for children (headers)
        val childWidthSpec = ViewGroup.getChildMeasureSpec(
            widthSpec,
            parent.getPaddingLeft() + parent.getPaddingRight(),
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
