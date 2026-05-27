/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@Suppress("TooManyFunctions")
class MoveFilesHierarchyTest : MoveFilesTestBase() {

    private lateinit var tempDir: File
    private val capturedEntities = slot<List<FileEntity>>()

    @Before
    fun setUpTempDir() {
        tempDir = Files.createTempDirectory("moveHierarchyTest").toFile()
        every { FileStorageUtils.getSavePath(any()) } returns tempDir.absolutePath
        every { FileStorageUtils.getDefaultSavePathFor(any(), any()) } answers {
            tempDir.absolutePath + secondArg<OCFile>().remotePath
        }
    }

    @After
    fun tearDownTempDir() {
        tempDir.deleteRecursively()
    }

    @Suppress("NestedBlockDepth")
    private fun arrangeFolderMove(
        folderPath: String,
        targetFolderPath: String,
        targetParentPath: String,
        entities: List<FileEntity>
    ) {
        val parent = OCFile(targetParentPath).apply {
            fileId = 99L
            mimeType = MimeType.DIRECTORY
        }
        every { manager.getFileByPath(targetParentPath) } returns parent
        every { mockFileDao.getFolderWithDescendants("$folderPath%", ACCOUNT_NAME) } returns entities
        every { mockFileDao.updateAll(capture(capturedEntities)) } returns Unit
        val folder = OCFile(folderPath).apply {
            fileId = 1
            mimeType = MimeType.DIRECTORY
        }

        try {
            for (entity in entities) {
                // entity.path may be nullable; treat as empty string if so
                val path = entity.path.orEmpty()
                val relative = path.removePrefix("/")
                val local = File(tempDir, relative)
                if (path.endsWith("/")) {
                    local.mkdirs()
                } else {
                    local.parentFile?.mkdirs()
                    if (!local.exists()) local.createNewFile()
                }
            }
        } catch (_: Exception) {
        }

        manager.moveLocalFile(folder, targetFolderPath, targetParentPath)
    }

    @Test
    fun testMoveLocalFileWhenMovingFolderWithChildrenShouldUpdateAllDescendantPaths() {
        val folderPath = "/docs/"
        val targetFolderPath = "/archive/docs/"
        val expectedFolderPath = targetFolderPath
        val expectedReportPath = "${targetFolderPath}report.pdf"
        val expectedNotesPath = "${targetFolderPath}notes.txt"
        val entities = listOf(
            createFileEntity(id = 1L, path = folderPath),
            createFileEntity(id = 2L, path = "${folderPath}report.pdf"),
            createFileEntity(id = 3L, path = "${folderPath}notes.txt")
        )

        arrangeFolderMove(folderPath, targetFolderPath, "/archive/", entities)

        val updated = capturedEntities.captured
        assertEquals(3, updated.size)
        assertEquals(expectedFolderPath, updated.find { it.id == 1L }?.path)
        assertEquals(expectedReportPath, updated.find { it.id == 2L }?.path)
        assertEquals(expectedNotesPath, updated.find { it.id == 3L }?.path)
    }

    @Test
    fun testMoveLocalFileWhenMovingFolderWithDeepNestedHierarchyShouldUpdateAllLevelPaths() {
        val folderPath = "/projects/"
        val targetFolderPath = "/archive/projects/"
        val expectedFolderPath = targetFolderPath
        val expectedSrcPath = "${targetFolderPath}src/"
        val expectedMainPath = "${targetFolderPath}src/main/"
        val expectedAppPath = "${targetFolderPath}src/main/App.kt"
        val expectedTestPath = "${targetFolderPath}src/test/"
        val expectedTestFilePath = "${targetFolderPath}src/test/AppTest.kt"
        val expectedReadmePath = "${targetFolderPath}README.md"
        val entities = listOf(
            createFileEntity(id = 1L, path = folderPath),
            createFileEntity(id = 2L, path = "${folderPath}src/"),
            createFileEntity(id = 3L, path = "${folderPath}src/main/"),
            createFileEntity(id = 4L, path = "${folderPath}src/main/App.kt"),
            createFileEntity(id = 5L, path = "${folderPath}src/test/"),
            createFileEntity(id = 6L, path = "${folderPath}src/test/AppTest.kt"),
            createFileEntity(id = 7L, path = "${folderPath}README.md")
        )

        arrangeFolderMove(folderPath, targetFolderPath, "/archive/", entities)

        val updated = capturedEntities.captured
        assertEquals(7, updated.size)
        assertEquals(expectedFolderPath, updated.find { it.id == 1L }?.path)
        assertEquals(expectedSrcPath, updated.find { it.id == 2L }?.path)
        assertEquals(expectedMainPath, updated.find { it.id == 3L }?.path)
        assertEquals(expectedAppPath, updated.find { it.id == 4L }?.path)
        assertEquals(expectedTestPath, updated.find { it.id == 5L }?.path)
        assertEquals(expectedTestFilePath, updated.find { it.id == 6L }?.path)
        assertEquals(expectedReadmePath, updated.find { it.id == 7L }?.path)
    }

