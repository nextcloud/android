/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.GrantStoragePermissionRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
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

        val textFile = OCFile("/1.png")
        textFile.mimeType = "image/png"
        textFile.fileLength = 1024000
        textFile.modificationTimestamp = 1188206955000
        textFile.parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
        sut.storageManager.saveFile(textFile)

        val imageFile = OCFile("/image.png")
        imageFile.mimeType = "image/png"
        imageFile.isPreviewAvailable = false
        imageFile.fileLength = 3072000
        imageFile.modificationTimestamp = 746443755000
        imageFile.parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
        sut.storageManager.saveFile(imageFile)

        OCFile("/video.mp4").apply {
            mimeType = "video/mp4"
            isPreviewAvailable = false
            fileLength = 12092000
            modificationTimestamp = 746143952000
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
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
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("Admin", "Server Admin", ShareType.USER))
            modificationTimestamp = 1000
            sut.storageManager.saveFile(this)
        }

        OCFile("/sharedToGroup.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("group", "Group", ShareType.GROUP))
            modificationTimestamp = 1000
            sut.storageManager.saveFile(this)
        }

        OCFile("/sharedToEmail.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("admin@nextcloud.localhost", "admin@nextcloud.localhost", ShareType.EMAIL))
            modificationTimestamp = 1000
            sut.storageManager.saveFile(this)
        }

        OCFile("/publicLink.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedViaLink = true
            modificationTimestamp = 1000
            sut.storageManager.saveFile(this)
        }

        OCFile("/sharedToFederatedUser.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(
                ShareeUser("admin@remote.nextcloud.com", "admin@remote.nextcloud.com (remote)", ShareType.FEDERATED)
            )
            modificationTimestamp = 1000
            sut.storageManager.saveFile(this)
        }

        OCFile("/sharedToPersonalCircle.jpg").apply {
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
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("Conversation", "Admin", ShareType.ROOM))
            modificationTimestamp = 1000
            sut.storageManager.saveFile(this)
        }

        OCFile("/sharedToGroupRoom.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("Conversation", "Meeting", ShareType.ROOM))
            modificationTimestamp = 1000
            sut.storageManager.saveFile(this)
        }

        OCFile("/sharedToUsers.jpg").apply {
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
            mimeType = MimeType.DIRECTORY
            modificationTimestamp = 1624003571000
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            sut.storageManager.saveFile(this)
        }

        OCFile("/groupFolder/").apply {
            mimeType = MimeType.DIRECTORY
            modificationTimestamp = 1615003571000
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            permissions += "M"
            sut.storageManager.saveFile(this)
        }

        OCFile("/encrypted/").apply {
            mimeType = MimeType.DIRECTORY
            isEncrypted = true
            decryptedRemotePath = "/encrypted/"
            modificationTimestamp = 1614003571000
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

        val imageFile = OCFile("/test/image.png")
        imageFile.mimeType = "image/png"
        imageFile.fileLength = 1024000
        imageFile.modificationTimestamp = 1188206955000
        imageFile.parentId = sut.storageManager.getFileByEncryptedRemotePath("/test/").fileId
        imageFile.storagePath = getFile("java.md").absolutePath
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

        val folder = OCFile("/test/")
        folder.setFolder()
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
            sut.adapter.setCurrentDirectory(testFolder)
            Assert.assertTrue(sut.adapter.shouldShowHeader())
        }
    }
}
