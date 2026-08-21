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
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.nextcloud.utils.thumbnail.ThumbnailArguments
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.UnifiedSearchItemBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchEntry
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class UnifiedSearchItemViewHolder(
    private val supportsOpeningCalendarContactsLocally: Boolean,
    val binding: UnifiedSearchItemBinding,
    private val listInterface: UnifiedSearchListInterface,
    private val filesAction: FilesAction,
    val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val isE2EEActivate: Boolean
) : SectionedViewHolder(binding.root) {

    interface FilesAction {
        fun showFilesAction(searchResultEntry: SearchResultEntry)
        fun loadFileThumbnail(searchResultEntry: SearchResultEntry, onClientReady: (NextcloudClient) -> Unit)
    }

    private val contactManager = ContactManager(context)
    private val calendarEventManager = CalendarEventManager(context)

    fun bind(unifiedSearchEntry: UnifiedSearchEntry) {
        val entry = unifiedSearchEntry.searchResult
        val file = unifiedSearchEntry.localFile

        bindTextView(binding.title, entry.title)
        bindTextView(binding.subline, entry.subline)
        bindLocalFileIndicator(entry, file)

        val entryType = entry.getType()
        bindThumbnail(entry, file, entryType)
        bindMoreButton(entry, file)
        binding.unifiedSearchItemLayout.setOnClickListener {
            searchEntryOnClick(entry, entryType)
        }
    }

    private fun bindTextView(view: TextView, text: String?) {
        if (text.isNullOrEmpty()) {
            view.visibility = View.GONE
        } else {
            view.visibility = View.VISIBLE
            view.text = text
        }
    }

    private fun bindLocalFileIndicator(entry: SearchResultEntry, file: OCFile?) {
        binding.localFileIndicator.setVisibleIf(entry.isFile && file != null)
    }

    private fun bindThumbnail(entry: SearchResultEntry, file: OCFile?, entryType: SearchResultEntryType) {
        Glide.with(context).clear(binding.thumbnail)
        binding.thumbnailOverlayIcon.setVisibleIf(false)

        when {
            file != null && file.isFolder -> bindFolderThumbnail(file)
            file != null && !file.isFolder -> bindLocalFileThumbnail(file)
            else -> bindRemoteThumbnail(entry, entryType)
        }
    }

    private fun bindFolderThumbnail(file: OCFile) {
        binding.thumbnail.apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.folder))
            viewThemeUtils.platform.colorImageView(this, ColorRole.PRIMARY)
        }
        thumbnailGenerator.folderThumbnailGenerator.setFolderOverlayIcon(file, binding.thumbnailOverlayIcon)
    }

    private fun bindLocalFileThumbnail(file: OCFile) {
        if (file.remoteId == null || !file.isPreviewAvailable) {
            val icon = MimeTypeUtil.getFileTypeIcon(file.mimeType, file.fileName, context, viewThemeUtils)
            binding.thumbnail.apply {
                setImageDrawable(icon)
                clearColorFilter()
                ImageViewCompat.setImageTintList(this, null)
            }
        } else {
            thumbnailGenerator.setThumbnail(
                file,
                binding.thumbnail,
                ThumbnailArguments.withShimmer(binding.thumbnailShimmer)
            )
        }
    }

    private fun bindRemoteThumbnail(entry: SearchResultEntry, entryType: SearchResultEntryType) {
        binding.thumbnail.apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_find_in_page))
            viewThemeUtils.platform.colorImageView(this, ColorRole.SECONDARY)
        }

        if (entry.thumbnailUrl.isNotBlank()) {
            filesAction.loadFileThumbnail(entry) { client ->
                if (entryType == SearchResultEntryType.Avatar) {
                    GlideHelper.loadCircularBitmapIntoImageView(
                        context,
                        entry.thumbnailUrl,
                        binding.thumbnail,
                        ContextCompat.getDrawable(context, R.drawable.ic_user)
                    )
                } else {
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
        } else {
            binding.thumbnail.setImageDrawable(ContextCompat.getDrawable(context, entryType.iconId()))
        }
    }

    private fun bindMoreButton(entry: SearchResultEntry, file: OCFile?) {
        val isEncryptedWithoutKeys = file?.isEncrypted == true && !isE2EEActivate
        binding.more.setVisibleIf(entry.isFile && !isEncryptedWithoutKeys)

        binding.more.setOnClickListener {
            filesAction.showFilesAction(entry)
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
