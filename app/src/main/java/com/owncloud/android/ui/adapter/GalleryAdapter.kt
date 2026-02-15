/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 * Copyright (C) 2023 TSI-mc
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.databinding.GalleryHeaderBinding
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryItems
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.util.Calendar
import java.util.Date
import java.util.regex.Pattern

@Suppress("LongParameterList", "TooManyFunctions")
class GalleryAdapter(
    val context: Context,
    user: User,
    ocFileListFragmentInterface: OCFileListFragmentInterface,
    preferences: AppPreferences,
    transferServiceGetter: ComponentsGetter,
    private val viewThemeUtils: ViewThemeUtils,
    var columns: Int,
    private val defaultThumbnailSize: Int
) : SectionedRecyclerViewAdapter<SectionedViewHolder>(),
    CommonOCFileListAdapterInterface,
    PopupTextProvider {

    companion object {
        private const val TAG = "GalleryAdapter"
        private const val FIRST_DAY_OF_MONTH = 1
        private const val FIRST_MONTH = 1
        private const val YEAR_GROUP = 1
        private const val MONTH_GROUP = 2
        private const val DAY_GROUP = 3

        // Pattern to extract YYYY, YYYY/MM, or YYYY/MM/DD from file path (requires zero-padded month/day)
        private val FOLDER_DATE_PATTERN: Pattern = Pattern.compile("/(\\d{4})(?:/(\\d{2}))?(?:/(\\d{2}))?/")

        /**
         * Extract folder date from path (YYYY, YYYY/MM, or YYYY/MM/DD).
         * Uses LocalDate for calendar-aware validation (leap years, days per month).
         * Invalid month/day values fall back to defaults. Future dates are rejected.
         * @return timestamp or null if no folder date found or date is in the future
         */
        @VisibleForTesting
        @Suppress("TooGenericExceptionCaught")
        fun extractFolderDate(path: String?): Long? {
            try {
                val matcher = path?.let { FOLDER_DATE_PATTERN.matcher(it) }
                val year = matcher?.takeIf { it.find() }?.group(YEAR_GROUP)?.toIntOrNull()

                return year?.let { y ->
                    val rawMonth = matcher.group(MONTH_GROUP)?.toIntOrNull()
                    val rawDay = matcher.group(DAY_GROUP)?.toIntOrNull()

                    val month = rawMonth ?: FIRST_MONTH
                    val day = rawDay ?: FIRST_DAY_OF_MONTH

                    val localDate = tryCreateDate(y, month, day)
                        ?: tryCreateDate(y, month, FIRST_DAY_OF_MONTH)
                        ?: tryCreateDate(y, FIRST_MONTH, FIRST_DAY_OF_MONTH)

                    localDate?.takeIf { !it.isAfter(java.time.LocalDate.now()) }
                        ?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                }
            } catch (e: Exception) {
                return null
            }
        }

        private fun tryCreateDate(year: Int, month: Int, day: Int): java.time.LocalDate? = try {
            java.time.LocalDate.of(year, month, day)
        } catch (e: java.time.DateTimeException) {
            null
        }
    }

    // fileId -> (section, row)
    private val filePositionMap = mutableMapOf<Long, Pair<Int, Int>>()

    // (section, row) -> unique stable ID for that row
    private val rowIdMap = mutableMapOf<Pair<Int, Int>, Long>()

    private var cachedAllFiles: List<OCFile>? = null
    private var cachedFilesCount: Int = 0

    private var _files: List<GalleryItems> = mutableListOf()
    var files: List<GalleryItems>
        get() = _files
        private set(value) {
            _files = value
            invalidateCaches()
        }

    private val ocFileListDelegate: OCFileListDelegate
    private var storageManager: FileDataStorageManager = transferServiceGetter.storageManager

    init {
        ocFileListDelegate = OCFileListDelegate(
            transferServiceGetter.fileUploaderHelper,
            context,
            ocFileListFragmentInterface,
            user,
            storageManager,
            false,
            preferences,
            true,
            transferServiceGetter,
            showMetadata = false,
            showShareAvatar = false,
            viewThemeUtils
        )
    }

    private fun invalidateCaches() {
        Log_OC.d(TAG, "invalidating caches")
        cachedAllFiles = null
        updateFilesCount()
        rebuildFilePositionMap()
    }

    private fun updateFilesCount() {
        cachedFilesCount = files.sumOf { it.rows.sumOf { it.files.size } }
    }

    private fun rebuildFilePositionMap() {
        filePositionMap.clear()
        rowIdMap.clear()

        files.forEachIndexed { sectionIndex, galleryItem ->
            galleryItem.rows.forEachIndexed { rowIndex, row ->
                val position = sectionIndex to rowIndex

                // since row can contain files two to five use first files id as adapter id
                row.files.firstOrNull()?.fileId?.let { firstFileId ->
                    rowIdMap[position] = firstFileId
                }

                // map all row files
                row.files.forEach { file ->
                    filePositionMap[file.fileId] = position
                }
            }
        }
    }

    override fun getItemId(section: Int, position: Int): Long = rowIdMap[section to position] ?: -1L

    override fun getItemCount(section: Int): Int = files.getOrNull(section)?.rows?.size ?: 0

    override fun getSectionCount(): Int = files.size

    override fun getFilesCount(): Int = cachedFilesCount

    override fun getItemPosition(file: OCFile): Int {
        val (section, row) = filePositionMap[file.fileId] ?: return -1
        return getAbsolutePosition(section, row)
    }

    override fun selectAll(value: Boolean) {
        if (value) {
            addAllFilesToCheckedFiles()
        } else {
            clearCheckedItems()
        }
    }

    override fun showFooters(): Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder =
        if (viewType == VIEW_TYPE_HEADER) {
            GalleryHeaderViewHolder(
                GalleryHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            GalleryRowHolder(
                GalleryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                defaultThumbnailSize.toFloat(),
                ocFileListDelegate,
                storageManager,
                this,
                viewThemeUtils
            )
        }

    override fun onBindViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (holder is GalleryRowHolder) {
            val row = files.getOrNull(section)?.rows?.getOrNull(relativePosition)
            row?.let { holder.bind(it) }
        }
    }

    override fun getPopupText(p0: View, position: Int): CharSequence = DisplayUtils.getDateByPattern(
        files[getRelativePosition(position).section()].date,
        context,
        DisplayUtils.MONTH_YEAR_PATTERN
    )

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder?, section: Int, expanded: Boolean) {
        if (holder != null) {
            val headerViewHolder = holder as GalleryHeaderViewHolder
            val galleryItem = files[section]

            headerViewHolder.binding.month.text = DisplayUtils.getDateByPattern(
                galleryItem.date,
                context,
                DisplayUtils.MONTH_PATTERN
            )
            headerViewHolder.binding.year.text = DisplayUtils.getDateByPattern(
                galleryItem.date,
                context,
                DisplayUtils.YEAR_PATTERN
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showAllGalleryItems(
        remotePath: String,
        mediaState: GalleryFragmentBottomSheetDialog.MediaState,
        photoFragment: GalleryFragment
    ) {
        val items = storageManager.allGalleryItems

        val filteredList = items.filter { it != null && it.remotePath.startsWith(remotePath) }

        setMediaFilter(
            filteredList,
            mediaState,
            photoFragment
        )
    }

    // Set Image/Video List According to Selection of Hide/Show Image/Video
    @SuppressLint("NotifyDataSetChanged")
    private fun setMediaFilter(
        items: List<OCFile>,
        mediaState: GalleryFragmentBottomSheetDialog.MediaState,
        photoFragment: GalleryFragment
    ) {
        val finalSortedList: List<OCFile> = when (mediaState) {
            GalleryFragmentBottomSheetDialog.MediaState.MEDIA_STATE_PHOTOS_ONLY -> {
                items.filter { MimeTypeUtil.isImage(it.mimeType) }.distinct()
            }

            GalleryFragmentBottomSheetDialog.MediaState.MEDIA_STATE_VIDEOS_ONLY -> {
                items.filter { MimeTypeUtil.isVideo(it.mimeType) }.distinct()
            }

            else -> items
        }

        if (finalSortedList.isEmpty()) {
            photoFragment.setEmptyListMessage(SearchType.GALLERY_SEARCH)
        }

        files = finalSortedList.toGalleryItems()
        notifyDataSetChanged()
    }

    private fun transformToRows(list: List<OCFile>): List<GalleryRow> {
        if (list.isEmpty()) return emptyList()

        // List is already sorted by toGalleryItems(), just chunk into rows
        return list
            .chunked(columns)
            .map { chunk -> GalleryRow(chunk, defaultThumbnailSize, defaultThumbnailSize) }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        files = emptyList()
        notifyDataSetChanged()
    }

    private fun firstOfMonth(timestamp: Long): Long = Calendar.getInstance().apply {
        time = Date(timestamp)
        set(Calendar.DAY_OF_MONTH, getActualMinimum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    fun isEmpty(): Boolean = files.isEmpty()

    fun getItem(position: Int): OCFile? {
        val itemCoordinates = getRelativePosition(position)

        return files
            .getOrNull(itemCoordinates.section())
            ?.rows
            ?.getOrNull(itemCoordinates.relativePos())
            ?.files
            ?.getOrNull(0)
    }

    override fun isMultiSelect(): Boolean = ocFileListDelegate.isMultiSelect

    override fun cancelAllPendingTasks() {
        ocFileListDelegate.cancelAllPendingTasks()
    }

    override fun addCheckedFile(file: OCFile) {
        ocFileListDelegate.addCheckedFile(file)
    }

    override fun isCheckedFile(file: OCFile): Boolean = ocFileListDelegate.isCheckedFile(file)

    override fun getCheckedItems(): Set<OCFile> = ocFileListDelegate.checkedItems

    override fun removeCheckedFile(file: OCFile) {
        ocFileListDelegate.removeCheckedFile(file)
    }

    override fun notifyItemChanged(file: OCFile) {
        val position = getItemPosition(file)
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }

    /**
     * Enables or disables multi-select mode in the gallery.
     *
     * When multi-select mode is enabled:
     * - Checkboxes are shown for all items.
     * - Users can select multiple files.
     *
     * When multi-select mode is disabled:
     * - Checkboxes are hidden.
     * - Selected files remain visually unselected.
     *
     * Note:
     * - This function is only called when the user explicitly enters or exits multi-select mode.
     *   It is **not** called for individual file selection or deselection.
     * - The entire adapter is refreshed using [notifyDataSetChanged] to properly show or hide
     *   checkboxes across all rows, as individual item updates are not sufficient in this case.
     *
     * @param isMultiSelect true to enable multi-select mode, false to disable it.
     */
    @SuppressLint("NotifyDataSetChanged")
    override fun setMultiSelect(isMultiSelect: Boolean) {
        ocFileListDelegate.isMultiSelect = isMultiSelect
        notifyDataSetChanged()
    }

    private fun getAllFiles(): List<OCFile> = cachedAllFiles ?: files.flatMap { galleryItem ->
        galleryItem.rows.flatMap { row -> row.files }
    }.also { cachedAllFiles = it }

    private fun addAllFilesToCheckedFiles() {
        val allFiles = getAllFiles()
        ocFileListDelegate.addToCheckedFiles(allFiles)
    }

    override fun clearCheckedItems() {
        ocFileListDelegate.clearCheckedItems()
    }

    @VisibleForTesting
    fun addFiles(items: List<GalleryItems>) {
        files = items
    }

    fun changeColumn(newColumn: Int) {
        columns = newColumn
    }

    fun markAsFavorite(remotePath: String, favorite: Boolean) {
        val allFiles = getAllFiles()
        allFiles.firstOrNull { it.remotePath == remotePath }?.also { file ->
            file.isFavorite = favorite
            files = allFiles.toGalleryItems()
            notifyItemChanged(file)
        }
    }

    /**
     * Get the grouping date for a file: use folder date from path if present,
     * otherwise fall back to modification timestamp month.
     */
    private fun getGroupingDate(file: OCFile): Long =
        firstOfMonth(extractFolderDate(file.remotePath) ?: file.modificationTimestamp)

    private fun List<OCFile>.toGalleryItems(): List<GalleryItems> {
        if (isEmpty()) return emptyList()

        return groupBy { getGroupingDate(it) }
            .map { (date, filesList) ->
                // Sort files within group: by folder day desc, then by modification timestamp desc
                val sortedFiles = filesList.sortedWith { a, b ->
                    val aFolderDate = extractFolderDate(a.remotePath)
                    val bFolderDate = extractFolderDate(b.remotePath)
                    when {
                        aFolderDate != null && bFolderDate != null -> {
                            // Both have folder dates - compare by folder day first (desc)
                            val dayCompare = bFolderDate.compareTo(aFolderDate)
                            if (dayCompare != 0) {
                                dayCompare
                            } else {
                                b.modificationTimestamp.compareTo(a.modificationTimestamp)
                            }
                        }

                        aFolderDate != null -> -1

                        // a has folder date, comes first
                        bFolderDate != null -> 1

                        // b has folder date, comes first
                        else -> b.modificationTimestamp.compareTo(a.modificationTimestamp)
                    }
                }
                GalleryItems(date, transformToRows(sortedFiles))
            }
            .sortedByDescending { it.date }
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder?, section: Int) = Unit

    override fun swapDirectory(
        user: User,
        directory: OCFile,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean,
        mLimitToMimeType: String
    ) = Unit

    override fun setHighlightedItem(file: OCFile) = Unit

    override fun setSortOrder(mFile: OCFile, sortOrder: FileSortOrder) = Unit

    fun cleanup() {
        ocFileListDelegate.cleanup()
    }
}
