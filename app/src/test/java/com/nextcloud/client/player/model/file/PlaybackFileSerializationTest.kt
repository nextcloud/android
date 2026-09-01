/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.file

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackFileSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val playbackFile = PlaybackFile(
        id = "42",
        uri = "https://cloud.example.com/remote.php/dav/files/user/track.mp3",
        name = "track.mp3",
        mimeType = "audio/mpeg",
        contentLength = 8_474_624L,
        lastModified = 1_718_236_800_000L,
        isFavorite = true
    )

    @Test
    fun roundTripsThroughJson() {
        val decoded = json.decodeFromString<PlaybackFile>(json.encodeToString(playbackFile))

        assertEquals(playbackFile, decoded)
    }

    @Test
    fun unknownKeysFromAnotherAppVersionAreIgnored() {
        val encoded = json.encodeToString(playbackFile).dropLast(1) + ""","aFieldAddedLater":true}"""

        assertEquals(playbackFile, json.decodeFromString<PlaybackFile>(encoded))
    }

    @Test
    fun malformedPayloadDecodesToNull() {
        val decoded = runCatching { json.decodeFromString<PlaybackFile>("not json") }.getOrNull()

        assertNull(decoded)
    }
}
