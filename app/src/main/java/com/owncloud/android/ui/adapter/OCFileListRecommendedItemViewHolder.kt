/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.owncloud.android.databinding.RecommendedFileItemBinding

class OCFileListRecommendedItemViewHolder(private val binding: RecommendedFileItemBinding) :
    RecyclerView.ViewHolder(binding.root),
    ListGridItemViewHolder {

    val reason: TextView get() = binding.reason
    override val fileName: TextView get() = binding.filename
    override val extension: TextView? get() = binding.extension
    override val thumbnail: ImageView get() = binding.thumbnail
    override val shimmerThumbnail: LoaderImageView get() = binding.thumbnailShimmer
    override val favorite: ImageView get() = binding.favoriteAction
    override val localFileIndicator: ImageView get() = binding.localFileIndicator
    override val shared: ImageView get() = binding.sharedIcon
    override val checkbox: ImageView get() = binding.customCheckbox
    override val itemLayout: View get() = binding.recommendedFileItemLayout
    override val unreadComments: ImageView get() = binding.unreadComments
    override val fileFeaturesLayout: LinearLayout get() = binding.fileFeaturesLayout
    override val more: ImageButton get() = binding.more

    override val imageFileName: TextView? get() = null
    override val gridLivePhotoIndicator: ImageView? get() = null
    override val livePhotoIndicator: TextView? get() = null
    override val livePhotoIndicatorSeparator: TextView? get() = null

    override fun showVideoOverlay() {
        binding.videoOverlay.visibility = View.VISIBLE
    }

    init {
        binding.favoriteAction.drawable.mutate()
    }
}
