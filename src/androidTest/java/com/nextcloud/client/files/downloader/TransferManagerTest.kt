/**
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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nextcloud.client.account.User
import com.nextcloud.client.core.ManualAsyncRunner
import com.nextcloud.client.core.OnProgressCallback
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.MockitoAnnotations

@RunWith(Suite::class)
@Suite.SuiteClasses(
    TransferManagerTest.Enqueue::class,
    TransferManagerTest.TransferStatusUpdates::class
)
class TransferManagerTest {

    abstract class Base {

        companion object {
            const val MAX_TRANSFER_THREADS = 4
        }

        @MockK
        lateinit var user: User

        @MockK
        lateinit var client: OwnCloudClient

        @MockK
        lateinit var mockDownloadTaskFactory: DownloadTask.Factory

        @MockK
        lateinit var mockUploadTaskFactory: UploadTask.Factory

        /**
         * All task mock functions created during test run are
         * stored here.
         */
        lateinit var downloadTaskMocks: MutableList<DownloadTask>
        lateinit var runner: ManualAsyncRunner
        lateinit var transferManager: TransferManagerImpl

        /**
         * Response value for all download tasks
         */
        var downloadTaskResult: Boolean = true

        /**
         * Progress values posted by all download task mocks before
         * returning result value
         */
        var taskProgress = listOf<Int>()

        @Before
        fun setUpBase() {
            MockKAnnotations.init(this, relaxed = true)
            MockitoAnnotations.initMocks(this)
            downloadTaskMocks = mutableListOf()
            runner = ManualAsyncRunner()
            transferManager = TransferManagerImpl(
                runner = runner,
                downloadTaskFactory = mockDownloadTaskFactory,
                uploadTaskFactory = mockUploadTaskFactory,
                threads = MAX_TRANSFER_THREADS
            )
            downloadTaskResult = true
            every { mockDownloadTaskFactory.create() } answers { createMockTask() }
        }

        private fun createMockTask(): DownloadTask {
            val task = mockk<DownloadTask>()
            every { task.download(any(), any(), any()) } answers {
                taskProgress.forEach {
                    arg<OnProgressCallback<Int>>(1).invoke(it)
                }
                val request = arg<Request>(0)
                DownloadTask.Result(request.file, downloadTaskResult)
            }
            downloadTaskMocks.add(task)
            return task
        }
    }

    class Enqueue : Base() {

        @Test
        fun enqueued_download_is_started_immediately() {
            // GIVEN
            //      downloader has no running downloads

            // WHEN
            //      download is enqueued
            val file = OCFile("/path")
            val request = DownloadRequest(user, file)
            transferManager.enqueue(request)

            // THEN
            //      download is started immediately
            val download = transferManager.getTransfer(request.uuid)
            assertEquals(TransferState.RUNNING, download?.state)
        }

        @Test
        fun enqueued_downloads_are_pending_if_running_queue_is_full() {
            // GIVEN
            //      downloader is downloading max simultaneous files
            for (i in 0 until MAX_TRANSFER_THREADS) {
                val file = OCFile("/running/download/path/$i")
                val request = DownloadRequest(user, file)
                transferManager.enqueue(request)
                val runningDownload = transferManager.getTransfer(request.uuid)
                assertEquals(runningDownload?.state, TransferState.RUNNING)
            }

            // WHEN
            //      another download is enqueued
            val file = OCFile("/path")
            val request = DownloadRequest(user, file)
            transferManager.enqueue(request)

            // THEN
            //      download is pending
            val download = transferManager.getTransfer(request.uuid)
            assertEquals(TransferState.PENDING, download?.state)
        }
    }

    class TransferStatusUpdates : Base() {

        @get:Rule
        val rule = InstantTaskExecutorRule()

        val file = OCFile("/path")

        @Test
        fun download_task_completes() {
            // GIVEN
            //      download is running
            //      download is being observed
            val downloadUpdates = mutableListOf<Transfer>()
            transferManager.registerTransferListener { downloadUpdates.add(it) }
            transferManager.enqueue(DownloadRequest(user, file))

            // WHEN
            //      download task finishes successfully
            runner.runOne()

            // THEN
            //      listener is notified about status change
            assertEquals(TransferState.RUNNING, downloadUpdates[0].state)
            assertEquals(TransferState.COMPLETED, downloadUpdates[1].state)
        }

        @Test
        fun download_task_fails() {
            // GIVEN
            //      download is running
            //      download is being observed
            val downloadUpdates = mutableListOf<Transfer>()
            transferManager.registerTransferListener { downloadUpdates.add(it) }
            transferManager.enqueue(DownloadRequest(user, file))

            // WHEN
            //      download task fails
            downloadTaskResult = false
            runner.runOne()

            // THEN
            //      listener is notified about status change
            assertEquals(TransferState.RUNNING, downloadUpdates[0].state)
            assertEquals(TransferState.FAILED, downloadUpdates[1].state)
        }

        @Test
        fun download_progress_is_updated() {
            // GIVEN
            //      download is running
            val downloadUpdates = mutableListOf<Transfer>()
            transferManager.registerTransferListener { downloadUpdates.add(it) }
            transferManager.enqueue(DownloadRequest(user, file))

            // WHEN
            //      download progress updated 4 times before completion
            taskProgress = listOf(25, 50, 75, 100)
            runner.runOne()

            // THEN
            //      listener receives 6 status updates
            //          transition to running
            //          4 progress updates
            //          completion
            assertEquals(6, downloadUpdates.size)
            if (downloadUpdates.size >= 6) {
                assertEquals(TransferState.RUNNING, downloadUpdates[0].state)
                assertEquals(25, downloadUpdates[1].progress)
                assertEquals(50, downloadUpdates[2].progress)
                assertEquals(75, downloadUpdates[3].progress)
                assertEquals(100, downloadUpdates[4].progress)
                assertEquals(TransferState.COMPLETED, downloadUpdates[5].state)
            }
        }

        @Test
        fun download_task_is_created_only_for_running_downloads() {
            // WHEN
            //      multiple downloads are enqueued
            for (i in 0 until MAX_TRANSFER_THREADS * 2) {
                transferManager.enqueue(DownloadRequest(user, file))
            }

            // THEN
            //      download task is created only for running downloads
            assertEquals(MAX_TRANSFER_THREADS, downloadTaskMocks.size)
        }
    }

    class RunningStatusUpdates : Base() {

        @get:Rule
        val rule = InstantTaskExecutorRule()

        @Test
        fun is_running_flag_on_enqueue() {
            // WHEN
            //      download is enqueued
            val file = OCFile("/path/to/file")
            val request = DownloadRequest(user, file)
            transferManager.enqueue(request)

            // THEN
            //      is running changes
            assertTrue(transferManager.isRunning)
        }

        @Test
        fun is_running_flag_on_completion() {
            // GIVEN
            //      a download is in progress
            val file = OCFile("/path/to/file")
            val request = DownloadRequest(user, file)
            transferManager.enqueue(request)
            assertTrue(transferManager.isRunning)

            // WHEN
            //      download is processed
            runner.runOne()

            // THEN
            //      downloader is not running
            assertFalse(transferManager.isRunning)
        }
    }
}
