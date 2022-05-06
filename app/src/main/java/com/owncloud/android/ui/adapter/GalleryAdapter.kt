/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.databinding.GalleryHeaderBinding
import com.owncloud.android.databinding.GridImageBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryItems
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.FileStorageUtils
import me.zhanghai.android.fastscroll.PopupTextProvider
import java.util.Calendar
import java.util.Date

class GalleryAdapter(
    val context: Context,
    user: User,
    ocFileListFragmentInterface: OCFileListFragmentInterface,
    preferences: AppPreferences,
    transferServiceGetter: ComponentsGetter
) : SectionedRecyclerViewAdapter<SectionedViewHolder>(), CommonOCFileListAdapterInterface, PopupTextProvider {
    var files: List<GalleryItems> = mutableListOf()
    private var ocFileListDelegate: OCFileListDelegate
    private var storageManager: FileDataStorageManager

    init {
        storageManager = transferServiceGetter.storageManager

        ocFileListDelegate = OCFileListDelegate(
            context,
            ocFileListFragmentInterface,
            user,
            storageManager,
            false,
            preferences,
            true,
            transferServiceGetter,
            showMetadata = false,
            showShareAvatar = false
        )
    }

    override fun showFooters(): Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            GalleryHeaderViewHolder(
                GalleryHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            GalleryItemViewHolder(
                GridImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (holder != null) {
            val itemViewHolder = holder as GalleryItemViewHolder
            val ocFile = files[section].files[relativePosition]

            ocFileListDelegate.bindGridViewHolder(itemViewHolder, ocFile)
        }
    }

    override fun getItemCount(section: Int): Int {
        return files[section].files.size
    }

    override fun getSectionCount(): Int {
        return files.size
    }

    override fun getPopupText(position: Int): String {
        return DisplayUtils.getDateByPattern(
            files[getRelativePosition(position).section()].date,
            context,
            DisplayUtils.MONTH_YEAR_PATTERN
        )
    }

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
                context, DisplayUtils.YEAR_PATTERN
            )
        }
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder?, section: Int) {
        TODO("Not yet implemented")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showAllGalleryItems() {
        val items = storageManager.allGalleryItems

        files = items
            .groupBy { firstOfMonth(it.modificationTimestamp) }
            .map { GalleryItems(it.key, FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(it.value)) }
            .sortedBy { it.date }.reversed()

        Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
    }

    private fun firstOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.time = Date(timestamp)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)

        return cal.timeInMillis
    }

    fun isEmpty(): Boolean {
        return files.isEmpty()
    }

    fun getItem(position: Int): OCFile {
        val itemCoord = getRelativePosition(position)

        return files[itemCoord.section()].files[itemCoord.relativePos()]
    }

    override fun isMultiSelect(): Boolean {
        return ocFileListDelegate.isMultiSelect
    }

    override fun cancelAllPendingTasks() {
        ocFileListDelegate.cancelAllPendingTasks()
    }

    override fun getItemPosition(file: OCFile): Int {
        val item = files.find { it.files.contains(file) }
        return getAbsolutePosition(files.indexOf(item), item?.files?.indexOf(file) ?: 0)
    }

    override fun swapDirectory(
        user: User,
        directory: OCFile,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean,
        mLimitToMimeType: String
    ) {
        TODO("Not yet implemented")
    }

    override fun setHighlightedItem(file: OCFile) {
        TODO("Not yet implemented")
    }

    override fun setSortOrder(mFile: OCFile, sortOrder: FileSortOrder) {
        TODO("Not yet implemented")
    }

    override fun addCheckedFile(file: OCFile) {
        ocFileListDelegate.addCheckedFile(file)
    }

    override fun isCheckedFile(file: OCFile): Boolean {
        return ocFileListDelegate.isCheckedFile(file)
    }

    override fun getCheckedItems(): Set<OCFile> {
        return ocFileListDelegate.checkedItems
    }

    override fun removeCheckedFile(file: OCFile) {
        ocFileListDelegate.removeCheckedFile(file)
    }

    override fun notifyItemChanged(file: OCFile) {
        notifyItemChanged(getItemPosition(file))
    }

    override fun getFilesCount(): Int {
        return files.fold(0) { acc, item -> acc + item.files.size }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun setMultiSelect(boolean: Boolean) {
        ocFileListDelegate.isMultiSelect = boolean
        notifyDataSetChanged()
    }

    override fun clearCheckedItems() {
        ocFileListDelegate.clearCheckedItems()
    }

    @VisibleForTesting
    fun addFiles(items: List<GalleryItems>) {
        files = items
    }
}
