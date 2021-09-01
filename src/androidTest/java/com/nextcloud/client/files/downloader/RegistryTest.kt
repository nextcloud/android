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
    RegistryTest.GetTransfers::class,
    RegistryTest.IsRunning::class
)
class RegistryTest {

    abstract class Base {
        companion object {
            const val MAX_TRANSFER_THREADS = 4
            const val PROGRESS_FULL = 100
            const val PROGRESS_HALF = 50
        }

        @MockK
        lateinit var user: User

        lateinit var file: OCFile

        @MockK
        lateinit var onTransferStart: (UUID, Request) -> Unit

        @MockK
        lateinit var onTransferChanged: (Transfer) -> Unit

        internal lateinit var registry: Registry

        @Before
        fun setUpBase() {
            MockKAnnotations.init(this, relaxed = true)
            file = OCFile("/test/path")
            registry = Registry(onTransferStart, onTransferChanged, MAX_TRANSFER_THREADS)
            resetMocks()
        }

        fun resetMocks() {
            clearAllMocks()
            every { onTransferStart(any(), any()) } answers {}
            every { onTransferChanged(any()) } answers {}
        }
    }

    class Pending : Base() {

        @Test
        fun inserting_pending_transfer() {
            // GIVEN
            //      registry has no pending transfers
            assertEquals(0, registry.pending.size)

            // WHEN
            //      new transfer requests added
            val addedTransfersCount = 10
            for (i in 0 until addedTransfersCount) {
                val request = DownloadRequest(user, file)
                registry.add(request)
            }

            // THEN
            //      transfer is added to the pending queue
            assertEquals(addedTransfersCount, registry.pending.size)
        }
    }

    class Start : Base() {

        companion object {
            const val ENQUEUED_REQUESTS_COUNT = 10
        }

        @Before
        fun setUp() {
            for (i in 0 until ENQUEUED_REQUESTS_COUNT) {
                registry.add(DownloadRequest(user, file))
            }
            assertEquals(ENQUEUED_REQUESTS_COUNT, registry.pending.size)
        }

        @Test
        fun starting_transfer() {
            // WHEN
            //      started
            registry.startNext()

            // THEN
            //      up to max threads requests are started
            //      start callback is triggered
            //      update callback is triggered on transfer transition
            //      started transfers are in running state
            assertEquals(
                "Transfers not moved to running queue",
                MAX_TRANSFER_THREADS,
                registry.running.size
            )
            assertEquals(
                "Transfers not moved from pending queue",
                ENQUEUED_REQUESTS_COUNT - MAX_TRANSFER_THREADS,
                registry.pending.size
            )
            verify(exactly = MAX_TRANSFER_THREADS) { onTransferStart(any(), any()) }
            val startedTransfers = mutableListOf<Transfer>()
            verify(exactly = MAX_TRANSFER_THREADS) { onTransferChanged(capture(startedTransfers)) }
            assertEquals(
                "Callbacks not invoked for running transfers",
                MAX_TRANSFER_THREADS,
                startedTransfers.size
            )
            startedTransfers.forEach {
                assertEquals("Transfer not placed into running state", TransferState.RUNNING, it.state)
            }
        }

        @Test
        fun start_is_ignored_if_no_more_free_threads() {
            // WHEN
            //      max number of running transfers
            registry.startNext()
            assertEquals(MAX_TRANSFER_THREADS, registry.running.size)
            clearAllMocks()

            // WHEN
            //      starting more transfers
            registry.startNext()

            // THEN
            //      no more transfers can be started
            assertEquals(MAX_TRANSFER_THREADS, registry.running.size)
            verify(exactly = 0) { onTransferStart(any(), any()) }
        }
    }

    class Progress : Base() {

        var uuid: UUID = UUID.randomUUID()

        @Before
        fun setUp() {
            val request = DownloadRequest(user, file)
            uuid = registry.add(request)
            registry.startNext()
            assertEquals(uuid, request.uuid)
            assertEquals(1, registry.running.size)
            resetMocks()
        }

        @Test
        fun transfer_progress_is_updated() {
            // GIVEN
            //      a transfer is running

            // WHEN
            //      transfer progress is updated
            val progressHalf = 50
            registry.progress(uuid, progressHalf)

            // THEN
            //      progress is updated
            //      update callback is invoked
            val transfer = mutableListOf<Transfer>()
            verify { onTransferChanged(capture(transfer)) }
            assertEquals(1, transfer.size)
            assertEquals(progressHalf, transfer.first().progress)
        }

        @Test
        fun updates_for_non_running_transfers_are_ignored() {
            // GIVEN
            //      transfer is not running
            registry.complete(uuid, true)
            assertEquals(0, registry.running.size)
            resetMocks()

            // WHEN
            //      progress for a non-running transfer is updated
            registry.progress(uuid, PROGRESS_HALF)

            // THEN
            //      progress update is ignored
            verify(exactly = 0) { onTransferChanged(any()) }
        }

        @Test
        fun updates_for_non_existing_transfers_are_ignored() {
            // GIVEN
            //      some transfer is running

            // WHEN
            //      progress is updated for non-existing transfer
            val nonExistingTransferId = UUID.randomUUID()
            registry.progress(nonExistingTransferId, PROGRESS_HALF)

            // THEN
            //      progress uppdate is ignored
            verify(exactly = 0) { onTransferChanged(any()) }
        }
    }

    class Complete : Base() {

        lateinit var uuid: UUID

        @Before
        fun setUp() {
            uuid = registry.add(DownloadRequest(user, file))
            registry.startNext()
            registry.progress(uuid, PROGRESS_FULL)
            resetMocks()
        }

