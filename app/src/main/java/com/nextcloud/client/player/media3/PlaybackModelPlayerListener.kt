/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3

import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import com.nextcloud.client.player.model.error.SourceException
import com.nextcloud.client.player.util.PeriodicAction

class PlaybackModelPlayerListener(
    private val checkProgressPeriodicAction: PeriodicAction,
    private val onPlaybackUpdate: () -> Unit,
    private val onPlaybackError: (Throwable) -> Unit
) : Player.Listener {

    companion object {
        private const val BROKEN_SOURCE_ERROR_CODE: Int = 416
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        onPlaybackUpdate()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        onPlaybackUpdate()
    }

    override fun onTracksChanged(tracks: Tracks) {
        onPlaybackUpdate()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        onPlaybackUpdate()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        onPlaybackUpdate()
        if (isPlaying) {
            checkProgressPeriodicAction.start()
        } else {
            checkProgressPeriodicAction.stop()
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        onPlaybackUpdate()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        onPlaybackUpdate()
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        onPlaybackUpdate()
    }

    @UnstableApi
    override fun onPlayerError(error: PlaybackException) {
        if (error is ExoPlaybackException && error.type == ExoPlaybackException.TYPE_SOURCE) {
            onPlaybackError(error.toSourceException())
        } else {
            onPlaybackError(error)
        }
    }

    @UnstableApi
    private fun ExoPlaybackException.toSourceException(): SourceException =
        if (sourceException is InvalidResponseCodeException) {
            SourceException((sourceException as InvalidResponseCodeException).responseCode)
        } else if (cause != null && cause is UnrecognizedInputFormatException) {
            SourceException(BROKEN_SOURCE_ERROR_CODE)
        } else {
            SourceException()
        }
}
