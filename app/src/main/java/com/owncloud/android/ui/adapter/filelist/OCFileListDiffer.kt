/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.datamodel.OCFile

class OCFileListDiffer(private val adapter: RecyclerView.Adapter<*>, private val shouldShowHeader: () -> Boolean) {

    private var isHeaderDisplayed = false

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            adapter.notifyItemRangeInserted(position + headerOffset(), count)
        }

        override fun onRemoved(position: Int, count: Int) {
            adapter.notifyItemRangeRemoved(position + headerOffset(), count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            adapter.notifyItemMoved(fromPosition + headerOffset(), toPosition + headerOffset())
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapter.notifyItemRangeChanged(position + headerOffset(), count, payload)
        }
    }

    private val differ = AsyncListDiffer(
        listUpdateCallback,
        AsyncDifferConfig.Builder(OCFileDiffCallback()).build()
    )

    val files: List<OCFile>
        get() = differ.currentList

    fun submit(newFiles: List<OCFile>) {
        differ.submitList(newFiles) { adapter.notifyItemChanged(adapter.itemCount - 1) }
    }

    fun updateHeader() {
        val shouldShowHeader = shouldShowHeader()

        if (shouldShowHeader == isHeaderDisplayed) {
            if (shouldShowHeader) {
                adapter.notifyItemChanged(0)
            }
            return
        }

        isHeaderDisplayed = shouldShowHeader

        if (shouldShowHeader) {
            adapter.notifyItemInserted(0)
        } else {
            adapter.notifyItemRemoved(0)
        }
    }

    fun syncHeaderState() {
        isHeaderDisplayed = shouldShowHeader()
    }

    private fun headerOffset(): Int = if (shouldShowHeader()) 1 else 0
}
