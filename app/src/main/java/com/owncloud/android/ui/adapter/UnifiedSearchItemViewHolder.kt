/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.model.SearchResultEntryType
import com.nextcloud.utils.CalendarEventManager
import com.nextcloud.utils.ContactManager
import com.nextcloud.utils.extensions.getType
import com.owncloud.android.databinding.UnifiedSearchItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.glide.CustomGlideStreamLoader
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class UnifiedSearchItemViewHolder(
    private val supportsOpeningCalendarContactsLocally: Boolean,
    val binding: UnifiedSearchItemBinding,
    val user: User,
    val clientFactory: ClientFactory,
    private val storageManager: FileDataStorageManager,
    private val listInterface: UnifiedSearchListInterface,
    private val filesAction: FilesAction,
    val context: Context,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedViewHolder(binding.root) {

    interface FilesAction {
        fun showFilesAction(searchResultEntry: SearchResultEntry)
    }

    private val contactManager = ContactManager(context)
    private val calendarEventManager = CalendarEventManager(context)

    fun bind(entry: SearchResultEntry) {
        binding.title.text = entry.title
        binding.subline.text = entry.subline

        if (entry.isFile && storageManager.getFileByDecryptedRemotePath(entry.remotePath()) != null) {
            binding.localFileIndicator.visibility = View.VISIBLE
        } else {
            binding.localFileIndicator.visibility = View.GONE
        }

        val mimetype = MimeTypeUtil.getBestMimeTypeByFilename(entry.title)

        val entryType = entry.getType()
        val placeholder = getPlaceholder(entry, entryType, mimetype)

        Glide.with(context).using(CustomGlideStreamLoader(user, clientFactory))
            .load(entry.thumbnailUrl)
            .asBitmap()
            .placeholder(placeholder)
            .error(placeholder)
            .animate(android.R.anim.fade_in)
            .listener(RoundIfNeededListener(entry))
            .into(binding.thumbnail)

        if (entry.isFile) {
            binding.more.visibility = View.VISIBLE
            binding.more.setOnClickListener {
                filesAction.showFilesAction(entry)
            }
        } else {
            binding.more.visibility = View.GONE
        }

        binding.unifiedSearchItemLayout.setOnClickListener {
            searchEntryOnClick(entry, entryType)
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

    private fun getPlaceholder(
        entry: SearchResultEntry,
        entryType: SearchResultEntryType,
        mimetype: String?
    ): Drawable {
        val iconId = entryType.run {
            iconId()
        }

        val defaultDrawable = MimeTypeUtil.getFileTypeIcon(mimetype, entry.title, context, viewThemeUtils)
        val drawable: Drawable = ResourcesCompat.getDrawable(context.resources, iconId, null) ?: defaultDrawable
        return viewThemeUtils.platform.tintDrawable(context, drawable, ColorRole.PRIMARY)
    }

    private inner class RoundIfNeededListener(private val entry: SearchResultEntry) :
        RequestListener<String, Bitmap> {
        override fun onException(
            e: Exception?,
            model: String?,
            target: Target<Bitmap>?,
            isFirstResource: Boolean
        ): Boolean = false

        override fun onResourceReady(
            resource: Bitmap?,
            model: String?,
            target: Target<Bitmap>?,
            isFromMemoryCache: Boolean,
            isFirstResource: Boolean
        ): Boolean {
            if (entry.rounded) {
                val drawable = BitmapUtils.bitmapToCircularBitmapDrawable(context.resources, resource)
                binding.thumbnail.setImageDrawable(drawable)
                return true
            }
            return false
        }
    }
}
