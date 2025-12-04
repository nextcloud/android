/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview

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
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PreviewTextFileFragmentTest : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.preview.PreviewTextFileFragmentTest"

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Test
    @ScreenshotTest
    @Throws(IOException::class)
    fun displaySimpleTextFile() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            var sut: FileDisplayActivity? = null
            scenario.onActivity { activity ->
                sut = activity
                val test = OCFile("/text.md").apply {
                    mimeType = MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN
                    storagePath = getDummyFile("nonEmpty.txt").absolutePath
                }
                sut.startTextPreview(test, true)
            }

            val screenShotName = createName(testClassName + "_" + "displaySimpleTextFile", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    @Throws(IOException::class)
    fun displayJavaSnippetFile() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            var sut: FileDisplayActivity? = null
            scenario.onActivity { activity ->
                sut = activity
                val test = OCFile("/java.md").apply {
                    mimeType = MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN
                    storagePath = getFile("java.md").absolutePath
                }
                sut.startTextPreview(test, true)
            }

            val screenShotName = createName(testClassName + "_" + "displayJavaSnippetFile", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }
}
