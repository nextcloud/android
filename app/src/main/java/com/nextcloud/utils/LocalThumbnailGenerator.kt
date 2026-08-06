/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.nextcloud.utils.extensions.toFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

@Suppress("TooGenericExceptionCaught")
object LocalThumbnailGenerator {
    private const val TAG = "LocalThumbnailGenerator"
    private const val LAST_FRAME = -1L

    @JvmStatic
    @JvmOverloads
    fun createThumbnail(path: String?, pxW: Int, pxH: Int, mimeType: String? = null): Bitmap? {
        val file = path.toFile() ?: return null

        return if (MimeTypeUtil.isVideo(mimeType) || MimeTypeUtil.isVideo(file)) {
            createVideoThumbnail(file, pxW, pxH)
        } else {
            BitmapUtils.decodeSampledBitmapFromFile(file.absolutePath, pxW, pxH)
        }
    }

    private fun createVideoThumbnail(file: File, pxW: Int, pxH: Int): Bitmap? =
        createWithThumbnailUtils(file, pxW, pxH) ?: createWithMetadataRetriever(file, pxW, pxH)

    private fun createWithThumbnailUtils(file: File, pxW: Int, pxH: Int): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ThumbnailUtils.createVideoThumbnail(file, Size(pxW, pxH), null)
        } else {
            @Suppress("DEPRECATION")
            ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
        }
    } catch (e: Exception) {
        Log_OC.d(TAG, "ThumbnailUtils could not decode ${file.name}: ${e.message}")
        null
    }

    private fun createWithMetadataRetriever(file: File, pxW: Int, pxH: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(LAST_FRAME)?.scaleDown(pxW, pxH)
        } catch (e: Exception) {
            Log_OC.e(TAG, "MediaMetadataRetriever could not decode ${file.name}", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log_OC.w(TAG, "Failed to release MediaMetadataRetriever: ${e.message}")
            }
        }
    }

    private fun Bitmap.scaleDown(pxW: Int, pxH: Int): Bitmap {
        val longestSide = maxOf(width, height)
        val limit = maxOf(pxW, pxH)

        return if (longestSide > limit) {
            BitmapUtils.scaleBitmap(this, limit.toFloat(), width, height, longestSide)
        } else {
            this
        }
    }
}