        @Test
        fun complete_successful_transfer_with_updated_file() {
            // GIVEN
            //      a transfer is running

            // WHEN
            //      transfer is completed
            //      file has been updated
            val updatedFile = OCFile("/updated/file")
            registry.complete(uuid, true, updatedFile)

            // THEN
            //      transfer is completed successfully
            //      status carries updated file
            val slot = CapturingSlot<Transfer>()
            verify { onTransferChanged(capture(slot)) }
            assertEquals(TransferState.COMPLETED, slot.captured.state)
            assertSame(slot.captured.file, updatedFile)
        }

        @Test
        fun complete_successful_transfer() {
            // GIVEN
            //      a transfer is running

            // WHEN
            //      transfer is completed
            //      file is not updated
            registry.complete(uuid = uuid, success = true, file = null)

            // THEN
            //      transfer is completed successfully
            //      status carries previous file
            val slot = CapturingSlot<Transfer>()
            verify { onTransferChanged(capture(slot)) }
            assertEquals(TransferState.COMPLETED, slot.captured.state)
            assertSame(slot.captured.file, file)
        }

        @Test
        fun complete_failed_transfer() {
            // GIVEN
            //      a transfer is running

            // WHEN
            //      transfer is failed
            registry.complete(uuid, false)

            // THEN
            //      transfer is completed successfully
            val slot = CapturingSlot<Transfer>()
            verify { onTransferChanged(capture(slot)) }
            assertEquals(TransferState.FAILED, slot.captured.state)
        }
    }

    class GetTransfers : Base() {

        val pendingTransferFile = OCFile("/pending")
        val runningTransferFile = OCFile("/running")
        val completedTransferFile = OCFile("/completed")

        lateinit var pendingTransferId: UUID
        lateinit var runningTransferId: UUID
        lateinit var completedTransferId: UUID

        @Before
        fun setUp() {
            completedTransferId = registry.add(DownloadRequest(user, completedTransferFile))
            registry.startNext()
            registry.complete(completedTransferId, true)

            runningTransferId = registry.add(DownloadRequest(user, runningTransferFile))
            registry.startNext()

            pendingTransferId = registry.add(DownloadRequest(user, pendingTransferFile))
            resetMocks()

            assertEquals(1, registry.pending.size)
            assertEquals(1, registry.running.size)
            assertEquals(1, registry.completed.size)
        }

        @Test
        fun get_by_path_searches_pending_queue() {
            // GIVEN
            //      file transfer is pending

            // WHEN
            //      transfer status is retrieved
            val transfer = registry.getTransfer(pendingTransferFile)

            // THEN
            //      transfer from pending queue is returned
            assertNotNull(transfer)
            assertEquals(pendingTransferId, transfer?.uuid)
        }

        @Test
        fun get_by_id_searches_pending_queue() {
            // GIVEN
            //      file transfer is pending

            // WHEN
            //      transfer status is retrieved
            val transfer = registry.getTransfer(pendingTransferId)

            // THEN
            //      transfer from pending queue is returned
            assertNotNull(transfer)
            assertEquals(pendingTransferId, transfer?.uuid)
        }

        @Test
        fun get_by_path_searches_running_queue() {
            // GIVEN
            //      file transfer is running

            // WHEN
            //      transfer status is retrieved
            val transfer = registry.getTransfer(runningTransferFile)

            // THEN
            //      transfer from pending queue is returned
            assertNotNull(transfer)
            assertEquals(runningTransferId, transfer?.uuid)
        }

        @Test
        fun get_by_id_searches_running_queue() {
            // GIVEN
            //      file transfer is running

            // WHEN
            //      transfer status is retrieved
            val transfer = registry.getTransfer(runningTransferId)

            // THEN
            //      transfer from pending queue is returned
            assertNotNull(transfer)
            assertEquals(runningTransferId, transfer?.uuid)
        }

        @Test
        fun get_by_path_searches_completed_queue() {
            // GIVEN
            //      file transfer is pending

            // WHEN
            //      transfer status is retrieved
            val transfer = registry.getTransfer(completedTransferFile)

            // THEN
            //      transfer from pending queue is returned
            assertNotNull(transfer)
            assertEquals(completedTransferId, transfer?.uuid)
        }

        @Test
        fun get_by_id_searches_completed_queue() {
            // GIVEN
            //      file transfer is pending

            // WHEN
            //      transfer status is retrieved
            val transfer = registry.getTransfer(completedTransferId)

            // THEN
            //      transfer from pending queue is returned
            assertNotNull(transfer)
            assertEquals(completedTransferId, transfer?.uuid)
        }

        @Test
        fun not_found_by_path() {
            // GIVEN
            //      no transfer for a file
            val nonExistingTransferFile = OCFile("/non-nexisting/transfer")

            // WHEN
            //      transfer status is retrieved for a file
            val transfer = registry.getTransfer(nonExistingTransferFile)

            // THEN
            //      no transfer is found
            assertNull(transfer)
        }

        @Test
        fun not_found_by_id() {
            // GIVEN
            //      no transfer for an id
            val nonExistingId = UUID.randomUUID()

            // WHEN
            //      transfer status is retrieved for a file
            val transfer = registry.getTransfer(nonExistingId)

            // THEN
            //      no transfer is found
            assertNull(transfer)
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
            val request = DownloadRequest(user, OCFile("/path/alpha/1"))
            registry.add(request)
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
            val request = DownloadRequest(user, OCFile("/path/alpha/1"))
            registry.add(request)
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
            val request = DownloadRequest(user, OCFile("/path/alpha/1"))
            val id = registry.add(request)
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
