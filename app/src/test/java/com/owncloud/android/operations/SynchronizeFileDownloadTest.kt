/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import android.text.TextUtils
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.MockedConstruction
import org.mockito.Mockito

class SynchronizeFileDownloadTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private val user = mockk<User>(relaxed = true)
    private val storage = mockk<FileDataStorageManager>(relaxed = true)
    private val helper = mockk<FileDownloadHelper>(relaxed = true)
    private val client = Mockito.mock(OwnCloudClient::class.java)
    private var downloadResult = RemoteOperationResult<Unit>(ResultCode.UNAUTHORIZED)
    private lateinit var downloads: MockedConstruction<DownloadFileOperation>
    private val localFile = OCFile("/Root/file.txt").apply {
        mimeType = "text/plain"
        etag = "old"
        lastSyncDateForData = Long.MAX_VALUE
    }
    private val serverFile = OCFile("/Root/file.txt").apply { etag = "new" }

    @Before
    fun setUp() {
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { firstArg<CharSequence?>().isNullOrEmpty() }
        mockkObject(FileDownloadHelper.Companion)
        every { FileDownloadHelper.instance() } returns helper
        downloads = Mockito.mockConstruction(DownloadFileOperation::class.java) { operation, _ ->
            Mockito.`when`(operation.execute(client)).thenAnswer { downloadResult }
        }
    }

    @After
    fun tearDown() {
        downloads.close()
        unmockkAll()
    }

    @Test
    fun `missing file download failure is returned to the caller`() {
        val operation = operation()
        assertEquals(ResultCode.UNAUTHORIZED, operation.execute(client).code)
        verify(exactly = 0) { helper.saveFile(any(), any(), any()) }
        assertTrue(operation.transferWasRequested)
    }

    @Test
    fun `existing file refresh failure is returned to the caller`() {
        localFile.storagePath = temporaryFolder.newFile().absolutePath
        assertEquals(ResultCode.UNAUTHORIZED, operation().execute(client).code)
        verify(exactly = 0) { helper.saveFile(any(), any(), any()) }
    }

    @Test
    fun `successful inline download is saved`() {
        downloadResult = RemoteOperationResult(ResultCode.OK)
        assertTrue(operation().execute(client).isSuccess)
        verify(exactly = 1) { helper.saveFile(localFile, any(), storage) }
    }

    @Test
    fun `persistence failure is reported rather than swallowed`() {
        downloadResult = RemoteOperationResult(ResultCode.OK)
        every { helper.saveFile(any(), any(), any()) } throws IllegalStateException("Cannot save downloaded file")
        assertFalse(operation().execute(client).isSuccess)
    }

    @Test
    fun `notification downloads retain asynchronous dispatch`() {
        val operation = operation(notificationWorker = true)
        assertTrue(operation.execute(client).isSuccess)
        assertTrue(operation.transferWasRequested)
        verify(exactly = 1) { helper.downloadFile(user, localFile) }
        assertTrue(downloads.constructed().isEmpty())
    }

    private fun operation(notificationWorker: Boolean = false) = SynchronizeFileOperation(
        localFile,
        serverFile,
        user,
        true,
        context,
        storage,
        notificationWorker
    )
}
