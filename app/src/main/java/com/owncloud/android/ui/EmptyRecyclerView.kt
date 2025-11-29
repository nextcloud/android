/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.lib.common.utils.Log_OC

class EmptyRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    RecyclerView(context, attrs, defStyle) {
    companion object {
        private const val TAG = "EmptyRecyclerView"
    }

    private var emptyView: View? = null
    private var hasFooter = false
    private var previousVisibilityState: Boolean? = null

    override fun setAdapter(adapter: Adapter<*>?) {
        this.adapter?.unregisterAdapterDataObserver(observer)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        previousVisibilityState = null
        configureEmptyView()
    }

    fun setEmptyView(view: View?) {
        emptyView = view
    }

    @Suppress("ReturnCount")
    private fun configureEmptyView() {
        val view = emptyView ?: run {
            Log_OC.e(TAG, "cannot configure empty view, view is null")
            return
        }

        val recyclerViewAdapter = adapter ?: run {
            Log_OC.e(TAG, "cannot configure empty view, recyclerViewAdapter is null")
            return
        }

        val emptyCount = if (hasFooter) 1 else 0
        val empty = (recyclerViewAdapter.itemCount == emptyCount)

        if (previousVisibilityState == empty) {
            Log_OC.d(TAG, "no need to configure empty view, state didn't change")
            return
        }

        Log_OC.d(TAG, "changing empty view state, adapter item count: ${recyclerViewAdapter.itemCount}")

        previousVisibilityState = empty
        view.setVisibleIf(empty)
        view.isFocusable = empty
        setVisibleIf(!empty)
    }

    private val observer: AdapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            configureEmptyView()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            configureEmptyView()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            configureEmptyView()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            configureEmptyView()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            configureEmptyView()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            configureEmptyView()
        }
    }

    fun setHasFooter(bool: Boolean) {
        hasFooter = bool
    }
}
