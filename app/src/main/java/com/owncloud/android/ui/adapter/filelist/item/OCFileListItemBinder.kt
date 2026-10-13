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
import com.nextcloud.utils.view.LocaleDirection
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.tags.Tag
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

internal class OCFileListItemBinder(
    private val activity: Activity,
    private val viewThemeUtils: ViewThemeUtils,
    private val fragmentInterface: OCFileListFragmentInterface,
    private val avatarGenerator: AvatarGenerator,
    private val userId: String?
) {
    private val avatarShareesProvider = AvatarShareesProvider()
    private val isRTL = LocaleDirection.isRtl

    // region Public methods
    fun bindDetails(
        holder: ListItemViewHolder,
        file: OCFile,
        user: User,
        storageManager: FileDataStorageManager,
        showItemOptions: Boolean,
        onlyOnDevice: Boolean
    ) {
        bindSharedAvatars(holder, file, user, showItemOptions)
        bindTags(holder, file.tags)
        bindFileSize(holder, file, storageManager, onlyOnDevice)
        bindLastModification(holder, file)
        bindOverflowMenu(holder, file, showItemOptions)
    }

    fun bindFilename(holder: ListGridItemViewHolder, file: OCFile, filename: String?) {
        val hasBidiControlCharacters = FileStorageUtils.containsBidiControlCharacters(filename)
        holder.fileName.setVisibleIf(!hasBidiControlCharacters)
        holder.bidiFilenameContainer.setVisibleIf(hasBidiControlCharacters)

        if (hasBidiControlCharacters) {
            bindBidiFilename(holder, file, filename)
        } else {
            bindPlainFilename(holder, filename)
        }

        holder.playerProgressIndicator?.setFile(file)
    }

    fun bindLivePhotoIndicator(holder: ListViewHolder, file: OCFile) {
        val isLivePhoto = file.linkedFileIdForLivePhoto != null

        when (holder) {
            is OCFileListItemViewHolder -> {
                holder.livePhotoIndicator.setVisibleIf(isLivePhoto)
                holder.livePhotoIndicatorSeparator.setVisibleIf(isLivePhoto)
            }

            is OCFileListViewHolder -> holder.gridLivePhotoIndicator.setVisibleIf(isLivePhoto)
        }
    }
    // endregion

    // region Private methods
    private fun bindPlainFilename(holder: ListGridItemViewHolder, filename: String?) {
        holder.fileName.text = filename

        if (holder is OCFileListGridItemViewHolder) {
            holder.extension?.visibility = View.GONE
        }
    }

    private fun bindBidiFilename(holder: ListGridItemViewHolder, file: OCFile, filename: String?) {
        val (name, extension) = FileStorageUtils.getFilenameAndExtension(filename, file.isFolder, isRTL)
        holder.bidiFilename.text = name
        holder.extension?.text = extension

        if (holder is OCFileListGridItemViewHolder) {
            holder.binding.more.visibility = View.GONE
            holder.binding.bidiMore.setOnClickListener { fragmentInterface.onOverflowIconClicked(file, it) }
        }
    }

    private fun bindSharedAvatars(holder: ListItemViewHolder, file: OCFile, user: User, showItemOptions: Boolean) {
        val sharedAvatars = holder.sharedAvatars
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
        sharedAvatars.setOnClickListener { fragmentInterface.onShareIconClick(file) }
        sharedAvatars.setAvatars(user, avatarShareesProvider.get(file, userId), viewThemeUtils, avatarGenerator)
    }

    private fun bindTags(holder: ListItemViewHolder, tags: List<Tag>) {
        val hasTags = tags.isNotEmpty()
        holder.tagsGroup.setVisibleIf(hasTags)
        holder.fileDetailGroup.setVisibleIf(!hasTags)

        if (!hasTags) {
            return
        }

        viewThemeUtils.material.themeChipSuggestion(holder.firstTag)
        holder.firstTag.visibility = View.VISIBLE
        holder.secondTag.visibility = View.GONE
        holder.tagMore.visibility = View.GONE

        applyChipVisuals(holder.firstTag, tags[0])

        if (tags.size > 1) {
            holder.secondTag.visibility = View.VISIBLE
            applyChipVisuals(holder.secondTag, tags[1])
        }

        val hiddenTagCount = tags.size - MAX_VISIBLE_TAGS
        if (hiddenTagCount > 0) {
            viewThemeUtils.material.themeChipSuggestion(holder.tagMore)
            holder.tagMore.visibility = View.VISIBLE
            holder.tagMore.text = activity.getString(R.string.tags_more).format(hiddenTagCount)
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
        holder: ListItemViewHolder,
        file: OCFile,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean
    ) {
        val localPath = file.storagePath?.takeIf { onlyOnDevice }

        when {
            localPath != null -> showFileSize(holder, file, localSizeOf(File(localPath)), storageManager)

            file.fileLength >= 0 -> showFileSize(holder, file, file.fileLength, storageManager)

            else -> {
                holder.fileSize.visibility = View.GONE
                holder.fileSizeSeparator.visibility = View.GONE
            }
        }
    }

    private fun localSizeOf(localFile: File): Long = if (localFile.isDirectory) {
        FileStorageUtils.getFolderSize(localFile)
    } else {
        localFile.length()
    }

    private fun showFileSize(
        holder: ListItemViewHolder,
        file: OCFile,
        size: Long,
        storageManager: FileDataStorageManager
    ) {
        holder.fileSize.visibility = View.VISIBLE
        holder.fileSize.text = fileSizeText(file, size, storageManager)
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

    private fun bindLastModification(holder: ListItemViewHolder, file: OCFile) {
        val timestamp = file.modificationTimestamp.takeIf { it > 0 }
            ?: file.firstShareTimestamp.takeIf { it > 0 }

        if (timestamp != null) {
            holder.lastModification.text = DisplayTextFormatter.formatRelativeTimestamp(activity, timestamp)
        }
        holder.lastModification.setVisibleIf(timestamp != null)
    }

    private fun bindOverflowMenu(holder: ListItemViewHolder, file: OCFile, showItemOptions: Boolean) {
        val overflowMenu = holder.overflowMenu
        overflowMenu.setVisibleIf(showItemOptions)

        if (showItemOptions) {
            overflowMenu.setOnClickListener { fragmentInterface.onOverflowIconClicked(file, it) }
        }

        val iconRes = if (file.isLocked) R.drawable.ic_locked_dots_small else R.drawable.ic_dots_vertical
        overflowMenu.setImageResource(iconRes)
    }
    // endregion

    companion object {
        private val TAG = OCFileListItemBinder::class.java.simpleName

        private const val MAX_VISIBLE_TAGS = 2
    }
}
