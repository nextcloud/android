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

import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.nextcloud.client.account.User
import com.nextcloud.client.core.LocalConnection
import com.owncloud.android.datamodel.OCFile
import java.util.UUID

class TransferManagerConnection(
    context: Context,
    val user: User
) : LocalConnection<FileTransferService>(context), TransferManager {

    private var transferListeners: MutableSet<(Transfer) -> Unit> = mutableSetOf()
    private var statusListeners: MutableSet<(TransferManager.Status) -> Unit> = mutableSetOf()
    private var binder: FileTransferService.Binder? = null
    private val transfersRequiringStatusRedelivery: MutableSet<UUID> = mutableSetOf()

    override val isRunning: Boolean
        get() = binder?.isRunning ?: false

    override val status: TransferManager.Status
        get() = binder?.status ?: TransferManager.Status.EMPTY

    override fun getTransfer(uuid: UUID): Transfer? = binder?.getTransfer(uuid)

    override fun getTransfer(file: OCFile): Transfer? = binder?.getTransfer(file)

    override fun enqueue(request: Request) {
        val intent = FileTransferService.createTransferRequestIntent(context, request)
        context.startService(intent)
        if (!isConnected && transferListeners.size > 0) {
            transfersRequiringStatusRedelivery.add(request.uuid)
        }
    }

    override fun registerTransferListener(listener: (Transfer) -> Unit) {
        transferListeners.add(listener)
        binder?.registerTransferListener(listener)
    }

    override fun removeTransferListener(listener: (Transfer) -> Unit) {
        transferListeners.remove(listener)
        binder?.removeTransferListener(listener)
    }

    override fun registerStatusListener(listener: (TransferManager.Status) -> Unit) {
        statusListeners.add(listener)
        binder?.registerStatusListener(listener)
    }

    override fun removeStatusListener(listener: (TransferManager.Status) -> Unit) {
        statusListeners.remove(listener)
        binder?.removeStatusListener(listener)
    }

    override fun createBindIntent(): Intent {
        return FileTransferService.createBindIntent(context, user)
    }

    override fun onBound(binder: IBinder) {
        super.onBound(binder)
        this.binder = binder as FileTransferService.Binder
        transferListeners.forEach { listener ->
            binder.registerTransferListener(listener)
        }
        statusListeners.forEach { listener ->
            binder.registerStatusListener(listener)
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
            binder?.getTransfer(uuid)
        }
        transferListeners.forEach { listener ->
            transferUpdates.forEach { update ->
                listener.invoke(update)
            }
        }
        transfersRequiringStatusRedelivery.clear()

        if (statusListeners.isNotEmpty()) {
            binder?.status?.let { status ->
                statusListeners.forEach { it.invoke(status) }
            }
        }
    }

    override fun onUnbind() {
        super.onUnbind()
        transferListeners.forEach { binder?.removeTransferListener(it) }
        statusListeners.forEach { binder?.removeStatusListener(it) }
    }
}
