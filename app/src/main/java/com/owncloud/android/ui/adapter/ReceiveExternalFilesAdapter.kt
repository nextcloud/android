/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.User
import com.owncloud.android.databinding.UploaderListItemLayoutBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTaskObject
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class ReceiveExternalFilesAdapter(
    private val files: List<OCFile>,
    private val context: Context,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val viewThemeUtils: ViewThemeUtils,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<ReceiveExternalFilesAdapter.ReceiveExternalViewHolder>() {

    private var filteredFiles: List<OCFile> = files

    interface OnItemClickListener {
        fun selectFile(file: OCFile)
    }

    inner class ReceiveExternalViewHolder(val binding: UploaderListItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener.selectFile(filteredFiles[position])
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        filteredFiles = if (query.isEmpty()) {
            files
        } else {
            files.filter { file ->
                file.fileName.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ReceiveExternalViewHolder {
        val binding = UploaderListItemLayoutBinding
            .inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ReceiveExternalViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ReceiveExternalViewHolder, position: Int) {
        val file = filteredFiles[position]

        viewHolder.binding.filename.text = file.fileName
        viewHolder.binding.lastMod.text = DisplayUtils.getRelativeTimestamp(context, file.modificationTimestamp)

        if (!file.isFolder) {
            viewHolder.binding.fileSize.text = DisplayUtils.bytesToHumanReadable(file.fileLength)
        }

        viewHolder.binding.fileSize.visibility = if (file.isFolder) {
            View.GONE
        } else {
            View.VISIBLE
        }
        viewHolder.binding.fileSeparator.visibility = if (file.isFolder) {
            View.GONE
        } else {
            View.VISIBLE
        }

        val thumbnailImageView = viewHolder.binding.thumbnail
        setupThumbnail(thumbnailImageView, file)
    }

    private fun setupThumbnail(thumbnailImageView: ImageView, file: OCFile) {
        thumbnailImageView.tag = file.fileId

        if (file.isFolder) {
            setupThumbnailForFolder(thumbnailImageView, file)
        } else if (MimeTypeUtil.isImage(file) && file.remoteId != null) {
            setupThumbnailForImage(thumbnailImageView, file)
        } else {
            setupDefaultThumbnail(thumbnailImageView, file)
        }
    }

    private fun setupThumbnailForFolder(thumbnailImageView: ImageView, file: OCFile) {
        val isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, user)
        val isDarkModeActive = syncedFolderProvider.preferences.isDarkModeEnabled
        val overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder)
        val icon = MimeTypeUtil.getFileIcon(isDarkModeActive, overlayIconId, context, viewThemeUtils)
        thumbnailImageView.setImageDrawable(icon)
    }

    @Suppress("NestedBlockDepth")
    private fun setupThumbnailForImage(thumbnailImageView: ImageView, file: OCFile) {
        var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(file.remoteId.toString())
        if (thumbnail != null && !file.isUpdateThumbnailNeeded) {
            thumbnailImageView.setImageBitmap(thumbnail)
        } else {
            // generate new Thumbnail
            if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailImageView)) {
                val task = ThumbnailGenerationTask(thumbnailImageView, storageManager, user)
                if (thumbnail == null) {
                    thumbnail = if (MimeTypeUtil.isVideo(file)) {
                        ThumbnailsCacheManager.mDefaultVideo
                    } else {
                        ThumbnailsCacheManager.mDefaultImg
                    }
                }
                val asyncDrawable = AsyncThumbnailDrawable(
                    context.resources,
                    thumbnail,
                    task
                )
                thumbnailImageView.setImageDrawable(asyncDrawable)

                @Suppress("DEPRECATION")
                task.execute(ThumbnailGenerationTaskObject(file, file.remoteId))
            }
        }
    }

    private fun setupDefaultThumbnail(thumbnailImageView: ImageView, file: OCFile) {
        val icon = MimeTypeUtil.getFileTypeIcon(
            file.mimeType,
            file.fileName,
            context,
            viewThemeUtils
        )
        thumbnailImageView.setImageDrawable(icon)
    }

    override fun getItemCount() = filteredFiles.size
}
