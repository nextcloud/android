/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.common

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.nextcloud.client.player.model.file.PlaybackFile

fun PlaybackFile.toMediaItem(): MediaItem = MediaItem
    .Builder()
    .setMediaId(id)
    .setUri(uri)
    .setMediaMetadata(MediaMetadata.Builder().setExtras(this).build())
    .setMimeType(mimeType)
    .build()
