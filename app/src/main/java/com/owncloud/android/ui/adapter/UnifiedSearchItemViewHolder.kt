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
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
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
    val binding: UnifiedSearchItemBinding,
    val user: User,
    val clientFactory: ClientFactory,
    private val storageManager: FileDataStorageManager,
    private val listInterface: UnifiedSearchListInterface,
    private val filesAction: FilesAction,
    val context: Context,
    private val viewThemeUtils: ViewThemeUtils
) :
    SectionedViewHolder(binding.root) {

    interface FilesAction {
        fun showFilesAction(searchResultEntry: SearchResultEntry)
    }

    fun bind(entry: SearchResultEntry) {
        binding.title.text = entry.title
        binding.subline.text = entry.subline

        if (entry.isFile && storageManager.getFileByDecryptedRemotePath(entry.remotePath()) != null) {
            binding.localFileIndicator.visibility = View.VISIBLE
        } else {
            binding.localFileIndicator.visibility = View.GONE
        }

        val mimetype = MimeTypeUtil.getBestMimeTypeByFilename(entry.title)

        val placeholder = getPlaceholder(entry, mimetype)

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
            binding.more.setOnClickListener { filesAction.showFilesAction(entry) }
        } else {
            binding.more.visibility = View.GONE
        }

        binding.unifiedSearchItemLayout.setOnClickListener { listInterface.onSearchResultClicked(entry) }
    }

    private fun getPlaceholder(entry: SearchResultEntry, mimetype: String?): Drawable {
        val drawable = with(entry.icon) {
            when {
                equals("icon-folder") ->
                    ResourcesCompat.getDrawable(context.resources, R.drawable.folder, null)
                startsWith("icon-note") ->
                    ResourcesCompat.getDrawable(context.resources, R.drawable.ic_edit, null)
                startsWith("icon-contacts") ->
                    ResourcesCompat.getDrawable(context.resources, R.drawable.file_vcard, null)
                startsWith("icon-calendar") ->
                    ResourcesCompat.getDrawable(context.resources, R.drawable.file_calendar, null)
                startsWith("icon-deck") ->
                    ResourcesCompat.getDrawable(context.resources, R.drawable.ic_deck, null)
                else ->
                    MimeTypeUtil.getFileTypeIcon(mimetype, entry.title, context, viewThemeUtils)
            }
        }
        return viewThemeUtils.platform.tintPrimaryDrawable(context, drawable)!!
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
