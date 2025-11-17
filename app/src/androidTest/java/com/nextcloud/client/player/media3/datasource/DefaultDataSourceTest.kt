/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.datasource

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.nextcloud.client.player.model.file.getPlaybackUri
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

class DefaultDataSourceTest {

    private val delegate = mockk<DataSource>()
    private val fileStore = mockk<FileDataStorageManager>()
    private val client = mockk<OwnCloudClient>()
    private val streamOperationFactory = mockk<StreamMediaFileOperationFactory>()

    private lateinit var dataSource: DefaultDataSource

    @Before
    fun setup() {
        every { delegate.responseHeaders } returns emptyMap()
        dataSource = DefaultDataSource(delegate, fileStore, client, streamOperationFactory)
    }

    @Test
    fun open_pass_through_when_uri_is_not_remote_file() {
        val spec = DataSpec.Builder().setUri(Uri.parse("https://example.com/a.mp3")).build()
        every { delegate.open(spec) } returns 123L

        val bytes = dataSource.open(spec)

        Assert.assertEquals(123L, bytes)
        verify(exactly = 1) { delegate.open(spec) }
        confirmVerified(delegate)
    }

    @Test
    fun open_opens_local_file_when_file_is_downloaded() {
        val id = 42L
        val tempFile = File.createTempFile("test_media", ".mp3")
        val ocFile = OCFile("/remote/path/file.mp3").apply {
            localId = id
            setStoragePath(tempFile.absolutePath)
            mimeType = "audio/mpeg"
        }

        assert(tempFile.exists())

        every { fileStore.getFileByLocalId(id) } returns ocFile
        every { delegate.open(any()) } returns 555L

        val bytes = dataSource.open(remoteFileSpec(id))

        Assert.assertEquals(555L, bytes)
        verify { fileStore.getFileByLocalId(id) }
        verify { delegate.open(match { it.uri == ocFile.storageUri }) }
        confirmVerified(fileStore, delegate)
    }

    @Test
    fun open_opens_remote_stream_when_file_not_downloaded() {
        val id = 7L
        every { fileStore.getFileByLocalId(id) } returns null

        val streamOperation = mockk<StreamMediaFileOperation>()
        every { streamOperationFactory.create(id) } returns streamOperation

        val result = RemoteOperationResult<Any>(RemoteOperationResult.ResultCode.OK)
        result.data = arrayListOf("https://stream/url.m3u8")
        every { streamOperation.execute(client) } returns result
        every { delegate.open(any()) } returns 777L

        val bytes = dataSource.open(remoteFileSpec(id))

        Assert.assertEquals(777L, bytes)
        verifySequence {
            fileStore.getFileByLocalId(id)
            streamOperationFactory.create(id)
            streamOperation.execute(client)
            delegate.open(match { it.uri.toString() == "https://stream/url.m3u8" })
        }
    }

    @Test
    fun open_throws_IOException_when_remote_operation_fails() {
        val id = 9L
        every { fileStore.getFileByLocalId(id) } returns null

        val streamOperation = mockk<StreamMediaFileOperation>()
        every { streamOperationFactory.create(id) } returns streamOperation

        val result = mockk<RemoteOperationResult<Any>>()
        every { result.isSuccess } returns false
        every { result.exception } returns RuntimeException("boom")
        every { streamOperation.execute(client) } returns result

        Assert.assertThrows(IOException::class.java) {
            dataSource.open(remoteFileSpec(id))
        }

        verify {
            fileStore.getFileByLocalId(id)
            streamOperationFactory.create(id)
            streamOperation.execute(client)
        }
        verify(exactly = 0) { delegate.open(any()) }
    }

    private fun remoteFileSpec(id: Long) = DataSpec.Builder()
        .setUri(getPlaybackUri(id))
        .build()
}
