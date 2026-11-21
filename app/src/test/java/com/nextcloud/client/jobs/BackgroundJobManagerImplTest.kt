/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.Futures
import com.nextcloud.client.core.Clock
import com.nextcloud.client.jobs.autoUpload.AutoUploadWorker
import com.nextcloud.client.preferences.AppPreferences
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.util.UUID

class BackgroundJobManagerImplTest {

    private val classTag = BackgroundJobManagerImpl.formatClassTag(AutoUploadWorker::class)
    private val workManager: WorkManager = mock()
    private val manager = BackgroundJobManagerImpl(workManager, mock<Clock>(), mock<AppPreferences>())

    private fun autoUploadWork(state: WorkInfo.State, vararg extraTags: String): WorkInfo =
        WorkInfo(UUID.randomUUID(), state, setOf(classTag, *extraTags))

    private fun givenAutoUploadWorks(vararg works: WorkInfo) {
        workManager.stub { on { getWorkInfosByTag(classTag) } doReturn Futures.immediateFuture(works.toList()) }
    }

    @Test
    fun `rescan restarts an enqueued folder run that never started`() {
        val stuck = autoUploadWork(WorkInfo.State.ENQUEUED, "name:immediate_files_sync_1")
        givenAutoUploadWorks(stuck)

        manager.cancelEnqueuedAutoUploads()

        verify(workManager).cancelWorkById(stuck.id)
    }

    @Test
    fun `rescan leaves running and finished folder runs alone`() {
        val running = autoUploadWork(WorkInfo.State.RUNNING)
        val succeeded = autoUploadWork(WorkInfo.State.SUCCEEDED)
        givenAutoUploadWorks(running, succeeded)

        manager.cancelEnqueuedAutoUploads()

        verify(workManager, never()).cancelWorkById(any())
    }

    @Test
    fun `rescan keeps an enqueued sync now run that ignores power saving`() {
        val syncNow = autoUploadWork(WorkInfo.State.ENQUEUED, "immediate_files_sync_1_ignore_power_saving")
        givenAutoUploadWorks(syncNow)

        manager.cancelEnqueuedAutoUploads()

        verify(workManager, never()).cancelWorkById(any())
    }
}
