/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.files

import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ComponentsGetter

class FileSynchronizationChecker(
    private val files: Collection<OCFile>,
    private val componentsGetter: ComponentsGetter,
    private val user: User
) {
    fun isAnyFileSynchronizing(): Boolean = anyFileComparedWithRemote() || anyFileDownloading() || anyFileUploading()

    private fun anyFileComparedWithRemote(): Boolean {
        val operationsServiceBinder = componentsGetter.operationsServiceBinder ?: return false

        return files.any { operationsServiceBinder.isSynchronizing(user, it) }
    }

    private fun anyFileDownloading(): Boolean {
        val fileDownloadHelper = FileDownloadHelper.instance()

        return files.any { fileDownloadHelper.isDownloading(user, it) }
    }

    private fun anyFileUploading(): Boolean {
        val fileUploadHelper = FileUploadHelper.instance()
        val accountName = user.accountName

        return files.any { fileUploadHelper.isUploading(it.remotePath, accountName) }
    }
}
