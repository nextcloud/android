/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.AutoUploadProgressForwarder
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.operations.UploadFileOperation
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class AutoUploadProgressForwarderTest {
    @After
    fun tearDown() {
        FileUploadHelper.mBoundListeners.clear()
    }

    @Test
    fun encryptedUploadForwardsBytesToLogicalQueuePathAndCorrectAccount() {
        val user: User = mock()
        val operation: UploadFileOperation = mock()
        val listener: OnDatatransferProgressListener = mock()
        val otherAccount: OnDatatransferProgressListener = mock()
        whenever(user.accountName).thenReturn("account-A")
        whenever(operation.user).thenReturn(user)
        whenever(operation.uploadRemotePath).thenReturn("/private/photo.jpg")
        whenever(operation.remotePath).thenReturn("/cipher-folder/cipher")
        whenever(operation.fileName).thenReturn("photo.jpg")
        FileUploadHelper.mBoundListeners["account-A/private/photo.jpg"] = listener
        FileUploadHelper.mBoundListeners["account-B/private/photo.jpg"] = otherAccount

        val progress = AutoUploadProgressForwarder(operation)
        progress.onTransferProgress(1, 50, 100, "/cache/encrypted")

        verify(listener).onTransferProgress(1, 50, 100, "photo.jpg")
        verifyNoInteractions(otherAccount)
        val replacement: OnDatatransferProgressListener = mock()
        FileUploadHelper.mBoundListeners["account-A/private/photo.jpg"] = replacement
        progress.onTransferProgress(1, 100, 100, "/cache/encrypted")
        verify(replacement).onTransferProgress(1, 100, 100, "photo.jpg")
    }
}
