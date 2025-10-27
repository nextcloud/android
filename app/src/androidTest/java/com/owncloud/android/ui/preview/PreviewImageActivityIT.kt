/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Philipp Hasper <vcs@hasper.info>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview

import androidx.appcompat.widget.ActionBarContainer
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.base.DefaultFailureHandler
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.nextcloud.test.ConnectivityServiceOfflineMock
import com.nextcloud.test.LoopFailureHandler
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import org.hamcrest.Matchers.allOf
import org.junit.Test
import java.io.File

class PreviewImageActivityIT : AbstractOnServerIT() {
    lateinit var testFiles: List<OCFile>

    fun createMockedImageFiles(count: Int, localOnly: Boolean) {
        val srcPngFile = getFile("imageFile.png")
        testFiles = (0 until count).map { i ->
            val pngFile = File(srcPngFile.parent ?: ".", "image$i.png")
            srcPngFile.copyTo(pngFile, overwrite = true)

            OCFile("/${pngFile.name}").apply {
                storagePath = pngFile.absolutePath
                mimeType = "image/png"
                modificationTimestamp = 1000000
                permissions = "D" // OCFile.PERMISSION_CAN_DELETE_OR_LEAVE_SHARE. Required for deletion button to show
                remoteId = if (localOnly) null else "abc-mocked-remote-id" // mocking the file to be on the server
            }.also {
                storageManager.saveNewFile(it)
            }
        }
    }

    fun veryImageThenDelete(index: Int) {
        val currentFileName = testFiles[index].fileName
        Espresso.setFailureHandler(
            LoopFailureHandler(targetContext, "Test failed with image file index $index, $currentFileName")
        )

        onView(withId(R.id.image))
            .check(matches(isDisplayed()))

        // Check that the Action Bar shows the file name as title
        onView(
            allOf(
                isDescendantOfA(isAssignableFrom(ActionBarContainer::class.java)),
                withText(currentFileName)
            )
        ).check(matches(isDisplayed()))

        // Open the Action Bar's overflow menu.
        // The official way would be:
        //   openActionBarOverflowOrOptionsMenu(targetContext)
        // But this doesn't find the view. Presumably because Espresso.OVERFLOW_BUTTON_MATCHER looks for the description
        // "More options", whereas it actually says "More menu".
        // selecting by this would also work:
        //   onView(withContentDescription("More menu")).perform(ViewActions.click())
        // For now, we identify it by the ID we know it to be
        onView(withId(R.id.custom_menu_placeholder_item)).perform(ViewActions.click())

        // Click the "Remove" button
        onView(withText(R.string.common_remove)).perform(ViewActions.click())

        // Check confirmation dialog and then confirm the deletion by clicking the main button of the dialog
        val expectedText = targetContext.getString(R.string.confirmation_remove_file_alert, currentFileName)
        onView(withId(android.R.id.message))
            .inRoot(isDialog())
            .check(matches(withText(expectedText)))

        onView(withId(android.R.id.button1))
            .inRoot(isDialog())
            .check(matches(withText(R.string.file_delete)))
            .perform(ViewActions.click())

        Espresso.setFailureHandler(DefaultFailureHandler(targetContext))
    }

    @Test
    fun deleteFromSlideshow_localOnly_online() {
        // Prepare local test data
        val imageCount = 5
        createMockedImageFiles(imageCount, localOnly = true)

        // Launch the activity with the first image
        val intent = PreviewImageActivity.previewFileIntent(targetContext, user, testFiles[0])
        launchActivity<PreviewImageActivity>(intent).use {
            onView(isRoot()).check(matches(isDisplayed()))

            for (i in 0 until imageCount) {
                veryImageThenDelete(i)
            }
        }
    }

    @Test
    fun deleteFromSlideshow_localOnly_offline() {
        // Prepare local test data
        val imageCount = 5
        createMockedImageFiles(imageCount, localOnly = true)

        // Launch the activity with the first image
        val intent = PreviewImageActivity.previewFileIntent(targetContext, user, testFiles[0])
        launchActivity<PreviewImageActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.connectivityService = ConnectivityServiceOfflineMock()
            }
            onView(isRoot()).check(matches(isDisplayed()))

            for (i in 0 until imageCount) {
                veryImageThenDelete(i)
            }
        }
    }
}
