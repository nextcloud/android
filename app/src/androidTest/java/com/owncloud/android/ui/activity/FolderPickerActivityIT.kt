/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Kilian Périsset <kilian.perisset@infomaniak.com>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import android.view.View
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderPickerActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.FolderPickerActivityIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @UiThread
    fun getActivityFile() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val origin = OCFile("/test/file.test").apply {
                        remotePath = "/remotePath/test"
                    }

                    // Act
                    sut.file = origin
                    val target = sut.file

                    // Assert
                    assertEquals(origin, target)
                }
            }
        }
    }

    @Test
    @UiThread
    fun getParentFolder_isNotRootFolder() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    // Arrange
                    val origin = OCFile("/test/").apply {
                        fileId = 1
                        remotePath = "/test/"
                        setStoragePath("/test/")
                        setFolder()
                    }

                    // Act
                    sut.file = origin
                    val target = sut.currentFolder

                    // Assert
                    assertEquals(origin, target)
                }
            }
        }
    }

    @Test
    @UiThread
    fun getParentFolder_isRootFolder() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    // Arrange
                    val origin = OCFile("/").apply {
                        fileId = 1
                        remotePath = "/"
                        setStoragePath("/")
                        setFolder()
                    }

                    // Act
                    sut.file = origin
                    val target = sut.currentFolder

                    // Assert
                    assertEquals(origin, target)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    @UiThread
    fun nullFile() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    // Arrange
                    val rootFolder = sut.storageManager.getFileByPath(OCFile.ROOT_PATH)

                    // Act
                    sut.file = null
                    val target = sut.currentFolder

                    // Assert
                    assertEquals(rootFolder, target)
                }
            }
        }
    }

    @Test
    @UiThread
    fun getParentFolder() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    // Arrange
                    val origin = OCFile("/test/file.test").apply {
                        remotePath = "/test/file.test"
                    }

                    val target = OCFile("/test/")

                    // Act
                    sut.file = origin

                    // Assert
                    assertEquals(origin, target)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun open() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val origin = OCFile("/test/file.txt")
                    sut.file = origin
                    sut.findViewById<View>(R.id.folder_picker_btn_copy).requestFocus()
                    val screenShotName = createName(testClassName + "_" + "open", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testMoveOrCopy() {
        val intent = Intent(targetContext, FolderPickerActivity::class.java)
        launchActivity<FolderPickerActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val screenShotName = createName(testClassName + "_" + "testMoveOrCopy", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testChooseLocationAction() {
        val intent = Intent(targetContext, FolderPickerActivity::class.java).apply {
            putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
        }

        launchActivity<FolderPickerActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val screenShotName = createName(testClassName + "_" + "testChooseLocationAction", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
