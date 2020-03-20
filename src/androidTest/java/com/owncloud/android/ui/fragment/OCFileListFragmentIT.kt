/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
import com.evernote.android.job.JobRequest
import com.facebook.testing.screenshot.Screenshot
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.client.preferences.DarkMode
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.resources.shares.CreateShareRemoteOperation
import com.owncloud.android.lib.resources.shares.GetShareesRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.FileStorageUtils
import junit.framework.TestCase
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OCFileListFragmentIT : AbstractIT() {
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

        override fun isOnlineWithWifi(): Boolean {
            return true
        }

        override fun getActiveNetworkType(): JobRequest.NetworkType {
            return JobRequest.NetworkType.ANY
        }
    }

    private val powerManagementServiceMock: PowerManagementService = object : PowerManagementService {
        override val isPowerSavingEnabled: Boolean
            get() = false

        override val isPowerSavingExclusionAvailable: Boolean
            get() = false

        override val isBatteryCharging: Boolean
            get() = false
    }

    @Test
    fun showRichWorkspace() {
        assertTrue(CreateFolderOperation("/test/", true).execute(client, storageManager).isSuccess)

        val ocUpload = OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
            "/test/Readme.md",
            account.name)
        val newUpload = UploadFileOperation(
            UploadsStorageManager(UserAccountManagerImpl.fromContext(targetContext), targetContext.contentResolver),
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false)

        newUpload.addRenameUploadListener {}
        newUpload.setRemoteFolderToBeCreated()

        assertTrue(newUpload.execute(client, storageManager).isSuccess)

        assertTrue(RefreshFolderOperation(storageManager.getFileByPath("/test/"),
            System.currentTimeMillis() / SECOND_IN_MILLIS,
            false,
            true,
            storageManager,
            account,
            targetContext).execute(client).isSuccess)

        val sut = ActivityScenario.launch(FileDisplayActivity::class.java)
        sut.onActivity { activity -> activity.onBrowsedDownTo(storageManager.getFileByPath("/test/")) }

        shortSleep()

        sut.onActivity { activity ->
            Screenshot.snapActivity(activity).setName("richworkspaces_light").record()
        }

        val preferences: AppPreferences = AppPreferencesImpl.fromContext(targetContext)
        preferences.darkThemeMode = DarkMode.DARK
        MainApp.setAppTheme(DarkMode.DARK)

        sut.onActivity { activity -> activity.onBackPressed() }

        sut.recreate()

        sut.onActivity { activity -> activity.onBrowsedDownTo(storageManager.getFileByPath("/test/")) }

        shortSleep()

        sut.onActivity { activity ->
            Screenshot.snapActivity(activity).setName("richworkspaces_dark").record()
        }

        // switch back to light mode
        preferences.darkThemeMode = DarkMode.LIGHT
        MainApp.setAppTheme(DarkMode.LIGHT)

        sut.onActivity { activity -> activity.onBackPressed() }

        sut.recreate()
    }

    @Test
    fun createAndShowShareToUser() {
        val path = "/shareToAdmin/"
        TestCase.assertTrue(CreateFolderOperation(path, true).execute(client, storageManager).isSuccess)

        // share folder to user "admin"
        TestCase.assertTrue(CreateShareRemoteOperation(path,
            ShareType.USER,
            "admin",
            false,
            "",
            OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER)
            .execute(client).isSuccess)

        val sut: FileDisplayActivity = activityRule.launchActivity(null)
        sut.startSyncFolderOperation(storageManager.getFileByPath("/"), true)

        shortSleep()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Screenshot.snapActivity(sut).record()
    }

    @Test
    fun createAndShowShareToGroup() {
        val path = "/shareToGroup/"
        TestCase.assertTrue(CreateFolderOperation(path, true).execute(client, storageManager).isSuccess)

        // share folder to group
        assertTrue(CreateShareRemoteOperation("/shareToGroup/",
            ShareType.GROUP,
            "users",
            false,
            "",
            OCShare.DEFAULT_PERMISSION)
            .execute(client).isSuccess)

        val sut: FileDisplayActivity = activityRule.launchActivity(null)
        sut.startSyncFolderOperation(storageManager.getFileByPath("/"), true)

        shortSleep()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Screenshot.snapActivity(sut).record()
    }

    @Test
    fun createAndShowShareToCircle() {
        val path = "/shareToCircle/"
        TestCase.assertTrue(CreateFolderOperation(path, true).execute(client, storageManager).isSuccess)

        // share folder to circle
        // get circleId
        val searchResult = GetShareesRemoteOperation("publicCircle", 1, RESULT_PER_PAGE).execute(client)
        assertTrue(searchResult.logMessage, searchResult.isSuccess)

        val resultJson: JSONObject = searchResult.data[0] as JSONObject
        val circleId: String = resultJson.getJSONObject("value").getString("shareWith")

        assertTrue(CreateShareRemoteOperation("/shareToCircle/",
            ShareType.CIRCLE,
            circleId,
            false,
            "",
            OCShare.DEFAULT_PERMISSION)
            .execute(client).isSuccess)

        val sut: FileDisplayActivity = activityRule.launchActivity(null)
        sut.startSyncFolderOperation(storageManager.getFileByPath("/"), true)

        shortSleep()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Screenshot.snapActivity(sut).record()
    }

    @Test
    fun createAndShowShareViaLink() {
        val path = "/shareViaLink/"
        TestCase.assertTrue(CreateFolderOperation(path, true).execute(client, storageManager).isSuccess)

        // share folder via public link
        TestCase.assertTrue(CreateShareRemoteOperation("/shareViaLink/",
            ShareType.PUBLIC_LINK,
            "",
            true,
            "",
            OCShare.READ_PERMISSION_FLAG)
            .execute(client).isSuccess)

        val sut: FileDisplayActivity = activityRule.launchActivity(null)
        sut.startSyncFolderOperation(storageManager.getFileByPath("/"), true)

        shortSleep()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Screenshot.snapActivity(sut).record()
    }
}
