/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.launchActivity
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.nextcloud.client.jobs.upload.FileUploadWorker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class OCFileListFragmentScanIT : AbstractIT() {

    private lateinit var uploadsStorageManager: UploadsStorageManager

    @Before
    fun setUp() {
        Intents.init()
        uploadsStorageManager = UploadsStorageManager(
            UserAccountManagerImpl.fromContext(targetContext),
            targetContext.contentResolver
        )
        uploadsStorageManager.removeUploads(account.name)
    }

    @After
    fun tearDown() {
        Intents.release()
        uploadsStorageManager.removeUploads(account.name)
    }

    @Test
    fun scanStartsEmbeddedFairScanWithAutoDeletionDisabled() {
        scan(Activity.RESULT_CANCELED, null)

        val scanIntent = Intents.getIntents().single { it.action == FAIRSCAN_ACTION }
        assertEquals(FAIRSCAN_ACTIVITY, scanIntent.component?.className)
        assertEquals(FAIRSCAN_ACTION, scanIntent.action)
        assertTrue(scanIntent.getBooleanExtra(FAIRSCAN_EXTRA_DISABLE_AUTO_DELETION, false))
    }

    @Test
    fun cancelledScanDoesNotUpload() {
        scan(Activity.RESULT_CANCELED, null)
        SystemClock.sleep(NO_UPLOAD_GRACE_MILLIS)

        assertEquals(0, uploadsStorageManager.getAllStoredUploads().size)
    }

    @Test
    fun embeddedScanResultIsQueuedForUploadWithMoveBehaviour() {
        val pdf = createFile("scan.pdf", 1)
        scan(Activity.RESULT_OK, Intent().putExtra(FAIRSCAN_EXTRA_PDF_PATH, pdf.absolutePath))

        val upload = waitForUpload()
        assertEquals(pdf.absolutePath, upload.localPath)
        assertTrue(upload.remotePath.startsWith("/"))
        assertTrue(upload.remotePath.endsWith(".pdf"))
        assertEquals(FileUploadWorker.LOCAL_BEHAVIOUR_MOVE, upload.localAction)
        assertEquals(account.name, upload.accountName)
    }

    private fun scan(resultCode: Int, data: Intent?) {
        launchActivity<TestActivity>().use { scenario ->
            intending(hasAction(FAIRSCAN_ACTION)).respondWith(Instrumentation.ActivityResult(resultCode, data))
            scenario.onActivity { activity ->
                val fragment = OCFileListFragment()
                activity.addFragment(fragment)
                activity.supportFragmentManager.executePendingTransactions()
                fragment.scanDocUploadFromApp()
            }
            SystemClock.sleep(RESULT_DELIVERY_MILLIS)
        }
    }

    private fun waitForUpload(): OCUpload {
        val deadline = SystemClock.elapsedRealtime() + WAIT_TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            uploadsStorageManager.getAllStoredUploads().firstOrNull()?.let { return it }
            SystemClock.sleep(WAIT_POLL_INTERVAL_MILLIS)
        }
        fail("No upload was queued for the scanned PDF")
        error("unreachable")
    }

    private companion object {
        const val FAIRSCAN_ACTIVITY = "org.fairscan.app.MainActivity"
        const val FAIRSCAN_ACTION = "org.fairscan.app.action.SCAN_TO_PDF"
        const val FAIRSCAN_EXTRA_DISABLE_AUTO_DELETION = "org.fairscan.app.extra.DISABLE_AUTO_DELETION"
        const val FAIRSCAN_EXTRA_PDF_PATH = "org.fairscan.app.extra.PDF_PATH"
        const val RESULT_DELIVERY_MILLIS = 1_000L
        const val NO_UPLOAD_GRACE_MILLIS = 1_000L
        const val WAIT_TIMEOUT_MILLIS = 10_000L
        const val WAIT_POLL_INTERVAL_MILLIS = 200L
    }
}
