/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.model

import com.nextcloud.client.account.User
import com.owncloud.android.db.OCUpload
import com.owncloud.android.operations.DownloadFileOperation

sealed class WorkerState {
    object Idle : WorkerState()
    class Download(var user: User?, var currentDownload: DownloadFileOperation?) : WorkerState()
    class Upload(var user: User?, var uploads: List<OCUpload>) : WorkerState()
}
