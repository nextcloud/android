/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import android.app.Activity
import android.view.View
import androidx.core.content.ContextCompat
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.owncloud.android.R
import com.owncloud.android.ui.interfaces.LocalFileListFragmentInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

@Suppress("LongParameterList")
internal class LocalFileListItemBinder(
    private val activity: Activity,
    private val viewThemeUtils: ViewThemeUtils,
    private val fragmentInterface: LocalFileListFragmentInterface,
    private val localFolderPickerMode: Boolean,
    private val isWithinEncryptedFolder: Boolean,
    private val isCheckedFile: (File) -> Boolean
) {
    fun bind(holder: LocalFileListGridItemViewHolder, file: File) {
        bindSelection(holder, file)
        bindThumbnail(holder, file)

        holder.itemLayout.setOnClickListener { fragmentInterface.onItemClicked(file) }

        if (holder is LocalFileListItemViewHolder) {
            bindFileDetails(holder, file)
        }

        holder.fileName.text = file.name
    }

    private fun bindSelection(holder: LocalFileListGridItemViewHolder, file: File) {
        if (localFolderPickerMode) {
            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(activity, R.color.bg_default))
            holder.checkbox.visibility = View.GONE
            return
        }

        holder.checkbox.visibility = View.VISIBLE

        if (isCheckedFile(file)) {
            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(activity, R.color.selected_item_background))
            holder.checkbox.setImageDrawable(
                viewThemeUtils.platform.tintDrawable(activity, R.drawable.ic_checkbox_marked, ColorRole.PRIMARY)
            )
        } else {
            holder.itemLayout.setBackgroundColor(ContextCompat.getColor(activity, R.color.bg_default))
            holder.checkbox.setImageResource(R.drawable.ic_checkbox_blank_outline)
        }

        holder.checkbox.setOnClickListener { fragmentInterface.onItemCheckboxClicked(file) }
    }

    private fun bindThumbnail(holder: LocalFileListGridItemViewHolder, file: File) {
        holder.thumbnail.tag = file.hashCode()
        LocalFileThumbnailBinder.setThumbnail(file, holder.thumbnail, activity, viewThemeUtils)
    }

    private fun bindFileDetails(holder: LocalFileListItemViewHolder, file: File) {
        if (file.isDirectory) {
            holder.fileSize.visibility = View.GONE
            holder.fileSeparator.visibility = View.GONE

            if (isWithinEncryptedFolder) {
                holder.checkbox.visibility = View.GONE
            }
        } else {
            holder.fileSize.visibility = View.VISIBLE
            holder.fileSeparator.visibility = View.VISIBLE
            holder.fileSize.text = DisplayUtils.bytesToHumanReadable(file.length())
        }

        holder.lastModification.text = DisplayUtils.getRelativeTimestamp(activity, file.lastModified())
    }
}
