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

import com.owncloud.android.datamodel.OCFile
import java.lang.IllegalStateException
import java.util.TreeMap
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * This class tracks status of all downloads. It serves as a state
 * machine and drives the download background task scheduler via callbacks.
 * Download status updates are triggering change callbacks that should be used
 * to notify listeners.
 *
 * No listener registration mechanism is provided at this level.
 *
 * This class is not thread-safe. All access from multiple threads shall
 * be lock protected.
 *
 * @property onStartDownload callback triggered when download is switched into running state
 * @property onDownloadChanged callback triggered whenever download status update
 * @property maxRunning maximum number of allowed simultaneous downloads
 */
internal class Registry(
    private val onStartDownload: (UUID, Request) -> Unit,
    private val onDownloadChanged: (Download) -> Unit,
    private val maxRunning: Int = 2
) {
    private val pendingQueue = TreeMap<UUID, Download>()
    private val runningQueue = TreeMap<UUID, Download>()
    private val completedQueue = TreeMap<UUID, Download>()

    val isRunning: Boolean get() = pendingQueue.size > 0 || runningQueue.size > 0

    val pending: List<Download> get() = pendingQueue.map { it.value }
    val running: List<Download> get() = runningQueue.map { it.value }
    val completed: List<Download> get() = completedQueue.map { it.value }

    /**
     * Insert new download into a pending queue.
     *
     * @return scheduled download id
     */
    fun add(request: Request): UUID {
        val download = Download(
            uuid = request.uuid,
            state = DownloadState.PENDING,
            progress = 0,
            file = request.file,
            request = request
        )
        pendingQueue[download.uuid] = download
        return download.uuid
    }

    /**
     * Move pending downloads into a running queue up
     * to max allowed simultaneous downloads.
     */
    fun startNext() {
        val freeThreads = max(0, maxRunning - runningQueue.size)
        for (i in 0 until min(freeThreads, pendingQueue.size)) {
            val key = pendingQueue.firstKey()
            val pendingDownload = pendingQueue.remove(key) ?: throw IllegalStateException("Download $key not exist.")
            val runningDownload = pendingDownload.copy(state = DownloadState.RUNNING)
            runningQueue[key] = runningDownload
            onStartDownload.invoke(key, runningDownload.request)
            onDownloadChanged(runningDownload)
        }
    }

    /**
     * Update progress for a given download. If no download of a given id is currently running,
     * update is ignored.
     *
     * @param uuid ID of the download to update
     * @param progress progress 0-100%
     */
    fun progress(uuid: UUID, progress: Int) {
        val download = runningQueue[uuid]
        if (download != null) {
            val runningDownload = download.copy(progress = progress)
            runningQueue[uuid] = runningDownload
            onDownloadChanged(runningDownload)
        }
    }

    /**
     * Complete currently running download. If no download of a given id is currently running,
     * update is ignored.
     *
     * @param uuid of the download to complete
     * @param success if true, download will be marked as completed; if false - as failed
     * @param file if provided, update file in download status; if null, existing value is retained
     */
    fun complete(uuid: UUID, success: Boolean, file: OCFile? = null) {
        val download = runningQueue.remove(uuid)
        if (download != null) {
            val status = if (success) {
                DownloadState.COMPLETED
            } else {
                DownloadState.FAILED
            }
            val completedDownload = download.copy(state = status, file = file ?: download.file)
            completedQueue[uuid] = completedDownload
            onDownloadChanged(completedDownload)
        }
    }

    /**
     * Search for a download by file path. It traverses
     * through all queues in order of pending, running and completed
     * downloads and returns first download status matching
     * file path.
     *
     * @param file Search for a file download
     * @return download status if found, null otherwise
     */
    fun getDownload(file: OCFile): Download? {
        arrayOf(pendingQueue, runningQueue, completedQueue).forEach { queue ->
            queue.forEach { entry ->
                if (entry.value.request.file.remotePath == file.remotePath) {
                    return entry.value
                }
            }
        }
        return null
    }

    /**
     * Get download status by id. It traverses
     * through all queues in order of pending, running and completed
     * downloads and returns first download status matching
     * file path.
     *
     * @param id download id
     * @return download status if found, null otherwise
     */
    fun getDownload(uuid: UUID): Download? {
        return pendingQueue[uuid] ?: runningQueue[uuid] ?: completedQueue[uuid]
    }
}
