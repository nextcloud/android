/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InternalTwoWaySyncIT : AbstractOnServerIT() {

    companion object {
        private const val TWO_WAY_SYNC_ENABLED = 0L
        private const val BINARY_MIME_TYPE = "application/octet-stream"
        private const val MILLIS_IN_SECOND = 1000
        private const val SERVER_POLL_ATTEMPTS = 10
        private const val SMALL_DUMMY_FILE = "nonEmpty.txt"
        private const val LARGE_DUMMY_FILE = "chunkedFile.txt"
        private const val FILE_NAME = "file.bin"
        private const val NESTED_FILE_NAME = "nested.bin"
        private const val SUB_FOLDER_NAME = "sub"
    }

    @Test
    fun testWhenLocalFileCreatedGivenShouldUploadToServer() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncLocalCreateFile/")
        File(folder.storagePath, FILE_NAME).writeText("hello")

        syncAndAssertSuccess(folder.remotePath)

        val uploaded = storageManager.getFileByPath(folder.childPath(FILE_NAME))
        assertNotNull(uploaded)
        assertTrue(File(uploaded.storagePath).exists())
        assertTrue(existsOnServer(folder.childPath(FILE_NAME)))
    }

    @Test
    fun testWhenLocalFolderWithContentsCreatedGivenShouldCreateFolderAndUploadContents() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncLocalCreateFolder/")
        val subFolder = File(folder.storagePath, SUB_FOLDER_NAME).apply { mkdir() }
        File(subFolder, NESTED_FILE_NAME).writeText("nested")

        syncAndAssertSuccess(folder.remotePath)

        val remoteSubFolder = storageManager.getFileByPath(folder.childFolderPath(SUB_FOLDER_NAME))
        assertNotNull(remoteSubFolder)
        assertTrue(remoteSubFolder.isFolder)

        val nested = storageManager.getFileByPath(remoteSubFolder.childPath(NESTED_FILE_NAME))
        assertNotNull(nested)
        assertTrue(File(nested.storagePath).exists())
    }

    @Test
    fun testWhenLocalFileUpdatedGivenShouldUploadNewVersion() {
        val folder = setUpTwoWaySyncFolderWithFile("/twoWaySyncLocalUpdate/")
        val before = storageManager.getFileByPath(folder.childPath(FILE_NAME))

        shortSleep()
        File(before.storagePath).writeText("updated by test")

        syncAndAssertSuccess(folder.remotePath)

        assertTrue(
            "Locally modified file was not uploaded within timeout",
            waitUntilServerEtagChanges(folder.childPath(FILE_NAME), before.etag)
        )
    }

    @Test
    fun testWhenLocalFileDeletedGivenShouldRemoveFromServer() {
        val folder = setUpTwoWaySyncFolderWithFile("/twoWaySyncLocalDelete/")
        val file = storageManager.getFileByPath(folder.childPath(FILE_NAME))
        assertTrue(File(file.storagePath).delete())

        syncAndAssertSuccess(folder.remotePath)

        assertNull(storageManager.getFileByPath(folder.childPath(FILE_NAME)))
        assertFalse(existsOnServer(folder.childPath(FILE_NAME)))
    }

    @Test
    fun testWhenRemoteFileCreatedGivenShouldDownload() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncRemoteCreateFile/")
        uploadDirectlyToServer(getDummyFile(SMALL_DUMMY_FILE), folder.childPath(FILE_NAME))

        syncAndAssertSuccess(folder.remotePath)

        val downloaded = storageManager.getFileByPath(folder.childPath(FILE_NAME))
        assertNotNull(downloaded)
        assertTrue(File(downloaded.storagePath).exists())
    }

    @Test
    fun testWhenRemoteFolderWithContentsCreatedGivenShouldDownloadFolderAndContents() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncRemoteCreateFolder/")
        val subFolderPath = folder.childFolderPath(SUB_FOLDER_NAME)
        assertTrue(CreateFolderRemoteOperation(subFolderPath, true).execute(client).isSuccess)
        uploadDirectlyToServer(getDummyFile(SMALL_DUMMY_FILE), subFolderPath + NESTED_FILE_NAME)

        syncAndAssertSuccess(folder.remotePath)

        val remoteSubFolder = storageManager.getFileByPath(subFolderPath)
        assertNotNull(remoteSubFolder)
        assertTrue(remoteSubFolder.isFolder)

        syncAndAssertSuccess(subFolderPath)

        val nested = storageManager.getFileByPath(subFolderPath + NESTED_FILE_NAME)
        assertNotNull(nested)
        assertTrue(File(nested.storagePath).exists())
    }

    @Test
    fun testWhenRemoteFileUpdatedGivenShouldDownloadNewVersion() {
        val folder = setUpTwoWaySyncFolderWithFile("/twoWaySyncRemoteUpdate/")
        val before = storageManager.getFileByPath(folder.childPath(FILE_NAME))
        val newVersion = getDummyFile(LARGE_DUMMY_FILE)
        uploadDirectlyToServer(newVersion, folder.childPath(FILE_NAME))

        syncAndAssertSuccess(folder.remotePath)

        val after = storageManager.getFileByPath(folder.childPath(FILE_NAME))
        assertNotEquals(before.etag, after.etag)
        assertEquals(newVersion.length(), File(after.storagePath).length())
    }

    @Test
    fun testWhenRemoteFileDeletedGivenShouldRemoveLocally() {
        val folder = setUpTwoWaySyncFolderWithFile("/twoWaySyncRemoteDeleteFile/")
        val localFile = File(storageManager.getFileByPath(folder.childPath(FILE_NAME)).storagePath)
        assertTrue(localFile.exists())
        assertTrue(RemoveFileRemoteOperation(folder.childPath(FILE_NAME)).execute(client).isSuccess)

        syncAndAssertSuccess(folder.remotePath)

        assertNull(storageManager.getFileByPath(folder.childPath(FILE_NAME)))
        assertFalse(localFile.exists())
    }

    @Test
    fun testWhenRemoteFolderWithContentsDeletedGivenShouldRemoveLocally() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncRemoteDeleteFolder/")
        val subFolderPath = folder.childFolderPath(SUB_FOLDER_NAME)
        val subFolder = File(folder.storagePath, SUB_FOLDER_NAME).apply { mkdir() }
        File(subFolder, NESTED_FILE_NAME).writeText("nested")
        syncAndAssertSuccess(folder.remotePath)
        assertNotNull(storageManager.getFileByPath(subFolderPath))
        assertTrue(RemoveFileRemoteOperation(subFolderPath).execute(client).isSuccess)

        syncAndAssertSuccess(folder.remotePath)

        assertNull(storageManager.getFileByPath(subFolderPath))
        assertFalse(subFolder.exists())
    }

    @Test
    fun testWhenBothSidesChangedGivenShouldMarkConflict() {
        val folder = setUpTwoWaySyncFolderWithFile("/twoWaySyncConflict/")
        val before = storageManager.getFileByPath(folder.childPath(FILE_NAME))

        shortSleep()
        File(before.storagePath).writeText("local edit")
        uploadDirectlyToServer(getDummyFile(LARGE_DUMMY_FILE), folder.childPath(FILE_NAME))

        sync(folder.remotePath)

        val after = storageManager.getFileByPath(folder.childPath(FILE_NAME))
        assertNotNull("Concurrently changed file should have been flagged as conflicting", after.etagInConflict)
    }

    @Test
    fun testWhenNoChangesGivenShouldSecondSyncNotWork() {
        val folder = setUpTwoWaySyncFolderWithFile("/twoWaySyncNoop/")
        val before = storageManager.getFileByPath(folder.childPath(FILE_NAME))

        syncAndAssertSuccess(folder.remotePath)

        val after = storageManager.getFileByPath(folder.childPath(FILE_NAME))
        assertEquals(before.etag, after.etag)
        assertEquals(before.fileId, after.fileId)
    }

    private fun sync(remotePath: String): RemoteOperationResult<*> =
        SynchronizeFolderOperation(targetContext, remotePath, user, storageManager, false, true)
            .execute(targetContext)

    private fun syncAndAssertSuccess(remotePath: String) = assertTrue(sync(remotePath).isSuccess)

    private fun setUpTwoWaySyncFolder(remotePath: String): OCFile {
        createFolder(remotePath)
        syncAndAssertSuccess(remotePath)

        return storageManager.getFileByPath(remotePath).apply {
            internalFolderSyncTimestamp = TWO_WAY_SYNC_ENABLED
            storageManager.saveFile(this)
        }
    }

    private fun setUpTwoWaySyncFolderWithFile(remotePath: String): OCFile = setUpTwoWaySyncFolder(remotePath).also {
        uploadFile(getDummyFile(SMALL_DUMMY_FILE), it.childPath(FILE_NAME))
        syncAndAssertSuccess(it.remotePath)
    }

    private fun uploadDirectlyToServer(localFile: File, remotePath: String) {
        val modificationTimestamp = System.currentTimeMillis() / MILLIS_IN_SECOND
        val result = UploadFileRemoteOperation(
            localFile.absolutePath,
            remotePath,
            BINARY_MIME_TYPE,
            modificationTimestamp
        ).execute(client)
        assertTrue(result.isSuccess)
    }

    private fun existsOnServer(remotePath: String): Boolean =
        ExistenceCheckRemoteOperation(remotePath, false).execute(client).isSuccess

    private fun serverEtagOf(remotePath: String): String? = ReadFileRemoteOperation(remotePath)
        .execute(client)
        .takeIf { it.isSuccess }
        ?.let { (it.data.first() as RemoteFile).etag }

    private fun waitUntilServerEtagChanges(remotePath: String, previousEtag: String): Boolean =
        (1..SERVER_POLL_ATTEMPTS).any {
            val changed = serverEtagOf(remotePath)?.let { etag -> etag != previousEtag } == true
            if (!changed) {
                shortSleep()
            }
            changed
        }

    private fun OCFile.childPath(name: String): String = remotePath + name

    private fun OCFile.childFolderPath(name: String): String = remotePath + name + OCFile.PATH_SEPARATOR
}
