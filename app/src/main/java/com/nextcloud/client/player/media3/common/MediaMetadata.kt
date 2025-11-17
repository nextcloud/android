/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.common

import android.os.Bundle
import androidx.media3.common.MediaMetadata
import com.nextcloud.client.player.model.file.PlaybackFile

private const val PLAYBACK_FILE_KEY = "playback_file"

fun MediaMetadata.Builder.setExtras(playbackFile: PlaybackFile): MediaMetadata.Builder = setExtras(
    Bundle().apply {
        putSerializable(PLAYBACK_FILE_KEY, playbackFile)
    }
)

val MediaMetadata.playbackFile: PlaybackFile?
    get() = extras?.getSerializable(PLAYBACK_FILE_KEY) as? PlaybackFile
