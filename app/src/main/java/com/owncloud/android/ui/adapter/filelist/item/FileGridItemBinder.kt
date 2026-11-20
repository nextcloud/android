/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist.item

import android.view.View
import androidx.core.view.isVisible
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.adapter.filelist.recommended.RecommendedFilesViewHolder
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface

internal class FileGridItemBinder(private val fragmentInterface: OCFileListFragmentInterface) {

    fun bind(holder: FileGridItemViewHolder, file: OCFile, showMoreOptions: Boolean) {
        val binding = holder.binding
        bindIndicators(holder, file, binding.gridLivePhotoIndicator, binding.fileFeaturesLayout)
        bindMoreButton(binding.more, file, showMoreOptions)

        binding.playerProgressIndicator.setFile(file)
    }

    fun bind(holder: RecommendedFilesViewHolder, file: OCFile, showMoreOptions: Boolean) {
        val binding = holder.binding
        bindIndicators(holder, file, binding.gridLivePhotoIndicator, binding.fileFeaturesLayout)

        bindMoreButton(binding.more, file, showMoreOptions)
    }

    private fun bindIndicators(holder: FileViewHolder, file: OCFile, livePhotoIndicator: View, featuresLayout: View) {
        if (file.unreadCommentsCount != 0) {
            holder.shared.visibility = View.GONE
        }

        livePhotoIndicator.setVisibleIf(file.linkedFileIdForLivePhoto != null)

        featuresLayout.setVisibleIf(
            livePhotoIndicator.isVisible ||
                holder.localFileIndicator.isVisible ||
                holder.unreadComments.isVisible ||
                holder.shared.isVisible ||
                holder.favorite.isVisible
        )
    }

    private fun bindMoreButton(more: View, file: OCFile, showMoreOptions: Boolean) {
        more.setOnClickListener { fragmentInterface.onOverflowIconClicked(file, it) }
        more.setVisibleIf(showMoreOptions)
    }
}
