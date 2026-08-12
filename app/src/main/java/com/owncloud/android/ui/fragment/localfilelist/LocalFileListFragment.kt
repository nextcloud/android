/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.localfilelist

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.localFileList.LocalFileListAdapter
import com.owncloud.android.ui.fragment.EmptyListState
import com.owncloud.android.ui.fragment.ExtendedListFragment
import com.owncloud.android.ui.interfaces.LocalFileListFragmentInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder
import java.io.File

class LocalFileListFragment :
    ExtendedListFragment(),
    LocalFileListFragmentInterface,
    Injectable {
    var currentDirectory: File? = null
        private set

    private lateinit var adapter: LocalFileListAdapter
    private lateinit var listener: LocalFileListListener

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            if (listener.isFolderPickerMode) {
                menu.removeItem(R.id.action_select_all)
                menu.removeItem(R.id.action_search)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }

    //region Lifecycle
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? LocalFileListListener
            ?: throw IllegalArgumentException(
                "$context must implement ${LocalFileListListener::class.java.simpleName}"
            )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log_OC.i(TAG, "onCreateView() start")
        val v = super.onCreateView(inflater, container, savedInstanceState)

        if (listener.isFolderPickerMode) {
            setEmptyListMessage(EmptyListState.LOCAL_FILE_LIST_EMPTY_FOLDER)
        } else {
            setEmptyListMessage(EmptyListState.LOCAL_FILE_LIST_EMPTY_FILE)
        }

        // Disable pull-to-refresh
        setSwipeEnabled(false)

        Log_OC.i(TAG, "onCreateView() end")
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupAdapter()
        listDirectory(listener.initialDirectory)
        setupSortButton()
        setupGridViewButton()
    }

    override fun onDestroyView() {
        adapter.cleanup()
        super.onDestroyView()
    }
    //endregion

    //region Setup
    private fun setupAdapter() {
        adapter = LocalFileListAdapter(
            listener.isFolderPickerMode,
            this,
            preferences,
            requireActivity(),
            viewThemeUtils,
            listener.isWithinEncryptedFolder
        )
        setRecyclerViewAdapter(adapter)
    }

    private fun setupSortButton() {
        val button = mSortButton ?: return

        button.setOnClickListener {
            val sortOrder = preferences.getSortOrderByType(FileSortOrder.Type.localFileListView)
            DisplayUtils.openSortingOrderDialogFragment(parentFragmentManager, sortOrder)
        }

        val sortOrder = preferences.getSortOrderByType(FileSortOrder.Type.localFileListView) ?: return
        button.setText(DisplayUtils.getSortOrderStringId(sortOrder))
    }

    private fun setupGridViewButton() {
        setLayoutSwitchButton()

        mSwitchGridViewButton?.setOnClickListener {
            if (isGridEnabled) {
                switchToListView()
            } else {
                switchToGridView()
            }
            setLayoutSwitchButton()
        }
    }
    //endregion

    //region Item clicks
    override fun onItemClicked(file: File?) {
        val file = file ?: return

        if (file.isDirectory()) {
            listDirectory(file)
            listener.onDirectoryClick(file)
            saveIndexAndTopPosition(adapter.getItemPosition(file))
            return
        }

        onItemCheckboxClicked(file)
    }

    override fun onItemCheckboxClicked(file: File?) {
        val file = file ?: return
        adapter.onItemCheckboxClicked(file)
        listener.onFileClick(file)
    }
    //endregion

    //region Directory navigation
    fun onNavigateUp() {
        val parentDir = currentDirectory?.getParentFile()
        listDirectory(parentDir)
        restoreIndexAndTopPosition()
    }

    @JvmOverloads
    fun listDirectory(directory: File? = null) {
        val target = directory ?: currentDirectory ?: Environment.getExternalStorageDirectory() ?: return
        val folder = target.asDirectoryOrParent() ?: return

        adapter.removeAllFilesFromCheckedFiles()
        adapter.swapDirectory(folder)
        currentDirectory = folder

        recyclerView?.scrollToPosition(0)
    }

    private fun File.asDirectoryOrParent(): File? {
        if (isDirectory) {
            return this
        }

        Log_OC.w(TAG, "You see, that is not a directory -> $this")

        val parent = parentFile
        if (parent == null) {
            Log_OC.w(TAG, "parent directory is null, cannot swap directory")
        }

        return parent
    }
    //endregion

    //region File selection
    val checkedFilePaths: Array<String>
        get() = adapter.checkedFilesPath

    val checkedFilesCount: Int
        get() = adapter.checkedFilesCount()

    val filesCount: Int
        get() = adapter.filesCount

    fun selectAllFiles(select: Boolean) {
        if (recyclerView == null) {
            return
        }

        val localFileListAdapter = recyclerView?.adapter as? LocalFileListAdapter? ?: return

        if (select) {
            localFileListAdapter.addAllFilesToCheckedFiles()
        } else {
            localFileListAdapter.removeAllFilesFromCheckedFiles()
        }

        adapter.notifyItemRangeChanged(0, adapter.getItemCount())
    }
    //endregion

    //region View options
    fun sortFiles(sortOrder: FileSortOrder) {
        mSortButton?.setText(DisplayUtils.getSortOrderStringId(sortOrder))
        adapter.setSortOrder(sortOrder)
    }

    override fun switchToGridView() {
        val recyclerView = recyclerView ?: return

        adapter.gridView = true
        recyclerView.adapter = adapter

        if (isGridEnabled) {
            return
        }

        recyclerView.layoutManager = createGridLayoutManager()
    }

    private fun createGridLayoutManager(): GridLayoutManager {
        val layoutManager = GridLayoutManager(context, columnsCount)

        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int = when (position) {
                adapter.itemCount - 1 -> layoutManager.spanCount
                else -> SINGLE_SPAN
            }
        }

        return layoutManager
    }

    override fun switchToListView() {
        if (recyclerView == null) {
            return
        }

        adapter.gridView = false
        recyclerView?.setAdapter(adapter)
        super.switchToListView()
    }
    //endregion

    //region Adapter updates
    @VisibleForTesting
    fun setFiles(newFiles: List<File>) {
        adapter.setFiles(newFiles)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setupStoragePermissionWarningBanner() {
        adapter.notifyDataSetChanged()
    }
    //endregion

    companion object {
        private const val SINGLE_SPAN = 1
        private val TAG: String = LocalFileListFragment::class.java.getSimpleName()
    }
}
