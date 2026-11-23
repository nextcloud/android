/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter.filelist.item

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.ui.component.filename.FilenameView
import com.owncloud.android.databinding.ListItemBinding

class FileItemViewHolder(val binding: ListItemBinding) :
    RecyclerView.ViewHolder(binding.root),
    FileViewHolder {

    override val itemLayout: View = binding.ListItemLayout
    override val thumbnail: ImageView = binding.thumbnailLayout.thumbnail
    override val shimmerThumbnail: LoaderImageView = binding.thumbnailLayout.thumbnailShimmer
    override val checkbox: ImageView = binding.customCheckbox
    override val favorite: ImageView = binding.favoriteAction
    override val localFileIndicator: ImageView = binding.localFileIndicator
    override val shared: ImageView = binding.sharedIcon
    override val unreadComments: ImageView = binding.unreadComments
    override val filename: FilenameView = binding.Filename

    init {
        binding.favoriteAction.drawable.mutate()
    }
}
