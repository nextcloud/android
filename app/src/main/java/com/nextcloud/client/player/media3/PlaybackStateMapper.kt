/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3

import androidx.media3.common.Player
import com.nextcloud.client.player.media3.common.playbackFile
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackItemMetadata
import com.nextcloud.client.player.model.state.PlaybackItemState
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.RepeatMode
import com.nextcloud.client.player.model.state.VideoSize
import com.owncloud.android.lib.common.utils.Log_OC

fun Player.toPlaybackState(currentFiles: List<PlaybackFile>): PlaybackState = PlaybackState(
    currentFiles = currentFiles,
    currentItemState = getCurrentItemState(),
    repeatMode = mapRepeatMode(),
    shuffle = shuffleModeEnabled
)

fun Player.readMediaIds(): List<String> = buildList {
    for (i in 0 until mediaItemCount) {
        add(getMediaItemAt(i).mediaId)
    }
}

fun Player.readCurrentFiles(): List<PlaybackFile> = buildList {
    for (i in 0 until mediaItemCount) {
        val mediaItem = getMediaItemAt(i)
        val playbackFile = mediaItem.mediaMetadata.playbackFile
        playbackFile?.let(::add)
    }
}

private fun Player.getCurrentItemState(): PlaybackItemState? {
    val currentFile = currentMediaItem?.mediaMetadata?.playbackFile ?: return null
    return PlaybackItemState(
        file = currentFile,
        playerState = mapPlayerState(),
        metadata = if (mediaMetadata.playbackFile?.id == currentFile.id) mapMetadata(currentFile) else null,
        videoSize = mapVideoSize(),
        currentTimeInMilliseconds = currentPosition,
        maxTimeInMilliseconds = duration
    )
}

private fun Player.mapPlayerState(): PlayerState = when (playbackState) {
    Player.STATE_IDLE -> PlayerState.IDLE
    Player.STATE_ENDED -> PlayerState.COMPLETED
    Player.STATE_BUFFERING, Player.STATE_READY -> if (playWhenReady) PlayerState.PLAYING else PlayerState.PAUSED
    else -> PlayerState.NONE
}

private fun Player.mapMetadata(currentFile: PlaybackFile) = PlaybackItemMetadata(
    title = mediaMetadata.title?.takeIf { it.isNotEmpty() } ?: currentFile.getNameWithoutExtension(),
    artist = mediaMetadata.artist,
    album = mediaMetadata.albumTitle,
    genre = mediaMetadata.genre,
    year = mediaMetadata.recordingYear,
    description = mediaMetadata.description,
    artworkData = mediaMetadata.artworkData,
    artworkUri = mediaMetadata.artworkUri?.toString()
)

private fun Player.mapVideoSize(): VideoSize? = videoSize
    .takeIf { it.width > 0 && it.height > 0 }
    ?.let { VideoSize(width = it.width, height = it.height) }

private fun Player.mapRepeatMode(): RepeatMode = when (repeatMode) {
    Player.REPEAT_MODE_ONE -> RepeatMode.SINGLE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}
