/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

import android.Manifest
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.rule.GrantPermissionRule
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FileDetailSharingFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    lateinit var file: OCFile
    lateinit var folder: OCFile
    lateinit var activity: TestActivity

    @Before
    fun before() {
        activity = testActivityRule.launchActivity(null)
        file = OCFile("/test.md").apply {
            parentId = activity.storageManager.getFileByEncryptedRemotePath("/").fileId
            permissions = OCFile.PERMISSION_CAN_RESHARE
        }

        folder = OCFile("/test").apply {
            setFolder()
            parentId = activity.storageManager.getFileByEncryptedRemotePath("/").fileId
            permissions = OCFile.PERMISSION_CAN_RESHARE
        }
    }

    @Test
    @ScreenshotTest
    fun listShares_file_none() {
        // todo search hint is not shown!?

        show(file)
    }

    @Test
    @ScreenshotTest
    fun listShares_file_resharing_not_allowed() {
        file.permissions = ""

        show(file)
    }

    @Test
    @ScreenshotTest
    fun listShares_file_allShareTypes() {
        OCShare(file.decryptedRemotePath).apply {
            remoteId = 1
            shareType = ShareType.USER
            sharedWithDisplayName = "Admin"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 2
            shareType = ShareType.GROUP
            sharedWithDisplayName = "Group"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 3
            shareType = ShareType.EMAIL
            sharedWithDisplayName = "admin@nextcloud.server.com"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 4
            shareType = ShareType.PUBLIC_LINK
            sharedWithDisplayName = "Customer"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 5
            shareType = ShareType.PUBLIC_LINK
            sharedWithDisplayName = "Colleagues"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 6
            shareType = ShareType.FEDERATED
            sharedWithDisplayName = "admin@nextcloud.remoteserver.com"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 7
            shareType = ShareType.CIRCLE
            sharedWithDisplayName = "Private circle"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 8
            shareType = ShareType.ROOM
            sharedWithDisplayName = "Meeting"
            activity.storageManager.saveShare(this)
        }

        show(file)
    }

    private fun show(file: OCFile) {
        val fragment = FileDetailSharingFragment.newInstance(file, user);

        activity.addFragment(fragment)

        waitForIdleSync()

        screenshot(activity)
    }

    @Test
    fun publicLink_optionMenu() {
        val sut = FileDetailSharingFragment()

        val overflowMenuShareLink = ImageView(targetContext)
        val popup = PopupMenu(targetContext, overflowMenuShareLink)
        popup.inflate(R.menu.fragment_file_detail_sharing_public_link)
        val publicShare = OCShare()

        sut.prepareLinkOptionsMenu(popup.menu, publicShare)

        // TODO check all options

        // scenarios: public link, email, …, both for file/folder
    }

    @After
    fun after() {
        activity.storageManager.cleanShares()
    }
}
