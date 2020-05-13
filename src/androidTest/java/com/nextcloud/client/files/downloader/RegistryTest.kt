/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.files.downloader

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import java.util.UUID

@RunWith(Suite::class)
@Suite.SuiteClasses(
    RegistryTest.Pending::class,
    RegistryTest.Start::class,
    RegistryTest.Progress::class,
    RegistryTest.Complete::class,
    RegistryTest.GetDownloads::class,
    RegistryTest.IsRunning::class
)
class RegistryTest {

    abstract class Base {
        companion object {
            const val MAX_DOWNLOAD_THREADS = 4
            const val PROGRESS_FULL = 100
            const val PROGRESS_HALF = 50
        }

        @MockK
        lateinit var user: User

        lateinit var file: OCFile

        @MockK
        lateinit var onDownloadStart: (UUID, Request) -> Unit

        @MockK
        lateinit var onDownloadChanged: (Download) -> Unit

        internal lateinit var registry: Registry

        @Before
        fun setUpBase() {
            MockKAnnotations.init(this, relaxed = true)
            file = OCFile("/test/path")
            registry = Registry(onDownloadStart, onDownloadChanged, MAX_DOWNLOAD_THREADS)
            resetMocks()
        }

        fun resetMocks() {
            clearAllMocks()
            every { onDownloadStart(any(), any()) } answers {}
            every { onDownloadChanged(any()) } answers {}
        }
    }

    class Pending : Base() {

        @Test
        fun inserting_pending_download() {
            // GIVEN
            //      registry has no pending downloads
            assertEquals(0, registry.pending.size)

            // WHEN
            //      new download requests added
            val addedDownloadsCount = 10
            for (i in 0 until addedDownloadsCount) {
                val request = Request(user, file)
                registry.add(request)
            }

            // THEN
            //      download is added to the pending queue
            assertEquals(addedDownloadsCount, registry.pending.size)
        }
    }

    class Start : Base() {

        companion object {
            const val ENQUEUED_REQUESTS_COUNT = 10
        }

        @Before
        fun setUp() {
            for (i in 0 until ENQUEUED_REQUESTS_COUNT) {
                registry.add(Request(user, file))
            }
            assertEquals(ENQUEUED_REQUESTS_COUNT, registry.pending.size)
        }

        @Test
        fun starting_download() {
            // WHEN
            //      started
            registry.startNext()

            // THEN
            //      up to max threads requests are started
            //      start callback is triggered
            //      update callback is triggered on download transition
            //      started downloads are in running state
            assertEquals(
                "Downloads not moved to running queue",
                MAX_DOWNLOAD_THREADS,
                registry.running.size
            )
            assertEquals(
                "Downloads not moved from pending queue",
                ENQUEUED_REQUESTS_COUNT - MAX_DOWNLOAD_THREADS,
                registry.pending.size
            )
            verify(exactly = MAX_DOWNLOAD_THREADS) { onDownloadStart(any(), any()) }
            val startedDownloads = mutableListOf<Download>()
            verify(exactly = MAX_DOWNLOAD_THREADS) { onDownloadChanged(capture(startedDownloads)) }
            assertEquals(
                "Callbacks not invoked for running downloads",
                MAX_DOWNLOAD_THREADS,
                startedDownloads.size
            )
            startedDownloads.forEach {
                assertEquals("Download not placed into running state", DownloadState.RUNNING, it.state)
            }
        }

        @Test
        fun start_is_ignored_if_no_more_free_threads() {
            // WHEN
            //      max number of running downloads
            registry.startNext()
            assertEquals(MAX_DOWNLOAD_THREADS, registry.running.size)
            clearAllMocks()

            // WHEN
            //      starting more downloads
            registry.startNext()

            // THEN
            //      no more downloads can be started
            assertEquals(MAX_DOWNLOAD_THREADS, registry.running.size)
            verify(exactly = 0) { onDownloadStart(any(), any()) }
        }
    }

    class Progress : Base() {

        var uuid: UUID = UUID.randomUUID()

