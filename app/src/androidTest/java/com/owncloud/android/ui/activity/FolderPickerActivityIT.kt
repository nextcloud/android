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
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderPickerActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.FolderPickerActivityIT"

    @Test
    fun getActivityFile() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            val origin = OCFile("/test/file.test").apply {
                remotePath = "/remotePath/test"
            }

            var target: OCFile? = null

            scenario.onActivity { sut ->
                sut.file = origin
                target = sut.file
            }

            assertEquals(origin, target)
        }
    }

    @Test
    fun getParentFolder_isNotRootFolder() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            val origin = OCFile("/test/").apply {
                fileId = 1
                remotePath = "/test/"
                setStoragePath("/test/")
                setFolder()
            }

            var target: OCFile? = null

            scenario.onActivity { sut ->
                sut.file = origin
                target = sut.currentFolder
            }

            assertEquals(origin, target)
        }
    }

    @Test
    fun getParentFolder_isRootFolder() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            val origin = OCFile("/").apply {
                fileId = 1
                remotePath = "/"
                setStoragePath("/")
                setFolder()
            }

            var target: OCFile? = null
            scenario.onActivity { sut ->
                sut.file = origin
                target = sut.currentFolder
            }

            assertEquals(origin, target)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun nullFile() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            var rootFolder: OCFile? = null
            var target: OCFile? = null

            scenario.onActivity { sut ->
                rootFolder = sut.storageManager.getFileByPath(OCFile.ROOT_PATH)
                sut.file = null
                target = sut.currentFolder
            }

            assertEquals(rootFolder, target)
        }
    }

    @Test
    fun getParentFolder() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            val origin = OCFile("/test/file.test").apply {
                remotePath = "/test/file.test"
            }

            val target = OCFile("/test/")

            scenario.onActivity { sut ->
                sut.file = origin
            }

            assertEquals(origin, target)
        }
    }

    @Test
    @ScreenshotTest
    fun open() {
        launchActivity<FolderPickerActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val origin = OCFile("/test/file.txt")
                sut.file = origin
                sut.findViewById<View>(R.id.folder_picker_btn_copy).requestFocus()
            }

            val screenShotName = createName(testClassName + "_" + "open", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun testMoveOrCopy() {
        val intent = Intent(targetContext, FolderPickerActivity::class.java)
        launchActivity<FolderPickerActivity>(intent).use { scenario ->
            val screenShotName = createName(testClassName + "_" + "testMoveOrCopy", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun testChooseLocationAction() {
        val intent = Intent(targetContext, FolderPickerActivity::class.java).apply {
            putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
        }

        launchActivity<FolderPickerActivity>(intent).use { scenario ->
            val screenShotName = createName(testClassName + "_" + "testChooseLocationAction", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }
}
