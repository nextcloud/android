/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.annotation.VisibleForTesting
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.core.Clock
import com.owncloud.android.R
import com.owncloud.android.databinding.GridSyncItemBinding
import com.owncloud.android.databinding.SyncedFoldersEmptyBinding
import com.owncloud.android.databinding.SyncedFoldersFooterBinding
import com.owncloud.android.databinding.SyncedFoldersItemHeaderBinding
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncMediaThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.MediaThumbnailGenerationTask
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Adapter to display all auto-synced folders and/or instant upload media folders.
 */
@Suppress("LongParameterList")
class SyncedFolderAdapter(
    private val context: Context,
    private val clock: Clock,
    private val gridWidth: Int,
    private val clickListener: ClickListener,
    private val light: Boolean,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedRecyclerViewAdapter<SectionedViewHolder>() {

    private val gridTotal = gridWidth * 2
    private val syncFolderItems: MutableList<SyncedFolderDisplayItem> = ArrayList()
    private val filteredSyncFolderItems: MutableList<SyncedFolderDisplayItem> = ArrayList()
    private var hideItems = true
    private val thumbnailThreadPool: Executor = Executors.newCachedThreadPool()

    init {
        shouldShowHeadersForEmptySections(true)
        shouldShowFooters(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun toggleHiddenItemsVisibility() {
        hideItems = !hideItems

        filterHiddenItems(syncFolderItems, hideItems)?.let {
            filteredSyncFolderItems.clear()
            filteredSyncFolderItems.addAll(it)
            notifyDataSetChanged()
        }
    }

    fun setSyncFolderItems(syncFolderItems: List<SyncedFolderDisplayItem>) {
        this.syncFolderItems.clear()
        this.syncFolderItems.addAll(syncFolderItems)

        filterHiddenItems(this.syncFolderItems, hideItems)?.let {
            filteredSyncFolderItems.clear()
            filteredSyncFolderItems.addAll(it)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSyncFolderItem(location: Int, syncFolderItem: SyncedFolderDisplayItem) {
        if (hideItems && syncFolderItem.isHidden && filteredSyncFolderItems.contains(syncFolderItem)) {
            filteredSyncFolderItems.removeAt(location)
        } else {
            if (filteredSyncFolderItems.contains(syncFolderItem)) {
                filteredSyncFolderItems[filteredSyncFolderItems.indexOf(syncFolderItem)] = syncFolderItem
            } else {
                filteredSyncFolderItems.add(syncFolderItem)
            }
        }

        if (syncFolderItems.contains(syncFolderItem)) {
            syncFolderItems[syncFolderItems.indexOf(syncFolderItem)] = syncFolderItem
        } else {
            syncFolderItems.add(syncFolderItem)
        }

        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addSyncFolderItem(syncFolderItem: SyncedFolderDisplayItem) {
        syncFolderItems.add(syncFolderItem)

        // add item for display when either all items should be shown (!hideItems)
        // or if item should be shown (!.isHidden())
        if (!hideItems || !syncFolderItem.isHidden) {
            filteredSyncFolderItems.add(syncFolderItem)
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(section: Int) {
        if (filteredSyncFolderItems.contains(syncFolderItems[section])) {
            filteredSyncFolderItems.remove(syncFolderItems[section])
            notifyDataSetChanged()
        }

        syncFolderItems.removeAt(section)
    }

    /**
     * Filter for hidden items
     *
     * @param items Collection of items to filter
     * @return Non-hidden items
     */
    private fun filterHiddenItems(
        items: List<SyncedFolderDisplayItem>?,
        hide: Boolean
    ): List<SyncedFolderDisplayItem>? {
        if (!hide) {
            return items
        } else {
            val result: MutableList<SyncedFolderDisplayItem> = ArrayList()

            for (item in items!!) {
                if (!item.isHidden && !result.contains(item)) {
                    result.add(item)
                }
            }

            return result
        }
    }

    override fun getSectionCount(): Int {
        return if (filteredSyncFolderItems.size > 0) {
            filteredSyncFolderItems.size + 1
        } else {
            0
        }
    }

    @VisibleForTesting
    fun clear() {
        filteredSyncFolderItems.clear()
        syncFolderItems.clear()
    }

    val unfilteredSectionCount: Int
        get() = if (syncFolderItems.size > 0) {
            syncFolderItems.size + 1
        } else {
            0
        }

    override fun getItemCount(section: Int): Int {
        if (section < filteredSyncFolderItems.size) {
            val filePaths = filteredSyncFolderItems[section].filePaths

            return if (filePaths != null) {
                filteredSyncFolderItems[section].filePaths.size
            } else {
                1
            }
        } else {
            return 1
        }
    }

    fun get(section: Int): SyncedFolderDisplayItem? {
        return if (section in filteredSyncFolderItems.indices) {
            filteredSyncFolderItems[section]
        } else {
            null
        }
    }

    override fun getItemViewType(section: Int, relativePosition: Int, absolutePosition: Int): Int {
        return if (isLastSection(section)) {
            VIEW_TYPE_EMPTY
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun getHeaderViewType(section: Int): Int {
        return if (isLastSection(section)) {
            VIEW_TYPE_EMPTY
        } else {
            VIEW_TYPE_HEADER
        }
    }

    override fun getFooterViewType(section: Int): Int {
        return if (isLastSection(section) && showFooter()) {
            VIEW_TYPE_FOOTER
        } else {
            // only show footer after last item and only if folders have been hidden
            VIEW_TYPE_EMPTY
        }
    }

    private fun showFooter(): Boolean {
        return syncFolderItems.size > filteredSyncFolderItems.size
    }

    /**
     * returns the section of a synced folder for the given local path and type.
     *
     * @param localPath the local path of the synced folder
     * @param type      the of the synced folder
     * @return the section index of the looked up synced folder, `-1` if not present
     */
    fun getSectionByLocalPathAndType(localPath: String?, type: Int): Int {
        for (i in filteredSyncFolderItems.indices) {
            if (filteredSyncFolderItems[i].localPath.equals(localPath, ignoreCase = true) &&
                filteredSyncFolderItems[i].type.id == type
            ) {
                return i
            }
        }

        return -1
    }

    override fun onBindHeaderViewHolder(commonHolder: SectionedViewHolder, section: Int, expanded: Boolean) {
        if (section < filteredSyncFolderItems.size) {
            val holder = commonHolder as HeaderViewHolder
            holder.binding.headerContainer.visibility = View.VISIBLE

            holder.binding.title.text = filteredSyncFolderItems[section].folderName

            if (MediaFolderType.VIDEO == filteredSyncFolderItems[section].type) {
                holder.binding.type.setImageResource(R.drawable.video_32dp)
            } else if (MediaFolderType.IMAGE == filteredSyncFolderItems[section].type) {
                holder.binding.type.setImageResource(R.drawable.image_32dp)
            } else {
                holder.binding.type.setImageResource(R.drawable.folder_star_32dp)
            }

            holder.binding.syncStatusButton.visibility = View.VISIBLE
            holder.binding.syncStatusButton.tag = section
            holder.binding.syncStatusButton.setOnClickListener {
                filteredSyncFolderItems[section].setEnabled(
                    !filteredSyncFolderItems[section].isEnabled,
                    clock.currentTime
                )
                setSyncButtonActiveIcon(
                    holder.binding.syncStatusButton,
                    filteredSyncFolderItems[section].isEnabled
                )
                clickListener.onSyncStatusToggleClick(section, filteredSyncFolderItems[section])
            }
            setSyncButtonActiveIcon(holder.binding.syncStatusButton, filteredSyncFolderItems[section].isEnabled)

            if (light) {
                holder.binding.settingsButton.visibility = View.GONE
            } else {
                holder.binding.settingsButton.visibility = View.VISIBLE
                holder.binding.settingsButton.tag = section
                holder.binding.settingsButton.setOnClickListener { v: View ->
                    onOverflowIconClicked(
                        section,
                        filteredSyncFolderItems[section],
                        v
                    )
                }
            }
        }
    }

    private fun onOverflowIconClicked(section: Int, item: SyncedFolderDisplayItem, view: View) {
        val popup = PopupMenu(context, view).apply {
            inflate(R.menu.synced_folders_adapter)
            setOnMenuItemClickListener { i: MenuItem -> optionsItemSelected(i, section, item) }
            menu
                .findItem(R.id.action_auto_upload_folder_toggle_visibility)
                .setChecked(item.isHidden)
        }

        popup.show()
    }

    private fun optionsItemSelected(menuItem: MenuItem, section: Int, item: SyncedFolderDisplayItem): Boolean {
        if (menuItem.itemId == R.id.action_auto_upload_folder_toggle_visibility) {
            clickListener.onVisibilityToggleClick(section, item)
        } else {
            // default: R.id.action_create_custom_folder
            clickListener.onSyncFolderSettingsClick(section, item)
        }
        return true
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder, section: Int) {
        if (isLastSection(section) && showFooter()) {
            val footerHolder = holder as FooterViewHolder
            footerHolder.binding.footerText.setOnClickListener { toggleHiddenItemsVisibility() }
            footerHolder.binding.footerText.text = context.resources.getQuantityString(
                R.plurals.synced_folders_show_hidden_folders,
                hiddenFolderCount,
                hiddenFolderCount
            )
        }
    }

    override fun onBindViewHolder(
        commonHolder: SectionedViewHolder,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (section < filteredSyncFolderItems.size && filteredSyncFolderItems[section].filePaths != null) {
            val holder = commonHolder as MainViewHolder

            val file = File(filteredSyncFolderItems[section].filePaths[relativePosition])

            val task =
                MediaThumbnailGenerationTask(
                    holder.binding.thumbnail,
                    context,
                    viewThemeUtils
                )

            val asyncDrawable =
                AsyncMediaThumbnailDrawable(
                    context.resources,
                    ThumbnailsCacheManager.mDefaultImg
                )
            holder.binding.thumbnail.setImageDrawable(asyncDrawable)

            task.executeOnExecutor(thumbnailThreadPool, file)

            // set proper tag
            holder.binding.thumbnail.tag = file.hashCode()

            holder.itemView.tag = relativePosition % gridWidth

            if (filteredSyncFolderItems[section].numberOfFiles > gridTotal &&
                relativePosition >= gridTotal - 1
            ) {
                holder.binding.counter.text = String.format(
                    Locale.US,
                    "%d",
                    filteredSyncFolderItems[section].numberOfFiles - gridTotal
                )
                holder.binding.counterLayout.visibility = View.VISIBLE
                holder.binding.thumbnailDarkener.visibility = View.VISIBLE
            } else {
                holder.binding.counterLayout.visibility = View.GONE
                holder.binding.thumbnailDarkener.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    SyncedFoldersItemHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_FOOTER -> {
                FooterViewHolder(
                    SyncedFoldersFooterBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_EMPTY -> {
                EmptyViewHolder(
                    SyncedFoldersEmptyBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                MainViewHolder(
                    GridSyncItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    private fun isLastSection(section: Int): Boolean {
        return section >= sectionCount - 1
    }

    val hiddenFolderCount: Int
        get() = syncFolderItems.size - filteredSyncFolderItems.size

    interface ClickListener {
        fun onSyncStatusToggleClick(section: Int, syncedFolderDisplayItem: SyncedFolderDisplayItem?)
        fun onSyncFolderSettingsClick(section: Int, syncedFolderDisplayItem: SyncedFolderDisplayItem?)
        fun onVisibilityToggleClick(section: Int, item: SyncedFolderDisplayItem?)
    }

    internal class HeaderViewHolder(var binding: SyncedFoldersItemHeaderBinding) : SectionedViewHolder(
        binding.root
    )

    internal class FooterViewHolder(var binding: SyncedFoldersFooterBinding) : SectionedViewHolder(
        binding.root
    )

    internal class EmptyViewHolder(binding: SyncedFoldersEmptyBinding) : SectionedViewHolder(binding.root)

    internal class MainViewHolder(var binding: GridSyncItemBinding) : SectionedViewHolder(
        binding.root
    )

    private fun setSyncButtonActiveIcon(syncStatusButton: ImageButton, enabled: Boolean) {
        if (enabled) {
            syncStatusButton.setImageDrawable(
                viewThemeUtils.platform.tintDrawable(context, R.drawable.ic_cloud_sync_on, ColorRole.PRIMARY)
            )
        } else {
            syncStatusButton.setImageResource(R.drawable.ic_cloud_sync_off)
        }
    }

    companion object {
        private const val VIEW_TYPE_EMPTY = Int.MAX_VALUE
        private const val VIEW_TYPE_ITEM = 1
        private const val VIEW_TYPE_HEADER = 2
        private const val VIEW_TYPE_FOOTER = 3
    }
}
