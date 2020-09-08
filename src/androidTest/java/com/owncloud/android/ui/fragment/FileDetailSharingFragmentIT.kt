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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FileDetailSharingFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

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
            permissions = OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 2
            shareType = ShareType.GROUP
            sharedWithDisplayName = "Group"
            permissions = OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 3
            shareType = ShareType.EMAIL
            sharedWithDisplayName = "admin@nextcloud.server.com"
            userId = getUserId(user)
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
            permissions = OCShare.FEDERATED_PERMISSIONS_FOR_FILE
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 7
            shareType = ShareType.CIRCLE
            sharedWithDisplayName = "Private circle"
            permissions = OCShare.SHARE_PERMISSION_FLAG
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 8
            shareType = ShareType.ROOM
            sharedWithDisplayName = "Meeting"
            permissions = OCShare.SHARE_PERMISSION_FLAG
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        show(file)
    }

    private fun show(file: OCFile) {
        val fragment = FileDetailSharingFragment.newInstance(file, user)

        activity.addFragment(fragment)

        waitForIdleSync()

        screenshot(activity)

        longSleep()
    }

    @Test
    fun publicLink_optionMenu() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val overflowMenuShareLink = ImageView(targetContext)
        val popup = PopupMenu(targetContext, overflowMenuShareLink)
        popup.inflate(R.menu.fragment_file_detail_sharing_public_link)
        val publicShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.PUBLIC_LINK
            permissions = OCShare.READ_PERMISSION_FLAG
        }

        sut.prepareLinkOptionsMenu(popup.menu, publicShare)

        // check if items are visible
        assertTrue(popup.menu.findItem(R.id.action_hide_file_download).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_password).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_share_expiration_date).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_share_send_link).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_share_send_note).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_edit_label).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_unshare).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_add_another_public_share_link).isVisible)

        assertTrue(popup.menu.findItem(R.id.link_share_read_only).isChecked)
        assertFalse(popup.menu.findItem(R.id.link_share_allow_upload_and_editing).isChecked)
        assertFalse(popup.menu.findItem(R.id.link_share_file_drop).isChecked)

        publicShare.permissions = OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.link_share_read_only).isChecked)
        assertTrue(popup.menu.findItem(R.id.link_share_allow_upload_and_editing).isChecked)
        assertFalse(popup.menu.findItem(R.id.link_share_file_drop).isChecked)

        // TODO
//        publicShare.permissions = OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
//        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
//        assertFalse(popup.menu.findItem(R.id.link_share_read_only).isChecked)
//        assertFalse(popup.menu.findItem(R.id.link_share_allow_upload_and_editing).isChecked)
//        assertTrue(popup.menu.findItem(R.id.link_share_file_drop).isChecked)

        // password protection
        publicShare.shareWith = "someValue"
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_password).title ==
            targetContext.getString(R.string.share_password_title))

        publicShare.shareWith = ""
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_password).title ==
            targetContext.getString(R.string.share_no_password_title))

        // hide download
        publicShare.isHideFileDownload = true
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_hide_file_download).isChecked)

        publicShare.isHideFileDownload = false
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.action_hide_file_download).isChecked)

        // TODO expires
//        publicShare.expirationDate =  1582019340000
//        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
//        assertTrue(popup.menu.findItem(R.id.action_share_expiration_date).title.startsWith(
//            targetContext.getString(R.string.share_expiration_date_label)))

        publicShare.expirationDate = 0
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_share_expiration_date).title ==
            targetContext.getString(R.string.share_no_expiration_date_label))

        // file
        publicShare.isFolder = false
        publicShare.permissions = OCShare.READ_PERMISSION_FLAG
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        // check if items are visible
        assertTrue(popup.menu.findItem(R.id.action_hide_file_download).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_password).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_share_expiration_date).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_share_send_link).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_share_send_note).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_edit_label).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_unshare).isVisible)
        assertTrue(popup.menu.findItem(R.id.action_add_another_public_share_link).isVisible)

        assertFalse(popup.menu.findItem(R.id.link_share_read_only).isVisible)
        assertFalse(popup.menu.findItem(R.id.link_share_allow_upload_and_editing).isVisible)
        assertFalse(popup.menu.findItem(R.id.link_share_file_drop).isVisible)
        assertTrue(popup.menu.findItem(R.id.allow_editing).isVisible)

        // allow editing
        assertFalse(popup.menu.findItem(R.id.allow_editing).isChecked)

        publicShare.permissions = OCShare.UPDATE_PERMISSION_FLAG
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.allow_editing).isChecked)

        // hide download
        publicShare.isHideFileDownload = true
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_hide_file_download).isChecked)

        publicShare.isHideFileDownload = false
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.action_hide_file_download).isChecked)

        // password protection
        publicShare.isPasswordProtected = true
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_password).title ==
            targetContext.getString(R.string.share_password_title))

        publicShare.isPasswordProtected = false
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.action_password).isChecked)
        assertTrue(popup.menu.findItem(R.id.action_password).title ==
            targetContext.getString(R.string.share_no_password_title))

        // expires
        publicShare.expirationDate = 1582019340
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_share_expiration_date).title.startsWith(
            targetContext.getString(R.string.share_expiration_date_label)))

        publicShare.expirationDate = 0
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_password).title ==
            targetContext.getString(R.string.share_no_expiration_date_label))

        // TODO check all options
        // scenarios: public link, email, …, both for file/folder
    }

    @After
    fun after() {
        activity.storageManager.cleanShares()
    }
}
