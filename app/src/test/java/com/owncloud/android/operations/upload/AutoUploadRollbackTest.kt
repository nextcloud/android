/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.e2e.E2EFiles
import com.owncloud.android.utils.EncryptionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

class AutoUploadRollbackTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val client: OwnCloudClient = mock()
    private val storage: UploadsStorageManager = mock()
    private val user: User = mock { on { accountName } doReturn "A" }
    private val file = OCFile("/private/photo.jpg").apply { storagePath = "/storage/photo.jpg" }
    private val upload = OCUpload(File.separator + "storage/photo.jpg", file.remotePath, "A").apply {
        createdBy = UploadFileOperation.CREATED_AS_INSTANT_PICTURE
        folderUnlockToken = "previous-token"
    }
    private val operation = UploadFileOperation(
        storage, mock(), mock(), user, file, upload,
        NameCollisionPolicy.OVERWRITE, 0, mock(), false, false, mock()
    )
    private val parent = OCFile("/private/").apply { remoteId = "folder-id" }

    @Test
    fun failedV1WriteLeavesStagedDataForServerRollback() {
        assertFailedWriteDoesNotCommit(mock<DecryptedFolderMetadataFileV1>())
    }

    @Test
    fun failedV2MetadataUpdateLeavesStagedDataForServerRollback() {
        assertFailedWriteDoesNotCommit(mock<DecryptedFolderMetadataFile>())
    }

    @Test
    fun conflictBeforeWritingStillReleasesCleanLock() {
        val conflict = RemoteOperationResult<Any>(ResultCode.SYNC_CONFLICT)
        mockStatic(EncryptionUtils::class.java).use { encryption ->
            encryption.`when`<RemoteOperationResult<Void>> {
                EncryptionUtils.unlockFolderV1(parent, client, "previous-token")
            }.thenReturn(RemoteOperationResult(ResultCode.OK))

            assertSame(conflict, finish(conflict, mock<DecryptedFolderMetadataFileV1>(), false))

            encryption.verify { EncryptionUtils.unlockFolderV1(parent, client, "previous-token") }
        }
    }

    @Test
    fun retryAcquiresNewLockInsteadOfReusingExpiredSavedToken() {
        mockStatic(EncryptionUtils::class.java).use { encryption ->
            encryption.`when`<String> { EncryptionUtils.lockFolder(parent, client, -1L) }
                .thenReturn("fresh-token")
            val lock = UploadFileOperation::class.java.getDeclaredMethod(
                "getFolderUnlockTokenOrLockFolder",
                OwnCloudClient::class.java,
                OCFile::class.java,
                Long::class.javaPrimitiveType
            ).apply { isAccessible = true }

            assertEquals("fresh-token", lock.invoke(operation, client, parent, -1L))
            assertEquals("fresh-token", upload.folderUnlockToken)
            encryption.verify { EncryptionUtils.lockFolder(parent, client, -1L) }
        }
    }

    private fun assertFailedWriteDoesNotCommit(metadata: Any) {
        val failed = RemoteOperationResult<Any>(ResultCode.UNKNOWN_ERROR)
        mockStatic(EncryptionUtils::class.java).use { encryption ->
            assertSame(failed, finish(failed, metadata, true))
            encryption.verifyNoInteractions()
            verifyNoInteractions(storage)
            assertEquals("previous-token", upload.folderUnlockToken)
        }
    }

    private fun finish(result: RemoteOperationResult<*>, metadata: Any, writeStarted: Boolean): Any? {
        val finish = UploadFileOperation::class.java.getDeclaredMethod(
            "releaseLocksAndUnlockE2EFolder", FileLock::class.java, FileChannel::class.java, E2EFiles::class.java,
            RemoteOperationResult::class.java, Any::class.java, OwnCloudClient::class.java, String::class.java,
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }
        val original = temporaryFolder.newFile()
        val files = E2EFiles(parent, null, original, null, null)
        return finish.invoke(operation, null, null, files, result, metadata, client, "previous-token", writeStarted)
    }
}
