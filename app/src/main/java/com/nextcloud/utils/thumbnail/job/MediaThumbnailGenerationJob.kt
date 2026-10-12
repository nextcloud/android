/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail.job

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.widget.ImageView
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaThumbnailGenerationJob(
    private val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "MediaThumbnailGenerationJob"
        private const val ANY_FRAME_TIME_US = -1L
    }

    suspend fun load(file: File, imageView: ImageView) {
        val key = file.cacheKey()
        val thumbnail = withContext(ioDispatcher) { generateThumbnail(file, key) }

        if (imageView.tag?.toString() != key) {
            return
        }

        if (thumbnail != null) {
            imageView.setImageBitmap(thumbnail)
        } else {
            setFallbackIcon(imageView, file)
        }
    }

    private fun File.cacheKey(): String = hashCode().toString()

    @Suppress("TooGenericExceptionCaught")
    private fun generateThumbnail(file: File, key: String): Bitmap? = try {
        when {
            MimeTypeUtil.isImage(file) -> ThumbnailsCacheManager.getBitmapFromDiskCache(key)
                ?: generateImageThumbnail(file, key)

            MimeTypeUtil.isVideo(file) -> ThumbnailsCacheManager.getBitmapFromDiskCache(key)
                ?: generateVideoThumbnail(file, key)

            else -> null
        }
    } catch (t: Throwable) {
        Log_OC.e(TAG, "Generation of thumbnail for ${file.absolutePath} failed", t)
        null
    }

    private fun generateImageThumbnail(file: File, key: String): Bitmap? {
        val px = ThumbnailsCacheManager.getThumbnailDimension()
        val bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.absolutePath, px, px) ?: return null
        return ThumbnailsCacheManager.addThumbnailToCache(key, bitmap, file.path, px, px)
    }

    private fun generateVideoThumbnail(file: File, key: String): Bitmap? =
        extractVideoFrame(file)?.let { frame -> downscaleAndCache(frame, file, key) }

    private fun downscaleAndCache(frame: Bitmap, file: File, key: String): Bitmap {
        val px = ThumbnailsCacheManager.getThumbnailDimension()
        val width = frame.width
        val height = frame.height
        val longestSide = maxOf(width, height)

        if (longestSide <= px) {
            return frame
        }

        val scaled = BitmapUtils.scaleBitmap(frame, px.toFloat(), width, height, longestSide)
        return ThumbnailsCacheManager.addThumbnailToCache(key, scaled, file.path, px, px)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun extractVideoFrame(file: File): Bitmap? {
        if (!file.exists()) {
            Log_OC.w(TAG, "Cannot extract thumbnail: file does not exist")
            return null
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(ANY_FRAME_TIME_US)
        } catch (_: Throwable) {
            Log_OC.w(TAG, "Failed to create bitmap from video ${file.absolutePath}")
            null
        } finally {
            retriever.releaseSafely()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun MediaMetadataRetriever.releaseSafely() {
        try {
            release()
        } catch (_: Throwable) {
            Log_OC.w(TAG, "Failed to release retriever")
        }
    }

    private fun setFallbackIcon(imageView: ImageView, file: File) {
        when {
            file.isDirectory -> imageView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context, viewThemeUtils))

            MimeTypeUtil.isVideo(file) -> imageView.setImageBitmap(ThumbnailsCacheManager.mDefaultVideo)

            else -> imageView.setImageDrawable(
                MimeTypeUtil.getFileTypeIcon(null, file.name, context, viewThemeUtils)
            )
        }
    }
}
