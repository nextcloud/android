/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist.item

import android.app.Activity
import android.content.res.ColorStateList
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.core.view.isNotEmpty
import com.google.android.material.chip.Chip
import com.nextcloud.client.account.User
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.utils.HumanReadableFormatter
import com.nextcloud.utils.avatar.AvatarGenerator
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.text.DisplayTextFormatter
import com.owncloud.android.R
import com.owncloud.android.databinding.ListItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.tags.Tag
import com.owncloud.android.ui.interfaces.FileListAdapterListener
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

internal class FileItemBinder(
    private val activity: Activity,
    private val viewThemeUtils: ViewThemeUtils,
    private val listener: FileListAdapterListener,
    private val avatarGenerator: AvatarGenerator,
    private val userId: String?
) {
    private val avatarShareesProvider = AvatarShareesProvider()

    // region Public methods
    fun bind(
        holder: FileItemViewHolder,
        file: OCFile,
        user: User,
        storageManager: FileDataStorageManager,
        showItemOptions: Boolean,
        onlyOnDevice: Boolean
    ) {
        val binding = holder.binding
        bindSharedAvatars(binding, file, user, showItemOptions)
        bindTags(binding, file.tags)
        bindFileSize(binding, file, storageManager, onlyOnDevice)
        bindLastModification(binding, file)
        bindOverflowMenu(binding, file, showItemOptions)
        bindLivePhotoIndicator(binding, file)

        binding.playerProgressIndicator.setFile(file)
    }
    // endregion

    // region Private methods
    private fun bindLivePhotoIndicator(binding: ListItemBinding, file: OCFile) {
        val isLivePhoto = file.linkedFileIdForLivePhoto != null
        binding.livePhotoIndicator.setVisibleIf(isLivePhoto)
        binding.livePhotoIndicatorSeparator.setVisibleIf(isLivePhoto)
    }

    private fun bindSharedAvatars(binding: ListItemBinding, file: OCFile, user: User, showItemOptions: Boolean) {
        val sharedAvatars = binding.sharedAvatars
        val isShared = file.isSharedWithMe || file.isSharedWithSharee

        if (!isShared || !showItemOptions) {
            sharedAvatars.boundFileId = null
            sharedAvatars.visibility = View.GONE
            if (sharedAvatars.isNotEmpty()) {
                sharedAvatars.removeAllViews()
            }
            return
        }

        sharedAvatars.boundFileId = file.fileId
        sharedAvatars.setOnClickListener { listener.onShareIconClick(file) }
        sharedAvatars.setAvatars(user, avatarShareesProvider.get(file, userId), viewThemeUtils, avatarGenerator)
    }

    private fun bindTags(binding: ListItemBinding, tags: List<Tag>) {
        val hasTags = tags.isNotEmpty()
        binding.tagsGroup.setVisibleIf(hasTags)
        binding.fileDetailGroup.setVisibleIf(!hasTags)

        if (!hasTags) {
            return
        }

        viewThemeUtils.material.themeChipSuggestion(binding.firstTag)
        binding.firstTag.visibility = View.VISIBLE
        binding.secondTag.visibility = View.GONE
        binding.tagMore.visibility = View.GONE

        applyChipVisuals(binding.firstTag, tags[0])

        if (tags.size > 1) {
            binding.secondTag.visibility = View.VISIBLE
            applyChipVisuals(binding.secondTag, tags[1])
        }

        val hiddenTagCount = tags.size - MAX_VISIBLE_TAGS
        if (hiddenTagCount > 0) {
            viewThemeUtils.material.themeChipSuggestion(binding.tagMore)
            binding.tagMore.visibility = View.VISIBLE
            binding.tagMore.text = activity.getString(R.string.tags_more).format(hiddenTagCount)
        }
    }

    private fun applyChipVisuals(chip: Chip, tag: Tag) {
        viewThemeUtils.material.themeChipSuggestion(chip)
        chip.text = tag.name

        val tagColor = tag.color
        if (tagColor.isNullOrEmpty()) {
            return
        }

        try {
            val color = tagColor.toColorInt()
            chip.chipStrokeColor = ColorStateList.valueOf(color)
            chip.setTextColor(color)
        } catch (e: IllegalArgumentException) {
            Log_OC.d(TAG, "Exception applyChipVisuals: $e")
        }
    }

    private fun bindFileSize(
        binding: ListItemBinding,
        file: OCFile,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean
    ) {
        val localPath = file.storagePath?.takeIf { onlyOnDevice }

        when {
            localPath != null -> showFileSize(binding, file, localSizeOf(File(localPath)), storageManager)

            file.fileLength >= 0 -> showFileSize(binding, file, file.fileLength, storageManager)

            else -> {
                binding.fileSize.visibility = View.GONE
                binding.fileSeparator.visibility = View.GONE
            }
        }
    }

    private fun localSizeOf(localFile: File): Long = if (localFile.isDirectory) {
        FileStorageUtils.getFolderSize(localFile)
    } else {
        localFile.length()
    }

    private fun showFileSize(
        binding: ListItemBinding,
        file: OCFile,
        size: Long,
        storageManager: FileDataStorageManager
    ) {
        binding.fileSize.visibility = View.VISIBLE
        binding.fileSize.text = fileSizeText(file, size, storageManager)
    }

    private fun fileSizeText(file: OCFile, size: Long, storageManager: FileDataStorageManager): String {
        if (!file.isOfflineOperation) {
            return HumanReadableFormatter.formatBytes(size)
        }

        val isRemoveOperation = storageManager.getOfflineEntityFromOCFile(file)?.type is OfflineOperationType.RemoveFile
        val descriptionRes = if (isRemoveOperation) {
            R.string.oc_file_list_adapter_offline_operation_remove_description_text
        } else {
            R.string.oc_file_list_adapter_offline_operation_description_text
        }
        return activity.getString(descriptionRes)
    }

    private fun bindLastModification(binding: ListItemBinding, file: OCFile) {
        val timestamp = file.modificationTimestamp.takeIf { it > 0 }
            ?: file.firstShareTimestamp.takeIf { it > 0 }

        if (timestamp != null) {
            binding.lastMod.text = DisplayTextFormatter.formatRelativeTimestamp(activity, timestamp)
        }
        binding.lastMod.setVisibleIf(timestamp != null)
    }

    private fun bindOverflowMenu(binding: ListItemBinding, file: OCFile, showItemOptions: Boolean) {
        val overflowMenu = binding.overflowMenu
        overflowMenu.setVisibleIf(showItemOptions)

        if (showItemOptions) {
            overflowMenu.setOnClickListener { listener.onOverflowIconClicked(file) }
        }

        val iconRes = if (file.isLocked) R.drawable.ic_locked_dots_small else R.drawable.ic_dots_vertical
        overflowMenu.setImageResource(iconRes)
    }
    // endregion

    companion object {
        private val TAG = FileItemBinder::class.java.simpleName

        private const val MAX_VISIBLE_TAGS = 2
    }
}
