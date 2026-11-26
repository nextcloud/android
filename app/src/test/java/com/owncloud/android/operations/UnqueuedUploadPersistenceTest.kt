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
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.utils.EncryptionUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@Suppress("DEPRECATION")
class UnqueuedUploadPersistenceTest {
    private val user: User = mock { on { accountName } doReturn "user@nextcloud.example" }
    private val storage: FileDataStorageManager = mock()
    private val uploads: UploadsStorageManager = mock()
    private val client: OwnCloudClient = mock()
    private val parent: OCFile = mock()
    private val ok = RemoteOperationResult<Void>(ResultCode.OK)

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
}
