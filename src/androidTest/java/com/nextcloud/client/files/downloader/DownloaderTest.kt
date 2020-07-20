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
    DownloaderTest.Enqueue::class,
    DownloaderTest.DownloadStatusUpdates::class
)
class DownloaderTest {

    abstract class Base {

        companion object {
            const val MAX_DOWNLOAD_THREADS = 4
        }

        @MockK
        lateinit var user: User

        @MockK
        lateinit var client: OwnCloudClient

        @MockK
        lateinit var mockTaskFactory: DownloadTask.Factory

        /**
         * All task mock functions created during test run are
         * stored here.
         */
        lateinit var downloadTaskMocks: MutableList<DownloadTask>
        lateinit var runner: ManualAsyncRunner
        lateinit var downloader: DownloaderImpl

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
            downloader = DownloaderImpl(
                runner = runner,
                taskFactory = mockTaskFactory,
                threads = MAX_DOWNLOAD_THREADS
            )
            downloadTaskResult = true
            every { mockTaskFactory.create() } answers { createMockTask() }
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
            val request = Request(user, file)
            downloader.download(request)

            // THEN
            //      download is started immediately
            val download = downloader.getDownload(request.uuid)
            assertEquals(DownloadState.RUNNING, download?.state)
        }

        @Test
        fun enqueued_downloads_are_pending_if_running_queue_is_full() {
            // GIVEN
            //      downloader is downloading max simultaneous files
            for (i in 0 until MAX_DOWNLOAD_THREADS) {
                val file = OCFile("/running/download/path/$i")
                val request = Request(user, file)
                downloader.download(request)
                val runningDownload = downloader.getDownload(request.uuid)
                assertEquals(runningDownload?.state, DownloadState.RUNNING)
            }

            // WHEN
            //      another download is enqueued
            val file = OCFile("/path")
            val request = Request(user, file)
            downloader.download(request)

            // THEN
            //      download is pending
            val download = downloader.getDownload(request.uuid)
            assertEquals(DownloadState.PENDING, download?.state)
        }
    }

    class DownloadStatusUpdates : Base() {

        @get:Rule
        val rule = InstantTaskExecutorRule()

        val file = OCFile("/path")

        @Test
        fun download_task_completes() {
            // GIVEN
            //      download is running
            //      download is being observed
            val downloadUpdates = mutableListOf<Download>()
            downloader.registerDownloadListener { downloadUpdates.add(it) }
            downloader.download(Request(user, file))

            // WHEN
            //      download task finishes successfully
            runner.runOne()

            // THEN
            //      listener is notified about status change
            assertEquals(DownloadState.RUNNING, downloadUpdates[0].state)
            assertEquals(DownloadState.COMPLETED, downloadUpdates[1].state)
        }

        @Test
        fun download_task_fails() {
            // GIVEN
            //      download is running
            //      download is being observed
            val downloadUpdates = mutableListOf<Download>()
            downloader.registerDownloadListener { downloadUpdates.add(it) }
            downloader.download(Request(user, file))

            // WHEN
            //      download task fails
            downloadTaskResult = false
            runner.runOne()

            // THEN
            //      listener is notified about status change
            assertEquals(DownloadState.RUNNING, downloadUpdates[0].state)
            assertEquals(DownloadState.FAILED, downloadUpdates[1].state)
        }

        @Test
        fun download_progress_is_updated() {
            // GIVEN
            //      download is running
            val downloadUpdates = mutableListOf<Download>()
            downloader.registerDownloadListener { downloadUpdates.add(it) }
            downloader.download(Request(user, file))

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
                assertEquals(DownloadState.RUNNING, downloadUpdates[0].state)
                assertEquals(25, downloadUpdates[1].progress)
                assertEquals(50, downloadUpdates[2].progress)
                assertEquals(75, downloadUpdates[3].progress)
                assertEquals(100, downloadUpdates[4].progress)
                assertEquals(DownloadState.COMPLETED, downloadUpdates[5].state)
            }
        }

        @Test
        fun download_task_is_created_only_for_running_downloads() {
            // WHEN
            //      multiple downloads are enqueued
            for (i in 0 until MAX_DOWNLOAD_THREADS * 2) {
                downloader.download(Request(user, file))
            }

            // THEN
            //      download task is created only for running downloads
            assertEquals(MAX_DOWNLOAD_THREADS, downloadTaskMocks.size)
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
            val request = Request(user, file)
            downloader.download(request)

            // THEN
            //      is running changes
            assertTrue(downloader.isRunning)
        }

        @Test
        fun is_running_flag_on_completion() {
            // GIVEN
            //      a download is in progress
            val file = OCFile("/path/to/file")
            val request = Request(user, file)
            downloader.download(request)
            assertTrue(downloader.isRunning)

            // WHEN
            //      download is processed
            runner.runOne()

            // THEN
            //      downloader is not running
            assertFalse(downloader.isRunning)
        }
    }
}
