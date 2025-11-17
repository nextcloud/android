/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.file

import android.net.Uri
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import org.junit.Assert
import org.junit.Test

class PlaybackFileUriMapperTest {

    @Test
    fun getPlaybackUri_from_OCFile_uses_localId() {
        val file = OCFile("/root/music/song.mp3").apply {
            localId = 12345L
        }

        val uri = file.getPlaybackUri()
        Assert.assertEquals("remoteFile", uri.scheme)
        Assert.assertEquals("12345", uri.lastPathSegment)
        Assert.assertEquals(12345L, uri.getRemoteFileId())
    }

    @Test
    fun getPlaybackUri_from_OCShare_uses_fileSource() {
        val share = OCShare().apply {
            fileSource = 9999L
        }

        val uri = share.getPlaybackUri()
        Assert.assertEquals("remoteFile", uri.scheme)
        Assert.assertEquals("9999", uri.lastPathSegment)
        Assert.assertEquals(9999L, uri.getRemoteFileId())
    }

    @Test
    fun getRemoteFileId_returns_null_for_different_scheme() {
        val other = Uri.parse("content://anything/123")
        Assert.assertNull(other.getRemoteFileId())
    }
}
