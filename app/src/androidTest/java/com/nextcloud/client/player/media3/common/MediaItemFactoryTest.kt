/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.common

import com.nextcloud.client.player.model.file.PlaybackFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MediaItemFactoryTest {

    private val factory = MediaItemFactory()

    @Test
    fun create_builds_MediaItem_with_expected_fields() {
        val file = PlaybackFile(
            id = "123",
            uri = "https://example.com/media.mp3",
            name = "media.mp3",
            mimeType = "audio/mpeg",
            contentLength = 42L,
            lastModified = 1736200000000L,
            isFavorite = true
        )

        val item = factory.create(file)

        assertEquals(file.id, item.mediaId)
        assertEquals(file.uri, item.localConfiguration?.uri.toString())
        assertEquals(file.mimeType, item.localConfiguration?.mimeType)

        val metadata = item.mediaMetadata
        assertNotNull(metadata)
        assertEquals(file, metadata.playbackFile)
    }
}
