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

/**
 * CRUD coverage for internal two-way sync, driving [SynchronizeFolderOperation] the same way
 * [com.nextcloud.client.jobs.InternalTwoWaySyncWork] does (`syncAll = true`).
 */
class InternalTwoWaySyncIT : AbstractOnServerIT() {

    private fun sync(remotePath: String): RemoteOperationResult<*> =
        SynchronizeFolderOperation(targetContext, remotePath, user, getStorageManager(), false, true)
            .execute(targetContext)

    /** Creates [remotePath] on the server, downloads it once, then marks it for two-way sync. */
    private fun setUpTwoWaySyncFolder(remotePath: String): OCFile {
        createFolder(remotePath)
        assertTrue(sync(remotePath).isSuccess)

        val folder = getStorageManager().getFileByPath(remotePath)
        folder.internalFolderSyncTimestamp = 0L
        getStorageManager().saveFile(folder)
        return folder
    }

    /** Uploads directly against the server, bypassing the local DB, to simulate a change made elsewhere. */
    private fun uploadDirectlyToServer(localFile: File, remotePath: String) {
        assertTrue(
            UploadFileRemoteOperation(
                localFile.absolutePath,
                remotePath,
                "text/plain",
                System.currentTimeMillis() / 1000
            ).execute(client).isSuccess
        )
    }

    private fun existsOnServer(remotePath: String): Boolean =
        ExistenceCheckRemoteOperation(remotePath, false).execute(client).isSuccess

    @Test
    fun localCreate_file_isUploaded() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncLocalCreateFile/")

        File(folder.storagePath, "newFile.txt").writeText("hello")

        assertTrue(sync(folder.remotePath).isSuccess)

