/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.Manifest
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment.Companion.newInstance
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class SyncFileNotEnoughSpaceDialogFragmentTest : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragmentTest"

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Test
    @ScreenshotTest
    fun showNotEnoughSpaceDialogForFolder() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            var sut: FileDisplayActivity? = null
            scenario.onActivity { activity ->
                val ocFile = OCFile("/Document/").apply {
                    fileLength = 5000000
                    setFolder()
                }

                newInstance(ocFile, 1000).apply {
                    show(activity.supportFragmentManager, "1")
                }

                sut = activity
            }

            val screenShotName = createName(testClassName + "_" + "showNotEnoughSpaceDialogForFolder", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun showNotEnoughSpaceDialogForFile() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            var sut: FileDisplayActivity? = null
            scenario.onActivity { activity ->
                val ocFile = OCFile("/Video.mp4").apply {
                    fileLength = 1000000
                }

                newInstance(ocFile, 2000).apply {
                    show(activity.supportFragmentManager, "2")
                }
                sut = activity
            }

            val screenShotName = createName(testClassName + "_" + "showNotEnoughSpaceDialogForFile", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }
}
