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

import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.OCShare.CREATE_PERMISSION_FLAG
import com.owncloud.android.lib.resources.shares.OCShare.DELETE_PERMISSION_FLAG
import com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
import com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
import com.owncloud.android.lib.resources.shares.OCShare.NO_PERMISSION
import com.owncloud.android.lib.resources.shares.OCShare.READ_PERMISSION_FLAG
import com.owncloud.android.lib.resources.shares.OCShare.SHARE_PERMISSION_FLAG
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
    fun listSharesFileNone() {
        show(file)
    }

    @Test
    @ScreenshotTest
    fun listSharesFileResharingNotAllowed() {
        file.permissions = ""

        show(file)
    }

    @Test
    @ScreenshotTest
    @Suppress("MagicNumber")
    /**
     * Use same values as {@link OCFileListFragmentStaticServerIT showSharedFiles }
     */
    fun listSharesFileAllShareTypes() {
        OCShare(file.decryptedRemotePath).apply {
            remoteId = 1
            shareType = ShareType.USER
            sharedWithDisplayName = "Admin"
            permissions = MAXIMUM_PERMISSIONS_FOR_FILE
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 2
            shareType = ShareType.GROUP
            sharedWithDisplayName = "Group"
            permissions = MAXIMUM_PERMISSIONS_FOR_FILE
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 3
            shareType = ShareType.EMAIL
            sharedWithDisplayName = "admin@nextcloud.localhost"
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 4
            shareType = ShareType.PUBLIC_LINK
            label = "Customer"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 5
            shareType = ShareType.PUBLIC_LINK
            label = "Colleagues"
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 6
            shareType = ShareType.FEDERATED
            sharedWithDisplayName = "admin@nextcloud.localhost"
            permissions = OCShare.FEDERATED_PERMISSIONS_FOR_FILE
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 7
            shareType = ShareType.CIRCLE
            sharedWithDisplayName = "Personal circle"
            permissions = SHARE_PERMISSION_FLAG
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 8
            shareType = ShareType.CIRCLE
            sharedWithDisplayName = "Public circle"
            permissions = SHARE_PERMISSION_FLAG
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 9
            shareType = ShareType.CIRCLE
            sharedWithDisplayName = "Closed circle"
            permissions = SHARE_PERMISSION_FLAG
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 10
            shareType = ShareType.CIRCLE
            sharedWithDisplayName = "Secret circle"
            permissions = SHARE_PERMISSION_FLAG
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 11
            shareType = ShareType.ROOM
            sharedWithDisplayName = "Admin"
            permissions = SHARE_PERMISSION_FLAG
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 12
            shareType = ShareType.ROOM
            sharedWithDisplayName = "Meeting"
            permissions = SHARE_PERMISSION_FLAG
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
    }

    @Test
    @Suppress("MagicNumber")
    // public link and email are handled the same way
    fun publicLinkOptionMenuFolder() {
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
            permissions = 17
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

        // read-only
        assertTrue(popup.menu.findItem(R.id.link_share_read_only).isChecked)
        assertFalse(popup.menu.findItem(R.id.link_share_allow_upload_and_editing).isChecked)
        assertFalse(popup.menu.findItem(R.id.link_share_file_drop).isChecked)

        // upload and editing
        publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.link_share_read_only).isChecked)
        assertTrue(popup.menu.findItem(R.id.link_share_allow_upload_and_editing).isChecked)
        assertFalse(popup.menu.findItem(R.id.link_share_file_drop).isChecked)

        // file drop
        publicShare.permissions = 4
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.link_share_read_only).isChecked)
        assertFalse(popup.menu.findItem(R.id.link_share_allow_upload_and_editing).isChecked)
        assertTrue(popup.menu.findItem(R.id.link_share_file_drop).isChecked)

        // password protection
        publicShare.shareWith = "someValue"
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_password).title == targetContext.getString(R.string.share_password_title)
        )

        publicShare.shareWith = ""
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_password).title == targetContext.getString(R.string.share_no_password_title)
        )

        // hide download
        publicShare.isHideFileDownload = true
        publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_hide_file_download).isChecked)

        publicShare.isHideFileDownload = false
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.action_hide_file_download).isChecked)

        publicShare.expirationDate = 1582019340000
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_share_expiration_date).title
                .startsWith(targetContext.getString(R.string.share_expiration_date_label).split(" ")[0])
        )

        publicShare.expirationDate = 0
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_share_expiration_date).title ==
                targetContext.getString(R.string.share_no_expiration_date_label)
        )

        // file
        publicShare.isFolder = false
        publicShare.permissions = READ_PERMISSION_FLAG
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
        publicShare.permissions = 17 // from server
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.allow_editing).isChecked)

        publicShare.permissions = 19 // from server
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
        publicShare.shareWith = "someValue"
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_password).title == targetContext.getString(R.string.share_password_title)
        )

        publicShare.isPasswordProtected = false
        publicShare.shareWith = ""
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_password).title == targetContext.getString(R.string.share_no_password_title)
        )

        // expires
        publicShare.expirationDate = 1582019340
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_share_expiration_date).title
                .startsWith(targetContext.getString(R.string.share_expiration_date_label).split(" ")[0])
        )

        publicShare.expirationDate = 0
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_share_expiration_date).title ==
                targetContext.getString(R.string.share_no_expiration_date_label)
        )
    }

    @Test
    @Suppress("MagicNumber")
    // public link and email are handled the same way
    fun publicLinkOptionMenuFile() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val overflowMenuShareLink = ImageView(targetContext)
        val popup = PopupMenu(targetContext, overflowMenuShareLink)
        popup.inflate(R.menu.fragment_file_detail_sharing_public_link)
        val publicShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.PUBLIC_LINK
            permissions = 17
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

        assertFalse(popup.menu.findItem(R.id.link_share_read_only).isVisible)
        assertFalse(popup.menu.findItem(R.id.link_share_allow_upload_and_editing).isVisible)
        assertFalse(popup.menu.findItem(R.id.link_share_file_drop).isVisible)
        assertTrue(popup.menu.findItem(R.id.allow_editing).isVisible)

        // password protection
        publicShare.shareWith = "someValue"
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_password).title == targetContext.getString(R.string.share_password_title)
        )

        publicShare.shareWith = ""
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_password).title == targetContext.getString(R.string.share_no_password_title)
        )

        // hide download
        publicShare.isHideFileDownload = true
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.action_hide_file_download).isChecked)

        publicShare.isHideFileDownload = false
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertFalse(popup.menu.findItem(R.id.action_hide_file_download).isChecked)

        // expiration date
        publicShare.expirationDate = 1582019340000
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_share_expiration_date).title
                .startsWith(targetContext.getString(R.string.share_expiration_date_label).split(" ")[0])
        )

        publicShare.expirationDate = 0
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(
            popup.menu.findItem(R.id.action_share_expiration_date).title ==
                targetContext.getString(R.string.share_no_expiration_date_label)
        )

        publicShare.isFolder = false
        publicShare.permissions = READ_PERMISSION_FLAG
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)

        // allow editing
        publicShare.permissions = 17 // from server
        assertFalse(popup.menu.findItem(R.id.allow_editing).isChecked)

        publicShare.permissions = 19 // from server
        sut.prepareLinkOptionsMenu(popup.menu, publicShare)
        assertTrue(popup.menu.findItem(R.id.allow_editing).isChecked)
    }

    @Test
    @Suppress("MagicNumber")
    // also applies for
    // group
    // conversation
    // circle
    // federated share
    fun userOptionMenuFile() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val overflowMenuShareLink = ImageView(targetContext)
        val popup = PopupMenu(targetContext, overflowMenuShareLink)
        popup.inflate(R.menu.item_user_sharing_settings)
        val userShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.USER
            permissions = 17
        }

        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertFalse(popup.menu.findItem(R.id.allow_creating).isVisible)
        assertFalse(popup.menu.findItem(R.id.allow_deleting).isVisible)

        // allow editing
        userShare.permissions = 17 // from server
        assertFalse(popup.menu.findItem(R.id.allow_editing).isChecked)

        userShare.permissions = 19 // from server
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(popup.menu.findItem(R.id.allow_editing).isChecked)

        // allow reshare
        userShare.permissions = 1 // from server
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertFalse(popup.menu.findItem(R.id.allow_resharing).isChecked)

        userShare.permissions = 17 // from server
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(popup.menu.findItem(R.id.allow_resharing).isChecked)

        // set expiration date
        userShare.expirationDate = 1582019340000
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(
            popup.menu.findItem(R.id.action_expiration_date).title
                .startsWith(targetContext.getString(R.string.share_expiration_date_label).split(" ")[0])
        )

        userShare.expirationDate = 0
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(
            popup.menu.findItem(R.id.action_expiration_date).title ==
                targetContext.getString(R.string.share_no_expiration_date_label)
        )

        // note
        assertTrue(popup.menu.findItem(R.id.action_share_send_note).isVisible)
    }

    @Test
    @Suppress("MagicNumber")
    // also applies for
    // group
    // conversation
    // circle
    // federated share
    fun userOptionMenuFolder() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val overflowMenuShareLink = ImageView(targetContext)
        val popup = PopupMenu(targetContext, overflowMenuShareLink)
        popup.inflate(R.menu.item_user_sharing_settings)
        val userShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.USER
            permissions = 17
        }

        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(popup.menu.findItem(R.id.allow_creating).isVisible)
        assertTrue(popup.menu.findItem(R.id.allow_deleting).isVisible)

        // allow editing
        userShare.permissions = 17 // from server
        assertFalse(popup.menu.findItem(R.id.allow_editing).isChecked)

        userShare.permissions = 19 // from server
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(popup.menu.findItem(R.id.allow_editing).isChecked)

        // allow reshare
        userShare.permissions = 1 // from server
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertFalse(popup.menu.findItem(R.id.allow_resharing).isChecked)

        userShare.permissions = 17 // from server
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(popup.menu.findItem(R.id.allow_resharing).isChecked)

        // set expiration date
        userShare.expirationDate = 1582019340000
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(
            popup.menu.findItem(R.id.action_expiration_date).title
                .startsWith(targetContext.getString(R.string.share_expiration_date_label).split(" ")[0])
        )

        userShare.expirationDate = 0
        sut.prepareUserOptionsMenu(popup.menu, userShare)
        assertTrue(
            popup.menu.findItem(R.id.action_expiration_date).title ==
                targetContext.getString(R.string.share_no_expiration_date_label)
        )

        // note
        assertTrue(popup.menu.findItem(R.id.action_share_send_note).isVisible)
    }

    @Test
    fun testUploadAndEditingSharePermissions() {
        val sut = FileDetailSharingFragment()

        val share = OCShare().apply {
            permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        }
        assertTrue(sut.isUploadAndEditingAllowed(share))

        share.permissions = NO_PERMISSION
        assertFalse(sut.isUploadAndEditingAllowed(share))

        share.permissions = READ_PERMISSION_FLAG
        assertFalse(sut.isUploadAndEditingAllowed(share))

        share.permissions = CREATE_PERMISSION_FLAG
        assertFalse(sut.isUploadAndEditingAllowed(share))

        share.permissions = DELETE_PERMISSION_FLAG
        assertFalse(sut.isUploadAndEditingAllowed(share))

        share.permissions = SHARE_PERMISSION_FLAG
        assertFalse(sut.isUploadAndEditingAllowed(share))
    }

    @Test
    @Suppress("MagicNumber")
    fun testReadOnlySharePermissions() {
        val sut = FileDetailSharingFragment()

        val share = OCShare().apply {
            permissions = 17
        }
        assertTrue(sut.isReadOnly(share))

        share.permissions = NO_PERMISSION
        assertFalse(sut.isReadOnly(share))

        share.permissions = READ_PERMISSION_FLAG
        assertTrue(sut.isReadOnly(share))

        share.permissions = CREATE_PERMISSION_FLAG
        assertFalse(sut.isReadOnly(share))

        share.permissions = DELETE_PERMISSION_FLAG
        assertFalse(sut.isReadOnly(share))

        share.permissions = SHARE_PERMISSION_FLAG
        assertFalse(sut.isReadOnly(share))

        share.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        assertFalse(sut.isReadOnly(share))

        share.permissions = MAXIMUM_PERMISSIONS_FOR_FILE
        assertFalse(sut.isReadOnly(share))
    }

    @Test
    @Suppress("MagicNumber")
    fun testFileDropSharePermissions() {
        val sut = FileDetailSharingFragment()

        val share = OCShare().apply {
            permissions = 4
        }
        assertTrue(sut.isFileDrop(share))

        share.permissions = NO_PERMISSION
        assertFalse(sut.isFileDrop(share))

        share.permissions = READ_PERMISSION_FLAG
        assertFalse(sut.isFileDrop(share))

        share.permissions = CREATE_PERMISSION_FLAG
        assertTrue(sut.isFileDrop(share))

        share.permissions = DELETE_PERMISSION_FLAG
        assertFalse(sut.isFileDrop(share))

        share.permissions = SHARE_PERMISSION_FLAG
        assertFalse(sut.isFileDrop(share))

        share.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        assertFalse(sut.isFileDrop(share))

        share.permissions = MAXIMUM_PERMISSIONS_FOR_FILE
        assertFalse(sut.isFileDrop(share))
    }

    @After
    fun after() {
        activity.storageManager.cleanShares()
    }
}
