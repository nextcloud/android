/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.gallery

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.nextcloud.client.account.User
import com.nextcloud.utils.allocationKilobyte
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class GalleryImageGenerationJob(
    private val user: User,
    private val storageManager: FileDataStorageManager
) {
    companion object {
        private const val TAG = "GalleryImageGenerationJob"
        private val semaphore = Semaphore(3)
    }

    suspend fun run(
        file: OCFile,
        imageView: ImageView,
        width: Int,
        viewThemeUtils: ViewThemeUtils,
        listener: GalleryImageGenerationListener
    ) {
        try {
            var newImage = false

            if (file.remoteId == null && !file.isPreviewAvailable) {
                listener.onError()
                return
            }

            val bitmap: Bitmap? = getBitmap(imageView, file, width, viewThemeUtils, onThumbnailGeneration = {
                newImage = true
            })

            if (bitmap == null) {
                listener.onError()
                return
            }

            setThumbnail(bitmap, file, imageView, newImage, listener)
        } catch (e: Exception) {
            Log_OC.e(TAG, "gallery image generation job: ", e)
            withContext(Dispatchers.Main) {
                listener.onError()
            }
        }
    }

    private fun getPlaceholder(
        file: OCFile,
        width: Int,
        viewThemeUtils: ViewThemeUtils
    ): BitmapDrawable {
        val context = MainApp.getAppContext()

        val placeholder = MimeTypeUtil.getFileTypeIcon(
            file.mimeType,
            file.fileName,
            context,
            viewThemeUtils
        )
            ?: ResourcesCompat.getDrawable(context.resources, R.drawable.file_image, null)
            ?: Color.GRAY.toDrawable()

        val bitmap = BitmapUtils.drawableToBitmap(
            placeholder,
            width / 2,
            width / 2
        )

        return BitmapDrawable(context.resources, bitmap)
    }

    private suspend fun getBitmap(
        imageView: ImageView,
        file: OCFile,
        width: Int,
        viewThemeUtils: ViewThemeUtils,
        onThumbnailGeneration: () -> Unit
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            if (file.remoteId == null && !file.isPreviewAvailable) {
                Log_OC.w(TAG, "file has no remoteId and no preview")
                return@withContext null
            }

            val key = file.remoteId
            val cachedThumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + file.remoteId
            )
            if (cachedThumbnail != null && !file.isUpdateThumbnailNeeded) {
                Log_OC.d(TAG, "cached thumbnail is used for: ${file.fileName}")
                return@withContext getThumbnailFromCache(file, cachedThumbnail, key)
            }

            Log_OC.d(TAG, "generating new thumbnail for: ${file.fileName}")

            // only add placeholder if new thumbnail will be generated because cached image will appear so quickly
            withContext(Dispatchers.Main) {
                val placeholderDrawable = getPlaceholder(file, width, viewThemeUtils)
                imageView.setImageDrawable(placeholderDrawable)
            }

            onThumbnailGeneration()
            semaphore.withPermit {
                return@withContext getThumbnailFromServerAndAddToCache(file, cachedThumbnail)
            }
        }

    private suspend fun setThumbnail(
        bitmap: Bitmap,
        file: OCFile,
        imageView: ImageView,
        newImage: Boolean,
        listener: GalleryImageGenerationListener
    ) = withContext(Dispatchers.Main) {
        val tagId = file.fileId.toString()
        if (imageView.tag?.toString() != tagId) return@withContext

        if ("image/png".equals(file.mimeType, ignoreCase = true)) {
            imageView.setBackgroundColor(
                ContextCompat.getColor(
                    MainApp.getAppContext(),
                    R.color.bg_default
                )
            )
        }

        if (newImage) {
            listener.onNewGalleryImage()
        }

        imageView.setImageBitmap(bitmap)
        imageView.invalidate()
        listener.onSuccess()
    }

    private fun getThumbnailFromCache(file: OCFile, thumbnail: Bitmap, key: String): Bitmap {
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

    @Suppress("DEPRECATION")
    private suspend fun getThumbnailFromServerAndAddToCache(file: OCFile, thumbnail: Bitmap?): Bitmap? {
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
