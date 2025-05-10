/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.model

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.operations.DownloadFileOperation

sealed class WorkerState {
    data class DownloadFinished(var currentFile: OCFile?) : WorkerState()
    data class DownloadStarted(var user: User?, var currentDownload: DownloadFileOperation?, var percent: Int) :
        WorkerState()
    data class UploadFinished(var currentFile: OCFile?) : WorkerState()
    data class UploadStarted(var user: User?, var uploads: List<OCUpload>) : WorkerState()
    data object OfflineOperationsCompleted : WorkerState()
}
