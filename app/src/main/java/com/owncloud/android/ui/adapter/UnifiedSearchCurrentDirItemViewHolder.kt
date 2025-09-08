/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.databinding.UnifiedSearchCurrentDirectoryItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.ui.interfaces.UnifiedSearchCurrentDirItemAction
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class UnifiedSearchCurrentDirItemViewHolder(
    val binding: UnifiedSearchCurrentDirectoryItemBinding,
    val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    private val storageManager: FileDataStorageManager,
    private val isRTL: Boolean,
    private val user: User,
    private val appPreferences: AppPreferences,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val action: UnifiedSearchCurrentDirItemAction
) : SectionedViewHolder(binding.unifiedSearchCurrentDirItemLayout) {

    fun bind(file: OCFile) {
        val filenameWithExtension = storageManager.getFilenameConsideringOfflineOperation(file)
        val isFolder = file.isFolder
        val (filename, extension) = FileStorageUtils.getFilenameAndExtension(filenameWithExtension, isFolder, isRTL)
        binding.extension.setVisibleIf(!isFolder)
        binding.extension.text = extension
        binding.filename.text = filename
        viewThemeUtils.platform.colorImageView(binding.thumbnail, ColorRole.PRIMARY)
        DisplayUtils.setThumbnail(
            file,
            binding.thumbnail,
            user,
            storageManager,
            listOf(),
            false,
            context,
            binding.thumbnailShimmer,
            appPreferences,
            viewThemeUtils,
            syncedFolderProvider
        )

        binding.more.setOnClickListener {
            action.openFile(file.decryptedRemotePath, true)
        }

        binding.unifiedSearchCurrentDirItemLayout.setOnClickListener {
            action.openFile(file.decryptedRemotePath, false)
        }
    }
}
