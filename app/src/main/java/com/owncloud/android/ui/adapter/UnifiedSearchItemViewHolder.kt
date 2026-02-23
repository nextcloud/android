/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.ImageViewCompat
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.bumptech.glide.Glide
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.common.NextcloudClient
import com.nextcloud.model.SearchResultEntryType
import com.nextcloud.utils.CalendarEventManager
import com.nextcloud.utils.ContactManager
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.extensions.getType
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.UnifiedSearchItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.overlay.OverlayManager
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class UnifiedSearchItemViewHolder(
    private val supportsOpeningCalendarContactsLocally: Boolean,
    val binding: UnifiedSearchItemBinding,
    private val storageManager: FileDataStorageManager,
    private val listInterface: UnifiedSearchListInterface,
    private val filesAction: FilesAction,
    val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    private val overlayManager: OverlayManager
) : SectionedViewHolder(binding.root) {

    interface FilesAction {
        fun showFilesAction(searchResultEntry: SearchResultEntry)
        fun loadFileThumbnail(searchResultEntry: SearchResultEntry, onClientReady: (NextcloudClient) -> Unit)
    }

    private val contactManager = ContactManager(context)
    private val calendarEventManager = CalendarEventManager(context)

    fun bind(entry: SearchResultEntry) {
        binding.title.text = entry.title
        bindSubline(entry)
        bindLocalFileIndicator(entry)

        val entryType = entry.getType()
        bindThumbnail(entry, entryType)
        bindMoreButton(entry)
        binding.unifiedSearchItemLayout.setOnClickListener {
            searchEntryOnClick(entry, entryType)
        }
    }

    private fun bindSubline(entry: SearchResultEntry) {
        if (entry.subline.isNotBlank()) {
            binding.subline.visibility = View.VISIBLE
            binding.subline.text = entry.subline
        } else {
            binding.subline.visibility = View.GONE

            val paddingInDp = context.resources.getDimension(R.dimen.standard_padding)
            val paddingInPx = DisplayUtils.convertDpToPixel(paddingInDp, context)
            binding.titleContainer.setPadding(0, paddingInPx, 0, 0)
        }
    }

    private fun bindLocalFileIndicator(entry: SearchResultEntry) {
        val showLocalFileIndicator =
            (entry.isFile && storageManager.getFileByDecryptedRemotePath(entry.remotePath()) != null)
        binding.localFileIndicator.setVisibleIf(showLocalFileIndicator)
    }

    private fun bindThumbnail(entry: SearchResultEntry, entryType: SearchResultEntryType) {
        // FIXME: ic_find_in_page not visible
        Glide.with(context).clear(binding.thumbnail)
        binding.thumbnail.drawable?.mutate()?.let { DrawableCompat.setTintList(it, null) }
        binding.thumbnail.clearColorFilter()
        ImageViewCompat.setImageTintList(binding.thumbnail, null)
        binding.thumbnail.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_find_in_page))
        binding.thumbnailOverlayIcon.setVisibleIf(false)

        if (entry.isFile) {
            val file = storageManager.getFileByRemotePath(entry.remotePath())

            if (file?.isFolder == true) {
                binding.thumbnail.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.folder))
                viewThemeUtils.platform.colorImageView(binding.thumbnail, ColorRole.PRIMARY)
                overlayManager.setFolderOverlayIcon(file, binding.thumbnailOverlayIcon)
                return
            }

            if (file != null) {
                val icon = MimeTypeUtil.getFileTypeIcon(
                    file.mimeType,
                    file.fileName,
                    context,
                    viewThemeUtils
                )
                binding.thumbnail.setImageDrawable(icon)
                binding.thumbnail.clearColorFilter()
                return
            }
        }

        // For Settings, Apps, Contacts, Calendar, etc.
        filesAction.loadFileThumbnail(entry) { client ->
            GlideHelper.loadIntoImageView(
                context,
                client,
                entry.thumbnailUrl,
                binding.thumbnail,
                entryType.iconId(),
                circleCrop = entry.rounded
            )
        }
    }

    private fun bindMoreButton(entry: SearchResultEntry) {
        if (entry.isFile) {
            binding.more.visibility = View.VISIBLE
            binding.more.setOnClickListener {
                filesAction.showFilesAction(entry)
            }
        } else {
            binding.more.visibility = View.GONE
        }
    }

    private fun searchEntryOnClick(entry: SearchResultEntry, entryType: SearchResultEntryType) {
        if (supportsOpeningCalendarContactsLocally) {
            when (entryType) {
                SearchResultEntryType.Contact -> {
                    contactManager.openContact(entry, listInterface)
                }

                SearchResultEntryType.CalendarEvent -> {
                    calendarEventManager.openCalendarEvent(entry, listInterface)
                }

                else -> {
                    listInterface.onSearchResultClicked(entry)
                }
            }
        } else {
            listInterface.onSearchResultClicked(entry)
        }
    }
}
