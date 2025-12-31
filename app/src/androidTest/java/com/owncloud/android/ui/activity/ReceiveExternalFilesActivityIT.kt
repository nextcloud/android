/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.facebook.testing.screenshot.internal.TestNameDetector
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.test.GrantStoragePermissionRule
import com.nextcloud.test.withSelectedText
import com.nextcloud.utils.extensions.removeFileExtension
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.io.File

class ReceiveExternalFilesActivityIT : AbstractIT() {

    @get:Rule
    var storagePermissionRule: TestRule = GrantStoragePermissionRule.grant()

    @Test
    @ScreenshotTest
    fun open() {
        // Screenshot name must be constructed outside of the scenario, otherwise it will not be reliably detected
        val screenShotName = TestNameDetector.getTestClass() + "_" + TestNameDetector.getTestName()
        launchActivity<ReceiveExternalFilesActivity>().use { scenario ->
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun openMultiAccount() {
        val secondAccount = createAccount("secondtest@https://nextcloud.localhost")
        open()
        removeAccount(secondAccount)
    }


    fun createSendIntent(file: File): Intent = Intent(targetContext, ReceiveExternalFilesActivity::class.java).apply {
        action = Intent.ACTION_SEND
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
    }

    @Test
    fun renameSingleFileUpload() {
        val imageFile = getDummyFile("image.jpg")
        val intent = createSendIntent(imageFile)

        // Create folders with the necessary permissions and another test file
        val mainFolder = OCFile("/folder/").apply {
            permissions = OCFile.PERMISSION_CAN_CREATE_FILE_AND_FOLDER
            setFolder()
            fileDataStorageManager.saveNewFile(this)
        }
        val subFolder = OCFile("${mainFolder.remotePath}sub folder/").apply {
            permissions = OCFile.PERMISSION_CAN_CREATE_FILE_AND_FOLDER
            setFolder()
            fileDataStorageManager.saveNewFile(this)
        }
        val otherFile = OCFile("${mainFolder.remotePath}Other Image File.jpg").apply {
            fileDataStorageManager.saveNewFile(this)
        }

        // Store the folder in preferences, so the activity starts from there.
        @Suppress("DEPRECATION")
        val preferences = AppPreferencesImpl.fromContext(targetContext)
        preferences.setLastUploadPath(mainFolder.remotePath)

        launchActivity<ReceiveExternalFilesActivity>(intent).use {
            val expectedMainFolderTitle = (getCurrentActivity() as ToolbarActivity).getActionBarTitle(mainFolder, false)
            // Verify that the test starts in the expected folder. If this fails, change the setup calls above
            onView(withId(R.id.toolbar))
                .check(matches(hasDescendant(withText(expectedMainFolderTitle))))

            onView(withText(R.string.uploader_btn_upload_text))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))

            // Test the pre-selection behavior (filename, but without extension, shall be selected)
            onView(withId(R.id.user_input))
                .check(matches(withText(imageFile.name)))
                .perform(ViewActions.click())
                .check(matches(withSelectedText(imageFile.name.removeFileExtension())))

            // Set a new file name
            val secondFileName = "New filename.jpg"
            onView(withId(R.id.user_input))
                .perform(ViewActions.typeTextIntoFocusedView(secondFileName.removeFileExtension()))
                .check(matches(withText(secondFileName)))
                // Leave the field and come back to verify the pre-selection behavior correctly handles the new name
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_TAB))
                .perform(ViewActions.click())
                .check(matches(withSelectedText(secondFileName.removeFileExtension())))
            onView(withText(R.string.uploader_btn_upload_text))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))

            // Set a file name without file extension
            val thirdFileName = "No extension"
            onView(withId(R.id.user_input))
                .perform(ViewActions.clearText())
                .perform(ViewActions.typeTextIntoFocusedView(thirdFileName))
                .check(matches(withText(thirdFileName)))
                // Leave the field and come back to verify the pre-selection behavior correctly handles the new name
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_TAB))
                .perform(ViewActions.click())
                .check(matches(withSelectedText(thirdFileName)))
            onView(withText(R.string.uploader_btn_upload_text))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))

            // Test an invalid filename. Note: as the user is null, the capabilities are also null, so the name checker
            // will not reject any special characters like '/'. So we only test empty and an existing file name
            onView(withId(R.id.user_input))
                .perform(ViewActions.clearText())
                .check(matches(withText("")))
            onView(withText(R.string.uploader_btn_upload_text))
                .check(matches(isDisplayed()))
                .check(matches(not(isEnabled())))
            onView(withId(R.id.user_input))
                .perform(ViewActions.click())
                .perform(ViewActions.typeTextIntoFocusedView(otherFile.fileName))
                .check(matches(withText(otherFile.fileName)))
            onView(withText(R.string.uploader_btn_upload_text))
                .check(matches(isDisplayed()))
                .check(matches(not(isEnabled())))

            val fourthFileName = "New file name.jpg"
            onView(withId(R.id.user_input))
                .perform(ViewActions.click())
                .perform(ViewActions.clearText())
                .perform(ViewActions.typeTextIntoFocusedView(fourthFileName))
                .check(matches(withText(fourthFileName)))
            onView(withText(R.string.uploader_btn_upload_text))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
        }
    }
}