        @Before
        fun setUp() {
            val request = Request(user, file)
            uuid = registry.add(request)
            registry.startNext()
            assertEquals(uuid, request.uuid)
            assertEquals(1, registry.running.size)
            resetMocks()
        }

        @Test
        fun download_progress_is_updated() {
            // GIVEN
            //      a download is running

            // WHEN
            //      download progress is updated
            val progressHalf = 50
            registry.progress(uuid, progressHalf)

            // THEN
            //      progress is updated
            //      update callback is invoked
            val download = mutableListOf<Download>()
            verify { onDownloadChanged(capture(download)) }
            assertEquals(1, download.size)
            assertEquals(progressHalf, download.first().progress)
        }

        @Test
        fun updates_for_non_running_downloads_are_ignored() {
            // GIVEN
            //      download is not running
            registry.complete(uuid, true)
            assertEquals(0, registry.running.size)
            resetMocks()

            // WHEN
            //      progress for a non-running download is updated
            registry.progress(uuid, PROGRESS_HALF)

            // THEN
            //      progress update is ignored
            verify(exactly = 0) { onDownloadChanged(any()) }
        }

        @Test
        fun updates_for_non_existing_downloads_are_ignored() {
            // GIVEN
            //      some download is running

            // WHEN
            //      progress is updated for non-existing download
            val nonExistingDownloadId = UUID.randomUUID()
            registry.progress(nonExistingDownloadId, PROGRESS_HALF)

            // THEN
            //      progress uppdate is ignored
            verify(exactly = 0) { onDownloadChanged(any()) }
        }
    }

    class Complete : Base() {

        lateinit var uuid: UUID

        @Before
        fun setUp() {
            uuid = registry.add(Request(user, file))
            registry.startNext()
            registry.progress(uuid, PROGRESS_FULL)
            resetMocks()
        }

        @Test
        fun complete_successful_download_with_updated_file() {
            // GIVEN
            //      a download is running

            // WHEN
            //      download is completed
            //      file has been updated
            val updatedFile = OCFile("/updated/file")
            registry.complete(uuid, true, updatedFile)

            // THEN
            //      download is completed successfully
            //      status carries updated file
            val slot = CapturingSlot<Download>()
            verify { onDownloadChanged(capture(slot)) }
            assertEquals(DownloadState.COMPLETED, slot.captured.state)
            assertSame(slot.captured.file, updatedFile)
        }

        @Test
        fun complete_successful_download() {
            // GIVEN
            //      a download is running

            // WHEN
            //      download is completed
            //      file is not updated
            registry.complete(uuid = uuid, success = true, file = null)

            // THEN
            //      download is completed successfully
            //      status carries previous file
            val slot = CapturingSlot<Download>()
            verify { onDownloadChanged(capture(slot)) }
            assertEquals(DownloadState.COMPLETED, slot.captured.state)
            assertSame(slot.captured.file, file)
        }

        @Test
        fun complete_failed_download() {
            // GIVEN
            //      a download is running

            // WHEN
            //      download is failed
            registry.complete(uuid, false)

            // THEN
            //      download is completed successfully
            val slot = CapturingSlot<Download>()
            verify { onDownloadChanged(capture(slot)) }
            assertEquals(DownloadState.FAILED, slot.captured.state)
        }
    }

    class GetDownloads : Base() {

        val pendingDownloadFile = OCFile("/pending")
        val runningDownloadFile = OCFile("/running")
        val completedDownloadFile = OCFile("/completed")

        lateinit var pendingDownloadId: UUID
        lateinit var runningDownloadId: UUID
        lateinit var completedDownloadId: UUID

        @Before
        fun setUp() {
            completedDownloadId = registry.add(Request(user, completedDownloadFile))
            registry.startNext()
            registry.complete(completedDownloadId, true)

            runningDownloadId = registry.add(Request(user, runningDownloadFile))
            registry.startNext()

            pendingDownloadId = registry.add(Request(user, pendingDownloadFile))
            resetMocks()

            assertEquals(1, registry.pending.size)
            assertEquals(1, registry.running.size)
            assertEquals(1, registry.completed.size)
        }

