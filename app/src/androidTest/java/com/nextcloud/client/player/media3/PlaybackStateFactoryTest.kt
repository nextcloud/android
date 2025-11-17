/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3

import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.nextcloud.client.player.media3.common.MediaItemFactory
import com.nextcloud.client.player.media3.common.setExtras
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.RepeatMode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.media3.common.VideoSize as ExoVideoSize

class PlaybackStateFactoryTest {

    private val stateFactory = PlaybackStateFactory()
    private val itemFactory = MediaItemFactory()

    @Test
    fun create_builds_PlaybackState_with_expected_fields() {
        val player = mockk<Player>(relaxed = true)

        val file1 = createPlaybackFile("1")
        val file2 = createPlaybackFile("2")
        val item1 = itemFactory.create(file1)
        val item2 = itemFactory.create(file2)

        val playerMetadata = MediaMetadata.Builder()
            .setTitle("") // force fallback to file name without extension
            .setArtist("Artist")
            .setAlbumTitle("Album")
            .setGenre("Rock")
            .setRecordingYear(1999)
            .setDescription("Desc")
            .setArtworkData(byteArrayOf(1, 2, 3), 0)
            .setExtras(file2)
            .build()

        every { player.mediaItemCount } returns 2
        every { player.getMediaItemAt(0) } returns item1
        every { player.getMediaItemAt(1) } returns item2
        every { player.shuffleModeEnabled } returns true
        every { player.repeatMode } returns Player.REPEAT_MODE_ALL
        every { player.currentMediaItem } returns item2
        every { player.mediaMetadata } returns playerMetadata
        every { player.currentPosition } returns 12_345L
        every { player.duration } returns 54_321L
        every { player.videoSize } returns ExoVideoSize(1920, 1080)
        every { player.playbackState } returns Player.STATE_READY
        every { player.playWhenReady } returns false

        val state = stateFactory.create(player).orElseThrow()
        val currentItemState = state.currentItemState!!
        val currentItemMetadata = currentItemState.metadata!!
        val currentItemVideoSize = currentItemState.videoSize!!

        assertEquals(listOf(file1, file2), state.currentFiles)
        assertEquals(RepeatMode.ALL, state.repeatMode)
        assertTrue(state.shuffle)
        assertEquals(file2, currentItemState.file)
        assertEquals(PlayerState.PAUSED, currentItemState.playerState)
        assertEquals("name2", currentItemMetadata.title) // fallback from empty title
        assertEquals("Artist", currentItemMetadata.artist)
        assertEquals("Album", currentItemMetadata.album)
        assertEquals("Rock", currentItemMetadata.genre)
        assertEquals(1999, currentItemMetadata.year)
        assertEquals("Desc", currentItemMetadata.description)
        assertArrayEquals(byteArrayOf(1, 2, 3), currentItemMetadata.artworkData)
        assertNull(currentItemMetadata.artworkUri)
        assertEquals(1920, currentItemVideoSize.width)
        assertEquals(1080, currentItemVideoSize.height)
        assertEquals(12_345L, currentItemState.currentTimeInMilliseconds)
        assertEquals(54_321L, currentItemState.maxTimeInMilliseconds)
    }

    private fun createPlaybackFile(id: String) = PlaybackFile(
        id = id,
        uri = "uri$id",
        name = "name$id.mp3",
        mimeType = "audio/mpeg",
        contentLength = 0,
        lastModified = 0,
        isFavorite = false
    )
}
