/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
    private var scenario: ActivityScenario<UploadFilesActivity>? = null
    val intent = Intent(ApplicationProvider.getApplicationContext(), UploadFilesActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<UploadFilesActivity>(intent)

    @After
    fun cleanup() {
        scenario?.close()
    }

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
        scenario = activityRule.scenario
        scenario?.onActivity { sut ->
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

            onIdleSync {
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun localFolderPickerMode() {
        val intent = Intent(targetContext, UploadFilesActivity::class.java).apply {
            putExtra(
                UploadFilesActivity.KEY_LOCAL_FOLDER_PICKER_MODE,
                true
            )
            putExtra(
                UploadFilesActivity.REQUEST_CODE_KEY,
                FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM
            )
        }

        val scenario = ActivityScenario.launch<UploadFilesActivity>(intent)
        scenario.onActivity { sut ->
            sut.runOnUiThread {
                sut.fileListFragment.setFiles(
                    directories
                )
            }

            onIdleSync {
                screenshot(sut)
            }
        }
    }
}
