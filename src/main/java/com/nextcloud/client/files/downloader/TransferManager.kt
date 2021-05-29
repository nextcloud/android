/**
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2021 Chris Narkiewicz <hello@ezaquarii.com>
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
import java.util.UUID

/**
 * Transfer manager provides API to upload and download files.
 */
interface TransferManager {

    /**
     * Snapshot of transfer manager status. All data is immutable and can be safely shared.
     */
    data class Status(
        val pending: List<Transfer>,
        val running: List<Transfer>,
        val completed: List<Transfer>
    ) {
        companion object {
            val EMPTY = Status(emptyList(), emptyList(), emptyList())
        }
    }

    /**
     * True if transfer manager has any pending or running transfers.
     */
    val isRunning: Boolean

    /**
     * Status snapshot of all transfers.
     */
    val status: Status

    /**
     * Register transfer progress listener. Registration is idempotent - a listener will be registered only once.
     */
    fun registerTransferListener(listener: (Transfer) -> Unit)

    /**
     * Removes registered listener if exists.
     */
    fun removeTransferListener(listener: (Transfer) -> Unit)

    /**
     * Register transfer manager status listener. Registration is idempotent - a listener will be registered only once.
     */
    fun registerStatusListener(listener: (Status) -> Unit)

    /**
     * Removes registered listener if exists.
     */
    fun removeStatusListener(listener: (Status) -> Unit)

    /**
     * Adds transfer request to pending queue and returns immediately.
     *
     * @param request Transfer request
     */
    fun enqueue(request: Request)

    /**
     * Find transfer status by UUID.
     *
     * @param uuid Download process uuid
     * @return transfer status or null if not found
     */
    fun getTransfer(uuid: UUID): Transfer?

    /**
     * Query user's transfer manager for a transfer status. It performs linear search
     * of all queues and returns first transfer matching [OCFile.remotePath].
     *
     * Since there can be multiple transfers with identical file in the queues,
     * order of search matters.
     *
     * It looks for pending transfers first, then running and completed queue last.
     *
     * @param file Downloaded file
     * @return transfer status or null, if transfer does not exist
     */
    fun getTransfer(file: OCFile): Transfer?
}
