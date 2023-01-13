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
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.GrantStoragePermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class UploadFilesActivityIT : AbstractIT() {
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
        shortSleep()

        screenshot(sut)
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

    fun fileSelected() {
        val sut: UploadFilesActivity = activityRule.launchActivity(null)

        // TODO select one

        screenshot(sut)
    }
}
