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
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.OCFileListAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder

class FileListLayoutManager(private val fragment: OCFileListFragment, private val preferences: AppPreferences) {

    fun sortFiles(sortOrder: FileSortOrder?) {
        fragment.mSortButton?.setText(DisplayUtils.getSortOrderStringId(sortOrder))
        sortOrder?.let { fragment.mAdapter.setSortOrder(fragment.mFile, it) }
    }

    /**
     * Determines whether a folder should be displayed in grid or list view.
     *
     *
     * The preference is checked for the given folder. If the folder itself does not have a preference set,
     * it will fall back to its parent folder recursively until a preference is found (root folder is always set).
     * Additionally, if a search event is active and is of type `SHARED_FILTER`, grid view is disabled.
     *
     * @param folder The folder to check, or `null` to refer to the root folder.
     * @return `true` if the folder should be displayed in grid mode, `false` if list mode is preferred.
     */
    fun isGridViewPreferred(folder: OCFile?): Boolean = if (fragment.searchEvent != null) {
        (fragment.searchEvent.toSearchType() != SearchType.SHARED_FILTER) &&
            (OCFileListFragment.FOLDER_LAYOUT_GRID == preferences.getFolderLayout(folder))
    } else {
        OCFileListFragment.FOLDER_LAYOUT_GRID == preferences.getFolderLayout(folder)
    }

    fun setLayoutViewMode() {
        val isGrid = isGridViewPreferred(fragment.mFile)

        if (isGrid) {
            switchToGridView()
        } else {
            switchToListView()
        }

        fragment.setLayoutSwitchButton(isGrid)
    }

    fun setListAsPreferred() {
        preferences.setFolderLayout(fragment.mFile, OCFileListFragment.FOLDER_LAYOUT_LIST)
        switchToListView()
    }

    fun switchToListView() {
        if (fragment.isGridEnabled) {
            switchLayoutManager(false)
        }
    }

    fun setGridAsPreferred() {
        preferences.setFolderLayout(fragment.mFile, OCFileListFragment.FOLDER_LAYOUT_GRID)
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
            val gridLayoutManager = layoutManager
            gridLayoutManager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = if (position == fragment.adapter.itemCount - 1 ||
                    (position == 0 && fragment.adapter.shouldShowHeader())
                ) {
                    gridLayoutManager.spanCount
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
