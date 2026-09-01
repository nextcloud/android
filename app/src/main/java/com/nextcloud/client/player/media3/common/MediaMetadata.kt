/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.common

import android.os.Bundle
import androidx.media3.common.MediaMetadata
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.util.getPlaybackFile
import com.nextcloud.client.player.util.putPlaybackFile

private const val PLAYBACK_FILE_KEY = "playback_file"

fun MediaMetadata.Builder.setExtras(playbackFile: PlaybackFile): MediaMetadata.Builder = setExtras(
    Bundle().apply {
        putPlaybackFile(PLAYBACK_FILE_KEY, playbackFile)
    }
)

val MediaMetadata.playbackFile: PlaybackFile?
    get() = extras.getPlaybackFile(PLAYBACK_FILE_KEY)
