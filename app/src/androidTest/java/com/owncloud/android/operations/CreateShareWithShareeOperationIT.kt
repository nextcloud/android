/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateShareWithShareeOperationIT : AbstractOnServerIT() {
    @Test
    fun testCreateShare() {
        val remotePath = "/share/"
        assertTrue(CreateFolderOperation(remotePath, user, targetContext, storageManager).execute(client).isSuccess)
        assertFalse(storageManager.getFileByDecryptedRemotePath(remotePath)!!.isSharedWithSharee)
        
        assertTrue(
            CreateShareWithShareeOperation(
                remotePath,
                "admin",
                ShareType.USER,
                OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER,
                "",
                "",
                -1L,
                false,
                storageManager,
                targetContext,
                user,
                arbitraryDataProvider
            ).execute(client)
                .isSuccess
        )

        assertTrue(storageManager.getFileByDecryptedRemotePath(remotePath)!!.isSharedWithSharee)
    }
}
