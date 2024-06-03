/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.UploadFileOperation

class FileUploaderDelegate {
    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners once we drop FileUploader
     */
    fun sendBroadcastUploadsAdded(context: Context, localBroadcastManager: LocalBroadcastManager) {
        val start = Intent(FileUploadWorker.getUploadsAddedMessage())
        // nothing else needed right now
        start.setPackage(context.packageName)
        localBroadcastManager.sendBroadcast(start)
    }

    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners once we drop FileUploader
     *
     * @param upload Finished upload operation
     */
    fun sendBroadcastUploadStarted(
        upload: UploadFileOperation,
        context: Context,
        localBroadcastManager: LocalBroadcastManager
    ) {
        val start = Intent(FileUploadWorker.getUploadStartMessage())
        start.putExtra(FileUploadWorker.EXTRA_REMOTE_PATH, upload.remotePath) // real remote
        start.putExtra(FileUploadWorker.EXTRA_OLD_FILE_PATH, upload.originalStoragePath)
        start.putExtra(FileUploadWorker.ACCOUNT_NAME, upload.user.accountName)
        start.setPackage(context.packageName)
        localBroadcastManager.sendBroadcast(start)
    }

    /**
     * Sends a broadcast in order to the interested activities can update their view
     *
     * TODO - no more broadcasts, replace with a callback to subscribed listeners once we drop FileUploader
     *
     * @param upload                 Finished upload operation
     * @param uploadResult           Result of the upload operation
     * @param unlinkedFromRemotePath Path in the uploads tree where the upload was unlinked from
     */
    fun sendBroadcastUploadFinished(
        upload: UploadFileOperation,
        uploadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?,
        context: Context,
        localBroadcastManager: LocalBroadcastManager
    ) {
        val end = Intent(FileUploadWorker.getUploadFinishMessage())
        // real remote path, after possible automatic renaming
        end.putExtra(FileUploadWorker.EXTRA_REMOTE_PATH, upload.remotePath)
        if (upload.wasRenamed()) {
            end.putExtra(FileUploadWorker.EXTRA_OLD_REMOTE_PATH, upload.oldFile!!.remotePath)
        }
        end.putExtra(FileUploadWorker.EXTRA_OLD_FILE_PATH, upload.originalStoragePath)
        end.putExtra(FileUploadWorker.ACCOUNT_NAME, upload.user.accountName)
        end.putExtra(FileUploadWorker.EXTRA_UPLOAD_RESULT, uploadResult.isSuccess)
        if (unlinkedFromRemotePath != null) {
            end.putExtra(FileUploadWorker.EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
        }
        end.setPackage(context.packageName)
        localBroadcastManager.sendBroadcast(end)
    }
}
