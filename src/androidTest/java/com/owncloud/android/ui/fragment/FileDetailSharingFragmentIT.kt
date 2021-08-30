/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2021 TSI-mc
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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withText
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
import com.owncloud.android.ui.fragment.util.SharingMenuHelper
import com.owncloud.android.utils.ScreenshotTest
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("TooManyFunctions")
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
    // for advanced permissions
    fun publicLinkOptionMenuFolderAdvancePermission() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val publicShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.PUBLIC_LINK
            permissions = 17
        }

        activity.runOnUiThread { sut.showSharingMenuActionSheet(publicShare) }

        // check if items are visible
        onView(ViewMatchers.withId(R.id.menu_share_open_in)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_send_link)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_add_another_link)).check(matches(isDisplayed()))

        // click event
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())

        // validate view shown on screen
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_change_name_switch)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_allow_resharing_checkbox)).check(matches(not(isDisplayed())))

        // read-only
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(isNotChecked()))
        goBack()

        // upload and editing
        publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(isNotChecked()))
        goBack()

        // file drop
        publicShare.permissions = 4
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(isChecked()))
        goBack()

        // password protection
        publicShare.shareWith = "someValue"
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isChecked()))
        goBack()

        publicShare.shareWith = ""
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isNotChecked()))
        goBack()

        // hide download
        publicShare.isHideFileDownload = true
        publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isChecked()))
        goBack()

        publicShare.isHideFileDownload = false
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isNotChecked()))
        goBack()

        publicShare.expirationDate = 1582019340000
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
        goBack()

        publicShare.expirationDate = 0
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(withText("")))
    }

    @Test
    @Suppress("MagicNumber")
    // public link and email are handled the same way
    // for send new email
    fun publicLinkOptionMenuFolderSendNewEmail() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val publicShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.PUBLIC_LINK
            permissions = 17
        }

        verifySendNewEmail(sut, publicShare)
    }

    @Test
    @Suppress("MagicNumber")
    // public link and email are handled the same way
    // for advanced permissions
    fun publicLinkOptionMenuFileAdvancePermission() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val publicShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.PUBLIC_LINK
            permissions = 17
        }
        activity.runOnUiThread { sut.showSharingMenuActionSheet(publicShare) }

        // check if items are visible
        onView(ViewMatchers.withId(R.id.menu_share_open_in)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_send_link)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_add_another_link)).check(matches(isDisplayed()))

        // click event
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())

        // validate view shown on screen
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_change_name_switch)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_allow_resharing_checkbox)).check(matches(not(isDisplayed())))

        // read-only
        publicShare.permissions = 17 // from server
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        goBack()

        // editing
        publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FILE // from server
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isChecked()))
        goBack()

        // hide download
        publicShare.isHideFileDownload = true
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isChecked()))
        goBack()

        publicShare.isHideFileDownload = false
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isNotChecked()))
        goBack()

        // password protection
        publicShare.isPasswordProtected = true
        publicShare.shareWith = "someValue"
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isChecked()))
        goBack()

        publicShare.isPasswordProtected = false
        publicShare.shareWith = ""
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isNotChecked()))
        goBack()

        // expires
        publicShare.expirationDate = 1582019340
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
        goBack()

        publicShare.expirationDate = 0
        openAdvancedPermissions(sut, publicShare)
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(withText("")))
    }

    @Test
    @Suppress("MagicNumber")
    // public link and email are handled the same way
    // for send new email
    fun publicLinkOptionMenuFileSendNewEmail() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val publicShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.PUBLIC_LINK
            permissions = 17
        }

        verifySendNewEmail(sut, publicShare)
    }

    @Test
    @Suppress("MagicNumber")
    // also applies for
    // group
    // conversation
    // circle
    // federated share
    // for advanced permissions
    fun userOptionMenuFileAdvancePermission() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val userShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.USER
            permissions = 17
        }

        activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }

        // check if items are visible
        onView(ViewMatchers.withId(R.id.menu_share_open_in)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_send_link)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_add_another_link)).check(matches(not(isDisplayed())))

        // click event
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())

        // validate view shown on screen
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_change_name_switch)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isDisplayed()))

        // read-only
        userShare.permissions = 17 // from server
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        goBack()

        // editing
        userShare.permissions = MAXIMUM_PERMISSIONS_FOR_FILE // from server
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isChecked()))
        goBack()

        // allow reshare
        userShare.permissions = 1 // from server
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isNotChecked()))
        goBack()

        userShare.permissions = 17 // from server
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isChecked()))
        goBack()

        // set expiration date
        userShare.expirationDate = 1582019340000
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
        goBack()

        userShare.expirationDate = 0
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(withText("")))
    }

    @Test
    @Suppress("MagicNumber")
    // also applies for
    // group
    // conversation
    // circle
    // federated share
    // for send new email
    fun userOptionMenuFileSendNewEmail() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val userShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.USER
            permissions = 17
        }

        verifySendNewEmail(sut, userShare)
    }

    @Test
    @Suppress("MagicNumber")
    // also applies for
    // group
    // conversation
    // circle
    // federated share
    // for advanced permissions
    fun userOptionMenuFolderAdvancePermission() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val userShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.USER
            permissions = 17
        }

        activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }

        // check if items are visible
        onView(ViewMatchers.withId(R.id.menu_share_open_in)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_send_link)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.menu_share_add_another_link)).check(matches(not(isDisplayed())))

        // click event
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())

        // validate view shown on screen
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isDisplayed()))
        onView(ViewMatchers.withId(R.id.share_process_change_name_switch)).check(matches(not(isDisplayed())))
        onView(ViewMatchers.withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isDisplayed()))

        // read-only
        userShare.permissions = 17 // from server
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(isNotChecked()))
        goBack()

        // allow upload & editing
        userShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER // from server
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(isNotChecked()))
        goBack()

        // file drop
        userShare.permissions = 4
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_permission_file_drop)).check(matches(isChecked()))
        goBack()

        // allow reshare
        userShare.permissions = 1 // from server
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isNotChecked()))
        goBack()

        userShare.permissions = 17 // from server
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isChecked()))
        goBack()

        // set expiration date
        userShare.expirationDate = 1582019340000
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
        onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
        goBack()

        userShare.expirationDate = 0
        openAdvancedPermissions(sut, userShare)
        onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
        onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(withText("")))
    }

    // open bottom sheet with actions
    private fun openAdvancedPermissions(
        sut: FileDetailSharingFragment,
        userShare: OCShare
    ) {
        activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }
        onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())
    }

    // remove the fragment shown
    private fun goBack() {
        onView(ViewMatchers.withId(R.id.share_process_btn_cancel)).perform(ViewActions.click())
    }

    @Test
    @Suppress("MagicNumber")
    // also applies for
    // group
    // conversation
    // circle
    // federated share
    // for send new email
    fun userOptionMenuFolderSendNewEmail() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val userShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.USER
            permissions = 17
        }

        verifySendNewEmail(sut, userShare)
    }

    /**
     * verify send new email note text
     */
    private fun verifySendNewEmail(
        sut: FileDetailSharingFragment,
        userShare: OCShare
    ) {
        activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }

        // click event
        onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).perform(ViewActions.click())

        // validate view shown on screen
        onView(ViewMatchers.withId(R.id.note_text)).check(matches(isDisplayed()))
    }

    @Test
    fun testUploadAndEditingSharePermissions() {
        val sut = FileDetailSharingFragment()

        val share = OCShare().apply {
            permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        }
        assertTrue(SharingMenuHelper.isUploadAndEditingAllowed(share))

        share.permissions = NO_PERMISSION
        assertFalse(SharingMenuHelper.isUploadAndEditingAllowed(share))

        share.permissions = READ_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isUploadAndEditingAllowed(share))

        share.permissions = CREATE_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isUploadAndEditingAllowed(share))

        share.permissions = DELETE_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isUploadAndEditingAllowed(share))

        share.permissions = SHARE_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isUploadAndEditingAllowed(share))
    }

    @Test
    @Suppress("MagicNumber")
    fun testReadOnlySharePermissions() {
        val sut = FileDetailSharingFragment()

        val share = OCShare().apply {
            permissions = 17
        }
        assertTrue(SharingMenuHelper.isReadOnly(share))

        share.permissions = NO_PERMISSION
        assertFalse(SharingMenuHelper.isReadOnly(share))

        share.permissions = READ_PERMISSION_FLAG
        assertTrue(SharingMenuHelper.isReadOnly(share))

        share.permissions = CREATE_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isReadOnly(share))

        share.permissions = DELETE_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isReadOnly(share))

        share.permissions = SHARE_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isReadOnly(share))

        share.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        assertFalse(SharingMenuHelper.isReadOnly(share))

        share.permissions = MAXIMUM_PERMISSIONS_FOR_FILE
        assertFalse(SharingMenuHelper.isReadOnly(share))
    }

    @Test
    @Suppress("MagicNumber")
    fun testFileDropSharePermissions() {
        val sut = FileDetailSharingFragment()

        val share = OCShare().apply {
            permissions = 4
        }
        assertTrue(SharingMenuHelper.isFileDrop(share))

        share.permissions = NO_PERMISSION
        assertFalse(SharingMenuHelper.isFileDrop(share))

        share.permissions = READ_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isFileDrop(share))

        share.permissions = CREATE_PERMISSION_FLAG
        assertTrue(SharingMenuHelper.isFileDrop(share))

        share.permissions = DELETE_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isFileDrop(share))

        share.permissions = SHARE_PERMISSION_FLAG
        assertFalse(SharingMenuHelper.isFileDrop(share))

        share.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
        assertFalse(SharingMenuHelper.isFileDrop(share))

        share.permissions = MAXIMUM_PERMISSIONS_FOR_FILE
        assertFalse(SharingMenuHelper.isFileDrop(share))
    }

    @After
    fun after() {
        activity.storageManager.cleanShares()
    }
}
