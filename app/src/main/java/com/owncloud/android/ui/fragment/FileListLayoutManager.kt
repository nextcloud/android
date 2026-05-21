/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.OCFileListAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder

class FileListLayoutManager(private val fragment: OCFileListFragment, private val preferences: AppPreferences) {

    enum class FolderLayout {
        Shared,
        Favorites,
        AllFiles,
        Child
    }

    fun sortFiles(sortOrder: FileSortOrder?) {
        fragment.mSortButton?.setText(DisplayUtils.getSortOrderStringId(sortOrder))
        sortOrder?.let { fragment.mAdapter.setSortOrder(fragment.mFile, it) }
    }

    private fun resolveLayoutType(): FolderLayout = when {
        fragment.getCurrentSearchType() == SearchType.SHARED_FILTER -> FolderLayout.Shared
        fragment.getCurrentSearchType() == SearchType.FAVORITE_SEARCH -> FolderLayout.Favorites
        fragment.mFile == null || fragment.mFile.isRootDirectory -> FolderLayout.AllFiles
        else -> FolderLayout.Child
    }

    fun isGridViewPreferred(): Boolean =
        OCFileListFragment.FOLDER_LAYOUT_GRID == preferences.getFolderLayout(resolveLayoutType())

    fun setLayoutViewMode() {
        val isGrid = isGridViewPreferred()

        if (isGrid) {
            switchToGridView()
        } else {
            switchToListView()
        }

        fragment.setLayoutSwitchButton(isGrid)
    }

    fun setListAsPreferred() {
        preferences.setFolderLayout(resolveLayoutType(), OCFileListFragment.FOLDER_LAYOUT_LIST)
        switchToListView()
    }

    fun switchToListView() {
        if (fragment.isGridEnabled) {
            switchLayoutManager(false)
        }
    }

    fun setGridAsPreferred() {
        preferences.setFolderLayout(resolveLayoutType(), OCFileListFragment.FOLDER_LAYOUT_GRID)
        switchToGridView()
    }

    fun switchToGridView() {
        if (!fragment.isGridEnabled) {
            switchLayoutManager(true)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun switchLayoutManager(grid: Boolean) {
        val recyclerView: RecyclerView? = fragment.recyclerView
        val adapter: OCFileListAdapter? = fragment.adapter
        val context: Context? = fragment.context

        if (context == null || adapter == null || recyclerView == null) {
            Log_OC.e(OCFileListFragment.TAG, "cannot switch layout, arguments are null")
            return
        }

        var position = 0

        if (recyclerView.layoutManager is LinearLayoutManager) {
            val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
            position = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        }

        val layoutManager: RecyclerView.LayoutManager?
        if (grid) {
            layoutManager = GridLayoutManager(context, fragment.columnsCount)
            layoutManager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = if (position == fragment.adapter.itemCount - 1 ||
                    (position == 0 && fragment.adapter.shouldShowHeader())
                ) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        } else {
            layoutManager = LinearLayoutManager(context)
        }

        recyclerView.setLayoutManager(layoutManager)
        recyclerView.scrollToPosition(position)
        adapter.setGridView(grid)
        recyclerView.setAdapter(adapter)
        adapter.notifyDataSetChanged()
    }
}
