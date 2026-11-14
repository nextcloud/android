/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs

import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.nextcloud.client.jobs.autoUpload.AutoUploadWorker
import com.owncloud.android.datamodel.SyncedFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Answers.RETURNS_SELF
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.withSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

class AutoUploadRecoveryScheduleTest {
    private val workManager: WorkManager = mock()
    private val manager: BackgroundJobManager = BackgroundJobManagerImpl(workManager, mock(), mock())

    @Test
    fun `recovery preserves existing folder work with network charging and retry constraints`() {
        val folder: SyncedFolder = mock {
            on { id } doReturn 3L
            on { isChargingOnly } doReturn true
        }
        mockConstruction(NetworkRequest.Builder::class.java, withSettings().defaultAnswer(RETURNS_SELF)) { builder, _ ->
            whenever(builder.build()).thenReturn(mock())
        }.use {
            manager.startAutoUpload(folder)
            manager.startAutoUpload(folder)
        }

        val requests = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager, times(2)).enqueueUniqueWork(
            eq(BackgroundJobManagerImpl.JOB_IMMEDIATE_FILES_SYNC + "_3"),
            eq(ExistingWorkPolicy.KEEP),
            requests.capture()
        )
        verifyNoMoreInteractions(workManager)

        val spec = requests.firstValue.workSpec
        assertEquals(AutoUploadWorker::class.java.name, spec.workerClassName)
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertTrue(spec.constraints.requiresCharging())
        assertEquals(BackoffPolicy.LINEAR, spec.backoffPolicy)
        assertEquals(
            TimeUnit.SECONDS.toMillis(BackgroundJobManagerImpl.DEFAULT_BACKOFF_CRITERIA_DELAY_SEC),
            spec.backoffDelayDuration
        )
        assertEquals(3L, spec.input.getLong(AutoUploadWorker.SYNCED_FOLDER_ID, -1))
        assertFalse(spec.input.getBoolean(AutoUploadWorker.OVERRIDE_POWER_SAVING, true))
    }
}
