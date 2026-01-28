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
import com.owncloud.android.operations.DownloadFileOperation

sealed class WorkerState {
    data class FolderDownloadCompleted(var folder: OCFile) : WorkerState()

    data class FileDownloadStarted(var user: User?, var currentDownload: DownloadFileOperation?) : WorkerState()
    data class FileDownloadCompleted(var currentFile: OCFile?) : WorkerState()

    data object OfflineOperationsCompleted : WorkerState()
}
