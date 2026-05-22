/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import com.nextcloud.client.database.entity.FileEntity
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@Suppress("TooManyFunctions")
class MoveFileEntitiesUpdateTest : MoveFilesTestBase() {

    private val capturedEntities = slot<List<FileEntity>>()

    private fun arrangeAndMove(entities: List<FileEntity>, targetPath: String = TARGET_PATH) {
        stubTargetParent()
        every { mockFileDao.getFolderWithDescendants("${entities.first().path}%", ACCOUNT_NAME) } returns entities
        every { mockFileDao.updateAll(capture(capturedEntities)) } returns Unit
        val file = OCFile(entities.first().path!!).apply { fileId = 1 }
        manager.moveLocalFile(file, targetPath, TARGET_PARENT_PATH)
    }

    @Test
    fun testMoveLocalFileWhenValidFileShouldCallUpdateAllOnFileDao() {
        val entities = listOf(createFileEntity(path = OLD_PATH))
        arrangeAndMove(entities)

        verify(exactly = 1) { mockFileDao.updateAll(any()) }
    }

    @Test
    fun testMoveLocalFileWhenValidFileShouldUpdatePathToTargetPath() {
        val entities = listOf(createFileEntity(path = OLD_PATH))
        val expectedPath = TARGET_PATH

        arrangeAndMove(entities)

        assertEquals(expectedPath, capturedEntities.captured.single().path)
    }

    @Test
    fun testMoveLocalFileWhenNonEncryptedFileShouldUpdatePathDecryptedToNewPath() {
        val entities = listOf(createFileEntity(path = OLD_PATH, pathDecrypted = OLD_PATH, isEncrypted = 0))
        val expectedDecryptedPath = TARGET_PATH

        arrangeAndMove(entities)

        assertEquals(expectedDecryptedPath, capturedEntities.captured.single().pathDecrypted)
    }

    @Test
    fun testMoveLocalFileWhenEncryptedFileShouldNotUpdatePathDecrypted() {
        val originalDecryptedPath = "/documents/encrypted_name"
        val entities = listOf(
            createFileEntity(path = OLD_PATH, pathDecrypted = originalDecryptedPath, isEncrypted = 1)
        )
        val expectedDecryptedPath = originalDecryptedPath

        arrangeAndMove(entities)

        assertEquals(expectedDecryptedPath, capturedEntities.captured.single().pathDecrypted)
    }

    @Test
    fun testMoveLocalFileWhenFileHasStoragePathUnderSavePathShouldUpdateStoragePath() {
        val originalStorage = "$SAVE_PATH$OLD_PATH"
        val expectedStoragePath = "$SAVE_PATH$TARGET_PATH"
        val entities = listOf(createFileEntity(path = OLD_PATH, storagePath = originalStorage))

        arrangeAndMove(entities)

        assertEquals(expectedStoragePath, capturedEntities.captured.single().storagePath)
    }

    @Test
    fun testMoveLocalFileWhenFileHasStoragePathOutsideSavePathShouldKeepOriginalStoragePath() {
        val originalStorage = "/sdcard/downloads/report.pdf"
        val expectedStoragePath = originalStorage
        val entities = listOf(createFileEntity(path = OLD_PATH, storagePath = originalStorage))

        arrangeAndMove(entities)

        assertEquals(expectedStoragePath, capturedEntities.captured.single().storagePath)
    }

    @Test
    fun testMoveLocalFileWhenFileHasNoStoragePathShouldKeepStoragePathNull() {
        val entities = listOf(createFileEntity(path = OLD_PATH, storagePath = null))

        arrangeAndMove(entities)

        assertNull(capturedEntities.captured.single().storagePath)
    }

    @Test
    fun testMoveLocalFileWhenMovingFileShouldUpdateParentIdToTargetParentId() {
        val expectedParentId = 99L
        val entities = listOf(createFileEntity(path = OLD_PATH, parent = 10L))
        every { mockFileDao.getFolderWithDescendants("$OLD_PATH%", ACCOUNT_NAME) } returns entities
        every { mockFileDao.updateAll(capture(capturedEntities)) } returns Unit
        val parent = OCFile(TARGET_PARENT_PATH).apply {
            fileId = expectedParentId
            mimeType = com.owncloud.android.utils.MimeType.DIRECTORY
        }
        every { manager.getFileByPath(TARGET_PARENT_PATH) } returns parent

        manager.moveLocalFile(OCFile(OLD_PATH).apply { fileId = 1 }, TARGET_PATH, TARGET_PARENT_PATH)

        assertEquals(expectedParentId, capturedEntities.captured.single().parent)
    }
}
