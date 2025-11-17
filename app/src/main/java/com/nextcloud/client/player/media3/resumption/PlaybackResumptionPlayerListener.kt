/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.resumption

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import javax.inject.Inject

class PlaybackResumptionPlayerListener @Inject constructor(
    private val playbackResumptionConfigStore: PlaybackResumptionConfigStore
) : Player.Listener {

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaItem?.let { playbackResumptionConfigStore.updateCurrentFileId(it.mediaId) }
    }
}
