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

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.owncloud.android.databinding.ListItemBinding
import com.owncloud.android.ui.AvatarGroupLayout

internal class OCFileListItemViewHolder(private var binding: ListItemBinding) :
    RecyclerView.ViewHolder(
        binding.root
    ),
    ListItemViewHolder {
    override val fileSize: TextView
        get() = binding.fileSize
    override val fileSizeSeparator: View
        get() = binding.fileSeparator
    override val lastModification: TextView
        get() = binding.lastMod
    override val overflowMenu: ImageView
        get() = binding.overflowMenu
    override val sharedAvatars: AvatarGroupLayout
        get() = binding.sharedAvatars
    override val fileName: TextView
        get() = binding.Filename
    override val thumbnail: ImageView
        get() = binding.thumbnailLayout.thumbnail

    override fun showVideoOverlay() {
        binding.thumbnailLayout.videoOverlay.visibility = View.VISIBLE
    }

    override val shimmerThumbnail: LoaderImageView
        get() = binding.thumbnailLayout.thumbnailShimmer
    override val favorite: ImageView
        get() = binding.favoriteAction
    override val localFileIndicator: ImageView
        get() = binding.localFileIndicator
    override val shared: ImageView
        get() = binding.sharedIcon
    override val checkbox: ImageView
        get() = binding.customCheckbox
    override val itemLayout: View
        get() = binding.ListItemLayout
    override val unreadComments: ImageView
        get() = binding.unreadComments

    init {
        binding.favoriteAction.drawable.mutate()
    }
}
