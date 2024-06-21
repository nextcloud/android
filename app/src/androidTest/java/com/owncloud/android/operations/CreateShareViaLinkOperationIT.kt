/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateShareViaLinkOperationIT : AbstractOnServerIT() {
    @Test
    fun createShare() {
        val remotePath = "/share/"
        val password = "12345"
        assertTrue(CreateFolderRemoteOperation(remotePath, true).execute(client).isSuccess)

        assertFalse(fileDataStorageManager.getFileByDecryptedRemotePath(remotePath)?.isSharedViaLink == true)

        assertTrue(
            CreateShareViaLinkOperation(remotePath, password, storageManager).execute(nextcloudClient)
                .isSuccess
        )

        assertFalse(fileDataStorageManager.getFileByDecryptedRemotePath(remotePath)?.isSharedViaLink == true)
    }
}
