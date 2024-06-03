/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.files

import com.nextcloud.client.jobs.transfer.Transfer
import com.nextcloud.client.jobs.transfer.TransferState
import com.owncloud.android.datamodel.OCFile
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * This class tracks status of file transfers. It serves as a state
 * machine and drives the transfer background task scheduler via callbacks.
 * Transfer status updates trigger change callbacks that should be used
 * to notify listeners.
 *
 * No listener registration mechanism is provided at this level.
 *
 * This class is not thread-safe. All access from multiple threads shall
 * be lock protected.
 *
 * @property onStartTransfer callback triggered when transfer is switched into running state
 * @property onTransferChanged callback triggered whenever transfer status update
 * @property maxRunning maximum number of allowed simultaneous transfers
 */
internal class Registry(
    private val onStartTransfer: (UUID, Request) -> Unit,
    private val onTransferChanged: (Transfer) -> Unit,
    private val maxRunning: Int = 2
) {
    private val pendingQueue = LinkedHashMap<UUID, Transfer>()
    private val runningQueue = LinkedHashMap<UUID, Transfer>()
    private val completedQueue = LinkedHashMap<UUID, Transfer>()

    val isRunning: Boolean get() = pendingQueue.size > 0 || runningQueue.size > 0

    val pending: List<Transfer> get() = pendingQueue.map { it.value }
    val running: List<Transfer> get() = runningQueue.map { it.value }
    val completed: List<Transfer> get() = completedQueue.map { it.value }

    /**
     * Insert new transfer into a pending queue.
     *
     * @return scheduled transfer id
     */
    fun add(request: Request): UUID {
        val transfer = Transfer(
            uuid = request.uuid,
            state = TransferState.PENDING,
            progress = 0,
            file = request.file,
            request = request
        )
        pendingQueue[transfer.uuid] = transfer
        return transfer.uuid
    }

    /**
     * Move pending transfers into a running queue up
     * to max allowed simultaneous transfers.
     */
    fun startNext() {
        val freeThreads = max(0, maxRunning - runningQueue.size)
        for (i in 0 until min(freeThreads, pendingQueue.size)) {
            val key = pendingQueue.keys.first()
            val pendingTransfer = pendingQueue.remove(key) ?: throw IllegalStateException("Transfer $key not found")
            val runningTransfer = pendingTransfer.copy(state = TransferState.RUNNING)
            runningQueue[key] = runningTransfer
            onStartTransfer.invoke(key, runningTransfer.request)
            onTransferChanged(runningTransfer)
        }
    }

    /**
     * Update progress for a given transfer. If no transfer of a given id is currently running,
     * update is ignored.
     *
     * @param uuid ID of the transfer to update
     * @param progress progress 0-100%
     */
    fun progress(uuid: UUID, progress: Int) {
        val transfer = runningQueue[uuid]
        if (transfer != null) {
            val runningTransfer = transfer.copy(progress = progress)
            runningQueue[uuid] = runningTransfer
            onTransferChanged(runningTransfer)
        }
    }

    /**
     * Complete currently running transfer. If no transfer of a given id is currently running,
     * update is ignored.
     *
     * @param uuid of the transfer to complete
     * @param success if true, transfer will be marked as completed; if false - as failed
     * @param file if provided, update file in transfer status; if null, existing value is retained
     */
    fun complete(uuid: UUID, success: Boolean, file: OCFile? = null) {
        val transfer = runningQueue.remove(uuid)
        if (transfer != null) {
            val status = if (success) {
                TransferState.COMPLETED
            } else {
                TransferState.FAILED
            }
            val completedTransfer = transfer.copy(state = status, file = file ?: transfer.file)
            completedQueue[uuid] = completedTransfer
            onTransferChanged(completedTransfer)
        }
    }

    /**
     * Search for a transfer by file path. It traverses
     * through all queues in order of pending, running and completed
     * transfers and returns first transfer status matching
     * file path.
     *
     * @param file Search for a file transfer
     * @return transfer status if found, null otherwise
     */
    fun getTransfer(file: OCFile): Transfer? {
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
     * Get transfer status by id. It traverses
     * through all queues in order of pending, running and completed
     * transfers and returns first transfer status matching
     * file path.
     *
     * @param id transfer id
     * @return transfer status if found, null otherwise
     */
    fun getTransfer(uuid: UUID): Transfer? {
        return pendingQueue[uuid] ?: runningQueue[uuid] ?: completedQueue[uuid]
    }
}
