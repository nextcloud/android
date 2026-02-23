/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.client.files.FileIndicator
import com.nextcloud.client.files.FileIndicatorManager
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class FileIndicatorManagerIT : AbstractIT() {

    private val testClassName = "com.nextcloud.client.files.FileIndicatorManagerIT"
    private val fakeRemotePath = "/upload_indicator_test_file.jpg"
    private var actualFileId = -1L

    @Before
    fun setup() {
        actualFileId = -1L
    }

    @After
    fun tearDown() {
        if (actualFileId != -1L) {
            FileIndicatorManager.update(actualFileId, FileIndicator.Idle)
        }
    }

    @Test
    @ScreenshotTest
    fun showIdleIndicator() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut -> insertFileIntoDB(sut) }
            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Idle) }
            waitForIndicator()
            screenshot(scenario, "showIdleIndicator")
        }
    }

    @Test
    @ScreenshotTest
    fun showSyncingIndicator() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut -> insertFileIntoDB(sut) }
            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Syncing) }
            waitForIndicator()
            screenshot(scenario, "showSyncingIndicator")
        }
    }

    @Test
    @ScreenshotTest
    fun showErrorIndicator() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut -> insertFileIntoDB(sut) }
            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Syncing) }
            waitForIndicator()
            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Error) }
            waitForIndicator()
            screenshot(scenario, "showErrorIndicator")
        }
    }

    @Test
    @ScreenshotTest
    fun showSyncedIndicator() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut -> insertFileIntoDB(sut) }
            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Syncing) }
            waitForIndicator()
            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Synced) }
            waitForIndicator()
            screenshot(scenario, "showSyncedIndicator")
        }
    }

    @Test
    @ScreenshotTest
    fun showFullUploadLifecycle() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut -> insertFileIntoDB(sut) }

            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Idle) }
            waitForIndicator()
            screenshot(scenario, "fullLifecycle_1_idle")

            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Syncing) }
            waitForIndicator()
            screenshot(scenario, "fullLifecycle_2_syncing")

            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Synced) }
            waitForIndicator()
            screenshot(scenario, "fullLifecycle_3_synced")

            scenario.onActivity { FileIndicatorManager.update(actualFileId, FileIndicator.Idle) }
            waitForIndicator()
            screenshot(scenario, "fullLifecycle_4_backToIdle")
        }
    }

    @Suppress("DEPRECATION")
    private fun insertFileIntoDB(sut: FileDisplayActivity) {
        val storageManager = sut.storageManager ?: return
        val rootDir = storageManager.getFileByPath(OCFile.ROOT_PATH) ?: return

        val fakeFile = OCFile(fakeRemotePath).apply {
            parentId = rootDir.fileId
            remoteId = Random.nextLong(10_000_000L, 99_999_999L)
                .toString()
                .padEnd(32, '0')
            mimeType = "image/jpeg"
            fileLength = 512 * 1024
            modificationTimestamp = System.currentTimeMillis()
        }

        storageManager.saveFile(fakeFile)

        actualFileId = storageManager.getFileByDecryptedRemotePath(fakeRemotePath)?.fileId ?: return

        sut.listOfFilesFragment?.listDirectory(rootDir, MainApp.isOnlyOnDevice())
    }

    private fun waitForIndicator() {
        onView(isRoot()).check(matches(isDisplayed()))
        Thread.sleep(250)
        onView(isRoot()).check(matches(isDisplayed()))
    }

    private fun screenshot(scenario: ActivityScenario<FileDisplayActivity>, suffix: String) {
        val name = createName("${testClassName}_$suffix", "")
        onView(isRoot()).check(matches(isDisplayed()))
        scenario.onActivity { sut -> screenshotViaName(sut, name) }
    }
}
