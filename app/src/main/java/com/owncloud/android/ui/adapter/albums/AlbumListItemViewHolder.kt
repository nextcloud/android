/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.albums

import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.owncloud.android.databinding.AlbumsListItemBinding

internal class AlbumListItemViewHolder(private var binding: AlbumsListItemBinding) :
    RecyclerView.ViewHolder(binding.root), AlbumItemViewHolder {
    override val thumbnail: ImageView
        get() = binding.thumbnail
    override val shimmerThumbnail: LoaderImageView
        get() = binding.thumbnailShimmer
    override val albumName: TextView
        get() = binding.Filename
    override val albumInfo: TextView
        get() = binding.fileInfo
}
