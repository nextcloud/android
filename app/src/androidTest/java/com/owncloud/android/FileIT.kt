/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.operations.SynchronizeFolderOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Tests related to file operations.
 *
 * Every test starts with an empty server and an empty local database, and [AbstractOnServerIT.after] wipes both again
 * afterwards, so no test needs to clean up after itself.
 */
@RunWith(AndroidJUnit4::class)
class FileIT : AbstractOnServerIT() {

    @Test
    fun testCreateFolder() {
        val path = "/testFolder/"
        assertNull(storageManager.getFileByDecryptedRemotePath(path))
        createFolderOrFail(path)
        assertTrue(localFile(path).isFolder)
    }

    @Test
    fun testCreateNonExistingSubFolder() {
        val path = "/subFolder/1/2/3/4/5/"
        assertNull(storageManager.getFileByDecryptedRemotePath(path))
        createFolderOrFail(path)
        assertTrue(localFile(path).isFolder)
    }

    @Test
    fun testRemoteIdNull() {
        storageManager.deleteAllFiles()
        assertEquals(0, storageManager.allFiles.size)

        storageManager.saveFile(OCFile("/123.txt"))
        assertEquals(1, storageManager.allFiles.size)

        storageManager.deleteAllFiles()
        assertEquals(0, storageManager.allFiles.size)
    }

    @Test
    fun testRenameFolder() {
        val folderPath = "/testRenameFolder/"
        val filePath = folderPath + UPLOADED_FILE_NAME
        val renamedFolderPath = "/$NEW_FOLDER_NAME/"
        val renamedFilePath = renamedFolderPath + UPLOADED_FILE_NAME

        // the upload creates the parent folder on the server and in the local database
        uploadFile(getDummyFile(DUMMY_FILE_NAME), filePath)
        synchronizeFolder(folderPath)

        val folderCopy = localCopyOf(folderPath)
        val fileCopy = localCopyOf(filePath)
        assertTrue("$folderPath was not downloaded", folderCopy.exists())
        assertTrue("$filePath was not downloaded", fileCopy.exists())

        val result = RenameFileOperation(folderPath, NEW_FOLDER_NAME, storageManager).execute(targetContext)
        assertTrue("Rename of $folderPath failed: ${result.logMessage}", result.isSuccess)

        assertTrue("$renamedFolderPath has no local copy", localCopyOf(renamedFolderPath).exists())
        assertTrue("$renamedFilePath has no local copy", localCopyOf(renamedFilePath).exists())

        assertNull(storageManager.getFileByDecryptedRemotePath(folderPath))
        assertNull(storageManager.getFileByDecryptedRemotePath(filePath))

        assertFalse("$folderPath was not moved locally", folderCopy.exists())
        assertFalse("$filePath was not moved locally", fileCopy.exists())
    }

    private fun createFolderOrFail(remotePath: String) {
        val result = CreateFolderOperation(remotePath, user, targetContext, storageManager).execute(client)
        assertTrue("Creation of $remotePath failed: ${result.logMessage}", result.isSuccess)
    }

    private fun synchronizeFolder(remotePath: String) {
        val result = SynchronizeFolderOperation(
            targetContext,
            remotePath,
            user,
            storageManager,
            false,
            false
        ).execute(targetContext)

        assertTrue("Sync of $remotePath failed: ${result.logMessage}", result.isSuccess)
    }

    private fun localFile(remotePath: String): OCFile {
        val file = storageManager.getFileByDecryptedRemotePath(remotePath)
        assertNotNull("$remotePath is missing from the local database", file)
        return file!!
    }

    private fun localCopyOf(remotePath: String): File {
        val storagePath = localFile(remotePath).storagePath
        assertFalse("$remotePath has no storage path", storagePath.isNullOrEmpty())
        return File(storagePath)
    }

    companion object {
        private const val DUMMY_FILE_NAME = "nonEmpty.txt"
        private const val UPLOADED_FILE_NAME = "text.txt"
        private const val NEW_FOLDER_NAME = "test123"
    }
}
