/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs.upload

import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.operations.UploadFileOperation

class AutoUploadProgressForwarder(private val operation: UploadFileOperation) : OnDatatransferProgressListener {
    override fun onTransferProgress(rate: Long, transferred: Long, total: Long, filename: String?) {
        val key = FileUploadHelper.buildRemoteName(operation.user.accountName, operation.uploadRemotePath)
        FileUploadHelper.mBoundListeners[key]?.onTransferProgress(rate, transferred, total, operation.fileName)
    }
}
