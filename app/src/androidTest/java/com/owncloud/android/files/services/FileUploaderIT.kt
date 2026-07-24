/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.files.services

import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.UploadFileOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FileUploaderIT : AbstractOnServerIT() {

    private lateinit var uploadsStorageManager: UploadsStorageManager

    private val powerManagementServiceMock = object : PowerManagementService {
        override val isIgnoringOptimization = true
        override val isPowerSavingEnabled = false
        override val battery = BatteryStatus()
    }

    @Before
    fun setUp() {
        val accountManager = UserAccountManagerImpl.fromContext(targetContext)
        uploadsStorageManager = UploadsStorageManager(accountManager, targetContext.contentResolver)
    }

    @Test
    fun uploadWithOverwritePolicyReplacesRemoteFile() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        uploadOriginalFile(originalFile)

        val replacement = getDummyFile(EMPTY_FILE)
        assertTrue(uploadOperation(replacement, NameCollisionPolicy.OVERWRITE).execute(client).isSuccess)

        assertRemoteFileLength(REMOTE_PATH, EMPTY_LENGTH)
    }

    @Test
    fun uploadWithRenamePolicyKeepsBothFiles() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        uploadOriginalFile(originalFile)

        val renamedFile = getDummyFile(EMPTY_FILE)
        var renameListenerTriggered = false
        val operation = uploadOperation(renamedFile, NameCollisionPolicy.RENAME)
            .addRenameUploadListener { renameListenerTriggered = true }
        assertTrue(operation.execute(client).isSuccess)

        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
        assertRemoteFileLength(REMOTE_PATH_RENAMED, renamedFile.length())
        assertTrue(renameListenerTriggered)
    }

    @Test
    fun uploadWithRenamePolicyKeepsBothFilesForNonEmptyOriginal() {
        val originalFile = getDummyFile(NON_EMPTY_FILE)
        uploadOriginalFile(originalFile)

        val renamedFile = getDummyFile(EMPTY_FILE)
        assertTrue(uploadOperation(renamedFile, NameCollisionPolicy.RENAME).execute(client).isSuccess)

        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
        assertRemoteFileLength(REMOTE_PATH_RENAMED, renamedFile.length())
    }

    @Test
    fun uploadWithSkipPolicyKeepsRemoteFileUnchanged() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        uploadOriginalFile(originalFile)

        val skippedFile = getDummyFile(EMPTY_FILE)
        val operation = uploadOperation(skippedFile, NameCollisionPolicy.SKIP)
        assertTrue(operation.execute(client).isSuccess)
        assertTrue(operation.wasSkipped())

        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
    }

    @Test
    fun uploadWithSkipPolicyUploadsWhenNoRemoteFileExists() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        val operation = uploadOperation(originalFile, NameCollisionPolicy.SKIP).setRemoteFolderToBeCreated()
        assertTrue(operation.execute(client).isSuccess)
        assertFalse(operation.wasSkipped())

        assertRemoteFileLength(REMOTE_PATH, originalFile.length())

        val skippedFile = getDummyFile(EMPTY_FILE)
        val skippedOperation = uploadOperation(skippedFile, NameCollisionPolicy.SKIP)
        assertTrue(skippedOperation.execute(client).isSuccess)
        assertTrue(skippedOperation.wasSkipped())

        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
    }

    private fun uploadOriginalFile(originalFile: File) {
        val operation = uploadOperation(originalFile, NameCollisionPolicy.DEFAULT).setRemoteFolderToBeCreated()
        assertTrue(operation.execute(client).isSuccess)
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
    }

    private fun uploadOperation(localFile: File, nameCollisionPolicy: NameCollisionPolicy) = UploadFileOperation(
        uploadsStorageManager,
        connectivityServiceMock,
        powerManagementServiceMock,
        user,
        null,
        OCUpload(localFile.absolutePath, REMOTE_PATH, account.name),
        nameCollisionPolicy,
        FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
        targetContext,
        false,
        false,
        storageManager
    )

    private fun assertRemoteFileLength(remotePath: String, expectedLength: Long) {
        val result = ReadFileRemoteOperation(remotePath).execute(client)
        assertTrue(result.isSuccess)
        assertEquals(expectedLength, (result.data[0] as RemoteFile).length)
    }

    companion object {
        private const val REMOTE_PATH = "/testFile.txt"
        private const val REMOTE_PATH_RENAMED = "/testFile (2).txt"
        private const val CHUNKED_FILE = "chunkedFile.txt"
        private const val NON_EMPTY_FILE = "nonEmpty.txt"
        private const val EMPTY_FILE = "empty.txt"
        private const val EMPTY_LENGTH = 0L
    }
}
