/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.UnifiedSearchCurrentDirectoryItemBinding
import com.owncloud.android.databinding.UnifiedSearchEmptyBinding
import com.owncloud.android.databinding.UnifiedSearchFooterBinding
import com.owncloud.android.databinding.UnifiedSearchHeaderBinding
import com.owncloud.android.databinding.UnifiedSearchItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

/**
 * This Adapter populates a SectionedRecyclerView with search results by unified search
 */
@Suppress("LongParameterList")
class UnifiedSearchListAdapter(
    private val supportsOpeningCalendarContactsLocally: Boolean,
    private val storageManager: FileDataStorageManager,
    private val listInterface: UnifiedSearchListInterface,
    private val filesAction: UnifiedSearchItemViewHolder.FilesAction,
    private val user: User,
    private val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    private val appPreferences: AppPreferences,
    private val syncedFolderProvider: SyncedFolderProvider
) : SectionedRecyclerViewAdapter<SectionedViewHolder>() {
    companion object {
        private const val VIEW_TYPE_EMPTY = Int.MAX_VALUE
        private const val VIEW_TYPE_CURRENT_DIR = 0
    }

    private var currentDirItems: List<OCFile> = listOf()
    private var sections: List<UnifiedSearchSection> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = UnifiedSearchHeaderBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
                UnifiedSearchHeaderViewHolder(binding, viewThemeUtils, context)
            }
            VIEW_TYPE_FOOTER -> {
                val binding = UnifiedSearchFooterBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
                UnifiedSearchFooterViewHolder(binding, context, listInterface)
            }
            VIEW_TYPE_ITEM -> {
                val binding = UnifiedSearchItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
                UnifiedSearchItemViewHolder(
                    supportsOpeningCalendarContactsLocally,
                    binding,
                    user,
                    storageManager,
                    listInterface,
                    filesAction,
                    context,
                    viewThemeUtils
                )
            }
            VIEW_TYPE_CURRENT_DIR -> {
                val isRTL = DisplayUtils.isRTL()
                val binding = UnifiedSearchCurrentDirectoryItemBinding.inflate(layoutInflater, parent, false)
                UnifiedSearchCurrentDirItemViewHolder(
                    binding,
                    context,
                    viewThemeUtils,
                    storageManager,
                    isRTL,
                    user,
                    appPreferences,
                    syncedFolderProvider
                )
            }
            VIEW_TYPE_EMPTY -> {
                val binding = UnifiedSearchEmptyBinding.inflate(layoutInflater, parent, false)
                EmptyViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    private fun isCurrentDirItem(section: Int): Boolean = (currentDirItems.isNotEmpty() && section == 0)

    private fun getIndex(section: Int): Int = if (currentDirItems.isNotEmpty()) section - 1 else section

    internal class EmptyViewHolder(binding: UnifiedSearchEmptyBinding) : SectionedViewHolder(binding.getRoot())

    override fun getSectionCount(): Int = (if (currentDirItems.isNotEmpty()) 1 else 0) + sections.size

    override fun getItemViewType(section: Int, relativePosition: Int, absolutePosition: Int): Int =
        if (isCurrentDirItem(section)) {
            VIEW_TYPE_CURRENT_DIR
        } else {
            VIEW_TYPE_ITEM
        }

    override fun getItemCount(section: Int): Int = if (isCurrentDirItem(section)) {
        currentDirItems.size
    } else {
        val index = if (currentDirItems.isNotEmpty()) section - 1 else section
        sections.getOrNull(index)?.entries?.size ?: 0
    }

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder, section: Int, expanded: Boolean) {
        if (holder is UnifiedSearchHeaderViewHolder) {
            if (isCurrentDirItem(section)) {
                val name = context.getString(R.string.unified_search_fragment_search_in_this_folder)
                val currentDirUnifiedSearchSection = UnifiedSearchSection("", name, listOf(), false)
                holder.bind(currentDirUnifiedSearchSection)
            } else {
                val index = getIndex(section)
                val sectionData = sections.getOrNull(index) ?: return
                holder.bind(sectionData)
            }
        }
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder, section: Int) {
        if (isCurrentDirItem(section)) {
            return
        }

        val index = getIndex(section)
        val sectionData = sections.getOrNull(index) ?: return

        if (sectionData.hasMoreResults && holder is UnifiedSearchFooterViewHolder) {
            holder.bind(sectionData)
        }
    }

    override fun getFooterViewType(section: Int): Int {
        if (isCurrentDirItem(section)) {
            return VIEW_TYPE_EMPTY
        }

        val index = getIndex(section)
        val sectionData = sections.getOrNull(index)

        return when {
            sectionData?.hasMoreResults == true -> VIEW_TYPE_FOOTER
            else -> VIEW_TYPE_EMPTY
        }
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (isCurrentDirItem(section) && holder is UnifiedSearchCurrentDirItemViewHolder) {
            val entry = currentDirItems.getOrNull(relativePosition) ?: return
            holder.bind(entry)
        } else if (holder is UnifiedSearchItemViewHolder) {
            val index = getIndex(section)
            val entry = sections.getOrNull(index)?.entries?.getOrNull(relativePosition) ?: return
            holder.bind(entry)
        }
    }

    override fun onViewAttachedToWindow(holder: SectionedViewHolder) {
        if (holder is UnifiedSearchItemViewHolder) {
            holder.binding.thumbnailShimmer.run {
                if (isVisible) {
                    setImageResource(R.drawable.background)
                    resetLoader()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(sections: List<UnifiedSearchSection>) {
        this.sections = sections
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setDataCurrentDirItems(currentDirItems: List<OCFile>) {
        this.currentDirItems = currentDirItems
        notifyDataSetChanged()
    }

    init {
        // initialise thumbnails cache on background thread
        ThumbnailsCacheManager.initDiskCacheAsync()
    }
}
