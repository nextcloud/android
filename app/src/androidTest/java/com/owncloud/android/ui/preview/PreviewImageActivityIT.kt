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
import com.nextcloud.test.FileRemovedIdlingResource
import com.nextcloud.test.LoopFailureHandler
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File

class PreviewImageActivityIT : AbstractOnServerIT() {
    companion object {
        private const val REMOTE_FOLDER: String = "/PreviewImageActivityIT/"
    }

    var fileRemovedIdlingResource = FileRemovedIdlingResource(storageManager)

    @Suppress("SameParameterValue")
    private fun createLocalMockedImageFiles(count: Int): List<OCFile> {
        val srcPngFile = getFile("imageFile.png")
        return (0 until count).map { i ->
            val pngFile = File(srcPngFile.parent ?: ".", "image$i.png")
            srcPngFile.copyTo(pngFile, overwrite = true)

            OCFile("/${pngFile.name}").apply {
                storagePath = pngFile.absolutePath
                mimeType = "image/png"
                modificationTimestamp = 1000000
                permissions = "D" // OCFile.PERMISSION_CAN_DELETE_OR_LEAVE_SHARE. Required for deletion button to show
            }.also {
                storageManager.saveNewFile(it)
            }
        }
    }

    /**
     * Create image files and upload them to the connected server.
     *
     * This function relies on the images not existing beforehand, as AbstractOnServerIT#deleteAllFilesOnServer()
     * should clean up. If it does fail, likely because that clean up didn't work and there are leftovers from
     * a previous run
     * @param count Number of files to create
     * @param folder Parent folder to which to upload. Must start and end with a slash
     */
    private fun createAndUploadImageFiles(count: Int, folder: String = REMOTE_FOLDER): List<OCFile> {
        val srcPngFile = getFile("imageFile.png")
        return (0 until count).map { i ->
            val pngFile = File(srcPngFile.parent ?: ".", "image$i.png")
            srcPngFile.copyTo(pngFile, overwrite = true)

            val ocUpload = OCUpload(
                pngFile.absolutePath,
                folder + pngFile.name,
                account.name
            ).apply {
                nameCollisionPolicy = NameCollisionPolicy.OVERWRITE
            }
            uploadOCUpload(ocUpload)

            fileDataStorageManager.getFileByDecryptedRemotePath(folder + pngFile.name)!!
        }
    }

    private fun veryImageThenDelete(testFile: OCFile) {
        Espresso.setFailureHandler(
            LoopFailureHandler(targetContext, "Test failed with image file ${testFile.fileName}")
        )

        assertTrue(testFile.exists())
        assertTrue(testFile.fileExists())

        onView(withId(R.id.image))
            .check(matches(isDisplayed()))

        // Check that the Action Bar shows the file name as title
        onView(
            allOf(
                isDescendantOfA(isAssignableFrom(ActionBarContainer::class.java)),
                withText(testFile.fileName)
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
        val expectedText = targetContext.getString(R.string.confirmation_remove_file_alert, testFile.fileName)
        onView(withId(android.R.id.message))
            .inRoot(isDialog())
            .check(matches(withText(expectedText)))

        onView(withId(android.R.id.button1))
            .inRoot(isDialog())
            .check(matches(withText(R.string.file_delete)))
            .perform(ViewActions.click())

        // Register the idling resource to wait for successful deletion
        fileRemovedIdlingResource.setFile(testFile)

        // Wait for idle, then verify that the file is gone. Somehow waitForIdleSync() doesn't work and we need onIdle()
        Espresso.onIdle()
        assertFalse("test file still exists: ${testFile.fileName}", testFile.exists())

        Espresso.setFailureHandler(DefaultFailureHandler(targetContext))
    }

    private fun executeDeletionTestScenario(
        localOnly: Boolean,
        offline: Boolean,
        fileListTransformation: (List<OCFile>) -> List<OCFile>
    ) {
        val imageCount = 5
        val testFiles = if (localOnly) {
            createLocalMockedImageFiles(
                imageCount
            )
        } else {
            createAndUploadImageFiles(imageCount)
        }
        val expectedFileOrder = fileListTransformation(testFiles)

        val intent = PreviewImageActivity.previewFileIntent(targetContext, user, expectedFileOrder.first())
        launchActivity<PreviewImageActivity>(intent).use { scenario ->
            if (offline) {
                scenario.onActivity { activity ->
                    activity.connectivityService = ConnectivityServiceOfflineMock()
                }
            }
            onView(isRoot()).check(matches(isDisplayed()))

            assertTrue("Do not merge - just testing test failures", false)

            for (testFile in expectedFileOrder) {
                veryImageThenDelete(testFile)
                assertTrue(
                    "Test file still exists on the server: ${testFile.remotePath}",
                    ExistenceCheckRemoteOperation(testFile.remotePath, true).execute(client).isSuccess
                )
            }
        }
    }

    private fun testDeleteFromSlideshow_impl(localOnly: Boolean, offline: Boolean) {
        // Case 1: start at first image
        executeDeletionTestScenario(localOnly, offline) { list -> list }
        // Case 2: start at last image (reversed)
        executeDeletionTestScenario(localOnly, offline) { list -> list.reversed() }
        // Case 3: Start in the middle. From middle to the end, then backwards through remaining files of the first half
        executeDeletionTestScenario(localOnly, offline) { list ->
            list.subList(list.size / 2, list.size) + list.subList(0, list.size / 2).reversed()
        }
    }

    @Before
    fun bringUp() {
        IdlingRegistry.getInstance().register(fileRemovedIdlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(fileRemovedIdlingResource)
    }

    @Test
    fun deleteFromSlideshow_localOnly_online() {
        testDeleteFromSlideshow_impl(localOnly = true, offline = false)
    }

    @Test
    fun deleteFromSlideshow_localOnly_offline() {
        testDeleteFromSlideshow_impl(localOnly = true, offline = true)
    }

    @Test
    fun deleteFromSlideshow_remote_online() {
        testDeleteFromSlideshow_impl(localOnly = false, offline = false)
    }

    @Test
    @Ignore(
        "Offline deletion is following a different UX and it is also brittle: Deletion might happen 10 minutes later"
    )
    fun deleteFromSlideshow_remote_offline() {
        // Note: the offline mock doesn't actually do what it is supposed to. The OfflineOperationsWorker uses its
        // own connectivityService, which is online, and may still execute the server deletion.
        // You'll need to address this, should you activate that test. Otherwise it might not catch all error cases
        testDeleteFromSlideshow_impl(localOnly = false, offline = true)
    }
}
