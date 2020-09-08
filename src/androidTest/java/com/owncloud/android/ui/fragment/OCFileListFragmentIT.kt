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
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.client.preferences.DarkMode
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import junit.framework.TestCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class OCFileListFragmentIT : AbstractOnServerIT() {
    companion object {
        val SECOND_IN_MILLIS = 1000L
        val RESULT_PER_PAGE = 50
    }

    @get:Rule
    val activityRule = IntentsTestRule(FileDisplayActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
        override fun isInternetWalled(): Boolean {
            return false
        }

        override fun getConnectivity(): Connectivity {
            return Connectivity.CONNECTED_WIFI
        }
    }

    private val powerManagementServiceMock: PowerManagementService = object : PowerManagementService {
        override val isPowerSavingEnabled: Boolean
            get() = false

        override val isPowerSavingExclusionAvailable: Boolean
            get() = false

        override val battery: BatteryStatus
            get() = BatteryStatus()
    }

    @Test
    // @ScreenshotTest // todo run without real server
    fun showRichWorkspace() {
        assertTrue(CreateFolderOperation("/test/", user, targetContext).execute(client, storageManager).isSuccess)

        val ocUpload = OCUpload(
            getDummyFile("/nonEmpty.txt").absolutePath,
            "/test/Readme.md",
            account.name
        )
        val newUpload = UploadFileOperation(
            UploadsStorageManager(UserAccountManagerImpl.fromContext(targetContext), targetContext.contentResolver),
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false
        )

        newUpload.addRenameUploadListener {}
        newUpload.setRemoteFolderToBeCreated()

        assertTrue(newUpload.execute(client, storageManager).isSuccess)

        assertTrue(
            RefreshFolderOperation(
                storageManager.getFileByPath("/test/"),
                System.currentTimeMillis() / SECOND_IN_MILLIS,
                false,
                true,
                storageManager,
                account,
                targetContext
            )
                .execute(client)
                .isSuccess
        )

        val sut = ActivityScenario.launch(FileDisplayActivity::class.java)
        shortSleep()
        sut.onActivity { activity -> activity.onBrowsedDownTo(storageManager.getFileByPath("/test/")) }

        shortSleep()
        shortSleep()

//        sut.onActivity { activity ->
//            Screenshot.snapActivity(activity).setName("richworkspaces_light").record()
//        }

        val preferences: AppPreferences = AppPreferencesImpl.fromContext(targetContext)
        preferences.darkThemeMode = DarkMode.DARK

        sut.onActivity { activity ->
            MainApp.setAppTheme(DarkMode.DARK)
        }

        shortSleep()
        sut.onActivity { activity -> activity.onBackPressed() }

        shortSleep()

        sut.recreate()

        sut.onActivity { activity -> activity.onBrowsedDownTo(storageManager.getFileByPath("/test/")) }

        shortSleep()
        shortSleep()

//        sut.onActivity { activity ->
//            Screenshot.snapActivity(activity).setName("richworkspaces_dark").record()
//        }

        // switch back to light mode
        preferences.darkThemeMode = DarkMode.LIGHT
        sut.onActivity { MainApp.setAppTheme(DarkMode.LIGHT) }

        shortSleep()
        sut.onActivity { activity -> activity.onBackPressed() }

        sut.recreate()
    }

    @Test
    // @ScreenshotTest // todo run without real server
    fun createAndShowShareToUser() {
        val path = "/shareToAdmin/"
        TestCase.assertTrue(
            CreateFolderOperation(path, user, targetContext)
                .execute(client, storageManager)
                .isSuccess
        )

        // share folder to user "admin"
        TestCase.assertTrue(
            CreateShareRemoteOperation(
                path,
                ShareType.USER,
                "admin",
                false,
                "",
                OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
            )
                .execute(client).isSuccess
        )

        val sut: FileDisplayActivity = activityRule.launchActivity(null)
        sut.startSyncFolderOperation(storageManager.getFileByPath("/"), true)

        shortSleep()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
//        Screenshot.snapActivity(sut).record()
    }

    @Test
    // @ScreenshotTest // todo run without real server
    fun createAndShowShareToGroup() {
        val path = "/shareToGroup/"
        TestCase.assertTrue(
            CreateFolderOperation(path, user, targetContext)
                .execute(client, storageManager)
                .isSuccess
        )

        // share folder to group
        assertTrue(
            CreateShareRemoteOperation(
                "/shareToGroup/",
                ShareType.GROUP,
                "users",
                false,
                "",
                OCShare.NO_PERMISSION
            )
                .execute(client)
                .isSuccess
        )

        val sut: FileDisplayActivity = activityRule.launchActivity(null)
        sut.startSyncFolderOperation(storageManager.getFileByPath("/"), true)

        shortSleep()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
//        Screenshot.snapActivity(sut).record()
    }

//    @Test
//    @ScreenshotTest
//    fun createAndShowShareToCircle() {
//        val path = "/shareToCircle/"
//        TestCase.assertTrue(CreateFolderOperation(path, account, targetContext)
//            .execute(client, storageManager)
//            .isSuccess)
//
//        // share folder to circle
//        // get circleId
//        val searchResult = GetShareesRemoteOperation("publicCircle", 1, RESULT_PER_PAGE).execute(client)
//        assertTrue(searchResult.logMessage, searchResult.isSuccess)
//
//        val resultJson: JSONObject = searchResult.data[0] as JSONObject
//        val circleId: String = resultJson.getJSONObject("value").getString("shareWith")
//
//        assertTrue(CreateShareRemoteOperation("/shareToCircle/",
//            ShareType.CIRCLE,
//            circleId,
//            false,
//            "",
//            OCShare.DEFAULT_PERMISSION)
//            .execute(client).isSuccess)
//
//        val sut: FileDisplayActivity = activityRule.launchActivity(null)
//        sut.startSyncFolderOperation(storageManager.getFileByPath("/"), true)
//
//        shortSleep()
//        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
//        Screenshot.snapActivity(sut).record()
//    }

    @Test
    // @ScreenshotTest // todo run without real server
    fun createAndShowShareViaLink() {
        val path = "/shareViaLink/"
        TestCase.assertTrue(
            CreateFolderOperation(path, user, targetContext)
                .execute(client, storageManager)
                .isSuccess
        )

        // share folder via public link
        TestCase.assertTrue(
            CreateShareRemoteOperation(
                "/shareViaLink/",
                ShareType.PUBLIC_LINK,
                "",
                true,
                "",
                OCShare.READ_PERMISSION_FLAG
            )
                .execute(client)
                .isSuccess
        )

        val sut: FileDisplayActivity = activityRule.launchActivity(null)
        sut.startSyncFolderOperation(storageManager.getFileByPath("/"), true)

        shortSleep()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
//        Screenshot.snapActivity(sut).record()
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithoutLocalFile() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test.txt")
        val file = File("/sdcard/test.txt")
        file.createNewFile()

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithLocalFile() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test.txt")
        val file = File("/sdcard/test.txt")
        file.writeText("123123")

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithoutLocalFolder() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test/")
        val file = File("/sdcard/test/")
        File("/sdcard/test/1.txt").writeText("123123")

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithLocalFolder() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test/")
        val folder = File("/sdcard/test/")
        folder.mkdirs()
        val file = File("/sdcard/test/1.txt")
        file.createNewFile()
        file.writeText("123123")

        ocFile.storagePath = folder.absolutePath
        ocFile.mimeType = "DIR"

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 44
        assertTrue(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithNoLocalFolder() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test/")

        ocFile.mimeType = "DIR"

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))
    }
}
