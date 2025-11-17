/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.common

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class TestPlayer(looper: Looper) : SimpleBasePlayer(looper) {
    private val mediaItems = mutableListOf<MediaItem>()
    private var currentIndex = 0
    private var isPlaying = false
    private var currentPositionMs: Long = 0L
    private var repeatModeInternal: Int = REPEAT_MODE_OFF
    private var shuffleEnabled: Boolean = false

    override fun getState(): State {
        val commands = Player.Commands.Builder()
            .addAllCommands()
            .build()

        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaybackState(if (isPlaying) STATE_READY else STATE_IDLE)
            .setPlayWhenReady(isPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaylist(mediaItems.map { MediaItemData.Builder(it.mediaId).setMediaItem(it).build() })
            .setCurrentMediaItemIndex(currentIndex)
            .setContentPositionMs(currentPositionMs)
            .setRepeatMode(repeatModeInternal)
            .setShuffleModeEnabled(shuffleEnabled)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        isPlaying = playWhenReady
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        isPlaying = false
        currentPositionMs = 0L
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        currentIndex = currentIndex.coerceAtMost(mediaItems.lastIndex.coerceAtLeast(0))
        currentPositionMs = 0L
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        isPlaying = false
        mediaItems.clear()
        currentIndex = 0
        currentPositionMs = 0L
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        repeatModeInternal = repeatMode
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        shuffleEnabled = shuffleModeEnabled
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        items: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<*> {
        mediaItems.clear()
        mediaItems.addAll(items)
        currentIndex = if (startIndex != C.INDEX_UNSET) startIndex else 0
        currentIndex = currentIndex.coerceAtMost(mediaItems.lastIndex.coerceAtLeast(0))
        currentPositionMs = if (startPositionMs != C.TIME_UNSET) startPositionMs else 0L
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleAddMediaItems(index: Int, newItems: List<MediaItem>): ListenableFuture<*> {
        mediaItems.addAll(index, newItems)
        if (index <= currentIndex) {
            currentIndex += newItems.size
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
        val movingItems = mediaItems.subList(fromIndex, toIndex).toList()
        mediaItems.subList(fromIndex, toIndex).clear()
        mediaItems.addAll(newIndex, movingItems)

        currentIndex = when {
            currentIndex in fromIndex until toIndex -> newIndex + (currentIndex - fromIndex)
            currentIndex < fromIndex && newIndex <= currentIndex -> currentIndex + movingItems.size
            currentIndex >= toIndex && newIndex < currentIndex -> currentIndex - movingItems.size
            else -> currentIndex
        }
        currentIndex = currentIndex.coerceAtMost(mediaItems.lastIndex.coerceAtLeast(0))

        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleReplaceMediaItems(fromIndex: Int, toIndex: Int, newItems: List<MediaItem>): ListenableFuture<*> {
        mediaItems.subList(fromIndex, toIndex).clear()
        mediaItems.addAll(fromIndex, newItems)

        if (currentIndex in fromIndex until toIndex) {
            currentIndex = fromIndex.coerceAtMost(mediaItems.lastIndex.coerceAtLeast(0))
            currentPositionMs = 0L
        } else if (currentIndex >= toIndex) {
            currentIndex += (newItems.size - (toIndex - fromIndex))
        }

        currentIndex = currentIndex.coerceAtMost(mediaItems.lastIndex.coerceAtLeast(0))
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        mediaItems.subList(fromIndex, toIndex).clear()

        if (currentIndex in fromIndex until toIndex) {
            currentIndex = fromIndex.coerceAtMost(mediaItems.lastIndex.coerceAtLeast(0))
            currentPositionMs = 0L
        } else if (currentIndex >= toIndex) {
            currentIndex -= (toIndex - fromIndex)
        }

        currentIndex = currentIndex.coerceAtMost(mediaItems.lastIndex.coerceAtLeast(0))
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        if (mediaItemIndex != C.INDEX_UNSET) {
            currentIndex = mediaItemIndex.coerceAtMost(mediaItems.lastIndex.coerceAtLeast(0))
        }
        currentPositionMs = if (positionMs != C.TIME_UNSET) positionMs else 0L
        invalidateState()
        return Futures.immediateVoidFuture()
    }
}
