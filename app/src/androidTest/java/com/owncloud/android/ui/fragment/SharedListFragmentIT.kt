/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
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

package com.owncloud.android.ui.fragment

import android.view.View
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.GrantStoragePermissionRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class SharedListFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantStoragePermissionRule.grant()

    lateinit var sut: TestActivity

    @Before
    fun before() {
        sut = testActivityRule.launchActivity(null)
    }

    @Test
    @ScreenshotTest
    fun showSharedFiles() {
        val fragment = SharedListFragment()

        val file = OCFile("/shared to admin.png").apply {
            remoteId = "00000001"
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            mimeType = "image/png"
            fileLength = 1024000
            modificationTimestamp = 1188206955
            permissions = OCFile.PERMISSION_CAN_RESHARE
            sut.storageManager.saveFile(this)
        }

        val file1 = OCFile("/shared to group.png").apply {
            remoteId = "00000001"
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            mimeType = "image/png"
            fileLength = 1024000
            modificationTimestamp = 1188206955
            permissions = OCFile.PERMISSION_CAN_RESHARE
            sut.storageManager.saveFile(this)
        }

        val file2 = OCFile("/shared via public link.png").apply {
            remoteId = "00000001"
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            mimeType = "image/png"
            fileLength = 1024000
            modificationTimestamp = 1188206955
            permissions = OCFile.PERMISSION_CAN_RESHARE
            sut.storageManager.saveFile(this)
        }

        val file3 = OCFile("/shared to personal circle.png").apply {
            remoteId = "00000001"
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            mimeType = "image/png"
            fileLength = 1024000
            modificationTimestamp = 1188206955
            permissions = OCFile.PERMISSION_CAN_RESHARE
            sut.storageManager.saveFile(this)
        }

        val file4 = OCFile("/shared to talk.png").apply {
            remoteId = "00000001"
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            mimeType = "image/png"
            fileLength = 1024000
            modificationTimestamp = 1188206955
            permissions = OCFile.PERMISSION_CAN_RESHARE
            sut.storageManager.saveFile(this)
        }

        val shares = listOf(
            OCShare(file.decryptedRemotePath).apply {
                remoteId = 1
                shareType = ShareType.USER
                sharedWithDisplayName = "Admin"
                permissions = OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
                userId = getUserId(user)
                sharedDate = 1188206955
                mimetype = "image/png"
                sut.storageManager.saveShare(this)
            },

            OCShare(file1.decryptedRemotePath).apply {
                remoteId = 2
                shareType = ShareType.GROUP
                sharedWithDisplayName = "Group"
                permissions = OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
                userId = getUserId(user)
                sharedDate = 1188206955
                mimetype = "image/png"
                sut.storageManager.saveShare(this)
            },

            OCShare(file2.decryptedRemotePath).apply {
                remoteId = 3
                shareType = ShareType.PUBLIC_LINK
                label = "Customer"
                sharedDate = 1188206955
                mimetype = "image/png"
                sut.storageManager.saveShare(this)
            },

            OCShare(file3.decryptedRemotePath).apply {
                remoteId = 4
                shareType = ShareType.CIRCLE
                sharedWithDisplayName = "Personal circle"
                permissions = OCShare.SHARE_PERMISSION_FLAG
                userId = getUserId(user)
                sharedDate = 1188206955
                mimetype = "image/png"
                sut.storageManager.saveShare(this)
            },

            OCShare(file4.decryptedRemotePath).apply {
                remoteId = 11
                shareType = ShareType.ROOM
                sharedWithDisplayName = "Admin"
                permissions = OCShare.SHARE_PERMISSION_FLAG
                userId = getUserId(user)
                sharedDate = 1188206955
                mimetype = "image/png"
                sut.storageManager.saveShare(this)
            }
        )

        sut.addFragment(fragment)

        shortSleep()

        sut.runOnUiThread {
            fragment.isLoading = false
            fragment.mEmptyListContainer.visibility = View.GONE
            fragment.adapter.setData(
                shares,
                SearchType.SHARED_FILTER,
                storageManager,
                null,
                true
            )
        }

        waitForIdleSync()
        shortSleep()
        shortSleep()
        shortSleep()

        screenshot(sut)
    }
}
