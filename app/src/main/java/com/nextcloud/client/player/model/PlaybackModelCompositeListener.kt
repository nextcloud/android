/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model

import com.nextcloud.client.player.model.state.PlaybackState

class PlaybackModelCompositeListener : PlaybackModel.Listener {
    private val listeners = mutableListOf<PlaybackModel.Listener>()

    fun addListener(listener: PlaybackModel.Listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: PlaybackModel.Listener?) {
        listeners.remove(listener)
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        for (i in 0 until listeners.size) {
            listeners.getOrNull(i)?.onPlaybackUpdate(state)
        }
    }

    override fun onPlaybackError(error: Throwable) {
        for (i in 0 until listeners.size) {
            listeners.getOrNull(i)?.onPlaybackError(error)
        }
    }
}
