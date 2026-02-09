/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.folderDownload

sealed class FolderDownloadState(open val id: Long) {
    data class Downloading(override val id: Long) : FolderDownloadState(id)
    data class Removed(override val id: Long) : FolderDownloadState(id)
}
