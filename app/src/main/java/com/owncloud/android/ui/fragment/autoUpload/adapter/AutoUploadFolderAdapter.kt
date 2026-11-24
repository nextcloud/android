/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.thumbnail.job.MediaThumbnailGenerationJob
import com.owncloud.android.R
import com.owncloud.android.databinding.GridSyncItemBinding
import com.owncloud.android.databinding.SyncedFoldersEmptyBinding
import com.owncloud.android.databinding.SyncedFoldersFooterBinding
import com.owncloud.android.databinding.SyncedFoldersItemHeaderBinding
import com.owncloud.android.datamodel.AutoUploadFolder
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class AutoUploadFolderAdapter(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val context: Context,
    private val clock: Clock,
    private val clickListener: AutoUploadFolderClickListener,
    private val light: Boolean,
    private val viewThemeUtils: ViewThemeUtils,
    private val powerManagementService: PowerManagementService,
    private val connectivityService: ConnectivityService
) : SectionedRecyclerViewAdapter<SectionedViewHolder>() {

    private val mediaThumbnailGenerationJob = MediaThumbnailGenerationJob(context, viewThemeUtils)

    private var items: List<AutoUploadFolder> = emptyList()
    private var visibleItems: List<AutoUploadFolder> = emptyList()
    private var hideHiddenItems = true

    val hasItems: Boolean
        get() = items.isNotEmpty()

    val hasVisibleItems: Boolean
        get() = visibleItems.isNotEmpty()

    val hiddenFolderCount: Int
        get() = items.size - visibleItems.size

    init {
        shouldShowHeadersForEmptySections(true)
        shouldShowFooters(true)
    }

    fun setItems(newItems: List<AutoUploadFolder>) {
        items = newItems
        refreshVisibleItems()
    }

    fun addItem(item: AutoUploadFolder) {
        items = items + item
        refreshVisibleItems()
    }

    fun removeItem(item: AutoUploadFolder) {
        items = items - item
        refreshVisibleItems()
    }

    @VisibleForTesting
    fun clear() {
        setItems(emptyList())
    }

    fun toggleHiddenItemsVisibility() {
        hideHiddenItems = !hideHiddenItems
        refreshVisibleItems()
    }

    fun onItemVisibilityChanged(item: AutoUploadFolder) {
        if (item.isHidden) {
            hideHiddenItems = true
        }

        refreshVisibleItems()
    }

    fun getItem(section: Int): AutoUploadFolder? = visibleItems.getOrNull(section)

    fun getSectionById(id: Long): Int = visibleItems.indexOfFirst { it.id == id }

    fun getSectionByLocalPathAndType(localPath: String?, type: Int): Int = visibleItems.indexOfFirst {
        it.localPath.equals(localPath, ignoreCase = true) && it.type.id == type
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refreshVisibleItems() {
        visibleItems = if (hideHiddenItems) items.filterNot { it.isHidden } else items
        notifyDataSetChanged()
    }

    private fun isFooterSection(section: Int): Boolean = section >= visibleItems.size

    override fun getSectionCount(): Int = if (visibleItems.isEmpty()) 0 else visibleItems.size + 1

    override fun getItemCount(section: Int): Int = visibleItems.getOrNull(section)?.filePaths?.size ?: 0

    override fun getItemViewType(section: Int, relativePosition: Int, absolutePosition: Int): Int =
        AutoUploadFolderAdapterType.ITEM.viewType

    override fun getHeaderViewType(section: Int): Int {
        val type = if (isFooterSection(section)) {
            AutoUploadFolderAdapterType.EMPTY
        } else {
            AutoUploadFolderAdapterType.HEADER
        }

        return type.viewType
    }

    override fun getFooterViewType(section: Int): Int {
        val type = when {
            isFooterSection(section) && hiddenFolderCount > 0 -> AutoUploadFolderAdapterType.FOOTER
            isEmptyFolder(section) -> AutoUploadFolderAdapterType.EMPTY_FOLDER
            else -> AutoUploadFolderAdapterType.EMPTY
        }

        return type.viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (AutoUploadFolderAdapterType.fromViewType(viewType)) {
            AutoUploadFolderAdapterType.ITEM -> ItemViewHolder(GridSyncItemBinding.inflate(inflater, parent, false))

            AutoUploadFolderAdapterType.HEADER -> createHeaderViewHolder(inflater, parent)

            AutoUploadFolderAdapterType.FOOTER ->
                FooterViewHolder(SyncedFoldersFooterBinding.inflate(inflater, parent, false))

            AutoUploadFolderAdapterType.EMPTY -> EmptyViewHolder(Space(parent.context))

            AutoUploadFolderAdapterType.EMPTY_FOLDER -> EmptyFolderViewHolder(
                SyncedFoldersEmptyBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )
        }
    }

    private fun createHeaderViewHolder(inflater: LayoutInflater, parent: ViewGroup) = AutoUploadFolderHeaderViewHolder(
        SyncedFoldersItemHeaderBinding.inflate(inflater, parent, false),
        lifecycleScope,
        clock,
        light,
        viewThemeUtils,
        clickListener,
        connectivityService,
        powerManagementService
    )

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder, section: Int, expanded: Boolean) {
        if (holder !is AutoUploadFolderHeaderViewHolder) {
            return
        }

        val item = visibleItems.getOrNull(section) ?: return
        holder.bind(item, visibleItems)
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder, section: Int) {
        if (holder !is FooterViewHolder) {
            return
        }

        holder.binding.footerText.text = context.resources.getQuantityString(
            R.plurals.synced_folders_show_hidden_folders,
            hiddenFolderCount,
            hiddenFolderCount
        )
        holder.binding.footerText.setOnClickListener { toggleHiddenItemsVisibility() }
    }

    private fun isEmptyFolder(section: Int): Boolean {
        val item = visibleItems.getOrNull(section) ?: return false

        return item.filePaths.isNullOrEmpty()
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (holder !is ItemViewHolder) {
            return
        }

        val item = visibleItems.getOrNull(section) ?: return
        val filePath = item.filePaths?.getOrNull(relativePosition) ?: return
        bindThumbnail(holder.binding, item, File(filePath), relativePosition)
    }

    private fun bindThumbnail(binding: GridSyncItemBinding, item: AutoUploadFolder, file: File, position: Int) =
        with(binding) {
            thumbnail.setImageBitmap(ThumbnailsCacheManager.mDefaultImg)
            thumbnail.tag = file.hashCode()
            lifecycleScope.launch { mediaThumbnailGenerationJob.load(file, thumbnail) }

            val displayedFileCount = item.filePaths?.size ?: 0
            val remainingFileCount = item.numberOfFiles - displayedFileCount
            val showRemainingFileCount = remainingFileCount > 0 && position == displayedFileCount - 1
            counter.text = String.format(Locale.US, "%d", remainingFileCount)
            counterLayout.isVisible = showRemainingFileCount
            thumbnailDarkener.isVisible = showRemainingFileCount
        }

    private class ItemViewHolder(val binding: GridSyncItemBinding) : SectionedViewHolder(binding.root)

    private class FooterViewHolder(val binding: SyncedFoldersFooterBinding) : SectionedViewHolder(binding.root)

    private class EmptyViewHolder(view: View) : SectionedViewHolder(view)

    private class EmptyFolderViewHolder(binding: SyncedFoldersEmptyBinding) : SectionedViewHolder(binding.root)
}
