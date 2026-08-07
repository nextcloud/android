/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import android.content.Context
import android.widget.ImageView
import com.owncloud.android.R
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTaskObject
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

object LocalFileThumbnailBinder {
    private val TAG: String = LocalFileThumbnailBinder::class.java.simpleName

    fun setThumbnail(file: File, thumbnailView: ImageView, context: Context, viewThemeUtils: ViewThemeUtils) {
        if (file.isDirectory) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context, viewThemeUtils))
            return
        }

        thumbnailView.setImageResource(R.drawable.file)

        // Cancellation needs to be checked and done before changing the drawable in fileIcon, or
        // ThumbnailsCacheManager#cancelPotentialThumbnailWork will NEVER cancel any task.
        val allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)

        if (!MimeTypeUtil.isImage(file)) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(null, file.name, context, viewThemeUtils))
            return
        }

        setImageThumbnail(file, thumbnailView, context, allowedToCreateNewThumbnail)
    }

    private fun setImageThumbnail(
        file: File,
        thumbnailView: ImageView,
        context: Context,
        allowedToCreateNewThumbnail: Boolean
    ) {
        val cachedThumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
            ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.hashCode()
        )

        when {
            cachedThumbnail != null -> thumbnailView.setImageBitmap(cachedThumbnail)

            // when a thumbnail is already being generated, don't restart it
            allowedToCreateNewThumbnail -> generateThumbnail(file, thumbnailView, context)
        }
    }

    private fun generateThumbnail(file: File, thumbnailView: ImageView, context: Context) {
        val task = ThumbnailGenerationTask(thumbnailView)
        val placeholder = if (MimeTypeUtil.isVideo(file)) {
            ThumbnailsCacheManager.mDefaultVideo
        } else {
            ThumbnailsCacheManager.mDefaultImg
        }

        thumbnailView.setImageDrawable(AsyncThumbnailDrawable(context.resources, placeholder, task))
        task.execute(ThumbnailGenerationTaskObject(file, null))
        Log_OC.v(TAG, "Executing task to generate a new thumbnail")
    }
}
