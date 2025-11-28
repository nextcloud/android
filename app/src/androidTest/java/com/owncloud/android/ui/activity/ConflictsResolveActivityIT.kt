/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.fragment.app.DialogFragment
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.utils.extensions.getDecryptedPath
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.ui.dialog.ConflictsResolveDialog
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Companion.newInstance
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Decision
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.OnConflictDecisionMadeListener
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.ScreenshotTest
import junit.framework.TestCase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictsResolveActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.ConflictsResolveActivityIT"
    private var returnCode = false

    @Test
    @ScreenshotTest
    fun screenshotTextFiles() {
        val newFile = OCFile("/newFile.txt").apply {
            remoteId = "0001"
            fileLength = 56000
            modificationTimestamp = 1522019340
            setStoragePath(FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt")
        }

        val existingFile = OCFile("/newFile.txt").apply {
            remoteId = "0002"
            fileLength = 1024000
            modificationTimestamp = 1582019340
        }

        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)

        val intent = Intent(targetContext, ConflictsResolveActivity::class.java).apply {
            putExtra(FileActivity.EXTRA_FILE, newFile)
            putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        }

        launchActivity<ConflictsResolveActivity>(intent).use { scenario ->
            var dialog: ConflictsResolveDialog? = null
            scenario.onActivity { sut ->
                dialog = newInstance(
                    storageManager.getDecryptedPath(existingFile),
                    targetContext,
                    newFile,
                    existingFile,
                    UserAccountManagerImpl
                        .fromContext(targetContext)
                        .getUser()
                )
                dialog.showDialog(sut)
            }

            onView(withId(R.id.headline))
                .check(matches(isDisplayed()))

            val screenShotName = createName(testClassName + "_" + "screenshotTextFiles", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(dialog!!.requireDialog().window?.decorView, screenShotName)
            }
        }
    }

    @Test
    fun cancel() {
        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )

        val existingFile = OCFile("/newFile.txt").apply {
            fileLength = 1024000
            modificationTimestamp = 1582019340
        }

        val newFile = OCFile("/newFile.txt").apply {
            fileLength = 56000
            modificationTimestamp = 1522019340
            setStoragePath(FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt")
        }

        EspressoIdlingResource.increment()
        FileDataStorageManager(user, targetContext.contentResolver).run {
            saveNewFile(existingFile)
        }
        EspressoIdlingResource.decrement()

        val intent = Intent(targetContext, ConflictsResolveActivity::class.java).apply {
            putExtra(FileActivity.EXTRA_FILE, newFile)
            putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
            putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        }

        launchActivity<ConflictsResolveActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                returnCode = false
                sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
                    assertEquals(decision, Decision.CANCEL)
                    returnCode = true
                }
            }

            onView(ViewMatchers.withText("Cancel")).perform(ViewActions.click())
            TestCase.assertTrue(returnCode)
        }
    }

    @Test
    @ScreenshotTest
    fun keepExisting() {
        returnCode = false

        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )

        val existingFile = OCFile("/newFile.txt").apply {
            remoteId = "0001"
            fileLength = 1024000
            modificationTimestamp = 1582019340
        }

        val newFile = OCFile("/newFile.txt").apply {
            fileLength = 56000
            remoteId = "0002"
            modificationTimestamp = 1522019340
            setStoragePath(FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt")
        }

        EspressoIdlingResource.increment()
        FileDataStorageManager(user, targetContext.contentResolver).run {
            saveNewFile(existingFile)
        }
        EspressoIdlingResource.decrement()

        val intent = Intent(targetContext, ConflictsResolveActivity::class.java).apply {
            putExtra(FileActivity.EXTRA_FILE, newFile)
            putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
            putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        }

        launchActivity<ConflictsResolveActivity>(intent).use { scenario ->
            var activity: ConflictsResolveActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
                    assertEquals(decision, Decision.KEEP_SERVER)
                    returnCode = true
                }
            }

            onView(withId(R.id.right_checkbox)).perform(ViewActions.click())

            val dialog = activity!!.supportFragmentManager.findFragmentByTag("conflictDialog") as DialogFragment?
            val screenShotName = createName(testClassName + "_" + "keepExisting", "")

            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(dialog?.requireDialog()?.window?.decorView, screenShotName)

            onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
            assertTrue(returnCode)
        }
    }

    @Test
    @ScreenshotTest
    fun keepNew() {
        returnCode = false

        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )

        val existingFile = OCFile("/newFile.txt").apply {
            fileLength = 1024000
            modificationTimestamp = 1582019340
            remoteId = "00000123abc"
        }

        val newFile = OCFile("/newFile.txt").apply {
            fileLength = 56000
            modificationTimestamp = 1522019340
            setStoragePath(FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt")
        }

        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)

        val intent = Intent(targetContext, ConflictsResolveActivity::class.java)
        intent.putExtra(FileActivity.EXTRA_FILE, newFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
        intent.putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)

        launchActivity<ConflictsResolveActivity>(intent).use { scenario ->
            var activity: ConflictsResolveActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
                    assertEquals(decision, Decision.KEEP_LOCAL)
                    returnCode = true
                }
            }

            onView(withId(R.id.left_checkbox)).perform(ViewActions.click())
            val dialog = activity!!.supportFragmentManager.findFragmentByTag("conflictDialog") as DialogFragment?
            val screenShotName = createName(testClassName + "_" + "keepNew", "")
            screenshotViaName(dialog?.requireDialog()?.window?.decorView, screenShotName)

            onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
            assertTrue(returnCode)
        }
    }

    @Test
    @ScreenshotTest
    fun keepBoth() {
        returnCode = false

        val newUpload = OCUpload(
            FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt",
            "/newFile.txt",
            user.accountName
        )

        val existingFile = OCFile("/newFile.txt").apply {
            remoteId = "0001"
            fileLength = 1024000
            modificationTimestamp = 1582019340
        }

        val newFile = OCFile("/newFile.txt").apply {
            fileLength = 56000
            remoteId = "0002"
            modificationTimestamp = 1522019340
            setStoragePath(FileStorageUtils.getSavePath(user.accountName) + "/nonEmpty.txt")
        }

        val storageManager = FileDataStorageManager(user, targetContext.contentResolver)
        storageManager.saveNewFile(existingFile)

        val intent = Intent(targetContext, ConflictsResolveActivity::class.java).apply {
            putExtra(FileActivity.EXTRA_FILE, newFile)
            putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, existingFile)
            putExtra(ConflictsResolveActivity.EXTRA_CONFLICT_UPLOAD_ID, newUpload.uploadId)
        }

        launchActivity<ConflictsResolveActivity>(intent).use { scenario ->
            var activity: ConflictsResolveActivity? = null
            scenario.onActivity { sut ->
                activity = sut
                sut.listener = OnConflictDecisionMadeListener { decision: Decision? ->
                    assertEquals(decision, Decision.KEEP_BOTH)
                    returnCode = true
                }
            }

            onView(withId(R.id.right_checkbox)).perform(ViewActions.click())
            onView(withId(R.id.left_checkbox)).perform(ViewActions.click())

            val dialog = activity!!.supportFragmentManager.findFragmentByTag("conflictDialog") as DialogFragment?
            val screenShotName = createName(testClassName + "_" + "keepBoth", "")
            screenshotViaName(dialog?.requireDialog()?.window?.decorView, screenShotName)

            onView(ViewMatchers.withText("OK")).perform(ViewActions.click())
            assertTrue(returnCode)
        }
    }

    @After
    override fun after() {
        storageManager.deleteAllFiles()
    }
}
