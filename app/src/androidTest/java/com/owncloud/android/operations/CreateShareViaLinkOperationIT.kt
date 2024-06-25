/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateShareViaLinkOperationIT : AbstractOnServerIT() {
    @Test
    fun createShare() {
        val remotePath = "/share/"
        val password = "12345"
        assertTrue(CreateFolderOperation(remotePath, user, targetContext, storageManager).execute(client).isSuccess)

        assertFalse(storageManager.getFileByDecryptedRemotePath(remotePath)!!.isSharedViaLink)

        assertTrue(
            CreateShareViaLinkOperation(remotePath, password, storageManager).execute(nextcloudClient)
                .isSuccess
        )

        assertTrue(storageManager.getFileByDecryptedRemotePath(remotePath)!!.isSharedViaLink)
    }
}
