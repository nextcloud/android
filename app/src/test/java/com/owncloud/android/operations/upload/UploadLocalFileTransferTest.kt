/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.utils.FileStorageUtils
import io.mockk.every
import io.mockk.mockk
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
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class UploadLocalFileTransferTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val contentResolver: ContentResolver = mockk()
    private val context: Context = mockk {
        every { contentResolver } returns this@UploadLocalFileTransferTest.contentResolver
    }
    private val contentUri: Uri = mockk()
    private val cancellationRequested = AtomicBoolean(false)

    private lateinit var sourceFile: File
    private lateinit var temporalFolder: File

    @Before
    fun setUp() {
        sourceFile = temporaryFolder.newFile(SOURCE_NAME).apply { writeText(CONTENT) }
        temporalFolder = temporaryFolder.newFolder(TEMPORAL_FOLDER_NAME)

        mockkStatic(FileStorageUtils::class)
        every { FileStorageUtils.usableSpace } returns Long.MAX_VALUE
        every { FileStorageUtils.getInternalTemporalPath(ACCOUNT_NAME, context) } returns temporalFolder.absolutePath

        mockkStatic(Uri::class)
        every { Uri.parse(CONTENT_URI) } returns contentUri
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createTransfer(
        originalStoragePath: String = sourceFile.absolutePath,
        localBehaviour: Int = FileUploadWorker.LOCAL_BEHAVIOUR_COPY
    ) = UploadLocalFileTransfer(context, ACCOUNT_NAME, originalStoragePath, localBehaviour, cancellationRequested)

    private fun targetFile(vararg segments: String) = File(temporaryFolder.root, segments.joinToString(File.separator))

    @Test
    fun `copy writes source content into target`() {
        val target = targetFile(TARGET_NAME)

        val result = createTransfer().copy(sourceFile, target)

        assertTrue(result.isSuccess)
        assertEquals(CONTENT, target.readText())
        assertTrue(sourceFile.exists())
    }

    @Test
    fun `copy creates missing parent folders`() {
        val target = targetFile(NESTED_FOLDER_NAME, NESTED_FOLDER_NAME, TARGET_NAME)

        val result = createTransfer().copy(sourceFile, target)

        assertTrue(result.isSuccess)
        assertEquals(CONTENT, target.readText())
    }

    @Test
    fun `copy overwrites existing target`() {
        val target = targetFile(TARGET_NAME).apply { writeText(OTHER_CONTENT) }

        val result = createTransfer().copy(sourceFile, target)

        assertTrue(result.isSuccess)
        assertEquals(CONTENT, target.readText())
    }

    @Test
    fun `copy returns local storage full when space is not enough`() {
        every { FileStorageUtils.usableSpace } returns 0L
        val target = targetFile(TARGET_NAME)

        val result = createTransfer().copy(sourceFile, target)

        assertEquals(ResultCode.LOCAL_STORAGE_FULL, result.code)
        assertFalse(target.exists())
    }

    @Test
    fun `copy returns cannot create file when parent is a file`() {
        val parentAsFile = temporaryFolder.newFile(PARENT_AS_FILE_NAME)

        val result = createTransfer().copy(sourceFile, File(parentAsFile, TARGET_NAME))

        assertEquals(ResultCode.CANNOT_CREATE_FILE, result.code)
    }

    @Test
    fun `copy returns local storage not copied when source is missing`() {
        val missingSource = targetFile(MISSING_NAME)

        val result = createTransfer(originalStoragePath = missingSource.absolutePath)
            .copy(missingSource, targetFile(TARGET_NAME))

        assertEquals(ResultCode.LOCAL_STORAGE_NOT_COPIED, result.code)
    }

    @Test
    fun `copy skips content when target is original file`() {
        val result = createTransfer().copy(sourceFile, sourceFile)

        assertTrue(result.isSuccess)
        assertEquals(CONTENT, sourceFile.readText())
    }

    @Test
    fun `copy returns cancelled result when cancellation requested`() {
        cancellationRequested.set(true)
        val target = targetFile(TARGET_NAME)

        val result = createTransfer().copy(sourceFile, target)

        assertEquals(ResultCode.CANCELLED, result.code)
        assertEquals(0L, target.length())
    }

    @Test
    fun `copy reads content uri through content resolver`() {
        every { contentResolver.openInputStream(contentUri) } returns ByteArrayInputStream(OTHER_CONTENT.toByteArray())
        val target = targetFile(TARGET_NAME)

        val result = createTransfer(originalStoragePath = CONTENT_URI).copy(sourceFile, target)

        assertTrue(result.isSuccess)
        assertEquals(OTHER_CONTENT, target.readText())
    }

    @Test
    fun `copy returns local storage not copied when content resolver returns null`() {
        every { contentResolver.openInputStream(contentUri) } returns null

        val result = createTransfer(originalStoragePath = CONTENT_URI).copy(sourceFile, targetFile(TARGET_NAME))

        assertEquals(ResultCode.LOCAL_STORAGE_NOT_COPIED, result.code)
    }

    @Test
    fun `move renames source into target`() {
        val target = targetFile(NESTED_FOLDER_NAME, TARGET_NAME)

        createTransfer().move(sourceFile, target)

        assertFalse(sourceFile.exists())
        assertEquals(CONTENT, target.readText())
    }

    @Test
    fun `move keeps file when source and target are same`() {
        createTransfer().move(sourceFile, File(sourceFile.absolutePath))

        assertEquals(CONTENT, sourceFile.readText())
    }

    @Test
    fun `copyToTemporalFileIfNeeded copies into temporal path for copy behaviour`() {
        val file = OCFile(REMOTE_PATH)

        val result = createTransfer().copyToTemporalFileIfNeeded(file, sourceFile, targetFile(TARGET_NAME).path)

        val expectedTemporalPath = temporalFolder.absolutePath + REMOTE_PATH
        assertTrue(result.isSuccess)
        assertEquals(expectedTemporalPath, file.storagePath)
        assertEquals(CONTENT, File(expectedTemporalPath).readText())
    }

    @Test
    fun `copyToTemporalFileIfNeeded skips copy when original is expected path`() {
        val file = OCFile(REMOTE_PATH).apply { storagePath = sourceFile.absolutePath }

        val result = createTransfer().copyToTemporalFileIfNeeded(file, sourceFile, sourceFile.absolutePath)

        assertTrue(result.isSuccess)
        assertEquals(sourceFile.absolutePath, file.storagePath)
        verify(exactly = 0) { FileStorageUtils.getInternalTemporalPath(any(), any()) }
    }

    @Test
    fun `copyToTemporalFileIfNeeded skips copy for move behaviour`() {
        val file = OCFile(REMOTE_PATH).apply { storagePath = sourceFile.absolutePath }

        val result = createTransfer(localBehaviour = FileUploadWorker.LOCAL_BEHAVIOUR_MOVE)
            .copyToTemporalFileIfNeeded(file, sourceFile, targetFile(TARGET_NAME).path)

        assertTrue(result.isSuccess)
        assertEquals(sourceFile.absolutePath, file.storagePath)
    }

    @Test(expected = OperationCancelledException::class)
    fun `copyToTemporalFileIfNeeded throws when cancelled without copy`() {
        cancellationRequested.set(true)

        createTransfer(localBehaviour = FileUploadWorker.LOCAL_BEHAVIOUR_MOVE)
            .copyToTemporalFileIfNeeded(OCFile(REMOTE_PATH), sourceFile, targetFile(TARGET_NAME).path)
    }

    companion object {
        private const val ACCOUNT_NAME = "user@nextcloud.example"
        private const val CONTENT = "local file content"
        private const val OTHER_CONTENT = "content from provider"
        private const val SOURCE_NAME = "source.txt"
        private const val TARGET_NAME = "target.txt"
        private const val MISSING_NAME = "missing.txt"
        private const val PARENT_AS_FILE_NAME = "parent.txt"
        private const val NESTED_FOLDER_NAME = "nested"
        private const val TEMPORAL_FOLDER_NAME = "tmp"
        private const val REMOTE_PATH = "/Photos/target.txt"
        private const val CONTENT_URI = "content://com.example.provider/document/1"
    }
}
