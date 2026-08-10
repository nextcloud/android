/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

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
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.localFileList.LocalFileListAdapter
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
    private lateinit var containerActivity: ContainerActivity

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            if (containerActivity.isFolderPickerMode) {
                menu.removeItem(R.id.action_select_all)
                menu.removeItem(R.id.action_search)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        containerActivity = context as? ContainerActivity
            ?: throw IllegalArgumentException(
                "$context must implement ${ContainerActivity::class.java.simpleName}"
            )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log_OC.i(TAG, "onCreateView() start")
        val v = super.onCreateView(inflater, container, savedInstanceState)

        if (containerActivity.isFolderPickerMode) {
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
        listDirectory(containerActivity.initialDirectory)
        setupSortButton()
        setupGridViewButton()
    }

    private fun setupAdapter() {
        adapter = LocalFileListAdapter(
            containerActivity.isFolderPickerMode,
            this,
            preferences,
            requireActivity(),
            viewThemeUtils,
            containerActivity.isWithinEncryptedFolder
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

    override fun onItemClicked(file: File?) {
        val file = file ?: return

        if (file.isDirectory()) {
            listDirectory(file)
            containerActivity.onDirectoryClick(file)
            saveIndexAndTopPosition(adapter.getItemPosition(file))
            return
        }

        onItemCheckboxClicked(file)
    }

    override fun onItemCheckboxClicked(file: File?) {
        val file = file ?: return
        adapter.onItemCheckboxClicked(file)
        containerActivity.onFileClick(file)
    }

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

    val checkedFilePaths: Array<String>
        get() = adapter.checkedFilesPath

    val checkedFilesCount: Int
        get() = adapter.checkedFilesCount()

    val filesCount: Int
        get() = adapter.filesCount

    fun sortFiles(sortOrder: FileSortOrder) {
        mSortButton?.setText(DisplayUtils.getSortOrderStringId(sortOrder))
        adapter.setSortOrder(sortOrder)
    }

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

        layoutManager.spanSizeLookup = object : SpanSizeLookup() {
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

    @VisibleForTesting
    fun setFiles(newFiles: MutableList<File>) {
        adapter.setFiles(newFiles)
    }

    interface ContainerActivity {
        fun onDirectoryClick(directory: File?)
        fun onFileClick(file: File?)
        val initialDirectory: File?
        val isFolderPickerMode: Boolean
        val isWithinEncryptedFolder: Boolean
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setupStoragePermissionWarningBanner() {
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        adapter.cleanup()
        super.onDestroyView()
    }

    companion object {
        private const val SINGLE_SPAN = 1
        private val TAG: String = LocalFileListFragment::class.java.getSimpleName()
    }
}
