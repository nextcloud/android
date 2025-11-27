/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment.Companion.newInstance
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SyncFileNotEnoughSpaceDialogFragmentTest : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragmentTest"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @ScreenshotTest
    fun showNotEnoughSpaceDialogForFolder() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val ocFile = OCFile("/Document/").apply {
                    fileLength = 5000000
                    setFolder()
                }

                onIdleSync {
                    EspressoIdlingResource.increment()
                    newInstance(ocFile, 1000).apply {
                        show(sut.supportFragmentManager, "1")
                    }
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showNotEnoughSpaceDialogForFolder", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showNotEnoughSpaceDialogForFile() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val ocFile = OCFile("/Video.mp4").apply {
                    fileLength = 1000000
                }

                onIdleSync {
                    EspressoIdlingResource.increment()
                    newInstance(ocFile, 2000).apply {
                        show(sut.supportFragmentManager, "2")
                    }
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showNotEnoughSpaceDialogForFile", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
