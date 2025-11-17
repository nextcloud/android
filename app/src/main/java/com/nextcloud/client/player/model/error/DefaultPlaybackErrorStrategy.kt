/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.error

import com.nextcloud.client.player.model.state.PlaybackState
import javax.inject.Inject

class DefaultPlaybackErrorStrategy @Inject constructor() : PlaybackErrorStrategy {

    override fun switchToNextSource(error: Throwable, state: PlaybackState): Boolean {
        val currentFile = state.currentItemState?.file
        val currentFiles = state.currentFiles
        val oneFileInQueue = currentFiles.size == 1
        val endOfQueue = currentFiles.indexOf(currentFile) == currentFiles.lastIndex
        return !oneFileInQueue && !endOfQueue
    }
}
