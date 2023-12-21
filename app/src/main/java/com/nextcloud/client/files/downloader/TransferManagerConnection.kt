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

import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.datamodel.OCFile
import java.util.UUID

class TransferManagerConnection(
    private val backgroundJobManager: BackgroundJobManager,
    val user: User
) : TransferManager {

    private var transferListeners: MutableSet<(Transfer) -> Unit> = mutableSetOf()
    private var statusListeners: MutableSet<(TransferManager.Status) -> Unit> = mutableSetOf()
    private var manager: FileTransferWorker.Manager? = null
    private val transfersRequiringStatusRedelivery: MutableSet<UUID> = mutableSetOf()

    override val isRunning: Boolean
        get() = manager?.isRunning ?: false

    override val status: TransferManager.Status
        get() = manager?.status ?: TransferManager.Status.EMPTY

    override fun getTransfer(uuid: UUID): Transfer? = manager?.getTransfer(uuid)

    override fun getTransfer(file: OCFile): Transfer? = manager?.getTransfer(file)

    override fun enqueue(request: Request) {
        if (transferListeners.size > 0) {
            backgroundJobManager.startFileTransfer(request)
        }
    }

    override fun registerTransferListener(listener: (Transfer) -> Unit) {
        transferListeners.add(listener)
        manager?.registerTransferListener(listener)
    }

    override fun removeTransferListener(listener: (Transfer) -> Unit) {
        transferListeners.remove(listener)
        manager?.removeTransferListener(listener)
    }

    override fun registerStatusListener(listener: (TransferManager.Status) -> Unit) {
        statusListeners.add(listener)
        manager?.registerStatusListener(listener)
    }

    override fun removeStatusListener(listener: (TransferManager.Status) -> Unit) {
        statusListeners.remove(listener)
        manager?.removeStatusListener(listener)
    }

    fun onBound() {
        this.manager = FileTransferWorker.manager

        transferListeners.forEach { listener ->
            manager?.registerTransferListener(listener)
        }
        statusListeners.forEach { listener ->
            manager?.registerStatusListener(listener)
        }
        deliverMissedUpdates()
    }

    /**
     * Since binding and transfer start are both asynchronous and the order
     * is not guaranteed, some transfers might already finish when service is bound,
     * resulting in lost notifications.
     *
     * Deliver all updates for pending transfers that were scheduled
     * before service was bound.
     */
    private fun deliverMissedUpdates() {
        val transferUpdates = transfersRequiringStatusRedelivery.mapNotNull { uuid ->
            manager?.getTransfer(uuid)
        }
        transferListeners.forEach { listener ->
            transferUpdates.forEach { update ->
                listener.invoke(update)
            }
        }
        transfersRequiringStatusRedelivery.clear()

        if (statusListeners.isNotEmpty()) {
            manager?.status?.let { status ->
                statusListeners.forEach { it.invoke(status) }
            }
        }
    }

    fun onUnbind() {
        transferListeners.forEach { manager?.removeTransferListener(it) }
        statusListeners.forEach { manager?.removeStatusListener(it) }
    }
}
