/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.filelist.recommended

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.ui.component.filename.FilenameView
import com.owncloud.android.databinding.RecommendedFileItemBinding
import com.owncloud.android.ui.adapter.filelist.item.FileViewHolder

class RecommendedFilesViewHolder(val binding: RecommendedFileItemBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileViewHolder {

    override val itemLayout: View = binding.recommendedFileItemLayout
    override val thumbnail: ImageView = binding.thumbnail
    override val shimmerThumbnail: LoaderImageView = binding.thumbnailShimmer
    override val checkbox: ImageView = binding.customCheckbox
    override val favorite: ImageView = binding.favoriteAction
    override val localFileIndicator: ImageView = binding.localFileIndicator
    override val shared: ImageView = binding.sharedIcon
    override val unreadComments: ImageView = binding.unreadComments
    override val filename: FilenameView = binding.filename

    init {
        binding.favoriteAction.drawable.mutate()
    }
}