        @Test
        fun get_by_path_searches_pending_queue() {
            // GIVEN
            //      file download is pending

            // WHEN
            //      download status is retrieved
            val download = registry.getDownload(pendingDownloadFile)

            // THEN
            //      download from pending queue is returned
            assertNotNull(download)
            assertEquals(pendingDownloadId, download?.uuid)
        }

        @Test
        fun get_by_id_searches_pending_queue() {
            // GIVEN
            //      file download is pending

            // WHEN
            //      download status is retrieved
            val download = registry.getDownload(pendingDownloadId)

            // THEN
            //      download from pending queue is returned
            assertNotNull(download)
            assertEquals(pendingDownloadId, download?.uuid)
        }

        @Test
        fun get_by_path_searches_running_queue() {
            // GIVEN
            //      file download is running

            // WHEN
            //      download status is retrieved
            val download = registry.getDownload(runningDownloadFile)

            // THEN
            //      download from pending queue is returned
            assertNotNull(download)
            assertEquals(runningDownloadId, download?.uuid)
        }

        @Test
        fun get_by_id_searches_running_queue() {
            // GIVEN
            //      file download is running

            // WHEN
            //      download status is retrieved
            val download = registry.getDownload(runningDownloadId)

            // THEN
            //      download from pending queue is returned
            assertNotNull(download)
            assertEquals(runningDownloadId, download?.uuid)
        }

        @Test
        fun get_by_path_searches_completed_queue() {
            // GIVEN
            //      file download is pending

            // WHEN
            //      download status is retrieved
            val download = registry.getDownload(completedDownloadFile)

            // THEN
            //      download from pending queue is returned
            assertNotNull(download)
            assertEquals(completedDownloadId, download?.uuid)
        }

        @Test
        fun get_by_id_searches_completed_queue() {
            // GIVEN
            //      file download is pending

            // WHEN
            //      download status is retrieved
            val download = registry.getDownload(completedDownloadId)

            // THEN
            //      download from pending queue is returned
            assertNotNull(download)
            assertEquals(completedDownloadId, download?.uuid)
        }

        @Test
        fun not_found_by_path() {
            // GIVEN
            //      no download for a file
            val nonExistingDownloadFile = OCFile("/non-nexisting/download")

            // WHEN
            //      download status is retrieved for a file
            val download = registry.getDownload(nonExistingDownloadFile)

            // THEN
            //      no download is found
            assertNull(download)
        }

        @Test
        fun not_found_by_id() {
            // GIVEN
            //      no download for an id
            val nonExistingId = UUID.randomUUID()

            // WHEN
            //      download status is retrieved for a file
            val download = registry.getDownload(nonExistingId)

            // THEN
            //      no download is found
            assertNull(download)
        }
    }

    class IsRunning : Base() {

        @Test
        fun no_requests() {
            // WHEN
            //      all queues empty
            assertEquals(0, registry.pending.size)
            assertEquals(0, registry.running.size)
            assertEquals(0, registry.completed.size)

            // THEN
            //      not running
            assertFalse(registry.isRunning)
        }

        @Test
        fun request_pending() {
            // WHEN
            //      request is enqueued
            registry.add(Request(user, OCFile("/path/alpha/1")))
            assertEquals(1, registry.pending.size)
            assertEquals(0, registry.running.size)
            assertEquals(0, registry.completed.size)

            // THEN
            //      is running
            assertTrue(registry.isRunning)
        }

        @Test
        fun request_running() {
            // WHEN
            //      request is running
            registry.add(Request(user, OCFile("/path/alpha/1")))
            registry.startNext()
            assertEquals(0, registry.pending.size)
            assertEquals(1, registry.running.size)
            assertEquals(0, registry.completed.size)

            // THEN
            //      is running
            assertTrue(registry.isRunning)
        }

        @Test
        fun request_completed() {
            // WHEN
            //      request is running
            val id = registry.add(Request(user, OCFile("/path/alpha/1")))
            registry.startNext()
            registry.complete(id, true)
            assertEquals(0, registry.pending.size)
            assertEquals(0, registry.running.size)
            assertEquals(1, registry.completed.size)

            // THEN
            //      is not running
            assertFalse(registry.isRunning)
        }
    }
}
