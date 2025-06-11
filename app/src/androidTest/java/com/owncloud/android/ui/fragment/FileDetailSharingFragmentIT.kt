/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.view.View
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
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
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.util.SharePermissionManager
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

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
    @UiThread
    @ScreenshotTest
    fun listSharesFileNone() {
        show(file)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun listSharesFileResharingNotAllowed() {
        file.permissions = ""

        show(file)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun listSharesDownloadLimit() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
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
                    EspressoIdlingResource.decrement()

                    show(file)
                }
            }
        }
    }

    /**
     * Use same values as {@link OCFileListFragmentStaticServerIT showSharedFiles }
     */
    @Test
    @UiThread
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun listSharesFileAllShareTypes() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
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
                    EspressoIdlingResource.decrement()

                    show(file)
                }
            }
        }
    }

    private fun show(file: OCFile) {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val fragment = FileDetailSharingFragment.newInstance(file, user)
                    sut.addFragment(fragment)
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "show", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    // public link and email are handled the same way
    // for advanced permissions
    @Test
    @Suppress("MagicNumber")
    fun publicLinkOptionMenuFolderAdvancePermission() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)
                onIdleSync {
                    EspressoIdlingResource.increment()
                    setupSecondaryFragment()
                    sut.refreshCapabilitiesFromDB()

                    val publicShare = OCShare().apply {
                        isFolder = true
                        shareType = ShareType.PUBLIC_LINK
                        permissions = 17
                    }

                    EspressoIdlingResource.decrement()
                    activity.runOnUiThread { sut.showSharingMenuActionSheet(publicShare) }

                    // check if items are visible
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_send_link)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))

                    // click event
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())

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
                    goBack()

                    // upload and editing
                    publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER
                    openAdvancedPermissions(sut, publicShare)
                    onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
                    onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isChecked()))
                    onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isNotChecked()))
                    goBack()

                    // file request
                    publicShare.permissions = 4
                    openAdvancedPermissions(sut, publicShare)
                    onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
                    onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isNotChecked()))
                    onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isChecked()))
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
                    onView(
                        ViewMatchers.withId(R.id.share_process_hide_download_checkbox)
                    ).check(matches(isNotChecked()))
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
            }
        }
    }

    // public link and email are handled the same way
    // for send new email
    @Test
    @Suppress("MagicNumber")
    fun publicLinkOptionMenuFolderSendNewEmail() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)
                onIdleSync {
                    EspressoIdlingResource.increment()
                    setupSecondaryFragment()
                    sut.refreshCapabilitiesFromDB()
                    EspressoIdlingResource.decrement()

                    val publicShare = OCShare().apply {
                        isFolder = true
                        shareType = ShareType.PUBLIC_LINK
                        permissions = 17
                    }

                    verifySendNewEmail(sut, publicShare)
                }
            }
        }
    }

    private fun setupSecondaryFragment() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
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
        }
    }

    // public link and email are handled the same way
    // for advanced permissions
    @Test
    @Suppress("MagicNumber")
    fun publicLinkOptionMenuFileAdvancePermission() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)

                onIdleSync {
                    EspressoIdlingResource.increment()
                    setupSecondaryFragment()
                    sut.refreshCapabilitiesFromDB()
                    EspressoIdlingResource.decrement()

                    val publicShare = OCShare().apply {
                        isFolder = false
                        shareType = ShareType.PUBLIC_LINK
                        permissions = 17
                    }
                    activity.handler.post { sut.showSharingMenuActionSheet(publicShare) }
                    waitForIdleSync()

                    // check if items are visible
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_send_link)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))

                    // click event
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())

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
                    goBack()

                    // editing
                    publicShare.permissions = MAXIMUM_PERMISSIONS_FOR_FILE // from server
                    openAdvancedPermissions(sut, publicShare)
                    onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
                    onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isChecked()))
                    goBack()

                    // hide download
                    publicShare.isHideFileDownload = true
                    openAdvancedPermissions(sut, publicShare)
                    onView(ViewMatchers.withId(R.id.share_process_hide_download_checkbox)).check(matches(isChecked()))
                    goBack()

                    publicShare.isHideFileDownload = false
                    openAdvancedPermissions(sut, publicShare)
                    onView(
                        ViewMatchers.withId(R.id.share_process_hide_download_checkbox)
                    ).check(matches(isNotChecked()))
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
            }
        }
    }

    // public link and email are handled the same way
    // for send new email
    @Test
    @Suppress("MagicNumber")
    fun publicLinkOptionMenuFileSendNewEmail() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)
                onIdleSync {
                    EspressoIdlingResource.increment()
                    setupSecondaryFragment()
                    sut.refreshCapabilitiesFromDB()
                    EspressoIdlingResource.decrement()

                    val publicShare = OCShare().apply {
                        isFolder = false
                        shareType = ShareType.PUBLIC_LINK
                        permissions = 17
                    }

                    verifySendNewEmail(sut, publicShare)
                }
            }
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
            scenario.onActivity { activity ->
                val sut = FileDetailSharingFragment.newInstance(file, user)
                suppressFDFAccessibilityChecks()
                activity.addFragment(sut)

                onIdleSync {
                    EspressoIdlingResource.increment()
                    setupSecondaryFragment()
                    sut.refreshCapabilitiesFromDB()
                    EspressoIdlingResource.decrement()

                    val userShare = OCShare().apply {
                        isFolder = false
                        shareType = ShareType.USER
                        permissions = 17
                    }

                    activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }

                    // check if items are visible
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_send_link)).check(matches(not(isDisplayed())))
                    onView(ViewMatchers.withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))

                    // click event
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())
                    shortSleep()
                    waitForIdleSync()

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
                    goBack()

                    // editing
                    userShare.permissions = MAXIMUM_PERMISSIONS_FOR_FILE // from server
                    openAdvancedPermissions(sut, userShare)
                    onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
                    onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isChecked()))
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
            }
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
            scenario.onActivity { activity ->
                val sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)

                onIdleSync {
                    EspressoIdlingResource.increment()
                    setupSecondaryFragment()
                    sut.refreshCapabilitiesFromDB()
                    EspressoIdlingResource.decrement()

                    val userShare = OCShare().apply {
                        remoteId = 1001L
                        isFolder = false
                        shareType = ShareType.USER
                        permissions = 17
                    }

                    verifySendNewEmail(sut, userShare)
                }
            }
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
            scenario.onActivity { activity ->
                val sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)

                onIdleSync {
                    EspressoIdlingResource.increment()
                    setupSecondaryFragment()
                    suppressFDFAccessibilityChecks()
                    sut.refreshCapabilitiesFromDB()
                    EspressoIdlingResource.decrement()

                    val userShare = OCShare().apply {
                        isFolder = true
                        shareType = ShareType.USER
                        permissions = 17
                    }

                    activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }

                    // check if items are visible
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.menu_share_send_link)).check(matches(not(isDisplayed())))
                    onView(ViewMatchers.withId(R.id.menu_share_unshare)).check(matches(isDisplayed()))

                    // click event
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())

                    // validate view shown on screen
                    onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isDisplayed()))
                    onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isDisplayed()))
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
                    onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isNotChecked()))
                    goBack()

                    // allow upload & editing
                    userShare.permissions = MAXIMUM_PERMISSIONS_FOR_FOLDER // from server
                    openAdvancedPermissions(sut, userShare)
                    onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
                    onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isChecked()))
                    onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isNotChecked()))
                    goBack()

                    // file request
                    userShare.permissions = 4
                    openAdvancedPermissions(sut, userShare)
                    onView(ViewMatchers.withId(R.id.view_only_radio_button)).check(matches(isNotChecked()))
                    onView(ViewMatchers.withId(R.id.can_edit_radio_button)).check(matches(isNotChecked()))
                    onView(ViewMatchers.withId(R.id.file_request_radio_button)).check(matches(isChecked()))
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
            }
        }
    }

    // open bottom sheet with actions
    private fun openAdvancedPermissions(sut: FileDetailSharingFragment, userShare: OCShare) {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    activity.handler.post {
                        sut.showSharingMenuActionSheet(userShare)
                    }
                    EspressoIdlingResource.decrement()
                    onView(ViewMatchers.withId(R.id.menu_share_advanced_permissions)).perform(ViewActions.click())
                }
            }
        }
    }

    // remove the fragment shown
    private fun goBack() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    activity.handler.post {
                        val processFragment =
                            activity.supportFragmentManager.findFragmentByTag(FileDetailsSharingProcessFragment.TAG) as
                                FileDetailsSharingProcessFragment
                        processFragment.onBackPressed()
                    }
                }
            }
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
            scenario.onActivity { activity ->
                val sut = FileDetailSharingFragment.newInstance(file, user)
                activity.addFragment(sut)
                onIdleSync {
                    EspressoIdlingResource.increment()
                    setupSecondaryFragment()
                    sut.refreshCapabilitiesFromDB()
                    EspressoIdlingResource.decrement()

                    val userShare = OCShare().apply {
                        isFolder = true
                        shareType = ShareType.USER
                        permissions = 17
                    }

                    verifySendNewEmail(sut, userShare)
                }
            }
        }
    }

    /**
     * verify send new email note text
     */
    private fun verifySendNewEmail(sut: FileDetailSharingFragment, userShare: OCShare) {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    activity.runOnUiThread { sut.showSharingMenuActionSheet(userShare) }
                    EspressoIdlingResource.decrement()

                    // click event
                    onView(ViewMatchers.withId(R.id.menu_share_send_new_email)).perform(ViewActions.click())

                    // validate view shown on screen
                    onView(ViewMatchers.withId(R.id.note_text)).check(matches(isDisplayed()))
                }
            }
        }
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
}
