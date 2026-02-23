/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.error

import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackItemState
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.RepeatMode
import org.junit.Assert
import org.junit.Test

class DefaultPlaybackErrorStrategyTest {
    private val strategy = DefaultPlaybackErrorStrategy()

    @Test
    fun `switchToNextSource returns false when one file in queue`() {
        val state = createState(mockWithName("a"), mockWithName("a"))
        val switchToNext = strategy.switchToNextSource(RuntimeException(), state)
        Assert.assertFalse(switchToNext)
    }

    @Test
    fun `switchToNextSource returns false when current file is last`() {
        val state = createState(mockWithName("b"), mockWithName("a"), mockWithName("b"))
        val switchToNext = strategy.switchToNextSource(RuntimeException(), state)
        Assert.assertFalse(switchToNext)
    }

    @Test
    fun `switchToNextSource returns true when current file is not last`() {
        val state = createState(mockWithName("b"), mockWithName("a"), mockWithName("a"))
        val switchToNext = strategy.switchToNextSource(RuntimeException(), state)
        Assert.assertTrue(switchToNext)
    }

    private fun createState(currentFile: PlaybackFile, vararg files: PlaybackFile): PlaybackState = PlaybackState(
        currentFiles = files.toList(),
        currentItemState = currentFile.toPlaybackItemState(),
        repeatMode = RepeatMode.OFF,
        shuffle = false
    )

    private fun mockWithName(name: String): PlaybackFile = PlaybackFile(
        id = name,
        uri = name,
        name = "fakeUri:///$name",
        mimeType = "audio/mp3",
        contentLength = 0,
        lastModified = 0,
        isFavorite = false
    )

    private fun PlaybackFile.toPlaybackItemState() = PlaybackItemState(
        file = this,
        playerState = PlayerState.NONE,
        metadata = null,
        videoSize = null,
        currentTimeInMilliseconds = 0L,
        maxTimeInMilliseconds = 0L
    )
}
