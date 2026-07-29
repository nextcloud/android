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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private var files: MutableList<File> = mutableListOf()
    private var allFiles: MutableList<File> = mutableListOf()
    private val checkedFiles: MutableSet<File> = linkedSetOf()
    private var currentOffset = 0

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

    val filesCount: Int
        get() = files.size

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
        if (isWithinEncryptedFolder) {
            checkedFiles.addAll(allFiles.filter { it.isFile })
        } else {
            checkedFiles.addAll(files)
        }
    }

    fun removeAllFilesFromCheckedFiles() {
        checkedFiles.clear()
    }

    fun checkedFilesCount(): Int = checkedFiles.size

    fun getItemPosition(file: File): Int = files.indexOf(file)

    private fun shouldShowHeader(): Boolean = !PermissionUtil.checkStoragePermission(activity)

    private val headerOffset: Int
        get() = if (shouldShowHeader()) HEADER_ITEM_COUNT else 0

    override fun getItemCount(): Int = files.size + FOOTER_ITEM_COUNT + headerOffset

    override fun getItemId(position: Int): Long = getStableItemId(position, headerOffset, files)

    override fun getItemViewType(position: Int): Int {
        val offset = headerOffset

        return when {
            offset == HEADER_ITEM_COUNT && position == 0 -> VIEW_TYPE_HEADER
            position == files.size + offset -> VIEW_TYPE_FOOTER
            MimeTypeUtil.isImageOrVideo(files[position - offset]) -> VIEW_TYPE_IMAGE
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
        val isHeader = offset == HEADER_ITEM_COUNT && position == 0
        val isFooter = position == files.size + offset

        when {
            // the header has no dynamic binding
            isHeader -> Unit

            isFooter -> (holder as LocalFileListFooterViewHolder).footerText.text = footerText

            else -> files.getOrNull(position - offset)?.let { file ->
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
        currentOffset = 0

        backgroundScope.launch {
            showFirstPage(loadFirstPage(directory))

            // load the remaining folders first, then all files
            loadRemainingEntries(directory, fetchFolders = true)

            currentOffset = 0
            loadRemainingEntries(directory, fetchFolders = false)
        }
    }

    private fun loadFirstPage(directory: File?): List<File> {
        val firstPage = FileHelper.listDirectoryEntries(directory, currentOffset, PAGE_SIZE, true)
        currentOffset += PAGE_SIZE

        return if (firstPage.isEmpty()) firstPage else sortAndFilterHiddenEntries(firstPage)
    }

    private suspend fun loadRemainingEntries(directory: File?, fetchFolders: Boolean) {
        while (true) {
            val nextPage = FileHelper.listDirectoryEntries(directory, currentOffset, PAGE_SIZE, fetchFolders)
            if (nextPage.isEmpty()) {
                return
            }

            currentOffset += PAGE_SIZE
            appendPage(sortAndFilterHiddenEntries(nextPage))
        }
    }

    private fun sortAndFilterHiddenEntries(page: List<File>): List<File> {
        val visibleEntries = if (preferences.isShowHiddenFilesEnabled) page else page.filterNot { it.isHidden }
        val sortOrder = preferences.getSortOrderByType(FileSortOrder.Type.localFileListView)

        return sortOrder.sortLocalFiles(visibleEntries.toMutableList())
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun showFirstPage(firstPage: List<File>) = withContext(Dispatchers.Main) {
        files = firstPage.toMutableList()
        allFiles = firstPage.toMutableList()

        notifyDataSetChanged()
        fragmentInterface.setLoading(false)
    }

    private suspend fun appendPage(page: List<File>) = withContext(Dispatchers.Main) {
        val startPositionInAdapter = files.size + headerOffset

        files.addAll(page)
        allFiles.addAll(page)

        Log_OC.d(TAG, "appendPage, item size: ${allFiles.size}")
        notifyItemRangeInserted(startPositionInAdapter, page.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSortOrder(sortOrder: FileSortOrder) {
        fragmentInterface.setLoading(true)

        backgroundScope.launch {
            val sortedFiles = sortOrder.sortLocalFiles(files.toMutableList())

            withContext(Dispatchers.Main) {
                files = sortedFiles.toMutableList()
                allFiles = sortedFiles.toMutableList()

                notifyDataSetChanged()
                fragmentInterface.setLoading(false)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun filter(text: String) {
        files = if (text.isEmpty()) allFiles else filterByName(allFiles, text).toMutableList()
        notifyDataSetChanged()
    }

    private val footerText: String
        get() {
            val (filesCount, foldersCount) = countFooterEntries(files)
            return generateFooterText(filesCount, foldersCount)
        }

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
        files = newFiles.toMutableList()
        allFiles = newFiles.toMutableList()

        notifyDataSetChanged()
        fragmentInterface.setLoading(false)
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
