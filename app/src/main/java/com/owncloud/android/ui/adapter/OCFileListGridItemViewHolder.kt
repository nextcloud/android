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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.owncloud.android.databinding.GridItemBinding

class OCFileListGridItemViewHolder(var binding: GridItemBinding) :
    RecyclerView.ViewHolder(
        binding.root
    ),
    ListGridItemViewHolder {
    val bidiFilename: TextView
        get() = binding.bidiFilename
    override val fileName: TextView
        get() = binding.Filename
    override val extension: TextView?
        get() = if (binding.bidiFilenameContainer.isVisible) {
            binding.bidiExtension
        } else {
            null
        }
    override val thumbnail: ImageView
        get() = binding.thumbnail

    override fun showVideoOverlay() {
        binding.videoOverlay.visibility = View.VISIBLE
    }

    override val shimmerThumbnail: LoaderImageView
        get() = binding.thumbnailShimmer
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

    override val gridLivePhotoIndicator: ImageView?
        get() = null
    override val livePhotoIndicator: TextView?
        get() = null
    override val livePhotoIndicatorSeparator: TextView?
        get() = null
    override val fileFeaturesLayout: LinearLayout
        get() = binding.fileFeaturesLayout
    override val more: ImageButton
        get() = if (binding.bidiFilenameContainer.isVisible) {
            binding.bidiMore
        } else {
            binding.more
        }
    init {
        binding.favoriteAction.drawable.mutate()
    }
}
