/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

import android.content.Context
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.bumptech.glide.Glide
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.UnifiedSearchItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.glide.CustomGlideStreamLoader

class UnifiedSearchItemViewHolder(
    val binding: UnifiedSearchItemBinding,
    val user: User,
    val clientFactory: ClientFactory,
    private val storageManager: FileDataStorageManager,
    private val listInterface: UnifiedSearchListInterface,
    val context: Context
) :
    SectionedViewHolder(binding.root) {

    fun bind(entry: SearchResultEntry) {
        binding.title.text = entry.title
        binding.subline.text = entry.subline

        storageManager.getFileByDecryptedRemotePath(entry.remotePath())?.let {
            if (it.isDown) {
                binding.localFileIndicator.visibility = View.VISIBLE
            } else {
                binding.localFileIndicator.visibility = View.GONE
            }
        }

        val mimetype = MimeTypeUtil.getBestMimeTypeByFilename(entry.title)

        val placeholder = if (entry.icon == "icon-folder") {
            ResourcesCompat.getDrawable(context.resources, R.drawable.folder, null)
        } else {
            MimeTypeUtil.getFileTypeIcon(mimetype, entry.title, context)
        }

        Glide.with(context).using(CustomGlideStreamLoader(user, clientFactory))
            .load(entry.thumbnailUrl)
            .placeholder(placeholder)
            .error(placeholder)
            .animate(android.R.anim.fade_in)
            .into(binding.thumbnail)

        binding.unifiedSearchItemLayout.setOnClickListener { listInterface.onSearchResultClicked(entry) }
    }
}
