/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.owncloud.android.databinding.ListItemBinding
import com.owncloud.android.ui.AvatarGroupLayout

class OCFileListItemViewHolder(private var binding: ListItemBinding) :
    RecyclerView.ViewHolder(
        binding.root
    ),
    ListItemViewHolder {
    override val gridLivePhotoIndicator: ImageView?
        get() = null
    override val livePhotoIndicator: TextView
        get() = binding.livePhotoIndicator
    override val livePhotoIndicatorSeparator: TextView
        get() = binding.livePhotoIndicatorSeparator

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
    override val extension: TextView
        get() = binding.extension
    override val thumbnail: ImageView
        get() = binding.thumbnailLayout.thumbnail
    override val tagsGroup: ChipGroup
        get() = binding.tagsGroup
    override val firstTag: Chip
        get() = binding.firstTag
    override val secondTag: Chip
        get() = binding.secondTag
    override val tagMore: Chip
        get() = binding.tagMore
    override val fileDetailGroup: LinearLayout
        get() = binding.fileDetailGroup

    override fun showVideoOverlay() {
        binding.thumbnailLayout.videoOverlay.visibility = View.VISIBLE
    }

    override val more: ImageButton?
        get() = null
    override val fileFeaturesLayout: LinearLayout?
        get() = null
    override val shimmerThumbnail: LoaderImageView
        get() = binding.thumbnailLayout.thumbnailShimmer
    override val favorite: ImageView
        get() = binding.favoriteAction
    override val localFileIndicator: ImageView
        get() = binding.localFileIndicator
    override val imageFileName: TextView?
        get() = null
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
