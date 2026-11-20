/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.operations.UploadFileOperation
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.File

class AutoUploadIdentityTest {
    @Test
    fun cancellationAndDuplicateDetectionUseQueueIdentityAndAccountAfterEncryption() {
        val uploadUser: com.nextcloud.client.account.User = mock { on { accountName } doReturn "account-A" }
        val operation: UploadFileOperation = mock {
            on { user } doReturn uploadUser
            on { ocUploadId } doReturn 1L
            on { remotePath } doReturn "/private/ciphertext"
            on { uploadRemotePath } doReturn "/private/photo.jpg"
        }
        FileUploadWorker.registerActiveUpload(operation)
        try {
            assertTrue(FileUploadWorker.isUploading("/private/photo.jpg", "account-A"))
            assertTrue(FileUploadWorker.isUploading("/private/ciphertext", "account-A"))
            assertFalse(FileUploadWorker.isUploading("/private/photo.jpg", "account-B"))

            FileUploadWorker.cancelUpload("/private/photo.jpg", "account-A")

            verify(operation).cancel(ResultCode.USER_CANCELLED)
            assertFalse(FileUploadWorker.isUploading("/private/photo.jpg", "account-A"))
        } finally {
            FileUploadWorker.unregisterActiveUpload(operation.ocUploadId)
        }
    }

    @Test
    fun ciphertextPathChangesDoNotChangeAutoUploadQueueIdentity() {
        val file = OCFile("/private/photo.jpg").apply { storagePath = "/storage/photo.jpg" }
        val upload = OCUpload(File.separator + "storage/photo.jpg", "/private/photo.jpg", "account-A").apply {
            createdBy = UploadFileOperation.CREATED_AS_INSTANT_PICTURE
        }
        val operation = UploadFileOperation(
            mock(), mock(), mock(), mock { on { accountName } doReturn "account-A" }, file, upload,
            NameCollisionPolicy.OVERWRITE, 0, mock(), false, false, mock()
        )

        file.remotePath = "/private/ciphertext"

        assertEquals("/private/ciphertext", operation.remotePath)
        assertEquals("/private/photo.jpg", operation.uploadRemotePath)
    }
}
