/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.e2e.E2EFiles
import com.owncloud.android.operations.e2e.E2EData
import com.owncloud.android.operations.e2e.E2EClientData
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

@Suppress("DEPRECATION")
class EncryptedTransferUploadTest {
    private val user: User = mock { on { accountName } doReturn "user@nextcloud.example" }
    private val storage: FileDataStorageManager = mock()
    private val uploads: UploadsStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val parent: OCFile = mock()
    private val metadata: DecryptedFolderMetadataFile = mock()
    private val files: E2EFiles = mock { on { parentFile } doReturn parent }
    private val ok = RemoteOperationResult<Void>(ResultCode.OK)
    private val failed = RemoteOperationResult<Void>(ResultCode.UNKNOWN_ERROR)

    @Test
    fun transientTransferDoesNotUpdateAnUnregisteredUpload() {
        val operation = operation(uploadId = -1)

        updateStoredResult(operation)

        verify(uploads, never()).updateDatabaseUploadResult(any(), any())
    }

    @Test
    fun queuedUploadStillUpdatesItsStoredResult() {
        val operation = operation(uploadId = 17)

        updateStoredResult(operation)

        verify(uploads).updateDatabaseUploadResult(ok, operation)
    }

    @Test
    fun transientTransferLocksWithoutPersistingAnUnregisteredUpload() {
        mockStatic(EncryptionUtils::class.java).use { crypto ->
            crypto.`when`<String> { EncryptionUtils.lockFolder(parent, client, 1L) }.thenReturn("token")

            val token = UploadFileOperation::class.java.getDeclaredMethod(
                "getFolderUnlockTokenOrLockFolder",
                OwnCloudClient::class.java,
                OCFile::class.java,
                Long::class.javaPrimitiveType
            ).apply { isAccessible = true }.invoke(operation(uploadId = -1), client, parent, 1L)

            assertEquals("token", token)
        }
        verify(uploads, never()).updateUpload(any())
        verify(uploads, never()).getUploadById(any())
    }

    @Test
    fun failedTransferDoesNotCommitCiphertextWithoutMetadata() {
        val operation = operation().apply { setKeepE2EELockOnFailure(true) }
        mockStatic(EncryptionUtils::class.java).use { crypto ->
            assertSame(failed, release(operation, failed))

            crypto.verify({ EncryptionUtils.unlockFolder(parent, client, "token") }, never())
        }
        verify(files).deleteEncryptedTempFile()
        verify(storage, never()).updateE2EECounter(parent, metadata)
    }

    @Test
    fun successfulTransferCommitsMetadataAndUpdatesCounter() {
        val operation = operation().apply { setKeepE2EELockOnFailure(true) }
        mockStatic(EncryptionUtils::class.java).use { crypto ->
            crypto.`when`<RemoteOperationResult<Void>> { EncryptionUtils.unlockFolder(parent, client, "token") }
                .thenReturn(ok)

            assertSame(ok, release(operation, ok))

            crypto.verify { EncryptionUtils.unlockFolder(parent, client, "token") }
        }
        verify(storage).updateE2EECounter(parent, metadata)
        verify(files).deleteEncryptedTempFile()
    }

    @Test
    fun defaultUploadKeepsItsExistingUnlockBehaviour() {
        mockStatic(EncryptionUtils::class.java).use { crypto ->
            crypto.`when`<RemoteOperationResult<Void>> { EncryptionUtils.unlockFolder(parent, client, "token") }
                .thenReturn(ok)

            assertSame(failed, release(operation(), failed))

            crypto.verify { EncryptionUtils.unlockFolder(parent, client, "token") }
        }
    }

    @Test
    fun metadataUsesTheDisplayFilenameWhileTheUploadedFileGetsANewCiphertextName() {
        val file = OCFile("/cipher-parent/季度报告.txt").apply {
            decryptedRemotePath = "/资料/加密目录/content"
        }
        doReturn("/资料/加密目录/").whenever(parent).decryptedRemotePath
        doReturn("/cipher-parent/").whenever(parent).remotePath
        val encrypted: EncryptedFile = mock { on { authenticationTag } doReturn "tag" }
        val data = E2EData(byteArrayOf(), byteArrayOf(), encrypted, "new-ciphertext-id")
        val utils: EncryptionUtilsV2 = mock()
        val clientData = E2EClientData(client, "token", "public-key")

        UploadFileOperation::class.java.getDeclaredMethod(
            "upsertMetadata",
            Any::class.java,
            E2EData::class.java,
            E2EClientData::class.java,
            E2EFiles::class.java,
            ArbitraryDataProvider::class.java,
            EncryptionUtilsV2::class.java,
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }.invoke(
            operation(file),
            metadata,
            data,
            clientData,
            files,
            mock<ArbitraryDataProvider>(),
            utils,
            false
        )

        assertEquals("/cipher-parent/new-ciphertext-id", file.remotePath)
        assertEquals("/资料/加密目录/季度报告.txt", file.decryptedRemotePath)
        assertEquals("季度报告.txt", file.decryptedFileName)
        verify(utils).addFileToMetadata("new-ciphertext-id", file, data.iv, "tag", data.key, metadata)
    }

    private fun updateStoredResult(operation: UploadFileOperation) {
        UploadFileOperation::class.java.getDeclaredMethod(
            "updateStoredUploadResult",
            RemoteOperationResult::class.java
        ).apply { isAccessible = true }.invoke(operation, ok)
    }

    private fun operation(file: OCFile = OCFile("/cipher/content"), uploadId: Long = 0): UploadFileOperation {
        file.storagePath = "/cache/content"
        val upload: OCUpload = mock {
            on { localPath } doReturn "/cache/content"
            on { getUploadId() } doReturn uploadId
        }
        return UploadFileOperation(
            uploads, mock(), mock(), user, file, upload, NameCollisionPolicy.ASK_USER,
            0, mock(), false, false, true, storage
        )
    }

    private fun release(operation: UploadFileOperation, result: RemoteOperationResult<Void>): Any? =
        UploadFileOperation::class.java.getDeclaredMethod(
            "releaseLocksAndUnlockE2EFolder",
            FileLock::class.java,
            FileChannel::class.java,
            E2EFiles::class.java,
            RemoteOperationResult::class.java,
            Any::class.java,
            OwnCloudClient::class.java,
            String::class.java
        ).apply { isAccessible = true }.invoke(operation, null, null, files, result, metadata, client, "token")
}
