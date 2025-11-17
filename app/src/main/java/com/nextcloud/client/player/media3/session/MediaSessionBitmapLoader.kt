/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.session

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.nextcloud.client.player.media3.common.playbackFile
import com.nextcloud.client.player.model.ThumbnailLoader
import com.nextcloud.client.player.model.file.PlaybackFile
import com.owncloud.android.R
import com.owncloud.android.utils.MimeTypeUtil
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.inject.Inject

@UnstableApi
class MediaSessionBitmapLoader @Inject constructor(
    private val context: Context,
    private val thumbnailLoader: ThumbnailLoader
) : BitmapLoader by DataSourceBitmapLoader(context) {

    companion object {
        private const val THUMBNAIL_TARGET_SIZE = 160
        private const val LARGE_THUMBNAIL_TARGET_SIZE = 320
    }

    private val thumbnailSize: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LARGE_THUMBNAIL_TARGET_SIZE
        } else {
            THUMBNAIL_TARGET_SIZE
        }

    private val executorService: ListeningExecutorService by lazy {
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())
    }

    private var currentBitmapRequest: BitmapRequest? = null

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val file = metadata.playbackFile
        val previousRequest = this.currentBitmapRequest

        if (previousRequest != null && previousRequest.isSameRequest(file, metadata)) {
            return previousRequest.bitmapFuture
        }

        val bitmapFuture = executorService.submit(
            Callable {
                getBitmapFromMetadata(metadata, file?.id) ?: run {
                    file?.let(::getBitmapForFile) ?: getDefaultBitmap(file)
                }
            }
        )

        this.currentBitmapRequest = BitmapRequest(
            file?.id,
            metadata.artworkData,
            metadata.artworkUri,
            bitmapFuture
        )

        return bitmapFuture
    }

    private fun getBitmapFromMetadata(metadata: MediaMetadata, fileId: String?): Bitmap? {
        val model = metadata.artworkData ?: metadata.artworkUri ?: return null
        return runCatching {
            thumbnailLoader.load(context, model, fileId, thumbnailSize, thumbnailSize).get()
        }.getOrElse {
            null
        }
    }

    private fun getBitmapForFile(file: PlaybackFile): Bitmap? = runCatching {
        thumbnailLoader.load(context, file, thumbnailSize, thumbnailSize).get()
    }.getOrElse {
        null
    }

    private fun getDefaultBitmap(file: PlaybackFile?): Bitmap {
        val drawable = if (file != null && MimeTypeUtil.isVideo(file.mimeType)) {
            ContextCompat.getDrawable(context, R.drawable.player_ic_notification_video)
        } else {
            ContextCompat.getDrawable(context, R.drawable.player_ic_notification_audio)
        }
        return drawable?.toBitmap() ?: throw IllegalStateException("Could not decode resource")
    }

    private class BitmapRequest(
        val mediaId: String?,
        val artworkData: ByteArray?,
        val artworkUri: Uri?,
        val bitmapFuture: ListenableFuture<Bitmap>
    ) {

        fun isSameRequest(file: PlaybackFile?, metadata: MediaMetadata): Boolean = mediaId == file?.id &&
            artworkData.contentEquals(metadata.artworkData) &&
            artworkUri == metadata.artworkUri
    }
}
