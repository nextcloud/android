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

import com.owncloud.android.datamodel.OCFile
import java.util.UUID

interface Downloader {

    /**
     * Snapshot of downloader status. All data is immutable and can be safely shared.
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
     * True if downloader has any pending or running downloads.
     */
    val isRunning: Boolean

    /**
     * Status snapshot of all downloads.
     */
    val status: Status

    /**
     * Register download progress listener. Registration is idempotent - listener can be registered only once.
     */
    fun registerDownloadListener(listener: (Transfer) -> Unit)

    /**
     * Removes registered listener if exists.
     */
    fun removeDownloadListener(listener: (Transfer) -> Unit)

    /**
     * Register downloader status listener. Registration is idempotent - listener can be registered only once.
     */
    fun registerStatusListener(listener: (Status) -> Unit)

    /**
     * Removes registered listener if exists.
     */
    fun removeStatusListener(listener: (Status) -> Unit)

    /**
     * Adds download request to pending queue and returns immediately.
     *
     * @param request Download request
     */
    fun download(request: Request)

    /**
     * Find download status by UUID.
     *
     * @param uuid Download process uuid
     * @return download status or null if not found
     */
    fun getDownload(uuid: UUID): Transfer?

    /**
     * Query user's downloader for a download status. It performs linear search
     * of all queues and returns first download matching [OCFile.remotePath].
     *
     * Since there can be multiple downloads with identical file in downloader's queues,
     * order of search matters.
     *
     * It looks for pending downloads first, then running and completed queue last.
     *
     * @param file Downloaded file
     * @return download status or null, if download does not exist
     */
    fun getDownload(file: OCFile): Transfer?
}
