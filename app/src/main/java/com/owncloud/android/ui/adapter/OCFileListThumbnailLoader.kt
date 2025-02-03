/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.view.View
import android.widget.ImageView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTaskObject
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

class OCFileListThumbnailLoader(
    private val file: OCFile,
    private val thumbnailView: ImageView,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val asyncTasks: MutableList<ThumbnailGenerationTask>,
    private val gridView: Boolean,
    private val context: Context,
    private val shimmerView: LoaderImageView,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils,
    private val syncedFolderProvider: SyncedFolderProvider?,
    private val iconView: ImageView,
) {

    fun load() {
        if (file.isFolder) {
            showFolderIcon()
        } else if (!file.isPreviewAvailable || file.remoteId == null) {
            showFileIcon()
        } else {
            loadFromCacheOrRemote()
        }
    }

    private fun loadFromCacheOrRemote() {
        val cacheKey = ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
        val cachedThumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(cacheKey)
        if (cachedThumbnail == null || file.isUpdateThumbnailNeeded) {
            loadFromRemote()
        } else if (MimeTypeUtil.isVideo(file)) {
            val cachedThumbnailWithOverlay = ThumbnailsCacheManager.addVideoOverlay(cachedThumbnail, context)
            showThumbnail(cachedThumbnailWithOverlay)
        } else {
            showThumbnail(cachedThumbnail)
        }
    }

    private fun loadFromRemote() {
        if (!ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
            return
        }

        showShimmer()

        try {
            val task = ThumbnailGenerationTask(
                thumbnailView,
                storageManager,
                user,
                asyncTasks,
                gridView,
                file.remoteId,
            )

            task.setListener(object : ThumbnailGenerationTask.Listener {
                override fun onSuccess() {
                    showExistedThumbnail()
                }

                override fun onError() {
                    showFileIcon()
                }
            })

            val px = ThumbnailsCacheManager.getThumbnailDimension()
            val tempBitmap = Bitmap.createBitmap(px, px, Bitmap.Config.RGB_565)
            val asyncDrawable = AsyncThumbnailDrawable(context.resources, tempBitmap, task)
            thumbnailView.setImageDrawable(asyncDrawable)

            asyncTasks.add(task)
            val taskObject = ThumbnailGenerationTaskObject(file, file.remoteId)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, taskObject)
        } catch (e: Exception) {
            Log_OC.d(this::class.simpleName, "ThumbnailGenerationTask : " + e.message)
        }
    }

    private fun showFolderIcon() {
        val isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, user)
        val isDarkModeActive = preferences.isDarkModeEnabled
        val overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder)
        val fileIcon = MimeTypeUtil.getFolderIcon(isDarkModeActive, overlayIconId, context, viewThemeUtils)
        showIcon(fileIcon)
    }

    private fun showFileIcon() {
        val fileIcon = MimeTypeUtil.getFileTypeIcon(file.mimeType, file.fileName, context, viewThemeUtils)
        showIcon(fileIcon)
    }

    private fun showIcon(icon: Drawable) {
        iconView.setImageDrawable(icon)
        iconView.visibility = View.VISIBLE
        thumbnailView.visibility = View.GONE
        shimmerView.visibility = View.GONE
    }

    private fun showThumbnail(thumbnail: Bitmap) {
        thumbnailView.setImageBitmap(thumbnail)
        showExistedThumbnail()
    }

    private fun showExistedThumbnail() {
        iconView.visibility = View.GONE
        thumbnailView.visibility = View.VISIBLE
        shimmerView.visibility = View.GONE
    }

    private fun showShimmer() {
        shimmerView.setImageResource(R.drawable.background)
        shimmerView.resetLoader()
        iconView.visibility = View.GONE
        thumbnailView.visibility = View.GONE
        shimmerView.visibility = View.VISIBLE
    }
}
