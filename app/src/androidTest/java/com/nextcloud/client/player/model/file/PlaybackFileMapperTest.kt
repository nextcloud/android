/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.file

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFileMapperTest {

    @Test
    fun ocFile_maps_to_PlaybackFile() {
        val ocFile = OCFile("/Documents/music/test.mp3").apply {
            fileId = 123L
            localId = 123L
            fileLength = 42_000
            modificationTimestamp = 1_700_000_000_000L
            mimeType = "audio/mpeg"
            isFavorite = true
        }

        val playback = ocFile.toPlaybackFile()

        assertEquals("123", playback.id)
        assertEquals("remoteFile:///123", playback.uri)
        assertEquals("test.mp3", playback.name)
        assertEquals("audio/mpeg", playback.mimeType)
        assertEquals(42_000, playback.contentLength)
        assertEquals(1_700_000_000_000L, playback.lastModified)
        assertEquals(true, playback.isFavorite)
    }

    @Test
    fun ocShare_maps_to_PlaybackFile_with_mimetype_fallback() {
        val share = OCShare("/Shared/music/test.mp3").apply {
            fileSource = 555L
            mimetype = null
            sharedDate = 1_700_111_222L
            isFavorite = false
        }

        val playback = share.toPlaybackFile()

        assertEquals("555", playback.id)
        assertEquals("remoteFile:///555", playback.uri)
        assertEquals("test.mp3", playback.name)
        assertEquals("audio/mpeg", playback.mimeType)
        assertEquals(1_700_111_222_000L, playback.lastModified)
        assertEquals(false, playback.isFavorite)
    }
}
