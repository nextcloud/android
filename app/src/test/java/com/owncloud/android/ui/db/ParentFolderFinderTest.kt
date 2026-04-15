/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.db

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.fragment.helper.ParentFolderFinder
import com.owncloud.android.utils.MimeType
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.Test

@Suppress("TooManyFunctions", "MagicNumber")
class ParentFolderFinderTest {

    private lateinit var storageManager: FileDataStorageManager
    private lateinit var finder: ParentFolderFinder

    @Before
    fun setup() {
        storageManager = mockk(relaxed = true)
        finder = ParentFolderFinder()
    }

    @Test
    fun getParentWithNullFileReturnsZeroAndNull() {
        val result = finder.getParent(null, storageManager)

        assertEquals(0, result.first)
        assertNull(result.second)
    }

    @Test
    fun getParentWithNullStorageManagerReturnsZeroAndFile() {
        val file = OCFile("/test.txt")

        val result = finder.getParent(file, null)

        assertEquals(0, result.first)
        assertEquals(file, result.second)
    }

    @Test
    fun getParentWithValidParentIdReturnsOneAndParentFile() {
        val parentFolder = OCFile("/parent/").apply {
            fileId = 10
            mimeType = MimeType.DIRECTORY
        }
        val childFile = OCFile("/parent/child.txt").apply {
            fileId = 11
            parentId = 10
        }

        every { storageManager.getFileById(10L) } returns parentFolder

        val result = finder.getParent(childFile, storageManager)

        assertEquals(1, result.first)
        assertEquals(parentFolder, result.second)
    }

    @Test
    fun getParentWithInvalidParentIdWalksUpPath() {
        val parentFolder = OCFile("/parent/").apply {
            fileId = 10
            mimeType = MimeType.DIRECTORY
        }
        val childFile = OCFile("/parent/child.txt").apply {
            fileId = 11
            parentId = 99
            remotePath = "/parent/child.txt"
        }

        every { storageManager.getFileById(99L) } returns null
        every { storageManager.getFileByEncryptedRemotePath("/parent/") } returns parentFolder

        val result = finder.getParent(childFile, storageManager)

        assertEquals(1, result.first)
        assertEquals(parentFolder, result.second)
    }

    @Test
    fun getParentWalksMultipleLevelsUp() {
        val grandParentFolder = OCFile("/parent/").apply {
            fileId = 10
            mimeType = MimeType.DIRECTORY
        }
        val childFile = OCFile("/parent/sub/child.txt").apply {
            fileId = 12
            parentId = 11
            remotePath = "/parent/sub/child.txt"
        }

        every { storageManager.getFileById(11L) } returns null
        every { storageManager.getFileByEncryptedRemotePath("/parent/sub/") } returns null
        every { storageManager.getFileByEncryptedRemotePath("/parent/") } returns grandParentFolder

        val result = finder.getParent(childFile, storageManager)

        assertEquals(2, result.first)
        assertEquals(grandParentFolder, result.second)
    }

    @Test
    fun getParentWalksUpToRootAndReturnsRootFallback() {
        val rootFolder = OCFile(OCFile.ROOT_PATH).apply {
            fileId = 1
            mimeType = MimeType.DIRECTORY
        }
        val childFile = OCFile("/a/b/c.txt").apply {
            fileId = 12
            parentId = 11
            remotePath = "/a/b/c.txt"
        }

        every { storageManager.getFileById(11L) } returns null
        every { storageManager.getFileByEncryptedRemotePath("/a/b/") } returns null
        every { storageManager.getFileByEncryptedRemotePath("/a/") } returns null
        every { storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH) } returns rootFolder

        val result = finder.getParent(childFile, storageManager)

        assertEquals(3, result.first)
        assertEquals(rootFolder, result.second)
    }

    @Test
    fun getParentWalksUpToRootWithTrailingSlash() {
        val rootFolder = OCFile(OCFile.ROOT_PATH).apply {
            fileId = 1
            mimeType = MimeType.DIRECTORY
        }
        val childFolder = OCFile("/folder/").apply {
            fileId = 2
            parentId = 99
            remotePath = "/folder/"
        }

        every { storageManager.getFileById(99L) } returns null
        every { storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH) } returns rootFolder

        val result = finder.getParent(childFolder, storageManager)

        assertEquals(1, result.first)
        assertEquals(rootFolder, result.second)
    }

    @Test
    fun getParentWhenInFolderCAndCIsRemovedRemotelyShouldReturnFolderB() {
        val folderB = OCFile("/A/B/").apply {
            fileId = 20
            mimeType = MimeType.DIRECTORY
        }
        val folderC = OCFile("/A/B/C/").apply {
            fileId = 30
            parentId = 20
            remotePath = "/A/B/C/"
        }

        // Even if C is deleted remotely, we still have its object in memory,
        // and its parentId still correctly maps to B in the local database.
        every { storageManager.getFileById(20L) } returns folderB

        val result = finder.getParent(folderC, storageManager)

        assertEquals(1, result.first)
        assertEquals(folderB, result.second)
    }

    @Test
    fun getParentWhenInFolderCAndBIsRemovedRemotelyShouldReturnFolderA() {
        val folderA = OCFile("/A/").apply {
            fileId = 10
            mimeType = MimeType.DIRECTORY
        }
        val folderC = OCFile("/A/B/C/").apply {
            fileId = 30
            parentId = 20 // Points to B
            remotePath = "/A/B/C/"
        }

        // ID lookup for B fails because it was removed
        every { storageManager.getFileById(20L) } returns null

        // Iteration 1: Tries to find /A/B/ -> also fails
        every { storageManager.getFileByEncryptedRemotePath("/A/B/") } returns null

        // Iteration 2: Tries to find /A/ -> Succeeds!
        every { storageManager.getFileByEncryptedRemotePath("/A/") } returns folderA

        val result = finder.getParent(folderC, storageManager)

        assertEquals(2, result.first)
        assertEquals(folderA, result.second)
    }
}
