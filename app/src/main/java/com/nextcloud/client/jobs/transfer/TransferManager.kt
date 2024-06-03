/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.transfer

import com.nextcloud.client.files.Request
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
