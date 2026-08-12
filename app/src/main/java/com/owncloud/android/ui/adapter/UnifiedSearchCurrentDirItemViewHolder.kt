/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import android.view.View
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.utils.thumbnail.ThumbnailArguments
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.databinding.UnifiedSearchCurrentDirectoryItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.interfaces.UnifiedSearchCurrentDirItemAction
import com.owncloud.android.utils.FileStorageUtils

@Suppress("LongParameterList")
class UnifiedSearchCurrentDirItemViewHolder(
    val binding: UnifiedSearchCurrentDirectoryItemBinding,
    val context: Context,
    private val storageManager: FileDataStorageManager,
    private val isRTL: Boolean,
    private val action: UnifiedSearchCurrentDirItemAction,
    private val thumbnailGenerator: ThumbnailGenerator
) : SectionedViewHolder(binding.unifiedSearchCurrentDirItemLayout) {

    fun bind(file: OCFile) {
        val filenameWithExtension = storageManager.getFilenameConsideringOfflineOperation(file)
        val isFolder = file.isFolder
        val containsBidiControlCharacters = FileStorageUtils.containsBidiControlCharacters(filenameWithExtension)

        if (!containsBidiControlCharacters || isFolder) {
            binding.extension.visibility = View.GONE
            binding.filename.text = filenameWithExtension
        } else {
            val (filename, extension) = FileStorageUtils.getFilenameAndExtension(filenameWithExtension, false, isRTL)
            binding.extension.text = extension
            binding.filename.text = filename
        }

        thumbnailGenerator.setThumbnail(
            file,
            binding.thumbnail,
            ThumbnailArguments.withShimmer(binding.thumbnailShimmer)
        )

        binding.more.setOnClickListener {
            action.openFile(file.decryptedRemotePath, true)
        }

        binding.unifiedSearchCurrentDirItemLayout.setOnClickListener {
            action.openFile(file.decryptedRemotePath, false)
        }
    }
}
