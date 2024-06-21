/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateShareWithShareeOperationIT : AbstractOnServerIT() {
    @Test
    fun testCreateShare() {
        val remotePath = "/share/"
        assertTrue(CreateFolderRemoteOperation(remotePath, true).execute(client).isSuccess)

        assertFalse(fileDataStorageManager.getFileByDecryptedRemotePath(remotePath)?.isSharedWithSharee == true)
        
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

        assertFalse(fileDataStorageManager.getFileByDecryptedRemotePath(remotePath)?.isSharedWithSharee == true)
    }
}
