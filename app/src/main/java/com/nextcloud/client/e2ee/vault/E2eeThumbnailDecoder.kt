/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.MimeTypeUtil

class E2eeThumbnailDecoder {
    fun decode(file: OCFile, bytes: ByteArray, width: Int, height: Int): Bitmap? = when {
        MimeTypeUtil.isVideo(file) -> decodeVideoThumbnail(bytes, width, height)
        else -> decodeImageThumbnail(file, bytes, width, height)
    }

    private fun decodeImageThumbnail(file: OCFile, bytes: ByteArray, width: Int, height: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        return if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            decodeSampledImageThumbnail(file, bytes, bounds, width, height)
        }
    }

    private fun decodeSampledImageThumbnail(
        file: OCFile,
        bytes: ByteArray,
        bounds: BitmapFactory.Options,
        width: Int,
        height: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds, width, height)
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.let { bitmap ->
            if (PNG_MIMETYPE.equals(file.mimeType, ignoreCase = true)) {
                ThumbnailsCacheManager.handlePNG(bitmap, width, height)
            } else {
                ThumbnailUtils.extractThumbnail(bitmap, width, height)
            }
        }
    }

    private fun decodeVideoThumbnail(bytes: ByteArray, width: Int, height: Int): Bitmap? {
        if (bytes.isEmpty()) {
            return null
        }

        val frame = decodeVideoFrameWithMediaDataSource(bytes) ?: return null

        return ThumbnailUtils.extractThumbnail(frame, width, height)
    }

    private fun decodeVideoFrameWithMediaDataSource(bytes: ByteArray): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(E2eeByteArrayMediaDataSource(bytes))
            retriever.getFrameAtTime(DEFAULT_FRAME_TIME_US)
        } catch (e: IllegalArgumentException) {
            Log_OC.w(TAG, "Could not decode E2EE video thumbnail from byte array source: ${e.javaClass.simpleName}")
            null
        } catch (e: RuntimeException) {
            Log_OC.w(TAG, "Could not decode E2EE video thumbnail from byte array source: ${e.javaClass.simpleName}")
            null
        } finally {
            releaseRetriever(retriever)
        }
    }

    private fun releaseRetriever(retriever: MediaMetadataRetriever) {
        try {
            retriever.release()
        } catch (e: RuntimeException) {
            Log_OC.w(TAG, "Could not release E2EE video thumbnail retriever: ${e.javaClass.simpleName}")
        }
    }

    private fun calculateSampleSize(options: BitmapFactory.Options, requestedWidth: Int, requestedHeight: Int): Int {
        var sampleSize = 1
        var width = options.outWidth
        var height = options.outHeight

        while (width / sampleSize > requestedWidth * SAMPLE_FACTOR ||
            height / sampleSize > requestedHeight * SAMPLE_FACTOR
        ) {
            sampleSize *= SAMPLE_FACTOR
        }

        return sampleSize
    }

    companion object {
        private const val TAG = "E2eeThumbnailDecoder"
        private const val PNG_MIMETYPE = "image/png"
        private const val SAMPLE_FACTOR = 2
        private const val DEFAULT_FRAME_TIME_US = -1L
    }
}
