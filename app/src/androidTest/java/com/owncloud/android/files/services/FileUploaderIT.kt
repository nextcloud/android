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
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.UploadFileOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
    fun uploadUpdatedFileWithOverwritePolicyReplacesRemoteFile() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        uploadNewTestFile(originalFile.absolutePath)
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())

        uploadUpdatedTestFile(updatedTestFile(EMPTY_FILE), NameCollisionPolicy.OVERWRITE)
        assertRemoteFileLength(REMOTE_PATH, EMPTY_LENGTH)
    }

    @Test
    fun uploadWithRenamePolicyKeepsBothFiles() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        assertTrue(
            uploadOperation(OCUpload(originalFile.absolutePath, REMOTE_PATH, account.name), NameCollisionPolicy.DEFAULT)
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess
        )
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())

        val renamedFile = getDummyFile(EMPTY_FILE)
        var renameListenerTriggered = false
        assertTrue(
            uploadOperation(OCUpload(renamedFile.absolutePath, REMOTE_PATH, account.name), NameCollisionPolicy.RENAME)
                .addRenameUploadListener { renameListenerTriggered = true }
                .execute(client)
                .isSuccess
        )

        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
        assertRemoteFileLength(REMOTE_PATH_RENAMED, renamedFile.length())
        assertTrue(renameListenerTriggered)
    }

    @Test
    fun uploadUpdatedFileWithRenamePolicyKeepsBothFiles() {
        val originalFile = getDummyFile(NON_EMPTY_FILE)
        uploadNewTestFile(originalFile.absolutePath)
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())

        val updatedFile = updatedTestFile(EMPTY_FILE)
        uploadUpdatedTestFile(updatedFile, NameCollisionPolicy.RENAME)

        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
        assertRemoteFileLength(REMOTE_PATH_RENAMED, updatedFile.fileLength)
    }

    @Test
    fun uploadWithSkipPolicyKeepsRemoteFileUnchanged() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        assertTrue(
            uploadOperation(OCUpload(originalFile.absolutePath, REMOTE_PATH, account.name), NameCollisionPolicy.DEFAULT)
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess
        )
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())

        val skippedUpload = OCUpload(getDummyFile(EMPTY_FILE).absolutePath, REMOTE_PATH, account.name)
        assertFalse(
            uploadOperation(skippedUpload, NameCollisionPolicy.SKIP)
                .execute(client)
                .isSuccess
        )
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
    }

    @Test
    fun uploadUpdatedFileWithSkipPolicyKeepsRemoteFileUnchanged() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        uploadNewTestFile(originalFile.absolutePath)
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())

        uploadUpdatedTestFile(updatedTestFile(EMPTY_FILE), NameCollisionPolicy.SKIP)
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
    }

    @Test
    fun uploadWithSkipPolicyIsCancelledWhenRemoteFileExists() {
        val originalFile = getDummyFile(CHUNKED_FILE)
        assertTrue(
            uploadOperation(OCUpload(originalFile.absolutePath, REMOTE_PATH, account.name), NameCollisionPolicy.SKIP)
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess
        )
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())

        val skippedUpload = OCUpload(getDummyFile(EMPTY_FILE).absolutePath, REMOTE_PATH, account.name)
        val uploadResult = uploadOperation(skippedUpload, NameCollisionPolicy.SKIP).execute(client)

        assertFalse(uploadResult.isSuccess)
        assertTrue(uploadResult.exception is OperationCancelledException)
        assertRemoteFileLength(REMOTE_PATH, originalFile.length())
    }

    private fun uploadOperation(upload: OCUpload, nameCollisionPolicy: NameCollisionPolicy) = UploadFileOperation(
        uploadsStorageManager,
        connectivityServiceMock,
        powerManagementServiceMock,
        user,
        null,
        upload,
        nameCollisionPolicy,
        FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
        targetContext,
        false,
        false,
        storageManager
    )

    private fun uploadNewTestFile(localPath: String) {
        FileUploadHelper().uploadNewFiles(
            user,
            arrayOf(localPath),
            arrayOf(REMOTE_PATH),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.DEFAULT
        )
    }

    private fun uploadUpdatedTestFile(file: OCFile, nameCollisionPolicy: NameCollisionPolicy) {
        FileUploadHelper().uploadUpdatedFile(
            user,
            arrayOf(file),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            nameCollisionPolicy
        )
    }

    private fun updatedTestFile(dummyFileName: String) = OCFile(REMOTE_PATH).apply {
        storagePath = getDummyFile(dummyFileName).absolutePath
    }

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
