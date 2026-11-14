/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.nextcloud.client.jobs.autoUpload.AutoUploadRescanWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import java.util.concurrent.TimeUnit

class AutoUploadScheduleTest {

    private val workManager: WorkManager = mock()
    private val manager: BackgroundJobManager = BackgroundJobManagerImpl(workManager, mock(), mock())

    @Test
    fun `startup preserves the existing periodic scan`() {
        manager.schedulePeriodicAutoUpload()

        verify(workManager).enqueueUniquePeriodicWork(
            eq(BackgroundJobManagerImpl.JOB_PERIODIC_FILES_SYNC),
            eq(ExistingPeriodicWorkPolicy.KEEP),
            any()
        )
        verifyNoMoreInteractions(workManager)
    }

    @Test
    fun `resume replaces only the periodic coordinator with an immediately eligible scan`() {
        manager.schedulePeriodicAutoUpload(resetSchedule = true)

        val requests = argumentCaptor<PeriodicWorkRequest>()
        verify(workManager).enqueueUniquePeriodicWork(
            eq(BackgroundJobManagerImpl.JOB_PERIODIC_FILES_SYNC),
            eq(ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE),
            requests.capture()
        )
        verifyNoMoreInteractions(workManager)

        val request = requests.firstValue
        val spec = request.workSpec
        assertEquals(AutoUploadRescanWorker::class.java.name, spec.workerClassName)
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertEquals(TimeUnit.MINUTES.toMillis(15), spec.intervalDuration)
        assertEquals(spec.intervalDuration, spec.flexDuration)
        assertEquals(0L, spec.initialDelay)
        spec.lastEnqueueTime = ENQUEUE_TIME
        assertEquals(ENQUEUE_TIME, spec.calculateNextRunTime())
        assertTrue(request.tags.contains(BackgroundJobManagerImpl.TAG_ALL))
        assertTrue(
            request.tags.contains(
                BackgroundJobManagerImpl.formatNameTag(BackgroundJobManagerImpl.JOB_PERIODIC_FILES_SYNC)
            )
        )
    }

    companion object {
        private const val ENQUEUE_TIME = 1_000_000L
    }
}
