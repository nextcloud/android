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

/**
 * This class represents current download process state.
 * This object is immutable by design.
 *
 * NOTE: Although [OCFile] object is mutable, it is caused by shortcomings
 * of legacy design; please behave like an adult and treat it as immutable value.
 *
 * @property uuid Unique download process id
 * @property state current download state
 * @property progress download progress, 0-100 percent
 * @property file downloaded file, if download is in progress or failed, it is remote; if finished successfully - local
 * @property request initial download request
 */
data class Download(
    val uuid: UUID,
    val state: DownloadState,
    val progress: Int,
    val file: OCFile,
    val request: Request
) {
    /**
     * True if download is no longer running, false if it is still being processed.
     */
    val isFinished: Boolean get() = state == DownloadState.COMPLETED || state == DownloadState.FAILED
}
