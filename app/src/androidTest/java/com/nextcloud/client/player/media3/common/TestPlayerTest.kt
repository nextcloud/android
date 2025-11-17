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
import androidx.test.annotation.UiThreadTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TestPlayerTest {

    private lateinit var player: TestPlayer

    @Before
    fun setUp() {
        player = TestPlayer(Looper.getMainLooper())
    }

    @Test
    @UiThreadTest
    fun setMediaItems_applies_startIndex_and_startPosition_bounds() {
        player.setMediaItems(listOf(item("a"), item("b"), item("c")), 1, 2_000)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(2_000L, player.currentPosition)
        assertEquals(3, player.mediaItemCount)
    }

    @Test
    @UiThreadTest
    fun play_pause_toggles_state() {
        player.setMediaItems(listOf(item("a")))
        assertFalse(player.playWhenReady)

        player.play()
        assertTrue(player.playWhenReady)

        player.pause()
        assertFalse(player.playWhenReady)
    }

    @Test
    @UiThreadTest
    fun addMediaItems_updates_indices_when_inserted_before_current() {
        player.setMediaItems(listOf(item("a"), item("b"), item("c")), 1, 0)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals("b", player.currentMediaItem?.mediaId)

        player.addMediaItems(0, listOf(item("x"), item("y")))
        assertEquals(3, player.currentMediaItemIndex)
        assertEquals("b", player.currentMediaItem?.mediaId)
    }

    @Test
    @UiThreadTest
    fun moveMediaItems_recomputes_current_index_inside_moved_block() {
        player.setMediaItems(listOf(item("a"), item("b"), item("c"), item("d")), 2, 0)
        assertEquals(2, player.currentMediaItemIndex)
        assertEquals("c", player.currentMediaItem?.mediaId)

        player.moveMediaItems(1, 3, 2)
        assertEquals(3, player.currentMediaItemIndex)
        assertEquals("c", player.currentMediaItem?.mediaId)
    }

    @Test
    @UiThreadTest
    fun replaceMediaItems_resets_position_when_current_replaced() {
        player.setMediaItems(listOf(item("a"), item("b"), item("c")), 1, 1_000)
        player.replaceMediaItems(1, 2, listOf(item("x"), item("y")))
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals("x", player.currentMediaItem?.mediaId)
        assertEquals(0L, player.currentPosition)
    }

    @Test
    @UiThreadTest
    fun removeMediaItems_updates_current_index_and_resets_if_removed() {
        player.setMediaItems(listOf(item("a"), item("b"), item("c")), 1, 500)
        player.removeMediaItems(1, 2)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals("c", player.currentMediaItem?.mediaId)
        assertEquals(0L, player.currentPosition)
    }

    @Test
    @UiThreadTest
    fun seekTo_updates_index_and_position() {
        player.setMediaItems(listOf(item("a"), item("b"), item("c")), 0, 0)
        player.seekTo(2, 12_345L)
        assertEquals(2, player.currentMediaItemIndex)
        assertEquals(12_345L, player.currentPosition)
    }

    @Test
    @UiThreadTest
    fun shuffle_and_repeat_flags_reflected_in_state() {
        player.setMediaItems(listOf(item("a"), item("b")))
        player.setShuffleModeEnabled(true)
        player.repeatMode = Player.REPEAT_MODE_ALL
        assertTrue(player.shuffleModeEnabled)
        assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
    }

    @Test
    @UiThreadTest
    fun stop_and_release_clear_state() {
        player.setMediaItems(listOf(item("a"), item("b")), 1, 100)
        player.play()
        player.stop()
        assertEquals(0L, player.currentPosition)
        assertFalse(player.playWhenReady)

        player.release()
        assertEquals(0, player.mediaItemCount)
        assertEquals(0, player.currentMediaItemIndex)
    }

    @Test
    @UiThreadTest
    fun unset_start_values_use_defaults() {
        player.setMediaItems(listOf(item("a"), item("b")), C.INDEX_UNSET, C.TIME_UNSET)
        assertEquals(0, player.currentMediaItemIndex)
        assertEquals(0L, player.currentPosition)
    }

    private fun item(id: String) = MediaItem.Builder()
        .setMediaId(id)
        .setUri("https://example.com/$id.mp3")
        .build()
}
