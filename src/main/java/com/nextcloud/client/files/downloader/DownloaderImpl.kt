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

import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.IsCancelled
import com.nextcloud.client.core.OnProgressCallback
import com.nextcloud.client.core.TaskFunction
import com.owncloud.android.datamodel.OCFile
import java.util.UUID

/**
 * Per-user file downloader.
 *
 * All notifications are performed on main thread. All download processes are run
 * in the background.
 *
 * @param runner Background task runner. It is important to provide runner that is not shared with UI code.
 * @param taskFactory Download task factory
 * @param threads maximum number of concurrent download processes
 */
@Suppress("LongParameterList") // download operations requires those resources
class DownloaderImpl(
    private val runner: AsyncRunner,
    private val taskFactory: DownloadTask.Factory,
    threads: Int = 1
) : Downloader {

    companion object {
        const val PROGRESS_PERCENTAGE_MAX = 100
        const val PROGRESS_PERCENTAGE_MIN = 0
        const val TEST_DOWNLOAD_PROGRESS_UPDATE_PERIOD_MS = 200L
    }

    private val registry = Registry(
        onStartTransfer = this::onStartDownload,
        onTransferChanged = this::onDownloadUpdate,
        maxRunning = threads
    )
    private val downloadListeners: MutableSet<(Transfer) -> Unit> = mutableSetOf()
    private val statusListeners: MutableSet<(Downloader.Status) -> Unit> = mutableSetOf()

    override val isRunning: Boolean get() = registry.isRunning

    override val status: Downloader.Status
        get() = Downloader.Status(
            pending = registry.pending,
            running = registry.running,
            completed = registry.completed
        )

    override fun registerDownloadListener(listener: (Transfer) -> Unit) {
        downloadListeners.add(listener)
    }

    override fun removeDownloadListener(listener: (Transfer) -> Unit) {
        downloadListeners.remove(listener)
    }

    override fun registerStatusListener(listener: (Downloader.Status) -> Unit) {
        statusListeners.add(listener)
    }

    override fun removeStatusListener(listener: (Downloader.Status) -> Unit) {
        statusListeners.remove(listener)
    }

    override fun download(request: Request) {
        registry.add(request)
        registry.startNext()
    }

    override fun getDownload(uuid: UUID): Transfer? = registry.getTransfer(uuid)

    override fun getDownload(file: OCFile): Transfer? = registry.getTransfer(file)

    private fun onStartDownload(uuid: UUID, request: Request) {
        val downloadTask = createDownloadTask(request)
        runner.postTask(
            task = downloadTask,
            onProgress = { progress: Int -> registry.progress(uuid, progress) },
            onResult = { result -> registry.complete(uuid, result.success, result.file); registry.startNext() },
            onError = { registry.complete(uuid, false); registry.startNext() }
        )
    }

    private fun createDownloadTask(request: Request): TaskFunction<DownloadTask.Result, Int> {
        return if (request.test) {
            { progress: OnProgressCallback<Int>, isCancelled: IsCancelled ->
                testDownloadTask(request.file, progress, isCancelled)
            }
        } else {
            val downloadTask = taskFactory.create()
            val wrapper: TaskFunction<DownloadTask.Result, Int> = { progress: ((Int) -> Unit), isCancelled ->
                downloadTask.download(request, progress, isCancelled)
            }
            wrapper
        }
    }

    private fun onDownloadUpdate(transfer: Transfer) {
        downloadListeners.forEach { it.invoke(transfer) }
        if (statusListeners.isNotEmpty()) {
            val status = this.status
            statusListeners.forEach { it.invoke(status) }
        }
    }

    /**
     *  Test download task is used only to simulate download process without
     *  any network traffic. It is used for development.
     */
    private fun testDownloadTask(
        file: OCFile,
        onProgress: OnProgressCallback<Int>,
        isCancelled: IsCancelled
    ): DownloadTask.Result {
        for (i in PROGRESS_PERCENTAGE_MIN..PROGRESS_PERCENTAGE_MAX) {
            onProgress(i)
            if (isCancelled()) {
                return DownloadTask.Result(file, false)
            }
            Thread.sleep(TEST_DOWNLOAD_PROGRESS_UPDATE_PERIOD_MS)
        }
        return DownloadTask.Result(file, true)
    }
}
