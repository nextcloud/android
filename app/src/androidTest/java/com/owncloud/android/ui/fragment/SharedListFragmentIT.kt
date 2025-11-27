/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.view.View
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.GrantStoragePermissionRule.Companion.grant
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.adapter.OCShareToOCFileConverter
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

internal class SharedListFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.SharedListFragmentIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @get:Rule
    var storagePermissionRule: TestRule = grant()

    @Test
    @ScreenshotTest
    fun showSharedFiles() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

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

                    fragment.isLoading = false
                    fragment.mEmptyListContainer?.visibility = View.GONE

                    val newList = runBlocking {
                        OCShareToOCFileConverter
                            .parseAndSaveShares(listOf(), shares, storageManager, user.accountName)
                    }
                    fragment.adapter.run {
                        prepareForSearchData(storageManager, SearchType.SHARED_FILTER)
                        updateAdapter(newList, null)
                    }
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showSharedFiles", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
