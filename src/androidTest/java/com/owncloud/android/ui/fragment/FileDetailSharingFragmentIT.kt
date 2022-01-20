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

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultBaseUtils.matchesCheckNames
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.client.RetryTestRule
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.OCShare.*
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.util.SharingMenuHelper
import com.owncloud.android.utils.ScreenshotTest
import org.hamcrest.CoreMatchers.*
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

    @get:Rule
    val retryRule = RetryTestRule()

    lateinit var file: OCFile
    lateinit var folder: OCFile
    lateinit var activity: TestActivity

    @Before
    fun before() {
        activity = testActivityRule.launchActivity(null)
        file = OCFile("/test.md").apply {
            remoteId = "00000001"
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
            permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
            userId = getUserId(user)
            activity.storageManager.saveShare(this)
        }

        OCShare(file.decryptedRemotePath).apply {
            remoteId = 2
            shareType = ShareType.GROUP
            sharedWithDisplayName = "Group"
            permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
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
            permissions = FEDERATED_PERMISSIONS_FOR_FILE
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
    fun verifyFileDetailSharingFragmentViews(){
        val sut = FileDetailSharingFragment.newInstance(file, user)
        setupSecondaryFragment()
        activity.addFragment(sut)
        shortSleep()

        onView(withId(R.id.tv_sharing_details_message)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.searchView)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.label_personal_share)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.share_create_new_link)).check(matches(isCompletelyDisplayed()))
    }

    @Test
    @Suppress("MagicNumber")
    // public link and email are handled the same way
    // for advanced permissions
    fun publicLinkOptionMenuFolderAdvancePermission() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        setupSecondaryFragment()
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val publicShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.PUBLIC_LINK
            permissions = READ_PERMISSION_FLAG
        }

        activity.runOnUiThread { sut.showSharingMenuActionSheet(publicShare) }
        shortSleep()
        waitForIdleSync()

        // check if items are visible
        onView(withId(R.id.menu_share_open_in)).check(matches(not(isDisplayed())))
        onView(withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
        onView(withId(R.id.menu_share_send_new_email)).check(matches(not(isDisplayed())))
        onView(withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))

        onView(withId(R.id.menu_share_advanced_permissions)).check(matches(isCompletelyDisplayed()))
        // click event
        onView(withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())
        addAdvancePermissionFragment(publicShare)

        // validate view shown on screen
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_hide_download_checkbox)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_set_password_switch)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_change_name_switch)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_allow_resharing_checkbox)).check(matches(not(isDisplayed())))

        // read-only
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(isNotChecked()))
        

       /* // upload and editing
        publicShare.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isChecked()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(isNotChecked()))
        

        // file drop
        publicShare.permissions = 4
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(isChecked()))
        

        // password protection
        publicShare.shareWith = "someValue"
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_set_password_switch)).check(matches(isChecked()))
        

        publicShare.shareWith = ""
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_set_password_switch)).check(matches(isNotChecked()))
        

        // hide download
        publicShare.isHideFileDownload = true
        publicShare.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_hide_download_checkbox)).check(matches(isChecked()))
        

        publicShare.isHideFileDownload = false
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_hide_download_checkbox)).check(matches(isNotChecked()))
        

        publicShare.expirationDate = 1582019340000
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
        onView(withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
        

        publicShare.expirationDate = 0
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_select_exp_date)).check(matches(withText("")))*/
    }

    private fun setupSecondaryFragment() {
       /* val secondary = FileDetailFragment.newInstance(file, user)
        activity.addSecondaryFragment(secondary, FileDisplayActivity.TAG_LIST_OF_FILES)*/
        activity.addView(
            FloatingActionButton(activity).apply { // needed for some reason
                visibility = View.GONE
                id = R.id.fab_main
            }
        )
    }

    @Test
    @Suppress("MagicNumber")
    // public link and email are handled the same way
    // for advanced permissions
    fun publicLinkOptionMenuFileAdvancePermission() {
        val sut = FileDetailSharingFragment.newInstance(file, user)
        setupSecondaryFragment()
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val publicShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.PUBLIC_LINK
            permissions = READ_PERMISSION_FLAG
        }
        activity.handler.post { sut.showSharingMenuActionSheet(publicShare) }
        waitForIdleSync()

        // check if items are visible
        onView(withId(R.id.menu_share_open_in)).check(matches(isDisplayed()))
        onView(withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
        onView(withId(R.id.menu_share_send_new_email)).check(matches(not(isDisplayed())))
        onView(withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))

        onView(withId(R.id.menu_share_advanced_permissions)).check(matches(isCompletelyDisplayed()))
        // click event
        onView(withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())
        addAdvancePermissionFragment(publicShare)

        // validate view shown on screen
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_hide_download_checkbox)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_set_password_switch)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_change_name_switch)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_allow_resharing_checkbox)).check(matches(not(isDisplayed())))

        // read-only
        publicShare.permissions = READ_PERMISSION_FLAG // from server
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        

        // editing
       /* publicShare.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE // from server
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isChecked()))
        

        // hide download
        publicShare.isHideFileDownload = true
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_hide_download_checkbox)).check(matches(isChecked()))
        

        publicShare.isHideFileDownload = false
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_hide_download_checkbox)).check(matches(isNotChecked()))
        

        // password protection
        publicShare.isPasswordProtected = true
        publicShare.shareWith = "someValue"
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_set_password_switch)).check(matches(isChecked()))
        

        publicShare.isPasswordProtected = false
        publicShare.shareWith = ""
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_set_password_switch)).check(matches(isNotChecked()))
        

        // expires
        publicShare.expirationDate = 1582019340
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
        onView(withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
        

        publicShare.expirationDate = 0
        openAdvancedPermissions(sut, publicShare)
        onView(withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_select_exp_date)).check(matches(withText("")))*/
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
        suppressFDFAccessibilityChecks()
        setupSecondaryFragment()
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val userShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.USER
            permissions = READ_PERMISSION_FLAG
        }

        activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }
        shortSleep()
        waitForIdleSync()

        // check if items are visible
        onView(withId(R.id.menu_share_open_in)).check(matches(isDisplayed()))
        onView(withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
        onView(withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
        onView(withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))

        onView(withId(R.id.menu_share_advanced_permissions)).check(matches(isCompletelyDisplayed()))
        // click event
        onView(withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())
        addAdvancePermissionFragment(userShare)

        // validate view shown on screen
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_hide_download_checkbox)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_set_password_switch)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_change_name_switch)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isDisplayed()))

        // read-only
        userShare.permissions = READ_PERMISSION_FLAG // from server
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        

       /* // editing
        userShare.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE // from server
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isChecked()))
        

        // allow reshare
        userShare.permissions = 1 // from server
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isNotChecked()))
        

        userShare.permissions = READ_PERMISSION_FLAG // from server
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isChecked()))
        

        // set expiration date
        userShare.expirationDate = 1582019340000
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
        onView(withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
        

        userShare.expirationDate = 0
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_select_exp_date)).check(matches(withText("")))*/
    }

    private fun suppressFDFAccessibilityChecks() {
        AccessibilityChecks.enable().apply {
            setSuppressingResultMatcher(
                allOf(
                    anyOf(
                        matchesCheckNames(`is`("TouchTargetSizeCheck")),
                        matchesCheckNames(`is`("SpeakableTextPresentCheck")),
                    ),
                    anyOf(
                        matchesViews(withId(R.id.favorite)),
                        matchesViews(withId(R.id.last_modification_timestamp))
                    )
                )
            )
        }
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
        setupSecondaryFragment()
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val userShare = OCShare().apply {
            isFolder = false
            shareType = ShareType.USER
            permissions = READ_PERMISSION_FLAG
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
        setupSecondaryFragment()
        activity.addFragment(sut)
        suppressFDFAccessibilityChecks()
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val userShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.USER
            permissions = READ_PERMISSION_FLAG
        }

        activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }
        shortSleep()
        waitForIdleSync()

        // check if items are visible
        onView(withId(R.id.menu_share_open_in)).check(matches(not(isDisplayed())))
        onView(withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
        onView(withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
        onView(withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))

        onView(withId(R.id.menu_share_advanced_permissions)).check(matches(isCompletelyDisplayed()))
        // click event
        onView(withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())

        addAdvancePermissionFragment(userShare)
        // validate view shown on screen
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isDisplayed()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isDisplayed()))
        //no file drop for internal share
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_hide_download_checkbox)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_set_password_switch)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_change_name_switch)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isDisplayed()))

        // read-only
        userShare.permissions = READ_PERMISSION_FLAG // from server
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(isNotChecked()))
        

       /* // allow upload & editing
        userShare.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER // from server
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isChecked()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(isNotChecked()))
        

        // file drop
        userShare.permissions = 4
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_permission_read_only)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_upload_editing)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_permission_file_drop)).check(matches(isChecked()))
        

        // allow reshare
        userShare.permissions = 1 // from server
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isNotChecked()))
        

        userShare.permissions = READ_PERMISSION_FLAG // from server
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_allow_resharing_checkbox)).check(matches(isChecked()))
        

        // set expiration date
        userShare.expirationDate = 1582019340000
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
        onView(withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
        

        userShare.expirationDate = 0
        openAdvancedPermissions(sut, userShare)
        onView(withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
        onView(withId(R.id.share_process_select_exp_date)).check(matches(withText("")))*/
    }

    // open bottom sheet with actions
    private fun openAdvancedPermissions(
        sut: FileDetailSharingFragment,
        userShare: OCShare
    ) {
        activity.handler.post {
            sut.showSharingMenuActionSheet(userShare)
        }
        shortSleep()
        waitForIdleSync()
        onView(withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())
        addAdvancePermissionFragment(userShare)
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
        setupSecondaryFragment()
        activity.addFragment(sut)
        shortSleep()
        sut.refreshCapabilitiesFromDB()

        val userShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.USER
            permissions = READ_PERMISSION_FLAG
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

        onView(withId(R.id.menu_share_send_new_email)).check(matches(isCompletelyDisplayed()))
        //add click event to dismiss bottom sheet
        onView(withId(R.id.menu_share_send_new_email)).perform(ViewActions.click())

        //add the note edit fragment
        val noteFragment = FileDetailsSharingProcessFragment.newInstance(userShare,
            FileDetailsSharingProcessFragment.SCREEN_TYPE_NOTE, isReshareShown = true, isExpirationDateShown = true,
            isFileWithNoTextFile = SharingMenuHelper.isFileWithNoTextFile(file))
        activity.addFragment(noteFragment)
        shortSleep()

        // validate view shown on screen
        onView(withId(R.id.note_text)).check(matches(isDisplayed()))
    }

    private fun addAdvancePermissionFragment(userShare: OCShare){
        //add the note edit fragment
        val noteFragment = FileDetailsSharingProcessFragment.newInstance(userShare,
            FileDetailsSharingProcessFragment.SCREEN_TYPE_PERMISSION, isReshareShown = true, isExpirationDateShown = true,
            isFileWithNoTextFile = SharingMenuHelper.isFileWithNoTextFile(file))
        activity.addFragment(noteFragment)
        shortSleep()
    }

    @Test
    fun testUploadAndEditingSharePermissions() {

        val share = OCShare().apply {
            permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER
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
        val share = OCShare().apply {
            permissions = READ_PERMISSION_FLAG
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

        share.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER
        assertFalse(SharingMenuHelper.isReadOnly(share))

        share.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
        assertFalse(SharingMenuHelper.isReadOnly(share))
    }

    @Test
    @Suppress("MagicNumber")
    fun testFileDropSharePermissions() {
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

        share.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER
        assertFalse(SharingMenuHelper.isFileDrop(share))

        share.permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
        assertFalse(SharingMenuHelper.isFileDrop(share))
    }

    @After
    fun after() {
        activity.storageManager.cleanShares()
        activity.finish()
    }
}
