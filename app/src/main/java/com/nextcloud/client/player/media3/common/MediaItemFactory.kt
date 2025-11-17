/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.common

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.nextcloud.client.player.model.file.PlaybackFile
import javax.inject.Inject

class MediaItemFactory @Inject constructor() {

    fun create(file: PlaybackFile): MediaItem = MediaItem
        .Builder()
        .setMediaId(file.id)
        .setUri(file.uri)
        .setMediaMetadata(createMetadata(file))
        .setMimeType(file.mimeType)
        .build()

    private fun createMetadata(file: PlaybackFile): MediaMetadata = MediaMetadata
        .Builder()
        .setExtras(file)
        .build()
}
