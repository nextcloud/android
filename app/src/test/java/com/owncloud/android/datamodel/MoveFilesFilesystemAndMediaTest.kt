/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import android.media.MediaScannerConnection
import com.owncloud.android.utils.FileStorageUtils
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@Suppress("TooManyFunctions")
class MoveFilesFilesystemAndMediaTest : MoveFilesTestBase() {

    private lateinit var tempDir: File

    @Before
    fun setUpTempDir() {
        tempDir = Files.createTempDirectory("moveLocalFileTest").toFile()
        every { FileStorageUtils.getSavePath(any()) } returns tempDir.absolutePath
        every { FileStorageUtils.getDefaultSavePathFor(any(), any()) } answers {
            tempDir.absolutePath + secondArg<OCFile>().remotePath
        }
    }

    @After
    fun tearDownTempDir() {
        tempDir.deleteRecursively()
    }

    private fun doMove(
        oldPath: String = OLD_PATH,
        targetPath: String = TARGET_PATH,
        entities: List<com.nextcloud.client.database.entity.FileEntity> = emptyList()
    ) {
        stubTargetParent()
        every { mockFileDao.getFolderWithDescendants("$oldPath%", ACCOUNT_NAME) } returns entities
        manager.moveLocalFile(OCFile(oldPath).apply { fileId = 1 }, targetPath, TARGET_PARENT_PATH)
    }

    @Test
    fun testMoveLocalFileWhenNoLocalFilePresentShouldSkipRenameAndMediaScan() {
        val expectedSourcePath = "${tempDir.absolutePath}$OLD_PATH"
        val expectedTargetPath = "${tempDir.absolutePath}$TARGET_PATH"

        doMove()

        assertFalse(File(expectedSourcePath).exists())
        assertFalse(File(expectedTargetPath).exists())
        verify(exactly = 0) { manager.deleteFileInMediaScan(any()) }
        verify(exactly = 0) { MediaScannerConnection.scanFile(any(), any(), any(), any()) }
    }

    @Test
    fun testMoveLocalFileWhenNoLocalCopyExistsShouldStillUpdateDatabasePaths() {
        val folderPath = "/docs/"
        val targetFolderPath = "/archive/docs/"
        val capturedEntities = slot<List<com.nextcloud.client.database.entity.FileEntity>>()
        every { mockFileDao.updateAll(capture(capturedEntities)) } returns Unit

        doMove(
            oldPath = folderPath,
            targetPath = targetFolderPath,
            entities = listOf(
                createFileEntity(id = 1L, path = folderPath),
                createFileEntity(id = 2L, path = "${folderPath}report.pdf")
            )
        )

        assertEquals(
            listOf(targetFolderPath, "${targetFolderPath}report.pdf"),
            capturedEntities.captured.map { it.path }
        )
    }

    @Test
    fun testMoveLocalFileWhenLocalFilePresentShouldRenameToTargetLocation() {
        val expectedSourcePath = "${tempDir.absolutePath}$OLD_PATH"
        val expectedTargetPath = "${tempDir.absolutePath}$TARGET_PATH"
        File(expectedSourcePath).also {
            it.parentFile?.mkdirs()
            it.createNewFile()
        }

        doMove()

        assertFalse("Source at $expectedSourcePath should no longer exist", File(expectedSourcePath).exists())
        assertTrue("Target at $expectedTargetPath should exist after move", File(expectedTargetPath).exists())
        assertEquals(expectedTargetPath, File(expectedTargetPath).absolutePath)
    }

    @Test
    fun testMoveLocalFileWhenRenameFailsShouldNotTriggerMediaScan() {
        val oldStoragePath = "${tempDir.absolutePath}$OLD_PATH"
        val mediaEntity = createFileEntity(path = OLD_PATH, storagePath = oldStoragePath, contentType = "image/jpeg")
        // Source file intentionally NOT created → renameTo returns false

        doMove(entities = listOf(mediaEntity))

        verify(exactly = 0) { manager.deleteFileInMediaScan(any()) }
        verify(exactly = 0) { MediaScannerConnection.scanFile(any(), any(), any(), any()) }
    }

    @Test
    fun testMoveLocalFileWhenMediaFileIsMovedShouldDeleteFromMediaScanAtOriginalPath() {
        val expectedDeletedPath = "${tempDir.absolutePath}$OLD_PATH"
        File(expectedDeletedPath).also {
            it.parentFile?.mkdirs()
            it.createNewFile()
        }
        val mediaEntity = createFileEntity(
            path = OLD_PATH,
            storagePath = expectedDeletedPath,
            contentType = "image/jpeg"
        )

        doMove(entities = listOf(mediaEntity))

        verify(exactly = 1) { manager.deleteFileInMediaScan(expectedDeletedPath) }
    }

    @Test
    fun testMoveLocalFileWhenMediaFileIsMovedShouldTriggerMediaScanAtNewStoragePath() {
        val oldStoragePath = "${tempDir.absolutePath}$OLD_PATH"
        val expectedNewStoragePath = "${tempDir.absolutePath}$TARGET_PATH"
        File(oldStoragePath).also {
            it.parentFile?.mkdirs()
            it.createNewFile()
        }
        val mediaEntity = createFileEntity(
            path = OLD_PATH,
            storagePath = oldStoragePath,
            contentType = "image/jpeg"
        )

        doMove(entities = listOf(mediaEntity))

        verify {
            MediaScannerConnection.scanFile(
                any(),
                match { paths -> paths.single() == expectedNewStoragePath },
                any(),
                any()
            )
        }
    }

    @Test
    fun testMoveLocalFileWhenNonMediaFileIsMovedShouldNotTriggerAnyMediaScan() {
        val oldStoragePath = "${tempDir.absolutePath}$OLD_PATH"
        File(oldStoragePath).also {
            it.parentFile?.mkdirs()
            it.createNewFile()
        }
        val docEntity = createFileEntity(
            path = OLD_PATH,
            storagePath = oldStoragePath,
            contentType = "application/pdf"
        )

        doMove(entities = listOf(docEntity))

        verify(exactly = 0) { manager.deleteFileInMediaScan(any()) }
        verify(exactly = 0) { MediaScannerConnection.scanFile(any(), any(), any(), any()) }
    }
}
