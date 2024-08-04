/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations

import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import junit.framework.TestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("MagicNumber")
class GetSharesForFileOperationIT : AbstractOnServerIT() {
    @Test
    fun shares() {
        val remotePath = "/share/"
        assertTrue(CreateFolderRemoteOperation(remotePath, true).execute(client).isSuccess)

        // share folder to user "admin"
        TestCase.assertTrue(
            CreateShareRemoteOperation(
                remotePath,
                ShareType.USER,
                "admin",
                false,
                "",
                OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
            )
                .execute(client).isSuccess
        )

        // share folder via public link
        TestCase.assertTrue(
            CreateShareRemoteOperation(
                remotePath,
                ShareType.PUBLIC_LINK,
                "",
                true,
                "",
                OCShare.READ_PERMISSION_FLAG
            )
                .execute(client).isSuccess
        )

        // share folder to group
        assertTrue(
            CreateShareRemoteOperation(
                remotePath,
                ShareType.GROUP,
                "users",
                false,
                "",
                OCShare.NO_PERMISSION
            )
                .execute(client).isSuccess
        )

        val shareResult = GetSharesForFileOperation(remotePath, false, false, storageManager).execute(client)
        assertTrue(shareResult.isSuccess)

        assertEquals(3, (shareResult.data as ArrayList<OCShare>).size)
    }
}
