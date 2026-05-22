/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import com.owncloud.android.utils.MimeType
import io.mockk.every
import io.mockk.verify
import org.junit.Test

@Suppress("TooManyFunctions")
class MoveFilesGuardTest : MoveFilesTestBase() {

    @Test
    fun testMoveLocalFileWhenFileIsNullShouldReturnEarlyWithoutInteractingWithDatabase() {
        manager.moveLocalFile(null, TARGET_PATH, TARGET_PARENT_PATH)

        verify(exactly = 0) { mockFileDao.getFolderWithDescendants(any(), any()) }
        verify(exactly = 0) { mockFileDao.updateAll(any()) }
    }

    @Test
    fun testMoveLocalFileWhenFileDoesNotExistShouldReturnEarlyWithoutInteractingWithDatabase() {
        val file = OCFile(OLD_PATH).apply { fileId = 0 }

        manager.moveLocalFile(file, TARGET_PATH, TARGET_PARENT_PATH)

        verify(exactly = 0) { mockFileDao.getFolderWithDescendants(any(), any()) }
        verify(exactly = 0) { mockFileDao.updateAll(any()) }
    }

    @Test
    fun testMoveLocalFileWhenFileNameIsRootPathShouldReturnEarlyWithoutInteractingWithDatabase() {
        val rootFile = OCFile(OCFile.ROOT_PATH).apply {
            fileId = 1
            mimeType = MimeType.DIRECTORY
        }

        manager.moveLocalFile(rootFile, TARGET_PATH, TARGET_PARENT_PATH)

        verify(exactly = 0) { mockFileDao.getFolderWithDescendants(any(), any()) }
        verify(exactly = 0) { mockFileDao.updateAll(any()) }
    }

    @Test
    fun testMoveLocalFileWhenSourceAndTargetPathsAreIdenticalShouldReturnEarlyWithoutInteractingWithDatabase() {
        val file = OCFile(OLD_PATH).apply { fileId = 1 }

        manager.moveLocalFile(file, OLD_PATH, TARGET_PARENT_PATH)

        verify(exactly = 0) { mockFileDao.getFolderWithDescendants(any(), any()) }
        verify(exactly = 0) { mockFileDao.updateAll(any()) }
    }

    @Test
    fun testMoveLocalFileWhenTargetParentNotFoundShouldReturnEarlyWithoutInteractingWithDatabase() {
        val file = OCFile(OLD_PATH).apply { fileId = 1 }
        // getFileByPath returns null by default from base setUp

        manager.moveLocalFile(file, TARGET_PATH, TARGET_PARENT_PATH)

        verify(exactly = 0) { mockFileDao.getFolderWithDescendants(any(), any()) }
        verify(exactly = 0) { mockFileDao.updateAll(any()) }
    }

    @Test
    fun testMoveLocalFileWhenTargetParentIsNotFolderShouldReturnEarlyWithoutInteractingWithDatabase() {
        val file = OCFile(OLD_PATH).apply { fileId = 1 }
        val notAFolder = OCFile(TARGET_PARENT_PATH).apply {
            fileId = 99
            mimeType = "application/pdf"
        }
        every { manager.getFileByPath(TARGET_PARENT_PATH) } returns notAFolder

        manager.moveLocalFile(file, TARGET_PATH, TARGET_PARENT_PATH)

        verify(exactly = 0) { mockFileDao.getFolderWithDescendants(any(), any()) }
        verify(exactly = 0) { mockFileDao.updateAll(any()) }
    }
}