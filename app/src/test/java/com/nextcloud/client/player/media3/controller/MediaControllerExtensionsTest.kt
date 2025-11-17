/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.controller

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.nextcloud.client.player.model.state.RepeatMode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaControllerExtensionsTest {

    private fun item(id: String) = MediaItem.Builder().setMediaId(id).build()

    @Test
    fun `indexOfFirst found and not found`() {
        val controller = mockk<MediaController>(relaxed = true)
        val items = listOf(item("A"), item("B"), item("C"))

        every { controller.mediaItemCount } returns items.size
        every { controller.getMediaItemAt(any()) } answers { items[firstArg<Int>()] }

        val found = controller.indexOfFirst { it.mediaId == "B" }
        val notFound = controller.indexOfFirst { it.mediaId == "X" }

        assertEquals(1, found)
        assertEquals(-1, notFound)
    }

    @Test
    fun `updateMediaItems updates around current`() {
        val controller = mockk<MediaController>(relaxed = true)

        every { controller.currentMediaItemIndex } returns 1
        every { controller.currentMediaItem } returns item("B")
        every { controller.mediaItemCount } returns 3

        val new = listOf(item("A"), item("B"), item("D"), item("E"))

        controller.updateMediaItems(new)

        verifyOrder {
            controller.removeMediaItems(2, 3)
            controller.addMediaItems(listOf(new[2], new[3]))
            controller.removeMediaItems(0, 1)
            controller.addMediaItems(0, listOf(new[0]))
            controller.replaceMediaItem(1, new[1])
        }

        verify(exactly = 0) {
            controller.setMediaItems(any<List<MediaItem>>())
        }
    }

    @Test
    fun `updateMediaItems falls back to setMediaItems when no match`() {
        val controller = mockk<MediaController>(relaxed = true)

        every { controller.currentMediaItemIndex } returns 0
        every { controller.currentMediaItem } returns item("X")
        every { controller.mediaItemCount } returns 2

        val new = listOf(item("A"), item("B"))

        controller.updateMediaItems(new)

        verify {
            controller.setMediaItems(new)
        }

        verify(exactly = 0) {
            controller.removeMediaItems(any(), any())
            controller.addMediaItems(any<List<MediaItem>>())
            controller.addMediaItems(any(), any<List<MediaItem>>())
            controller.replaceMediaItem(any(), any())
        }
    }

    @Test
    fun `setRepeatMode maps to Player constants`() {
        val controller = mockk<MediaController>(relaxed = true)

        controller.setRepeatMode(RepeatMode.SINGLE)
        verify { controller.repeatMode = Player.REPEAT_MODE_ONE }

        clearMocks(controller, answers = false, recordedCalls = true, verificationMarks = true)

        controller.setRepeatMode(RepeatMode.ALL)
        verify { controller.repeatMode = Player.REPEAT_MODE_ALL }

        clearMocks(controller, answers = false, recordedCalls = true, verificationMarks = true)

        controller.setRepeatMode(RepeatMode.OFF)
        verify { controller.repeatMode = Player.REPEAT_MODE_OFF }
    }
}
