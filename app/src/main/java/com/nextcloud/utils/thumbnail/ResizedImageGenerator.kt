/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import com.nextcloud.utils.AppConstants
import com.nextcloud.utils.extensions.getBigThumbnailKey
import com.nextcloud.utils.extensions.getPreviewEndpoint
import com.nextcloud.utils.extensions.getVideoPreviewEndpoint
import com.nextcloud.utils.extensions.isPNG
import com.nextcloud.utils.view.ScreenMetrics
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.MimeTypeUtil
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod

object ResizedImageGenerator {
    private const val TAG = "ResizedImageGenerator"

    @Suppress("MagicNumber")
    private val VIDEO_PREVIEW_SIZES = listOf(1024, 512, 256, 128, 64)

    fun getOrCreate(file: OCFile, client: OwnCloudClient, storageManager: FileDataStorageManager): Bitmap? {
        val cacheKey = file.getBigThumbnailKey()
        val cachedImage = ThumbnailsCacheManager.getBitmapFromDiskCache(cacheKey)
        if (cachedImage != null && !file.isUpdateThumbnailNeeded) {
            Log_OC.d(TAG, "cache hit: ${file.remotePath}")
            return cachedImage
        }

        val screenSize = ScreenMetrics.defaultDisplaySize()
        val resizedImage = if (file.isDown && MimeTypeUtil.isImage(file)) {
            Log_OC.d(TAG, "creating from local file: ${file.remotePath}")
            val decodedImage = BitmapUtils.decodeSampledBitmapFromFile(file.storagePath, screenSize.x, screenSize.y)
            if (decodedImage == null) {
                Log_OC.w(TAG, "local file decode failed, falling back to cached image: ${file.remotePath}")
                cachedImage
            } else {
                resizeAndCacheLocalImage(file, cacheKey, decodedImage, screenSize)
            }
        } else {
            Log_OC.d(TAG, "creating from server preview: ${file.remotePath}")
            downloadAndCacheServerPreview(file, cacheKey, client, screenSize)
        }

        if (resizedImage != null && file.imageDimension == null) {
            saveImageDimension(file, resizedImage, storageManager)
        }

        return resizedImage
    }

    private fun resizeAndCacheLocalImage(
        file: OCFile,
        cacheKey: String,
        decodedImage: Bitmap,
        screenSize: Point
    ): Bitmap? {
        val image = if (file.isPNG()) {
            BitmapUtils.centerCropOnPngBackground(decodedImage, screenSize.x, screenSize.y)
        } else {
            decodedImage
        }
        file.isUpdateThumbnailNeeded = false
        return ThumbnailsCacheManager.addThumbnailToCache(cacheKey, image, file.storagePath, screenSize.x, screenSize.y)
    }

    private fun downloadAndCacheServerPreview(
        file: OCFile,
        cacheKey: String,
        client: OwnCloudClient,
        screenSize: Point
    ): Bitmap? {
        val preview = if (MimeTypeUtil.isVideo(file)) {
            downloadVideoPreview(file, client)
        } else {
            downloadPreview(file, client, client.getPreviewEndpoint(file.localId, screenSize.x, screenSize.y))
        } ?: return null

        val image = if (file.isPNG()) {
            BitmapUtils.centerCropOnPngBackground(preview, preview.width, preview.height)
        } else {
            preview
        }
        ThumbnailsCacheManager.addBitmapToCache(cacheKey, image)
        return image
    }

    private fun downloadVideoPreview(file: OCFile, client: OwnCloudClient): Bitmap? =
        VIDEO_PREVIEW_SIZES.firstNotNullOfOrNull { size ->
            downloadPreview(file, client, client.getVideoPreviewEndpoint(file.localId, size))
        }

    @Suppress("TooGenericExceptionCaught")
    private fun downloadPreview(file: OCFile, client: OwnCloudClient, uri: String): Bitmap? {
        Log_OC.d(TAG, "downloading preview: ${file.remotePath}, uri: $uri")

        var getMethod: GetMethod? = null
        return try {
            getMethod = GetMethod(uri).apply { params.soTimeout = AppConstants.DEFAULT_READ_TIMEOUT_MS }

            val status = client.executeMethod(getMethod)
            if (status != HttpStatus.SC_OK) {
                Log_OC.e(TAG, "preview download failed, status: $status, file: ${file.remotePath}")
                client.exhaustResponse(getMethod.responseBodyAsStream)
                return null
            }

            getMethod.responseBodyAsStream.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log_OC.e(TAG, "preview download failed: ${file.remotePath}", e)
            null
        } finally {
            getMethod?.releaseConnection()
        }
    }

    private fun saveImageDimension(file: OCFile, image: Bitmap, storageManager: FileDataStorageManager) {
        file.imageDimension = ImageDimension(image.width.toFloat(), image.height.toFloat())
        file.isUpdateThumbnailNeeded = false
        storageManager.saveFile(file)
    }
}
