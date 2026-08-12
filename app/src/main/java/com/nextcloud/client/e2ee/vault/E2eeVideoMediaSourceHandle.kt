/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.owncloud.android.datamodel.OCFile
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(UnstableApi::class)
class E2eeVideoMediaSourceHandle internal constructor(file: OCFile, private val plaintext: ByteArray) : AutoCloseable {
    val mediaSource: MediaSource by lazy {
        ProgressiveMediaSource
            .Factory(DataSource.Factory { ByteArrayDataSource(plaintext) })
            .createMediaSource(buildMediaItem(file))
    }

    private val closed = AtomicBoolean(false)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            plaintext.fill(0)
        }
    }

    companion object {
        private const val E2EE_VIDEO_URI_SCHEME = "e2ee-vault-video"
        private const val E2EE_VIDEO_URI_AUTHORITY = "media"
        private const val AVI_EXTENSION = ".avi"
        private const val AVI_MIME_TYPE = "video/x-msvideo"
        private const val LEGACY_AVI_MIME_TYPE = "video/avi"

        internal fun normalizedMimeType(file: OCFile): String? = when {
            file.fileName.endsWith(AVI_EXTENSION, ignoreCase = true) -> AVI_MIME_TYPE
            file.mimeType.equals(LEGACY_AVI_MIME_TYPE, ignoreCase = true) -> AVI_MIME_TYPE
            else -> file.mimeType
        }

        private fun buildMediaItem(file: OCFile): MediaItem = MediaItem.Builder()
            .setUri(
                Uri.Builder()
                    .scheme(E2EE_VIDEO_URI_SCHEME)
                    .authority(E2EE_VIDEO_URI_AUTHORITY)
                    .appendPath(file.fileName)
                    .build()
            )
            .setMimeType(normalizedMimeType(file))
            .setMediaId(file.remoteId ?: file.localId.toString())
            .build()
    }
}