    @Test
    fun testMoveLocalFileWhenMovingFolderShouldOnlyUpdateParentForTopLevelMovedFolder() {
        val folderPath = "/docs/"
        val targetFolderPath = "/archive/docs/"
        val expectedTopLevelParentId = 99L
        val expectedChildParentId = 1L
        val expectedSubFolderParentId = 1L
        val expectedDeepFileParentId = 3L
        val entities = listOf(
            createFileEntity(id = 1L, path = folderPath, parent = 10L),
            createFileEntity(id = 2L, path = "${folderPath}child.txt", parent = 1L),
            createFileEntity(id = 3L, path = "${folderPath}sub/", parent = 1L),
            createFileEntity(id = 4L, path = "${folderPath}sub/deep.txt", parent = 3L)
        )

        arrangeFolderMove(folderPath, targetFolderPath, "/archive/", entities)

        val updated = capturedEntities.captured
        assertEquals(expectedTopLevelParentId, updated.find { it.id == 1L }?.parent)
        assertEquals(expectedChildParentId, updated.find { it.id == 2L }?.parent)
        assertEquals(expectedSubFolderParentId, updated.find { it.id == 3L }?.parent)
        assertEquals(expectedDeepFileParentId, updated.find { it.id == 4L }?.parent)
    }

    @Test
    fun testMoveLocalFileWhenFolderContainsMixedMediaAndDocumentsShouldTriggerMediaScanOnlyForMedia() {
        val folderPath = "/gallery/"
        val targetFolderPath = "/backup/gallery/"
        val savePath = tempDir.absolutePath
        val photoStorage = "$savePath${folderPath}photo.jpg"
        val docStorage = "$savePath${folderPath}notes.pdf"
        val videoStorage = "$savePath${folderPath}clip.mp4"
        val expectedPhotoStorageAfterMove = "$savePath${targetFolderPath}photo.jpg"
        val expectedDocStorageAfterMove = "$savePath${targetFolderPath}notes.pdf"
        val expectedVideoStorageAfterMove = "$savePath${targetFolderPath}clip.mp4"
        val entities = listOf(
            createFileEntity(id = 1L, path = folderPath),
            createFileEntity(
                id = 2L,
                path = "${folderPath}photo.jpg",
                storagePath = photoStorage,
                contentType = "image/jpeg"
            ),
            createFileEntity(
                id = 3L,
                path = "${folderPath}notes.pdf",
                storagePath = docStorage,
                contentType = "application/pdf"
            ),
            createFileEntity(
                id = 4L,
                path = "${folderPath}clip.mp4",
                storagePath = videoStorage,
                contentType = "video/mp4"
            )
        )
        File(tempDir, "gallery").mkdirs()

        arrangeFolderMove(folderPath, targetFolderPath, "/backup/", entities)

        val updated = capturedEntities.captured
        assertEquals(expectedPhotoStorageAfterMove, updated.find { it.id == 2L }?.storagePath)
        assertEquals(expectedDocStorageAfterMove, updated.find { it.id == 3L }?.storagePath)
        assertEquals(expectedVideoStorageAfterMove, updated.find { it.id == 4L }?.storagePath)
        verify(exactly = 1) { manager.deleteFileInMediaScan(photoStorage) }
        verify(exactly = 1) { manager.deleteFileInMediaScan(videoStorage) }
        verify(exactly = 0) { manager.deleteFileInMediaScan(docStorage) }
    }
}
