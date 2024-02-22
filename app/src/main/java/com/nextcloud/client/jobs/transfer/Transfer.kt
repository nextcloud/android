/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.jobs.transfer

import com.nextcloud.client.files.Direction
import com.nextcloud.client.files.DownloadRequest
import com.nextcloud.client.files.Request
import com.nextcloud.client.files.UploadRequest
import com.owncloud.android.datamodel.OCFile
import java.util.UUID

/**
 * This class represents current transfer (download or upload) process state.
 * This object is immutable by design.
 *
 * NOTE: Although [OCFile] object is mutable, it is caused by shortcomings
 * of legacy design; please behave like an adult and treat it as immutable value.
 *
 * @property uuid Unique transfer id
 * @property state current transfer state
 * @property progress transfer progress, 0-100 percent
 * @property file transferred file
 * @property request initial transfer request
 * @property direction transfer direction, download or upload
 */
data class Transfer(
    val uuid: UUID,
    val state: TransferState,
    val progress: Int,
    val file: OCFile,
    val request: Request
) {
    /**
     * True if download is no longer running, false if it is still being processed.
     */
    val isFinished: Boolean get() = state == TransferState.COMPLETED || state == TransferState.FAILED

    val direction: Direction
        get() = when (request) {
            is DownloadRequest -> Direction.DOWNLOAD
            is UploadRequest -> Direction.UPLOAD
        }
}
