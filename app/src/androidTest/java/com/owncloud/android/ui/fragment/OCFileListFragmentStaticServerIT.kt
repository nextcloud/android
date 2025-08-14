/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import androidx.annotation.UiThread
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
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.lib.resources.tags.Tag
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class OCFileListFragmentStaticServerIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.OCFileListFragmentStaticServerIT"

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
    @UiThread
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun showFiles() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    OCFile("/1.png").apply {
                        remoteId = "00000001"
                        mimeType = "image/png"
                        fileLength = 1024000
                        modificationTimestamp = 1188206955000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/image.png").apply {
                        remoteId = "00000002"
                        mimeType = "image/png"
                        isPreviewAvailable = false
                        fileLength = 3072000
                        modificationTimestamp = 746443755000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        tags = listOf(Tag("", "Top secret", null))
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/live photo.png").apply {
                        remoteId = "00000003"
                        mimeType = "image/png"
                        isPreviewAvailable = false
                        fileLength = 3072000
                        modificationTimestamp = 746443755000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        setLivePhoto("/video.mov")
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/video.mp4").apply {
                        remoteId = "00000004"
                        mimeType = "video/mp4"
                        isPreviewAvailable = false
                        fileLength = 12092000
                        modificationTimestamp = 746143952000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        tags = listOf(Tag("", "Confidential", null), Tag("", "+5", null))
                        sut.storageManager.saveFile(this)
                    }

                    sut.addFragment(OCFileListFragment())

                    val fragment = (sut.fragment as OCFileListFragment)
                    val root = sut.storageManager.getFileByEncryptedRemotePath("/")
                    fragment.listDirectory(root, false, false)

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showFiles", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    /**
     * Use same values as {@link FileDetailSharingFragmentIT listSharesFileAllShareTypes }
     */
    @Test
    @ScreenshotTest
    fun showSharedFiles() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val fragment = OCFileListFragment()

                    OCFile("/s2haredToUser.jpg").apply {
                        remoteId = "00000001"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedWithSharee = true
                        sharees = listOf(ShareeUser("Admin", "Server Admin", ShareType.USER))
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/sharedT124oGroup.jpg").apply {
                        remoteId = "00000002"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedWithSharee = true
                        sharees = listOf(ShareeUser("group", "Group", ShareType.GROUP))
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/sharedToEmail.jpg").apply {
                        remoteId = "00000003"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedWithSharee = true
                        sharees =
                            listOf(
                                ShareeUser("admin@nextcloud.localhost", "admin@nextcloud.localhost", ShareType.EMAIL)
                            )
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/publicLink.jpg").apply {
                        remoteId = "00000004"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedViaLink = true
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/sharedToFederatedUser.jpg").apply {
                        remoteId = "00000005"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedWithSharee = true
                        sharees = listOf(
                            ShareeUser(
                                "admin@remote.nextcloud.com",
                                "admin@remote.nextcloud.com (remote)",
                                ShareType.FEDERATED
                            )
                        )
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/sharedToPersonalCircle.jpg").apply {
                        remoteId = "00000006"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedWithSharee = true
                        sharees = listOf(ShareeUser("circle", "Circle (Personal circle)", ShareType.CIRCLE))
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/sharedToUserRoom.jpg").apply {
                        remoteId = "00000007"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedWithSharee = true
                        sharees = listOf(ShareeUser("Conversation", "Admin", ShareType.ROOM))
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/sharedToGroupRoom.jpg").apply {
                        remoteId = "00000008"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedWithSharee = true
                        sharees = listOf(ShareeUser("Conversation", "Meeting", ShareType.ROOM))
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/sharedToUsers.jpg").apply {
                        remoteId = "00000009"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        isSharedWithSharee = true
                        sharees = listOf(
                            ShareeUser("Admin", "Server Admin", ShareType.USER),
                            ShareeUser("User", "User", ShareType.USER),
                            ShareeUser("Christine", "Christine Scott", ShareType.USER)
                        )
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/notShared.jpg").apply {
                        remoteId = "000000010"
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        modificationTimestamp = 1000
                        sut.storageManager.saveFile(this)
                    }

                    sut.addFragment(fragment)
                    val root = sut.storageManager.getFileByEncryptedRemotePath("/")
                    fragment.listDirectory(root, false, false)
                    fragment.adapter.setShowShareAvatar(true)

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showSharedFiles", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    /**
     * Use same values as {@link FileDetailSharingFragmentIT listSharesFileAllShareTypes }
     */
    @Test
    @ScreenshotTest
    fun showFolderTypes() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val fragment = OCFileListFragment()

                    OCFile("/normal/").apply {
                        remoteId = "00000001"
                        mimeType = MimeType.DIRECTORY
                        modificationTimestamp = 1624003571000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/sharedViaLink/").apply {
                        remoteId = "00000002"
                        mimeType = MimeType.DIRECTORY
                        isSharedViaLink = true
                        modificationTimestamp = 1619003571000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/share/").apply {
                        remoteId = "00000003"
                        mimeType = MimeType.DIRECTORY
                        isSharedWithSharee = true
                        modificationTimestamp = 1619303571000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/groupaaaaaFolder/").apply {
                        remoteId = "00000004"
                        mimeType = MimeType.DIRECTORY
                        modificationTimestamp = 1615003571000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        permissions += "M"
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/encrypted/").apply {
                        remoteId = "00000005"
                        mimeType = MimeType.DIRECTORY
                        isEncrypted = true
                        decryptedRemotePath = "/encrypted/"
                        modificationTimestamp = 1614003571000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/locked/").apply {
                        remoteId = "00000006"
                        mimeType = MimeType.DIRECTORY
                        isLocked = true
                        decryptedRemotePath = "/locked/"
                        modificationTimestamp = 1613003571000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        sut.storageManager.saveFile(this)
                    }

                    OCFile("/offlineOperation/").apply {
                        mimeType = MimeType.DIRECTORY
                        decryptedRemotePath = "/offlineOperation/"
                        modificationTimestamp = System.currentTimeMillis()
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
                        sut.storageManager.saveFile(this)
                    }

                    sut.addFragment(fragment)

                    val root = sut.storageManager.getFileByEncryptedRemotePath("/")
                    fragment.listDirectory(root, false, false)
                    fragment.adapter.setShowShareAvatar(true)

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showFolderTypes", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun showRichWorkspace() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val fragment = OCFileListFragment()

                    val folder = OCFile("/test/")
                    folder.setFolder()
                    sut.storageManager.saveFile(folder)

                    val imageFile = OCFile("/test/image.png").apply {
                        remoteId = "00000001"
                        mimeType = "image/png"
                        fileLength = 1024000
                        modificationTimestamp = 1188206955000
                        parentId = sut.storageManager.getFileByEncryptedRemotePath("/test/").fileId
                        storagePath = getFile("java.md").absolutePath
                    }

                    sut.storageManager.saveFile(imageFile)

                    sut.addFragment(fragment)
                    val testFolder: OCFile = sut.storageManager.getFileByEncryptedRemotePath("/test/")
                    testFolder.richWorkspace = getFile("java.md").readText()
                    fragment.listDirectory(testFolder, false, false)

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showRichWorkspace", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    fun shouldShowHeader() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val sut = OCFileListFragment()
                    val folder = OCFile("/test/").apply {
                        remoteId = "000001"
                        setFolder()
                    }
                    activity.storageManager.saveFile(folder)
                    activity.addFragment(sut)
                    val testFolder: OCFile = activity.storageManager.getFileByEncryptedRemotePath("/test/")
                    EspressoIdlingResource.decrement()

                    // richWorkspace is not set
                    Assert.assertFalse(sut.adapter.shouldShowHeader())

                    EspressoIdlingResource.increment()
                    testFolder.richWorkspace = " "
                    activity.storageManager.saveFile(testFolder)
                    sut.adapter.swapDirectory(user, testFolder, activity.storageManager, false, "")
                    EspressoIdlingResource.decrement()

                    Assert.assertFalse(sut.adapter.shouldShowHeader())

                    EspressoIdlingResource.increment()
                    testFolder.richWorkspace = null
                    activity.storageManager.saveFile(testFolder)
                    sut.adapter.swapDirectory(user, testFolder, activity.storageManager, false, "")
                    EspressoIdlingResource.decrement()
                    Assert.assertFalse(sut.adapter.shouldShowHeader())

                    EspressoIdlingResource.increment()
                    testFolder.richWorkspace = "1"
                    activity.storageManager.saveFile(testFolder)
                    sut.adapter.setCurrentDirectory(testFolder)
                    EspressoIdlingResource.decrement()

                    Assert.assertTrue(sut.adapter.shouldShowHeader())
                }
            }
        }
    }
}
