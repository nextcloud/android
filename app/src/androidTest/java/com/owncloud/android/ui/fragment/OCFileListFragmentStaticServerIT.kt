/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.GrantStoragePermissionRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.lib.resources.systemTag.SystemTag
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class OCFileListFragmentStaticServerIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantStoragePermissionRule.grant()

    @Test
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun showFiles() {
        val sut = testActivityRule.launchActivity(null)

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
            tags = listOf(SystemTag("Top secret", null))
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
            tags = listOf(SystemTag("Confidential", null), SystemTag("+5", null))
            sut.storageManager.saveFile(this)
        }

        sut.addFragment(OCFileListFragment())

        val fragment = (sut.fragment as OCFileListFragment)
        val root = sut.storageManager.getFileByEncryptedRemotePath("/")

        shortSleep()

        sut.runOnUiThread { fragment.listDirectory(root, false, false) }

        waitForIdleSync()

        screenshot(sut)
    }

    /**
     * Use same values as {@link FileDetailSharingFragmentIT listSharesFileAllShareTypes }
     */
    @Test
    @ScreenshotTest
    fun showSharedFiles() {
        val sut = testActivityRule.launchActivity(null)
        val fragment = OCFileListFragment()

        OCFile("/sharedToUser.jpg").apply {
            remoteId = "00000001"
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("Admin", "Server Admin", ShareType.USER))
            modificationTimestamp = 1000
            sut.storageManager.saveFile(this)
        }

        OCFile("/sharedToGroup.jpg").apply {
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
            sharees = listOf(ShareeUser("admin@nextcloud.localhost", "admin@nextcloud.localhost", ShareType.EMAIL))
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
                ShareeUser("admin@remote.nextcloud.com", "admin@remote.nextcloud.com (remote)", ShareType.FEDERATED)
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

        // as we cannot distinguish circle types, we do not need them right now
//        OCFile("/sharedToPublicCircle.jpg").apply {
//            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
//            isSharedWithSharee = true
//            sharees = listOf(ShareeUser("circle", "Circle (Public circle)", ShareType.CIRCLE))
//            modificationTimestamp = 1000
//            sut.storageManager.saveFile(this)
//        }
//
//        OCFile("/sharedToClosedCircle.jpg").apply {
//            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
//            isSharedWithSharee = true
//            sharees = listOf(ShareeUser("circle", "Circle (Closed circle)", ShareType.CIRCLE))
//            modificationTimestamp = 1000
//            sut.storageManager.saveFile(this)
//        }
//
//        OCFile("/sharedToSecretCircle.jpg").apply {
//            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
//            isSharedWithSharee = true
//            sharees = listOf(ShareeUser("circle", "Circle (Secret circle)", ShareType.CIRCLE))
//            modificationTimestamp = 1000
//            sut.storageManager.saveFile(this)
//        }

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

        shortSleep()

        val root = sut.storageManager.getFileByEncryptedRemotePath("/")

        sut.runOnUiThread {
            fragment.listDirectory(root, false, false)
            fragment.adapter.setShowShareAvatar(true)
        }

        waitForIdleSync()
        shortSleep()
        shortSleep()
        shortSleep()

        screenshot(sut)
    }

    /**
     * Use same values as {@link FileDetailSharingFragmentIT listSharesFileAllShareTypes }
     */
    @Test
    @ScreenshotTest
    fun showFolderTypes() {
        val sut = testActivityRule.launchActivity(null)
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

        OCFile("/groupFolder/").apply {
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

        shortSleep()

        val root = sut.storageManager.getFileByEncryptedRemotePath("/")

        sut.runOnUiThread {
            fragment.listDirectory(root, false, false)
            fragment.adapter.setShowShareAvatar(true)
        }

        waitForIdleSync()
        shortSleep()
        shortSleep()
        shortSleep()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun showRichWorkspace() {
        val sut = testActivityRule.launchActivity(null)
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

        sut.runOnUiThread { fragment.listDirectory(testFolder, false, false) }

        shortSleep()

        screenshot(sut)
    }

    @Test
    fun shouldShowHeader() {
        val activity = testActivityRule.launchActivity(null)
        val sut = OCFileListFragment()

        val folder = OCFile("/test/").apply {
            remoteId = "000001"
            setFolder()
        }

        activity.storageManager.saveFile(folder)

        activity.addFragment(sut)
        val testFolder: OCFile = activity.storageManager.getFileByEncryptedRemotePath("/test/")

        activity.runOnUiThread {
            // richWorkspace is not set
            Assert.assertFalse(sut.adapter.shouldShowHeader())

            testFolder.richWorkspace = " "
            activity.storageManager.saveFile(testFolder)
            sut.adapter.swapDirectory(user, testFolder, activity.storageManager, false, "")
            Assert.assertFalse(sut.adapter.shouldShowHeader())

            testFolder.richWorkspace = null
            activity.storageManager.saveFile(testFolder)
            sut.adapter.swapDirectory(user, testFolder, activity.storageManager, false, "")
            Assert.assertFalse(sut.adapter.shouldShowHeader())

            testFolder.richWorkspace = "1"
            activity.storageManager.saveFile(testFolder)
            sut.adapter.currentDirectory = testFolder
            Assert.assertTrue(sut.adapter.shouldShowHeader())
        }
    }
}
