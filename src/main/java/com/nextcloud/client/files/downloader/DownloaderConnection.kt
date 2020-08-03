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

class DownloaderConnection(context: Context, val user: User) : LocalConnection<DownloaderService>(context), Downloader {

    private var downloadListeners: MutableSet<(Transfer) -> Unit> = mutableSetOf()
    private var statusListeners: MutableSet<(Downloader.Status) -> Unit> = mutableSetOf()
    private var binder: DownloaderService.Binder? = null
    private val downloadsRequiringStatusRedelivery: MutableSet<UUID> = mutableSetOf()

    override val isRunning: Boolean
        get() = binder?.isRunning ?: false

    override val status: Downloader.Status
        get() = binder?.status ?: Downloader.Status.EMPTY

    override fun getDownload(uuid: UUID): Transfer? = binder?.getDownload(uuid)

    override fun getDownload(file: OCFile): Transfer? = binder?.getDownload(file)

    override fun download(request: Request) {
        val intent = DownloaderService.createDownloadIntent(context, request)
        context.startService(intent)
        if (!isConnected && downloadListeners.size > 0) {
            downloadsRequiringStatusRedelivery.add(request.uuid)
        }
    }

    override fun registerDownloadListener(listener: (Transfer) -> Unit) {
        downloadListeners.add(listener)
        binder?.registerDownloadListener(listener)
    }

    override fun removeDownloadListener(listener: (Transfer) -> Unit) {
        downloadListeners.remove(listener)
        binder?.removeDownloadListener(listener)
    }

    override fun registerStatusListener(listener: (Downloader.Status) -> Unit) {
        statusListeners.add(listener)
        binder?.registerStatusListener(listener)
    }

    override fun removeStatusListener(listener: (Downloader.Status) -> Unit) {
        statusListeners.remove(listener)
        binder?.removeStatusListener(listener)
    }

    override fun createBindIntent(): Intent {
        return DownloaderService.createBindIntent(context, user)
    }

    override fun onBound(binder: IBinder) {
        super.onBound(binder)
        this.binder = binder as DownloaderService.Binder
        downloadListeners.forEach { listener ->
            binder.registerDownloadListener(listener)
        }
        statusListeners.forEach { listener ->
            binder.registerStatusListener(listener)
        }
        deliverMissedUpdates()
    }

    /**
     * Since binding and download start are both asynchronous and the order
     * is not guaranteed, some downloads might already finish when service is bound,
     * resulting in lost notifications.
     *
     * Deliver all updates for pending downloads that were scheduled
     * before service has been bound.
     */
    private fun deliverMissedUpdates() {
        val downloadUpdates = downloadsRequiringStatusRedelivery.mapNotNull { uuid ->
            binder?.getDownload(uuid)
        }
        downloadListeners.forEach { listener ->
            downloadUpdates.forEach { update ->
                listener.invoke(update)
            }
        }
        downloadsRequiringStatusRedelivery.clear()

        if (statusListeners.isNotEmpty()) {
            binder?.status?.let { status ->
                statusListeners.forEach { it.invoke(status) }
            }
        }
    }

    override fun onUnbind() {
        super.onUnbind()
        downloadListeners.forEach { binder?.removeDownloadListener(it) }
        statusListeners.forEach { binder?.removeStatusListener(it) }
    }
}
