/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model

import android.view.SurfaceView
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.PlaybackFiles
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.RepeatMode
import kotlinx.coroutines.flow.Flow
import java.util.Optional

@Suppress("TooManyFunctions")
interface PlaybackModel {

    val state: Optional<PlaybackState>

    suspend fun start()

    fun setFilesFlow(filesFlow: Flow<PlaybackFiles>)

    fun setFiles(files: PlaybackFiles)

    fun release()

    fun setVideoSurfaceView(surfaceView: SurfaceView?)

    fun addListener(listener: Listener)

    fun removeListener(listener: Listener)

    fun play()

    fun pause()

    fun playNext()

    fun playPrevious()

    fun seekToPosition(positionInMilliseconds: Long)

    fun setRepeatMode(repeatMode: RepeatMode)

    fun setShuffle(shuffle: Boolean)

    fun switchToFile(file: PlaybackFile)

    interface Listener {

        fun onPlaybackUpdate(state: PlaybackState)

        fun onPlaybackError(error: Throwable) {
            // Default empty implementation
        }
    }
}
