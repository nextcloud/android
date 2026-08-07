/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.FileHelper
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.FilterableListAdapter
import com.owncloud.android.ui.adapter.storagePermissionBanner.StoragePermissionBannerViewHolder
import com.owncloud.android.ui.interfaces.LocalFileListFragmentInterface
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Suppress("LongParameterList", "TooManyFunctions")
class LocalFileListAdapter(
    localFolderPickerMode: Boolean,
    private val fragmentInterface: LocalFileListFragmentInterface,
    private val preferences: AppPreferences,
    private val activity: Activity,
    viewThemeUtils: ViewThemeUtils,
    private val isWithinEncryptedFolder: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    FilterableListAdapter {

    /** Every entry loaded so far, in listing order. Pages are appended, the list is never copied. */
    private val loadedEntries: MutableList<File> = mutableListOf()

    /** Entries matching [searchQuery]. Holds the same [File] instances as [loadedEntries], not copies. */
    private val matchingEntries: MutableList<File> = mutableListOf()

    private val checkedFiles: MutableSet<File> = linkedSetOf()

    private var searchQuery = ""

    /** Set by "select all". Entries of the pages that arrive afterwards are selected as well. */
    private var selectAllActive = false

    private var visibleFilesCount = 0
    private var visibleFoldersCount = 0

    private var loadJob: Job? = null
    private var sortJob: Job? = null

    var gridView: Boolean = false

    private val itemBinder = LocalFileListItemBinder(
        activity,
        viewThemeUtils,
        fragmentInterface,
        localFolderPickerMode,
        isWithinEncryptedFolder,
        ::isCheckedFile
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    init {
        setHasStableIds(true)
    }

    /** Entries the list renders: the search matches while a search is active, all of them otherwise. */
    private val visibleEntries: List<File>
        get() = if (searchQuery.isEmpty()) loadedEntries else matchingEntries

    val filesCount: Int
        get() = visibleEntries.size

    val checkedFilesPath: Array<String>
        get() {
            val result = FileHelper.listFilesRecursive(checkedFiles)
            Log_OC.d(TAG, "Returning ${result.size} selected files")
            return result.toTypedArray()
        }

    fun isCheckedFile(file: File): Boolean = checkedFiles.contains(file)

    fun addCheckedFile(file: File) {
        checkedFiles.add(file)
    }

    fun removeCheckedFile(file: File) {
        checkedFiles.remove(file)
    }

    fun addAllFilesToCheckedFiles() {
        selectAllActive = true
        checkedFiles.addAll(selectableEntries(visibleEntries))
    }

    fun removeAllFilesFromCheckedFiles() {
        selectAllActive = false
        checkedFiles.clear()
    }

    /** Folders cannot be picked inside an encrypted folder, their checkbox stays hidden there. */
    private fun selectableEntries(entries: List<File>): List<File> =
        if (isWithinEncryptedFolder) entries.filter { it.isFile } else entries

    fun checkedFilesCount(): Int = checkedFiles.size

    /** Adapter position of [file], or [RecyclerView.NO_POSITION] when it is not shown. */
    fun getItemPosition(file: File): Int {
        val index = visibleEntries.indexOf(file)
        return if (index < 0) RecyclerView.NO_POSITION else index + headerOffset
    }

    private fun shouldShowHeader(): Boolean = !PermissionUtil.checkStoragePermission(activity)

    private val headerOffset: Int
        get() = if (shouldShowHeader()) HEADER_ITEM_COUNT else 0

    override fun getItemCount(): Int = visibleEntries.size + FOOTER_ITEM_COUNT + headerOffset

    override fun getItemId(position: Int): Long = getStableItemId(position, headerOffset, visibleEntries)

    override fun getItemViewType(position: Int): Int {
        val offset = headerOffset
        val entries = visibleEntries

        return when {
            offset == HEADER_ITEM_COUNT && position == 0 -> VIEW_TYPE_HEADER
            position == entries.size + offset -> VIEW_TYPE_FOOTER
            MimeTypeUtil.isImageOrVideo(entries[position - offset]) -> VIEW_TYPE_IMAGE
            else -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(activity)

        return when (viewType) {
            VIEW_TYPE_ITEM, VIEW_TYPE_IMAGE -> if (gridView) {
                LocalFileListGridItemViewHolder(inflater.inflate(R.layout.grid_item, parent, false))
            } else {
                LocalFileListItemViewHolder(inflater.inflate(R.layout.list_item, parent, false))
            }

            VIEW_TYPE_FOOTER -> LocalFileListFooterViewHolder(inflater.inflate(R.layout.list_footer, parent, false))

            VIEW_TYPE_HEADER -> StoragePermissionBannerViewHolder(
                activity,
                inflater.inflate(R.layout.storage_permission_warning_banner, parent, false)
            )

            else -> throw IllegalArgumentException("Invalid viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val offset = headerOffset
        val entries = visibleEntries
        val isHeader = offset == HEADER_ITEM_COUNT && position == 0
        val isFooter = position == entries.size + offset

        when {
            // the header has no dynamic binding
            isHeader -> Unit

            isFooter -> (holder as LocalFileListFooterViewHolder).footerText.text = footerText

            else -> entries.getOrNull(position - offset)?.let { file ->
                itemBinder.bind(holder as LocalFileListGridItemViewHolder, file)
            }
        }
    }

    /**
     * Change the adapted directory for a new one
     *
     * @param directory New file to adapt. Can be NULL, meaning "no content to adapt".
     */
    fun swapDirectory(directory: File?) {
        fragmentInterface.setLoading(true)

        // pending pages and sorts belong to the previously shown directory
        loadJob?.cancel()
        sortJob?.cancel()

        loadJob = backgroundScope.launch {
            var isFirstPage = true

            val consumePage: suspend (List<File>) -> Unit = { page ->
                deliver(sortAndFilterHiddenEntries(page), if (isFirstPage) ::showFirstPage else ::appendPage)
                isFirstPage = false
            }

            // folders are shown on top of the list, so they are read before the files
            FileHelper.forEachDirectoryPage(directory, PAGE_SIZE, fetchFolders = true, consumePage)
            FileHelper.forEachDirectoryPage(directory, PAGE_SIZE, fetchFolders = false, consumePage)

            // an empty directory reports no page at all, the list still has to drop its old entries
            if (isFirstPage) {
                deliver(emptyList(), ::showFirstPage)
            }
        }
    }

    private fun sortAndFilterHiddenEntries(page: List<File>): List<File> {
        if (page.isEmpty()) {
            return page
        }

        val pageEntries = if (preferences.isShowHiddenFilesEnabled) {
            page.toMutableList()
        } else {
            page.filterNotTo(mutableListOf()) { it.isHidden }
        }

        val sortOrder = preferences.getSortOrderByType(FileSortOrder.Type.localFileListView)
        return sortOrder.sortLocalFiles(pageEntries)
    }

    /** Hands a page over to the main thread. Pages of a cancelled load are dropped. */
    private suspend fun deliver(page: List<File>, consumer: (List<File>) -> Unit) = withContext(Dispatchers.Main) {
        if (isActive) {
            consumer(page)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showFirstPage(firstPage: List<File>) {
        loadedEntries.clear()
        matchingEntries.clear()
        searchQuery = ""
        selectAllActive = false

        loadedEntries.addAll(firstPage)
        recountFooterEntries()

        notifyDataSetChanged()
        fragmentInterface.setLoading(false)
    }

    private fun appendPage(page: List<File>) {
        if (page.isEmpty()) {
            return
        }

        val startPositionInAdapter = visibleEntries.size + headerOffset
        loadedEntries.addAll(page)

        // while searching only the matches of the page become visible, keeping the listing order
        val insertedEntries = if (searchQuery.isEmpty()) {
            page
        } else {
            filterByName(page, searchQuery).also { matchingEntries.addAll(it) }
        }

        if (selectAllActive) {
            checkedFiles.addAll(selectableEntries(insertedEntries))
        }

        addToFooterEntries(insertedEntries)
        Log_OC.d(TAG, "appendPage, item size: ${loadedEntries.size}")

        if (insertedEntries.isEmpty()) {
            return
        }

        notifyItemRangeInserted(startPositionInAdapter, insertedEntries.size)

        // inserting in front of the footer only shifts it down, it has to be rebound to show the
        // counts of the page that just arrived
        notifyItemChanged(itemCount - 1, PAYLOAD_FOOTER_COUNTS)
    }

    fun setSortOrder(sortOrder: FileSortOrder) {
        fragmentInterface.setLoading(true)
        sortJob?.cancel()

        // sorting a list that keeps growing while pages arrive is not safe, hand over a copy
        val entriesToSort = loadedEntries.toMutableList()

        sortJob = backgroundScope.launch {
            deliver(sortOrder.sortLocalFiles(entriesToSort), ::showSortedEntries)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showSortedEntries(sortedEntries: List<File>) {
        // pages that arrived while sorting stay at the end, they are sorted among themselves
        for (index in 0 until minOf(sortedEntries.size, loadedEntries.size)) {
            loadedEntries[index] = sortedEntries[index]
        }

        // the search matches have to follow the new order of the listing
        applySearch(searchQuery)

        notifyDataSetChanged()
        fragmentInterface.setLoading(false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun filter(text: String) {
        applySearch(text)
        notifyDataSetChanged()
    }

    private fun applySearch(query: String) {
        searchQuery = query
        matchingEntries.clear()

        if (query.isNotEmpty()) {
            matchingEntries.addAll(filterByName(loadedEntries, query))
        }

        recountFooterEntries()
    }

    private fun recountFooterEntries() {
        val (filesCount, foldersCount) = countFooterEntries(visibleEntries)
        visibleFilesCount = filesCount
        visibleFoldersCount = foldersCount
    }

    private fun addToFooterEntries(entries: List<File>) {
        val (filesCount, foldersCount) = countFooterEntries(entries)
        visibleFilesCount += filesCount
        visibleFoldersCount += foldersCount
    }

    private val footerText: String
        get() = generateFooterText(visibleFilesCount, visibleFoldersCount)

    private fun generateFooterText(filesCount: Int, foldersCount: Int): String {
        val resources = activity.resources
        val fileText = resources.getQuantityString(R.plurals.file_list__footer__file, filesCount, filesCount)
        val folderText = resources.getQuantityString(R.plurals.file_list__footer__folder, foldersCount, foldersCount)

        return when {
            filesCount + foldersCount <= 0 -> ""
            foldersCount <= 0 -> fileText
            filesCount <= 0 -> folderText
            else -> "$fileText, $folderText"
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @VisibleForTesting
    fun setFiles(newFiles: List<File>) {
        showFirstPage(newFiles)
    }

    fun cleanup() {
        backgroundScope.cancel()
    }

    companion object {
        private val TAG: String = LocalFileListAdapter::class.java.simpleName

        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_FOOTER = 1
        private const val VIEW_TYPE_IMAGE = 2
        private const val VIEW_TYPE_HEADER = 3

        private const val PAGE_SIZE = 50
        private const val HEADER_ITEM_COUNT = 1
        private const val FOOTER_ITEM_COUNT = 1

        private const val HEADER_ID = Long.MIN_VALUE
        private const val FOOTER_ID = Long.MIN_VALUE + 1

        /** Rebinds the footer in place, without the change animation of a payload free update. */
        private val PAYLOAD_FOOTER_COUNTS = Any()

        @VisibleForTesting
        fun getStableItemId(position: Int, headerOffset: Int, files: List<File>): Long {
            if (headerOffset == HEADER_ITEM_COUNT && position == 0) {
                return HEADER_ID
            }

            val file = files.getOrNull(position - headerOffset)
            return file?.absolutePath?.hashCode()?.toLong() ?: FOOTER_ID
        }

        @VisibleForTesting
        fun filterByName(files: List<File>, text: String): List<File> {
            val filterText = text.lowercase(Locale.getDefault())
            return files.filter { it.name.lowercase(Locale.getDefault()).contains(filterText) }
        }

        /**
         * Counts the entries shown in the footer: folders are always counted, files only when they are not hidden.
         *
         * @return amount of files and amount of folders
         */
        @VisibleForTesting
        fun countFooterEntries(files: List<File>): Pair<Int, Int> {
            val (folders, plainFiles) = files.partition { it.isDirectory }
            return plainFiles.count { !it.isHidden } to folders.size
        }
    }
}
