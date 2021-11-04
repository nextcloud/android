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

import android.Manifest
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.rule.GrantPermissionRule
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class OCFileListFragmentStaticServerIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    @ScreenshotTest
    @Suppress("MagicNumber")
    fun showFiles() {
        val sut = testActivityRule.launchActivity(null)

        val textFile = OCFile("/1.png", "00000001")
        textFile.mimeType = "image/png"
        textFile.fileLength = 1024000
        textFile.modificationTimestamp = 1188206955000
        textFile.parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
        sut.storageManager.saveFile(textFile)

        val imageFile = OCFile("/image.png", "00000002")
        imageFile.mimeType = "image/png"
        imageFile.isPreviewAvailable = false
        imageFile.fileLength = 3072000
        imageFile.modificationTimestamp = 746443755000
        imageFile.parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
        sut.storageManager.saveFile(imageFile)

        sut.addFragment(OCFileListFragment())

        val fragment = (sut.fragment as OCFileListFragment)
        val root = sut.storageManager.getFileByEncryptedRemotePath("/")

        shortSleep()

        sut.runOnUiThread { fragment.listDirectory(root, false, false) }

        waitForIdleSync()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    /**
     * Use same values as {@link FileDetailSharingFragmentIT listSharesFileAllShareTypes }
     */
    fun showSharedFiles() {
        val sut = testActivityRule.launchActivity(null)
        val fragment = OCFileListFragment()

        val userShare = OCFile("/sharedToUser.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("Admin", "Server Admin", ShareType.USER))
        }
        sut.storageManager.saveFile(userShare)

        val groupShare = OCFile("/sharedToGroup.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("group", "Group", ShareType.GROUP))
        }
        sut.storageManager.saveFile(groupShare)

        val emailShare = OCFile("/sharedToEmail.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("admin@nextcloud.localhost", "admin@nextcloud.localhost", ShareType.EMAIL))
        }
        sut.storageManager.saveFile(emailShare)

        val publicLink = OCFile("/publicLink.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedViaLink = true
        }
        sut.storageManager.saveFile(publicLink)

        val federatedUserShare = OCFile("/sharedToFederatedUser.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(
                ShareeUser("admin@remote.nextcloud.com", "admin@remote.nextcloud.com (remote)", ShareType.FEDERATED)
            )
        }
        sut.storageManager.saveFile(federatedUserShare)

        val personalCircleShare = OCFile("/sharedToPersonalCircle.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("circle", "Circle (Personal circle)", ShareType.CIRCLE))
        }
        sut.storageManager.saveFile(personalCircleShare)

        // as we cannot distinguish circle types, we do not need them right now
//        val publicCircleShare = OCFile("/sharedToPublicCircle.jpg").apply {
//            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
//            isSharedWithSharee = true
//            sharees = listOf(ShareeUser("circle", "Circle (Public circle)", ShareType.CIRCLE))
//        }
//        sut.storageManager.saveFile(publicCircleShare)
//
//        val closedCircleShare = OCFile("/sharedToClosedCircle.jpg").apply {
//            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
//            isSharedWithSharee = true
//            sharees = listOf(ShareeUser("circle", "Circle (Closed circle)", ShareType.CIRCLE))
//        }
//        sut.storageManager.saveFile(closedCircleShare)
//
//        val secretCircleShare = OCFile("/sharedToSecretCircle.jpg").apply {
//            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
//            isSharedWithSharee = true
//            sharees = listOf(ShareeUser("circle", "Circle (Secret circle)", ShareType.CIRCLE))
//        }
//        sut.storageManager.saveFile(secretCircleShare)

        val userRoomShare = OCFile("/sharedToUserRoom.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("Conversation", "Admin", ShareType.ROOM))
        }
        sut.storageManager.saveFile(userRoomShare)

        val groupRoomShare = OCFile("/sharedToGroupRoom.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(ShareeUser("Conversation", "Meeting", ShareType.ROOM))
        }
        sut.storageManager.saveFile(groupRoomShare)

        val usersShare = OCFile("/sharedToUsers.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
            isSharedWithSharee = true
            sharees = listOf(
                ShareeUser("Admin", "Server Admin", ShareType.USER),
                ShareeUser("User", "User", ShareType.USER),
                ShareeUser("Christine", "Christine Scott", ShareType.USER)
            )
        }
        sut.storageManager.saveFile(usersShare)

        val noShare = OCFile("/notShared.jpg").apply {
            parentId = sut.storageManager.getFileByEncryptedRemotePath("/").fileId
        }
        sut.storageManager.saveFile(noShare)

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

    @After
    fun after() {
        fileDataStorageManager.deleteAllFiles()
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

        val imageFile = OCFile("/test/image.png", "00000001")
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

        val folder = OCFile("/test/", "00001")
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
