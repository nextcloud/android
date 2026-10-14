/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist

import android.annotation.SuppressLint
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.fragment.SearchType

class OCFileListDiffer(
    private val adapter: RecyclerView.Adapter<*>,
    private val preferences: AppPreferences,
    private val shouldShowHeader: () -> Boolean
) {

    companion object {
        @JvmField
        val REBIND_PAYLOAD = Any()
    }

    var isHeaderDisplayed = false
        private set

    var latestFiles: List<OCFile> = emptyList()
        private set

    private var isRebindPending = false
    private var isReplacing = false
    private var recyclerView: RecyclerView? = null

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            if (isReplacing) return
            val adapterPosition = position + headerOffset()
            keepTopVisibleWhile(adapterPosition) { adapter.notifyItemRangeInserted(adapterPosition, count) }
        }

        override fun onRemoved(position: Int, count: Int) {
            if (isReplacing) return
            adapter.notifyItemRangeRemoved(position + headerOffset(), count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            if (isReplacing) return
            val adapterToPosition = toPosition + headerOffset()
            keepTopVisibleWhile(adapterToPosition) {
                adapter.notifyItemMoved(fromPosition + headerOffset(), adapterToPosition)
            }
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            if (isReplacing) return
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
        latestFiles = newFiles
        differ.submitList(newFiles) { onListCommitted() }
    }

    fun submitAndRebind(newFiles: List<OCFile>) {
        isRebindPending = true
        submit(newFiles)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun replace(newFiles: List<OCFile>) {
        latestFiles = newFiles
        isRebindPending = false
        isReplacing = true
        try {
            differ.submitList(null)
            differ.submitList(newFiles)
        } finally {
            isReplacing = false
        }
        adapter.notifyDataSetChanged()
    }

    fun sortForCurrentView(
        files: MutableList<OCFile>,
        searchType: SearchType?,
        currentDirectory: OCFile?
    ): MutableList<OCFile> {
        if (searchType == SearchType.SHARED_FILTER) {
            files.sortByDescending { it.firstShareTimestamp }
            return files
        }

        val sortOrder = preferences.getSortOrderByFolder(currentDirectory)
        return sortOrder.sortCloudFiles(files, preferences.isSortFoldersBeforeFiles, preferences.isSortFavoritesFirst)
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
            keepTopVisibleWhile(0) { adapter.notifyItemInserted(0) }
        } else {
            adapter.notifyItemRemoved(0)
        }
    }

    fun syncHeaderState() {
        isHeaderDisplayed = shouldShowHeader()
    }

    fun headerOffset(): Int = if (isHeaderDisplayed) 1 else 0

    fun attach(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    fun detach() {
        recyclerView = null
    }

    private fun keepTopVisibleWhile(adapterPosition: Int, notify: () -> Unit) {
        val wasAtTop = adapterPosition == 0 && recyclerView?.canScrollVertically(-1) == false
        notify()
        if (wasAtTop) {
            recyclerView?.scrollToPosition(0)
        }
    }

    private fun onListCommitted() {
        adapter.notifyItemChanged(adapter.itemCount - 1)

        if (!isRebindPending) return
        isRebindPending = false

        if (files.isNotEmpty()) {
            adapter.notifyItemRangeChanged(headerOffset(), files.size, REBIND_PAYLOAD)
        }
    }
}
