/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.annotation.UiThread
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.GrantStoragePermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class UploadFilesActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.UploadFilesActivityIT"

    @get:Rule
    var activityRule = IntentsTestRule(UploadFilesActivity::class.java, true, false)

    @get:Rule
    var permissionRule = GrantStoragePermissionRule.grant()

    private val directories = listOf("A", "B", "C", "D")
        .map { File("${FileStorageUtils.getTemporalPath(account.name)}${File.separator}$it") }

    @Before
    fun setUp() {
        directories.forEach { it.mkdirs() }
    }

    @After
    fun tearDown() {
        directories.forEach { it.deleteRecursively() }
    }

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
    fun noneSelected() {
        val sut: UploadFilesActivity = activityRule.launchActivity(null)

        sut.runOnUiThread {
            sut.fileListFragment.setFiles(
                directories +
                    listOf(
                        File("1.txt"),
                        File("2.pdf"),
                        File("3.mp3")
                    )
            )
        }

        waitForIdleSync()
        longSleep()

        screenshot(sut.fileListFragment.binding?.listRoot)
    }

    @Test
    @ScreenshotTest
    fun localFolderPickerMode() {
        val sut: UploadFilesActivity = activityRule.launchActivity(
            Intent().apply {
                putExtra(
                    UploadFilesActivity.KEY_LOCAL_FOLDER_PICKER_MODE,
                    true
                )
                putExtra(
                    UploadFilesActivity.REQUEST_CODE_KEY,
                    FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM
                )
            }
        )

        sut.runOnUiThread {
            sut.fileListFragment.setFiles(
                directories
            )
        }

        waitForIdleSync()

        screenshot(sut)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun search() {
        val sut: UploadFilesActivity = activityRule.launchActivity(null)

        sut.runOnUiThread {
            sut.fileListFragment.setFiles(
                directories +
                    listOf(
                        File("1.txt"),
                        File("2.pdf"),
                        File("3.mp3")
                    )
            )

            onIdleSync {
                EspressoIdlingResource.increment()
                sut.fileListFragment.performSearch("1.txt", arrayListOf(), false)
                EspressoIdlingResource.decrement()
                val screenShotName = createName(testClassName + "_" + "search", "")
                onView(isRoot()).check(matches(isDisplayed()))
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun selectAll() {
        val sut: UploadFilesActivity = activityRule.launchActivity(null)

        sut.runOnUiThread {
            sut.fileListFragment.setFiles(
                listOf(
                    File("1.txt"),
                    File("2.pdf"),
                    File("3.mp3")
                )
            )

            sut.fileListFragment.selectAllFiles(true)
        }

        onIdleSync {
            val screenShotName = createName(testClassName + "_" + "selectAll", "")
            screenshotViaName(sut.fileListFragment.binding?.listRoot, screenShotName)
        }
    }
}
