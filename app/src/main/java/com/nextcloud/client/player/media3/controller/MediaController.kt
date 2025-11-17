/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.controller

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.nextcloud.client.player.model.state.RepeatMode

fun Player.indexOfFirst(satisfies: (MediaItem) -> Boolean): Int {
    for (index in 0..<mediaItemCount) {
        val mediaItem = getMediaItemAt(index)
        if (satisfies(mediaItem)) {
            return index
        }
    }
    return -1
}

fun Player.updateMediaItems(newMediaItems: List<MediaItem>) {
    val oldCurrentMediaItemIndex = currentMediaItemIndex
        .takeIf { it >= 0 }

    val newCurrentMediaItemIndex = currentMediaItem
        ?.mediaId
        ?.let { currentMediaId -> newMediaItems.indexOfFirst { it.mediaId == currentMediaId } }
        ?.takeIf { it >= 0 }

    if (oldCurrentMediaItemIndex != null && newCurrentMediaItemIndex != null) {
        if (oldCurrentMediaItemIndex < mediaItemCount - 1) {
            removeMediaItems(oldCurrentMediaItemIndex + 1, mediaItemCount)
        }
        if (newCurrentMediaItemIndex < newMediaItems.size - 1) {
            val itemsToAdd = newMediaItems.subList(newCurrentMediaItemIndex + 1, newMediaItems.size)
            addMediaItems(itemsToAdd)
        }
        if (oldCurrentMediaItemIndex > 0) {
            removeMediaItems(0, oldCurrentMediaItemIndex)
        }
        if (newCurrentMediaItemIndex > 0) {
            val itemsToAdd = newMediaItems.subList(0, newCurrentMediaItemIndex)
            addMediaItems(0, itemsToAdd)
        }
        replaceMediaItem(newCurrentMediaItemIndex, newMediaItems[newCurrentMediaItemIndex])
    } else {
        setMediaItems(newMediaItems)
    }
}

fun Player.setRepeatMode(mode: RepeatMode) {
    repeatMode = when (mode) {
        RepeatMode.SINGLE -> Player.REPEAT_MODE_ONE
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    }
}
