/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.gallery

import android.graphics.Bitmap
import android.widget.ImageView
import com.nextcloud.client.account.User
import com.nextcloud.utils.allocationKilobyte
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryImageGenerationJob(
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val imageView: ImageView,
    private val file: OCFile,
    private val key: String,
    private val listener: GalleryImageGenerationListener,
    private val backgroundColor: Int
) {
    companion object {
        private const val TAG = "GalleryImageGenerationJob"
    }

    suspend fun execute() {
        var newImage = false
        val bitmap: Bitmap? = withContext(Dispatchers.IO) {
            var thumbnail: Bitmap?
            if (file.remoteId != null || file.isPreviewAvailable) {
                thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + file.remoteId
                )

                if (thumbnail != null && !file.isUpdateThumbnailNeeded) {
                    return@withContext getThumbnailFromCache(thumbnail)
                }

                newImage = true
                return@withContext getThumbnailFromServerAndAddToCache(thumbnail)
            }
            return@withContext null
        }

        withContext(Dispatchers.Main) {
            if (bitmap != null) {
                val tagId = file.fileId.toString()
                if (imageView.tag?.toString() == tagId) {
                    if ("image/png".equals(file.mimeType, ignoreCase = true)) {
                        imageView.setBackgroundColor(backgroundColor)
                    }

                    if (newImage) {
                        listener.onNewGalleryImage()
                    }

                    imageView.setImageBitmap(bitmap)
                    imageView.invalidate()
                }
                listener.onSuccess()
            } else {
                listener.onError()
            }
        }
    }

    private fun getThumbnailFromCache(thumbnail: Bitmap): Bitmap {
        val size = ThumbnailsCacheManager.getThumbnailDimension().toFloat()

        val imageDimension = file.imageDimension
        if (imageDimension == null || imageDimension.width != size || imageDimension.height != size) {
            val newDimension = ImageDimension(
                thumbnail.getWidth().toFloat(),
                thumbnail.getHeight().toFloat()
            )
            file.imageDimension = newDimension
            storageManager.saveFile(file)
        }

        var result = thumbnail
        if (MimeTypeUtil.isVideo(file)) {
            result = ThumbnailsCacheManager.addVideoOverlay(thumbnail, MainApp.getAppContext())
        }

        if (thumbnail.allocationKilobyte() > ThumbnailsCacheManager.THUMBNAIL_SIZE_IN_KB) {
            result = ThumbnailsCacheManager.getScaledThumbnailAfterSave(result, key)
        }

        return result
    }

    private suspend fun getThumbnailFromServerAndAddToCache(thumbnail: Bitmap?): Bitmap? {
        var thumbnail = thumbnail
        try {
            val client = withContext(Dispatchers.IO) {
                OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(
                    user.toOwnCloudAccount(),
                    MainApp.getAppContext()
                )
            }
            ThumbnailsCacheManager.setClient(client)
            thumbnail = ThumbnailsCacheManager.doResizedImageInBackground(file, storageManager)

            if (MimeTypeUtil.isVideo(file) && thumbnail != null) {
                thumbnail = ThumbnailsCacheManager.addVideoOverlay(thumbnail, MainApp.getAppContext())
            }
        } catch (t: Throwable) {
            Log_OC.e(TAG, "Generation of gallery image for $file failed", t)
        }

        return thumbnail
    }
}
