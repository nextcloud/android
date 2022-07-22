/*
 *  Nextcloud Android Library is available under MIT license
 *
 *  @author Álvaro Brey Vilas
 *  Copyright (C) 2022 Álvaro Brey Vilas
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.owncloud.android.ui.fragment.util

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.afollestad.sectionedrecyclerview.ItemCoord
import com.owncloud.android.datamodel.GalleryItems
import com.owncloud.android.ui.adapter.GalleryAdapter
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.fastscroll.Predicate
import kotlin.math.ceil
import kotlin.math.min

/**
 * Custom ViewHelper to get fast scroll working on gallery, which has a gridview and variable height (due to headers)
 *
 * Copied from me.zhanghai.android.fastscroll.RecyclerViewHelper and heavily modified for gallery structure
 */
class GalleryFastScrollViewHelper(
    private val mView: RecyclerView,
    private val mPopupTextProvider: PopupTextProvider?
) : FastScroller.ViewHelper {
    // used to calculate paddings
    private val mTempRect = Rect()

    private val layoutManager by lazy { mView.layoutManager as GridLayoutManager }

    // header is always 1st in the adapter
    private val headerHeight by lazy { getItemHeight(0) }

    // the 2nd element is always an item
    private val rowHeight by lazy { getItemHeight(1) }

    private val columnCount by lazy { layoutManager.spanCount }

    private fun getItemHeight(position: Int): Int {
        if (mView.childCount <= position) {
            return 0
        }
        val itemView = mView.getChildAt(position)
        mView.getDecoratedBoundsWithMargins(itemView, mTempRect)
        return mTempRect.height()
    }

    override fun addOnPreDrawListener(onPreDraw: Runnable) {
        mView.addItemDecoration(object : ItemDecoration() {
            override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                onPreDraw.run()
            }
        })
    }

    override fun addOnScrollChangedListener(onScrollChanged: Runnable) {
        mView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrollChanged.run()
            }
        })
    }

    override fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent?>) {
        mView.addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent): Boolean {
                return onTouchEvent.test(event)
            }

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                onTouchEvent.test(event)
            }
        })
    }

    override fun getScrollRange(): Int {
        val headerCount = getHeaderCount()
        val rowCount = getRowCount()

        if (headerCount == 0 || rowCount == 0) {
            return 0
        }
        val totalHeaderHeight = headerCount * headerHeight
        val totalRowHeight = rowCount * rowHeight
        return mView.paddingTop + totalHeaderHeight + totalRowHeight + mView.paddingBottom
    }

    private fun getHeaderCount(): Int {
        val adapter = mView.adapter as GalleryAdapter
        return adapter.sectionCount
    }

    private fun getRowCount(): Int {
        val adapter = mView.adapter as GalleryAdapter
        if (adapter.sectionCount == 0) return 0
        // in each section, the final row may contain less than the max of items
        return adapter.files.sumOf { itemCountToRowCount(it.rows.size) }
    }

    /**
     * Calculates current absolute offset depending on view state (first visible element)
     */
    override fun getScrollOffset(): Int {
        val firstItemPosition = getFirstItemAdapterPosition()
        if (firstItemPosition == RecyclerView.NO_POSITION) {
            return 0
        }

        val adapter = mView.adapter as GalleryAdapter
        val itemCoord: ItemCoord = adapter.getRelativePosition(firstItemPosition)
        val isHeader = itemCoord.relativePos() == -1

        val seenRowsInPreviousSections = adapter.files
            .subList(0, min(itemCoord.section(), adapter.files.size))
            .sumOf { itemCountToRowCount(it.rows.size) }
        val seenRowsInThisSection = if (isHeader) 0 else itemCountToRowCount(itemCoord.relativePos())
        val totalSeenRows = seenRowsInPreviousSections + seenRowsInThisSection

        val seenHeaders = when {
            isHeader -> itemCoord.section() // don't count the current section header
            else -> itemCoord.section() + 1
        }

        val firstItemTop = getFirstItemOffset()

        val totalRowOffset = totalSeenRows * rowHeight
        val totalHeaderOffset = seenHeaders * headerHeight
        return mView.paddingTop + totalHeaderOffset + totalRowOffset - firstItemTop
    }

    /**
     * Scrolls to an absolute offset
     */
    override fun scrollTo(offset: Int) {
        mView.stopScroll()
        val offsetTmp = offset - mView.paddingTop
        val (position, remainingOffset) = findPositionForOffset(offsetTmp)
        scrollToPositionWithOffset(position, -remainingOffset)
    }

    /**
     * Given an absolute offset, returns the closest position to that offset (without going over it),
     * and the remaining offset
     */
    private fun findPositionForOffset(offset: Int): Pair<Int, Int> {
        val adapter = mView.adapter as GalleryAdapter

        // find section
        val sectionStartOffsets = getSectionStartOffsets(adapter.files)
        val previousSections = sectionStartOffsets.filter { it <= offset }

        val section = previousSections.size - 1
        val sectionStartOffset = previousSections.last()

        // now calculate where to scroll within the section
        var remainingOffset = offset - sectionStartOffset
        val positionWithinSection: Int
        if (remainingOffset <= headerHeight) {
            // header position
            positionWithinSection = -1
        } else {
            // row position
            remainingOffset -= headerHeight
            val rowCount = remainingOffset / rowHeight
            if (rowCount > 0) {
                val rowStartIndex = rowCount * columnCount
                positionWithinSection = rowStartIndex

                remainingOffset -= rowCount * rowHeight
            } else {
                positionWithinSection = 0 // first item
            }
        }
        val absolutePosition = adapter.getAbsolutePosition(section, positionWithinSection)
        return Pair(absolutePosition, remainingOffset)
    }

    /**
     * Returns a list of the offset heights at which the section corresponding to that index starts
     */
    private fun getSectionStartOffsets(files: List<GalleryItems>): List<Int> {
        val sectionHeights =
            files.map { headerHeight + itemCountToRowCount(it.rows.size) * rowHeight }
        val sectionStartOffsets = sectionHeights.indices.map { i ->
            when (i) {
                0 -> 0
                else -> sectionHeights.subList(0, i).sum()
            }
        }
        return sectionStartOffsets
    }

    private fun itemCountToRowCount(itemsCount: Int): Int {
        return ceil(itemsCount.toDouble() / columnCount).toInt()
    }

    override fun getPopupText(): String? {
        var popupTextProvider: PopupTextProvider? = mPopupTextProvider
        if (popupTextProvider == null) {
            val adapter = mView.adapter
            if (adapter is PopupTextProvider) {
                popupTextProvider = adapter
            }
        }
        if (popupTextProvider == null) {
            return null
        }
        val position = getFirstItemAdapterPosition()
        return if (position == RecyclerView.NO_POSITION) {
            null
        } else {
            popupTextProvider.getPopupText(position)
        }
    }

    private fun getFirstItemAdapterPosition(): Int {
        if (mView.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itemView = mView.getChildAt(0)
        return layoutManager.getPosition(itemView)
    }

    private fun getFirstItemOffset(): Int {
        if (mView.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itemView = mView.getChildAt(0)
        mView.getDecoratedBoundsWithMargins(itemView, mTempRect)
        return mTempRect.top
    }

    private fun scrollToPositionWithOffset(position: Int, offset: Int) {
        var offsetTmp = offset
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        offsetTmp -= mView.paddingTop
        layoutManager.scrollToPositionWithOffset(position, offsetTmp)
    }
}