        val uploaded = getStorageManager().getFileByPath(folder.remotePath + "newFile.txt")
        assertNotNull(uploaded)
        assertTrue(File(uploaded.storagePath).exists())
        assertTrue(existsOnServer(folder.remotePath + "newFile.txt"))
    }

    @Test
    fun localCreate_folderWithContents_isCreatedAndUploaded() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncLocalCreateFolder/")

        val subFolder = File(folder.storagePath, "sub").apply { mkdir() }
        File(subFolder, "nested.txt").writeText("nested")

        // createRemoteFolder() recurses synchronously, so one pass is enough here - unlike the
        // remote-create-folder case below, which relies on an async OperationsService intent.
        assertTrue(sync(folder.remotePath).isSuccess)

        val remoteSubFolder = getStorageManager().getFileByPath(folder.remotePath + "sub/")
        assertNotNull(remoteSubFolder)
        assertTrue(remoteSubFolder.isFolder)

        val nested = getStorageManager().getFileByPath(folder.remotePath + "sub/nested.txt")
        assertNotNull(nested)
        assertTrue(File(nested.storagePath).exists())
    }

    @Test
    fun localUpdate_isUploaded() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncLocalUpdate/")
        uploadFile(getDummyFile("nonEmpty.txt"), folder.remotePath + "file.txt")
        assertTrue(sync(folder.remotePath).isSuccess)

        val before = getStorageManager().getFileByPath(folder.remotePath + "file.txt")

        shortSleep() // makes sure the new mtime is strictly after lastSyncDateForData
        File(before.storagePath).writeText("updated by test")

        assertTrue(sync(folder.remotePath).isSuccess)

        // an already-known file that changed locally is uploaded via FileUploadHelper's
        // WorkManager job (SynchronizeFileOperation.handleLocalChange), not synchronously -
        // poll the server instead of asserting right away
        var updated = false
        for (i in 0 until 10) {
            val remote = ReadFileRemoteOperation(folder.remotePath + "file.txt").execute(client)
            if (remote.isSuccess && (remote.data[0] as RemoteFile).etag != before.etag) {
                updated = true
                break
            }
            shortSleep()
        }
        assertTrue("Locally modified file was not uploaded within timeout", updated)
    }

    @Test
    fun localDelete_isRemovedFromServer() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncLocalDelete/")
        uploadFile(getDummyFile("nonEmpty.txt"), folder.remotePath + "file.txt")
        assertTrue(sync(folder.remotePath).isSuccess)

        val file = getStorageManager().getFileByPath(folder.remotePath + "file.txt")
        assertTrue(File(file.storagePath).delete())

        assertTrue(sync(folder.remotePath).isSuccess)

        assertNull(getStorageManager().getFileByPath(folder.remotePath + "file.txt"))
        assertFalse(existsOnServer(folder.remotePath + "file.txt"))
    }

    @Test
    fun remoteCreate_file_isDownloaded() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncRemoteCreateFile/")

        uploadDirectlyToServer(getDummyFile("nonEmpty.txt"), folder.remotePath + "remote.txt")

        assertTrue(sync(folder.remotePath).isSuccess)

        val downloaded = getStorageManager().getFileByPath(folder.remotePath + "remote.txt")
        assertNotNull(downloaded)
        assertTrue(File(downloaded.storagePath).exists())
    }

    @Test
    fun remoteCreate_folderWithContents_isDownloaded() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncRemoteCreateFolder/")
        val subFolderRemotePath = folder.remotePath + "remoteSub/"

        assertTrue(CreateFolderRemoteOperation(subFolderRemotePath, true).execute(client).isSuccess)
        uploadDirectlyToServer(getDummyFile("nonEmpty.txt"), subFolderRemotePath + "nested.txt")

        assertTrue(sync(folder.remotePath).isSuccess)

        val remoteSubFolder = getStorageManager().getFileByPath(subFolderRemotePath)
        assertNotNull(remoteSubFolder)
        assertTrue(remoteSubFolder.isFolder)

        // descending into a newly discovered subfolder normally happens asynchronously via an
        // OperationsService intent (SynchronizeFolderOperation#startSyncFolderOperation) -
        // drive it directly here so the assertion below is deterministic
        assertTrue(sync(subFolderRemotePath).isSuccess)

        val nested = getStorageManager().getFileByPath(subFolderRemotePath + "nested.txt")
        assertNotNull(nested)
        assertTrue(File(nested.storagePath).exists())
    }

    @Test
    fun remoteUpdate_isDownloaded() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncRemoteUpdate/")
        uploadFile(getDummyFile("nonEmpty.txt"), folder.remotePath + "file.txt")
        assertTrue(sync(folder.remotePath).isSuccess)

        val before = getStorageManager().getFileByPath(folder.remotePath + "file.txt")

        uploadDirectlyToServer(getDummyFile("chunkedFile.txt"), folder.remotePath + "file.txt")

        assertTrue(sync(folder.remotePath).isSuccess)

        val after = getStorageManager().getFileByPath(folder.remotePath + "file.txt")
        assertNotEquals(before.etag, after.etag)
        assertEquals(getDummyFile("chunkedFile.txt").length(), File(after.storagePath).length())
    }

    @Test
    fun remoteDelete_file_isRemovedLocally() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncRemoteDeleteFile/")
        uploadFile(getDummyFile("nonEmpty.txt"), folder.remotePath + "file.txt")
        assertTrue(sync(folder.remotePath).isSuccess)

        val file = getStorageManager().getFileByPath(folder.remotePath + "file.txt")
        assertTrue(File(file.storagePath).exists())

        assertTrue(RemoveFileRemoteOperation(folder.remotePath + "file.txt").execute(client).isSuccess)

        assertTrue(sync(folder.remotePath).isSuccess)

        assertNull(getStorageManager().getFileByPath(folder.remotePath + "file.txt"))
        assertFalse(File(file.storagePath).exists())
    }

    @Test
    fun remoteDelete_folderWithContents_isRemovedLocally() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncRemoteDeleteFolder/")

        val subFolder = File(folder.storagePath, "sub").apply { mkdir() }
        File(subFolder, "nested.txt").writeText("nested")
        assertTrue(sync(folder.remotePath).isSuccess)
        assertNotNull(getStorageManager().getFileByPath(folder.remotePath + "sub/"))

        assertTrue(RemoveFileRemoteOperation(folder.remotePath + "sub/").execute(client).isSuccess)

        assertTrue(sync(folder.remotePath).isSuccess)

        assertNull(getStorageManager().getFileByPath(folder.remotePath + "sub/"))
        assertFalse(subFolder.exists())
    }

    @Test
    fun conflict_bothSidesChanged_isMarked() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncConflict/")
        uploadFile(getDummyFile("nonEmpty.txt"), folder.remotePath + "file.txt")
        assertTrue(sync(folder.remotePath).isSuccess)

        val before = getStorageManager().getFileByPath(folder.remotePath + "file.txt")

        shortSleep()
        File(before.storagePath).writeText("local edit")
        uploadDirectlyToServer(getDummyFile("chunkedFile.txt"), folder.remotePath + "file.txt")

        sync(folder.remotePath)

        val after = getStorageManager().getFileByPath(folder.remotePath + "file.txt")
        assertNotNull(
            "Concurrently changed file should have been flagged as conflicting",
            after.etagInConflict
        )
    }

    @Test
    fun noChanges_secondSyncIsNoop() {
        val folder = setUpTwoWaySyncFolder("/twoWaySyncNoop/")
        uploadFile(getDummyFile("nonEmpty.txt"), folder.remotePath + "file.txt")
        assertTrue(sync(folder.remotePath).isSuccess)

        val before = getStorageManager().getFileByPath(folder.remotePath + "file.txt")

        assertTrue(sync(folder.remotePath).isSuccess)

        val after = getStorageManager().getFileByPath(folder.remotePath + "file.txt")
        assertEquals(before.etag, after.etag)
        assertEquals(before.fileId, after.fileId)
    }
}
