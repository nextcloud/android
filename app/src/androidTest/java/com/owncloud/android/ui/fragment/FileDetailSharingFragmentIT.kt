/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultBaseUtils.matchesCheckNames
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesViews
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.android.lib.resources.files.FileDownloadLimit
import com.nextcloud.test.RetryTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.OCShare.Companion.CREATE_PERMISSION_FLAG
import com.owncloud.android.lib.resources.shares.OCShare.Companion.DELETE_PERMISSION_FLAG
import com.owncloud.android.lib.resources.shares.OCShare.Companion.MAXIMUM_PERMISSIONS_FOR_FILE
import com.owncloud.android.lib.resources.shares.OCShare.Companion.MAXIMUM_PERMISSIONS_FOR_FOLDER
import com.owncloud.android.lib.resources.shares.OCShare.Companion.NO_PERMISSION
import com.owncloud.android.lib.resources.shares.OCShare.Companion.READ_PERMISSION_FLAG
import com.owncloud.android.lib.resources.shares.OCShare.Companion.SHARE_PERMISSION_FLAG
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.util.SharePermissionManager
import com.owncloud.android.utils.ScreenshotTest
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("TooManyFunctions")
class FileDetailSharingFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.FileDetailSharingFragmentIT"

    @get:Rule
    val retryRule = RetryTestRule()

    lateinit var file: OCFile
    lateinit var folder: OCFile

    @Before
    fun before() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                file = OCFile("/test.md").apply {
                    remoteId = "00000001"
                    parentId = activity.storageManager.getFileByEncryptedRemotePath("/").fileId
                    permissions = OCFile.PERMISSION_CAN_RESHARE
                    fileDataStorageManager.saveFile(this)
                }

                folder = OCFile("/test").apply {
                    setFolder()
                    parentId = activity.storageManager.getFileByEncryptedRemotePath("/").fileId
                    permissions = OCFile.PERMISSION_CAN_RESHARE
                }
            }
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
    fun listSharesDownloadLimit() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                OCShare(file.decryptedRemotePath).apply {
                    remoteId = 1
                    shareType = ShareType.PUBLIC_LINK
                    token = "AAAAAAAAAAAAAAA"
                    activity.storageManager.saveShare(this)
                }

                OCShare(file.decryptedRemotePath).apply {
                    remoteId = 2
                    shareType = ShareType.PUBLIC_LINK
                    token = "BBBBBBBBBBBBBBB"
                    fileDownloadLimit = FileDownloadLimit("BBBBBBBBBBBBBBB", 0, 0)
                    activity.storageManager.saveShare(this)
                }

                OCShare(file.decryptedRemotePath).apply {
                    remoteId = 3
                    shareType = ShareType.PUBLIC_LINK
                    token = "CCCCCCCCCCCCCCC"
                    fileDownloadLimit = FileDownloadLimit("CCCCCCCCCCCCCCC", 10, 0)
                    activity.storageManager.saveShare(this)
                }

                OCShare(file.decryptedRemotePath).apply {
                    remoteId = 4
                    shareType = ShareType.PUBLIC_LINK
                    token = "DDDDDDDDDDDDDDD"
                    fileDownloadLimit = FileDownloadLimit("DDDDDDDDDDDDDDD", 10, 5)
                    activity.storageManager.saveShare(this)
                }

                OCShare(file.decryptedRemotePath).apply {
                    remoteId = 5
                    shareType = ShareType.PUBLIC_LINK
                    token = "FFFFFFFFFFFFFFF"
                    fileDownloadLimit = FileDownloadLimit("FFFFFFFFFFFFFFF", 10, 10)
                    activity.storageManager.saveShare(this)
                }
            }

            show(file)
        }
    }

    /**
     * Use same values as {@link OCFileListFragmentStaticServerIT showSharedFiles }
     */
    @Test
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun listSharesFileAllShareTypes() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
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
                    sharedWithDisplayName = "Personal team"
                    permissions = SHARE_PERMISSION_FLAG
                    userId = getUserId(user)
                    activity.storageManager.saveShare(this)
                }

                OCShare(file.decryptedRemotePath).apply {
                    remoteId = 8
                    shareType = ShareType.CIRCLE
                    sharedWithDisplayName = "Public team"
                    permissions = SHARE_PERMISSION_FLAG
                    userId = getUserId(user)
                    activity.storageManager.saveShare(this)
                }

                OCShare(file.decryptedRemotePath).apply {
                    remoteId = 9
                    shareType = ShareType.CIRCLE
                    sharedWithDisplayName = "Closed team"
                    permissions = SHARE_PERMISSION_FLAG
                    userId = getUserId(user)
                    activity.storageManager.saveShare(this)
                }

                OCShare(file.decryptedRemotePath).apply {
                    remoteId = 10
                    shareType = ShareType.CIRCLE
                    sharedWithDisplayName = "Secret team"
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
            }

            show(file)
        }
    }

    private fun show(file: OCFile) {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                val fragment = FileDetailSharingFragment.newInstance(file, user)
                sut.addFragment(fragment)
            }

            val screenShotName = createName(testClassName + "_" + "show", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    // public link and email are handled the same way
    // for advanced permissions
    @Test
    @Suppress("MagicNumber")
    fun publicLinkOptionMenuFolderAdvancePermission() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var sut: FileDetailSharingFragment
            scenario.onActivity { activity ->
                sut = addSharingFragment(activity)
            }

            val publicShare = OCShare().apply {
                isFolder = true
                shareType = ShareType.PUBLIC_LINK
                permissions = 17
            }

            scenario.onActivity { sut.showSharingMenuActionSheet(publicShare) }

            // check if items are visible
            onMenuItem(R.id.menu_share_advanced_permissions).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_send_new_email).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_send_link).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_unshare).check(matches(isDisplayed()))

            // click event
            onMenuItem(R.id.menu_share_advanced_permissions).perform(ViewActions.click())

            // validate view shown on screen
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.share_process_change_name_switch)).check(matches(isDisplayed()))

            // read-only
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isNotChecked()))
            goBack(scenario)

            // upload and editing
            publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isNotChecked()))
            goBack(scenario)

            // file request
            publicShare.permissions = 4
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isChecked()))
            goBack(scenario)

            // password protection
            publicShare.shareWith = "someValue"
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isChecked()))
            goBack(scenario)

            publicShare.shareWith = ""
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isNotChecked()))
            goBack(scenario)

            // hide download
            publicShare.isHideFileDownload = true
            publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isChecked()))
            goBack(scenario)

            publicShare.isHideFileDownload = false
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(
                ViewMatchers.withId(R.id.share_process_hide_download_checkbox)
            ).check(matches(isNotChecked()))
            goBack(scenario)

            publicShare.expirationDate = 1582019340000
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
            goBack(scenario)

            publicShare.expirationDate = 0
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(withText("")))
        }
    }

    // public link and email are handled the same way
    // for send new email
    @Test
    @Suppress("MagicNumber")
    fun publicLinkOptionMenuFolderSendNewEmail() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var sut: FileDetailSharingFragment
            scenario.onActivity { activity ->
                sut = addSharingFragment(activity)
            }

            val publicShare = OCShare().apply {
                isFolder = true
                shareType = ShareType.PUBLIC_LINK
                permissions = 17
            }

            verifySendNewEmail(scenario, sut, publicShare)
        }
    }

    private fun addSharingFragment(activity: TestActivity): FileDetailSharingFragment {
        val fragment = FileDetailSharingFragment.newInstance(file, user)
        activity.addFragment(fragment)
        fragment.refreshCapabilitiesFromDB()
        setupSecondaryFragment(activity)
        activity.supportFragmentManager.executePendingTransactions()
        return fragment
    }

    // the sharing action sheet is a BottomSheetDialog, so its items must be matched in the dialog window
    private fun onMenuItem(id: Int) = onView(ViewMatchers.withId(id)).inRoot(isDialog())

    private fun setupSecondaryFragment(activity: TestActivity) {
        val parentFolder = OCFile("/")
        val secondary = FileDetailFragment.newInstance(file, parentFolder, user)
        activity.addSecondaryFragment(secondary, FileDisplayActivity.TAG_LIST_OF_FILES)
        activity.addView(
            FloatingActionButton(activity).apply {
                // needed for some reason
                visibility = View.GONE
                id = R.id.fab_main
            }
        )
    }

    // public link and email are handled the same way
    // for advanced permissions
    @Test
    @Suppress("MagicNumber")
    fun publicLinkOptionMenuFileAdvancePermission() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var sut: FileDetailSharingFragment
            scenario.onActivity { activity ->
                sut = addSharingFragment(activity)
            }

            val publicShare = OCShare().apply {
                isFolder = false
                shareType = ShareType.PUBLIC_LINK
                permissions = 17
            }
            scenario.onActivity { sut.showSharingMenuActionSheet(publicShare) }

            // check if items are visible
            onMenuItem(R.id.menu_share_advanced_permissions).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_send_new_email).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_send_link).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_unshare).check(matches(isDisplayed()))

            // click event
            onMenuItem(R.id.menu_share_advanced_permissions).perform(ViewActions.click())

            // validate view shown on screen
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isDisplayed()))
            onView(
                ViewMatchers.withId(R.id.file_request_radio_button)
            ).check(matches(not(isDisplayed())))
            onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.share_process_change_name_switch)).check(matches(isDisplayed()))

            // read-only
            publicShare.permissions = 17 // from server
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isNotChecked()))
            goBack(scenario)

            // editing
            publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FILE // from server
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isChecked()))
            goBack(scenario)

            // hide download
            publicShare.isHideFileDownload = true
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isChecked()))
            goBack(scenario)

            publicShare.isHideFileDownload = false
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(
                ViewMatchers.withId(R.id.share_process_hide_download_checkbox)
            ).check(matches(isNotChecked()))
            goBack(scenario)

            // password protection
            publicShare.isPasswordProtected = true
            publicShare.shareWith = "someValue"
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isChecked()))
            goBack(scenario)

            publicShare.isPasswordProtected = false
            publicShare.shareWith = ""
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_set_password_switch)).check(matches(isNotChecked()))
            goBack(scenario)

            // expires
            publicShare.expirationDate = 1582019340
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
            goBack(scenario)

            publicShare.expirationDate = 0
            openAdvancedPermissions(scenario, sut, publicShare)
            onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(withText("")))
        }
    }

    // public link and email are handled the same way
    // for send new email
    @Test
    @Suppress("MagicNumber")
    fun publicLinkOptionMenuFileSendNewEmail() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var sut: FileDetailSharingFragment
            scenario.onActivity { activity ->
                sut = addSharingFragment(activity)
            }

            val publicShare = OCShare().apply {
                isFolder = false
                shareType = ShareType.PUBLIC_LINK
                permissions = 17
            }

            verifySendNewEmail(scenario, sut, publicShare)
        }
    }

    // also applies for
    // group
    // conversation
    // circle
    // federated share
    // for advanced permissions
    @Test
    @Suppress("MagicNumber")
    fun userOptionMenuFileAdvancePermission() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var sut: FileDetailSharingFragment
            scenario.onActivity { activity ->
                suppressFDFAccessibilityChecks()
                sut = addSharingFragment(activity)
            }

            val userShare = OCShare().apply {
                isFolder = false
                shareType = ShareType.USER
                permissions = 17
            }

            scenario.onActivity { sut.showSharingMenuActionSheet(userShare) }

            // check if items are visible
            onMenuItem(R.id.menu_share_advanced_permissions).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_send_new_email).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_send_link).check(matches(not(isDisplayed())))
            onMenuItem(R.id.menu_share_unshare).check(matches(isDisplayed()))

            // click event
            onMenuItem(R.id.menu_share_advanced_permissions).perform(ViewActions.click())

            // validate view shown on screen
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isDisplayed()))
            onView(
                ViewMatchers.withId(R.id.file_request_radio_button)
            ).check(matches(not(isDisplayed())))
            onView(
                ViewMatchers.withId(R.id.share_process_hide_download_checkbox)
            ).check(matches(not(isDisplayed())))
            onView(
                ViewMatchers.withId(R.id.share_process_set_password_switch)
            ).check(matches(not(isDisplayed())))
            onView(
                ViewMatchers.withId(R.id.share_process_change_name_switch)
            ).check(matches(not(isDisplayed())))

            // read-only
            userShare.permissions = 17 // from server
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isNotChecked()))
            goBack(scenario)

            // editing
            userShare.permissions = MAXIMUM_PERMISSIONS_FOR_FILE // from server
            openAdvancedPermissions(scenario, sut, userShare)
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isChecked()))
            goBack(scenario)

            // set expiration date
            userShare.expirationDate = 1582019340000
            openAdvancedPermissions(scenario, sut, userShare)
            onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
            goBack(scenario)

            userShare.expirationDate = 0
            openAdvancedPermissions(scenario, sut, userShare)
            onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(withText("")))
        }
    }

    private fun suppressFDFAccessibilityChecks() {
        AccessibilityChecks.enable().apply {
            setSuppressingResultMatcher(
                allOf(
                    anyOf(
                        matchesCheckNames(`is`("TouchTargetSizeCheck")),
                        matchesCheckNames(`is`("SpeakableTextPresentCheck"))
                    ),
                    anyOf(
                        matchesViews(ViewMatchers.withId(R.id.favorite)),
                        matchesViews(ViewMatchers.withId(R.id.last_modification_timestamp))
                    )
                )
            )
        }
    }

    // also applies for
    // group
    // conversation
    // circle
    // federated share
    // for send new email
    @Test
    @Suppress("MagicNumber")
    fun userOptionMenuFileSendNewEmail() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var sut: FileDetailSharingFragment
            scenario.onActivity { activity ->
                sut = addSharingFragment(activity)
            }

            val userShare = OCShare().apply {
                remoteId = 1001L
                isFolder = false
                shareType = ShareType.USER
                permissions = 17
            }

            verifySendNewEmail(scenario, sut, userShare)
        }
    }

    // also applies for
    // group
    // conversation
    // circle
    // federated share
    // for advanced permissions
    @Test
    @Suppress("MagicNumber")
    fun userOptionMenuFolderAdvancePermission() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var sut: FileDetailSharingFragment
            scenario.onActivity { activity ->
                suppressFDFAccessibilityChecks()
                sut = addSharingFragment(activity)
            }

            val userShare = OCShare().apply {
                isFolder = true
                shareType = ShareType.USER
                permissions = 17
            }

            scenario.onActivity { sut.showSharingMenuActionSheet(userShare) }

            // check if items are visible
            onMenuItem(R.id.menu_share_advanced_permissions).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_send_new_email).check(matches(isDisplayed()))
            onMenuItem(R.id.menu_share_send_link).check(matches(not(isDisplayed())))
            onMenuItem(R.id.menu_share_unshare).check(matches(isDisplayed()))

            // click event
            onMenuItem(R.id.menu_share_advanced_permissions).perform(ViewActions.click())

            // validate view shown on screen
            // file request is only offered for public link and email shares, not for user shares
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isDisplayed()))
            onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(not(isDisplayed())))
            onView(
                ViewMatchers.withId(R.id.share_process_hide_download_checkbox)
            ).check(matches(not(isDisplayed())))
            onView(
                ViewMatchers.withId(R.id.share_process_set_password_switch)
            ).check(matches(not(isDisplayed())))
            onView(
                ViewMatchers.withId(R.id.share_process_change_name_switch)
            ).check(matches(not(isDisplayed())))

            // read-only
            userShare.permissions = 17 // from server
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isNotChecked()))
            goBack(scenario)

            // allow upload & editing
            userShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER // from server
            openAdvancedPermissions(scenario, sut, userShare)
            onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isChecked()))
            goBack(scenario)

            // set expiration date
            userShare.expirationDate = 1582019340000
            openAdvancedPermissions(scenario, sut, userShare)
            onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isChecked()))
            onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(not(withText(""))))
            goBack(scenario)

            userShare.expirationDate = 0
            openAdvancedPermissions(scenario, sut, userShare)
            onView(ViewMatchers.withId(R.id.share_process_set_exp_date_switch)).check(matches(isNotChecked()))
            onView(ViewMatchers.withId(R.id.share_process_select_exp_date)).check(matches(withText("")))
        }
    }

    // open bottom sheet with actions
    private fun openAdvancedPermissions(
        scenario: ActivityScenario<TestActivity>,
        sut: FileDetailSharingFragment,
        userShare: OCShare
    ) {
        scenario.onActivity { sut.showSharingMenuActionSheet(userShare) }
        onMenuItem(R.id.menu_share_advanced_permissions).perform(ViewActions.click())
    }

    // remove the fragment shown
    private fun goBack(scenario: ActivityScenario<TestActivity>) {
        scenario.onActivity { activity ->
            val processFragment =
                activity.supportFragmentManager.findFragmentByTag(FileDetailsSharingProcessFragment.TAG) as
                    FileDetailsSharingProcessFragment
            processFragment.onBackPressed()
        }
    }

    // also applies for
    // group
    // conversation
    // circle
    // federated share
    // for send new email
    @Test
    @Suppress("MagicNumber")
    fun userOptionMenuFolderSendNewEmail() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var sut: FileDetailSharingFragment
            scenario.onActivity { activity ->
                sut = addSharingFragment(activity)
            }

            val userShare = OCShare().apply {
                isFolder = true
                shareType = ShareType.USER
                permissions = 17
            }

            verifySendNewEmail(scenario, sut, userShare)
        }
    }

    /**
     * verify send new email note text
     */
    private fun verifySendNewEmail(
        scenario: ActivityScenario<TestActivity>,
        sut: FileDetailSharingFragment,
        userShare: OCShare
    ) {
        scenario.onActivity { sut.showSharingMenuActionSheet(userShare) }

        // click event
        onMenuItem(R.id.menu_share_send_new_email).perform(ViewActions.click())

        // validate view shown on screen
        onView(ViewMatchers.withId(R.id.note_text)).check(matches(isDisplayed()))
    }

    @Test
    fun testUploadAndEditingSharePermissions() {
        val testCases = mapOf(
            MAXIMUM_PERMISSIONS_FOR_FOLDER to true,
            NO_PERMISSION to false,
            READ_PERMISSION_FLAG to false,
            CREATE_PERMISSION_FLAG to false,
            DELETE_PERMISSION_FLAG to false,
            SHARE_PERMISSION_FLAG to false
        )

        val share = OCShare()
        for ((permission, expected) in testCases) {
            share.permissions = permission
            assertEquals("Failed for permission: $permission", expected, SharePermissionManager.canEdit(share))
        }
    }

    @Test
    fun testReadOnlySharePermissions() {
        val testCases = mapOf(
            READ_PERMISSION_FLAG to true,
            NO_PERMISSION to false,
            CREATE_PERMISSION_FLAG to false,
            DELETE_PERMISSION_FLAG to false,
            SHARE_PERMISSION_FLAG to false,
            MAXIMUM_PERMISSIONS_FOR_FOLDER to false,
            MAXIMUM_PERMISSIONS_FOR_FILE to false
        )

        val share = OCShare()
        for ((permission, expected) in testCases) {
            share.permissions = permission
            assertEquals("Failed for permission: $permission", expected, SharePermissionManager.isViewOnly(share))
        }
    }

    @Test
    fun testFileRequestSharePermission() {
        val testCases = mapOf(
            CREATE_PERMISSION_FLAG to true,
            NO_PERMISSION to false,
            READ_PERMISSION_FLAG to false,
            DELETE_PERMISSION_FLAG to false,
            SHARE_PERMISSION_FLAG to false,
            MAXIMUM_PERMISSIONS_FOR_FOLDER to false,
            MAXIMUM_PERMISSIONS_FOR_FILE to false
        )

        val share = OCShare().apply {
            isFolder = true
        }

        for ((permission, expected) in testCases) {
            share.permissions = permission
            assertEquals("Failed for permission: $permission", expected, SharePermissionManager.isFileRequest(share))
        }
    }

    @Test
    fun internalLinkUsesPrettyPathWhenModRewriteWorking() {
        launchActivity<TestActivity>().use { scenario ->
            var sut: FileDetailSharingFragment? = null
            scenario.onActivity { activity ->
                sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)
                activity.supportFragmentManager.executePendingTransactions()
            }

            val capabilities = OCCapability().apply { modRewriteWorking = CapabilityBooleanType.TRUE }
            val link = sut!!.createInternalLink(user, file, capabilities)

            assertTrue(link.endsWith("/f/" + file.localId))
            assertFalse(link.contains("/index.php/"))
        }
    }

    @Test
    fun internalLinkUsesDefaultPathWhenModRewriteNotWorking() {
        launchActivity<TestActivity>().use { scenario ->
            var sut: FileDetailSharingFragment? = null
            scenario.onActivity { activity ->
                sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)
                activity.supportFragmentManager.executePendingTransactions()
            }

            val capabilities = OCCapability().apply { modRewriteWorking = CapabilityBooleanType.FALSE }
            val link = sut!!.createInternalLink(user, file, capabilities)

            assertTrue(link.endsWith("/index.php/f/" + file.localId))
        }
    }

    @Test
    fun internalLinkFallsBackToDefaultPathWhenCapabilitiesNull() {
        launchActivity<TestActivity>().use { scenario ->
            var sut: FileDetailSharingFragment? = null
            scenario.onActivity { activity ->
                sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)
                activity.supportFragmentManager.executePendingTransactions()
            }

            val link = sut!!.createInternalLink(user, file, null)

            assertTrue(link.endsWith("/index.php/f/" + file.localId))
        }
    }
}
