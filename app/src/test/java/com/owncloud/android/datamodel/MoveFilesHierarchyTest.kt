/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.utils.MimeType
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("TooManyFunctions")
class MoveFilesHierarchyTest : MoveFilesTestBase() {

    private val capturedEntities = slot<List<FileEntity>>()

    private fun arrangeFolderMove(
        folderPath: String,
        targetFolderPath: String,
        targetParentPath: String,
        entities: List<FileEntity>
    ) {
        val parent = OCFile(targetParentPath).apply { fileId = 99L; mimeType = MimeType.DIRECTORY }
        every { manager.getFileByPath(targetParentPath) } returns parent
        every { mockFileDao.getFolderWithDescendants("$folderPath%", ACCOUNT_NAME) } returns entities
        every { mockFileDao.updateAll(capture(capturedEntities)) } returns Unit
        val folder = OCFile(folderPath).apply { fileId = 1; mimeType = MimeType.DIRECTORY }
        manager.moveLocalFile(folder, targetFolderPath, targetParentPath)
    }

    @Test
    fun testMoveLocalFileWhenMovingFolderWithChildrenShouldUpdateAllDescendantPaths() {
        val folderPath = "/docs/"
        val targetFolderPath = "/archive/docs/"
        val entities = listOf(
            createFileEntity(id = 1L, path = folderPath),
            createFileEntity(id = 2L, path = "${folderPath}report.pdf"),
            createFileEntity(id = 3L, path = "${folderPath}notes.txt")
        )

        arrangeFolderMove(folderPath, targetFolderPath, "/archive/", entities)

        val updated = capturedEntities.captured
        assertEquals(3, updated.size)
        assertEquals(targetFolderPath, updated.find { it.id == 1L }?.path)
        assertEquals("${targetFolderPath}report.pdf", updated.find { it.id == 2L }?.path)
        assertEquals("${targetFolderPath}notes.txt", updated.find { it.id == 3L }?.path)
    }

    @Test
    fun testMoveLocalFileWhenMovingFolderWithDeepNestedHierarchyShouldUpdateAllLevelPaths() {
        val folderPath = "/projects/"
        val targetFolderPath = "/archive/projects/"
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
        assertEquals(targetFolderPath, updated.find { it.id == 1L }?.path)
        assertEquals("${targetFolderPath}src/", updated.find { it.id == 2L }?.path)
        assertEquals("${targetFolderPath}src/main/App.kt", updated.find { it.id == 4L }?.path)
        assertEquals("${targetFolderPath}src/test/AppTest.kt", updated.find { it.id == 6L }?.path)
        assertEquals("${targetFolderPath}README.md", updated.find { it.id == 7L }?.path)
    }

    @Test
    fun testMoveLocalFileWhenMovingFolderShouldOnlyUpdateParentForTopLevelMovedFolder() {
        val folderPath = "/docs/"
        val targetFolderPath = "/archive/docs/"
        val originalParentId = 10L
        val targetParentId = 99L
        val entities = listOf(
            createFileEntity(id = 1L, path = folderPath, parent = originalParentId),
            createFileEntity(id = 2L, path = "${folderPath}child.txt", parent = 1L),
            createFileEntity(id = 3L, path = "${folderPath}sub/", parent = 1L),
            createFileEntity(id = 4L, path = "${folderPath}sub/deep.txt", parent = 3L)
        )

        arrangeFolderMove(folderPath, targetFolderPath, "/archive/", entities)

        val updated = capturedEntities.captured
        assertEquals(targetParentId, updated.find { it.id == 1L }?.parent)
        assertEquals(1L, updated.find { it.id == 2L }?.parent)
        assertEquals(1L, updated.find { it.id == 3L }?.parent)
        assertEquals(3L, updated.find { it.id == 4L }?.parent)
    }

    @Test
    fun testMoveLocalFileWhenFolderContainsMixedMediaAndDocumentsShouldTriggerMediaScanOnlyForMedia() {
        val folderPath = "/gallery/"
        val targetFolderPath = "/backup/gallery/"
        val photoStorage = "$SAVE_PATH${folderPath}photo.jpg"
        val docStorage = "$SAVE_PATH${folderPath}notes.pdf"
        val videoStorage = "$SAVE_PATH${folderPath}clip.mp4"
        val entities = listOf(
            createFileEntity(id = 1L, path = folderPath),
            createFileEntity(id = 2L, path = "${folderPath}photo.jpg",
                storagePath = photoStorage, contentType = "image/jpeg"),
            createFileEntity(id = 3L, path = "${folderPath}notes.pdf",
                storagePath = docStorage, contentType = "application/pdf"),
            createFileEntity(id = 4L, path = "${folderPath}clip.mp4",
                storagePath = videoStorage, contentType = "video/mp4")
        )

        arrangeFolderMove(folderPath, targetFolderPath, "/backup/", entities)

        verify(exactly = 1) { manager.deleteFileInMediaScan(photoStorage) }
        verify(exactly = 1) { manager.deleteFileInMediaScan(videoStorage) }
        verify(exactly = 0) { manager.deleteFileInMediaScan(docStorage) }
    }
}