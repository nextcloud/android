/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
