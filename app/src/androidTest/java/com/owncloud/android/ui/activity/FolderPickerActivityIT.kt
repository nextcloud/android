package com.owncloud.android.ui.activity

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/*
 * Nextcloud Android client application
 *
 * @author Kilian Périsset
 * Copyright (C) 2019 Kilian Périsset (Infomaniak Network SA)
 * Copyright (C) 2019 Nextcloud GmbH
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
 */@RunWith(AndroidJUnit4::class) //@LargeTest

class FolderPickerActivityIT : AbstractIT() {
    @Rule
    var activityRule = ActivityTestRule(
        FolderPickerActivity::class.java
    )

    @get:Test
    val activityFile: Unit
        get() {
            // Arrange
            val targetActivity = activityRule.activity
            val origin = OCFile("/test/file.test")
            origin.remotePath = "/remotePath/test"

            // Act
            targetActivity.file = origin
            val target = targetActivity.file

            // Assert
            Assert.assertEquals(origin, target)
        }

    @get:Test
    val parentFolder_isNotRootFolder: Unit
        get() {
            // Arrange
            val targetActivity = activityRule.activity
            val origin = OCFile("/test/")
            origin.fileId = 1
            origin.remotePath = "/test/"
            origin.storagePath = "/test/"
            origin.setFolder()

            // Act
            targetActivity.file = origin
            val target = targetActivity.currentFolder

            // Assert
            Assert.assertEquals(origin, target)
        }

    @get:Test
    val parentFolder_isRootFolder: Unit
        get() {
            // Arrange
            val targetActivity = activityRule.activity
            val origin = OCFile("/")
            origin.fileId = 1
            origin.remotePath = "/"
            origin.storagePath = "/"
            origin.setFolder()

            // Act
            targetActivity.file = origin
            val target = targetActivity.currentFolder

            // Assert
            Assert.assertEquals(origin, target)
        }

    @Test
    fun nullFile() {
        // Arrange
        val targetActivity = activityRule.activity
        val rootFolder = targetActivity.storageManager.getFileByPath(OCFile.ROOT_PATH)

        // Act
        targetActivity.file = null
        val target = targetActivity.currentFolder

        // Assert
        Assert.assertEquals(rootFolder, target)
    }

    @get:Test
    val parentFolder: Unit
        get() {
            // Arrange
            val targetActivity = activityRule.activity
            val origin = OCFile("/test/file.test")
            origin.remotePath = "/test/file.test"
            val target = OCFile("/test/")

            // Act
            targetActivity.file = origin

            // Assert
            Assert.assertEquals(origin, target)
        }

    @Test
    @ScreenshotTest
    fun open() {
        val sut = activityRule.activity
        val origin = OCFile("/test/file.txt")
        sut.file = origin
        sut.runOnUiThread { sut.findViewById<View>(R.id.folder_picker_btn_choose).requestFocus() }
        waitForIdleSync()
        screenshot(sut)
    }
}