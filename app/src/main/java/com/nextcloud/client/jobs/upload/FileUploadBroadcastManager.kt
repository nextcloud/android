/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation

/**
 * Manages local broadcasts related to file upload lifecycle events.
 *
 * This class is responsible for notifying interested components about
 * upload queue changes and upload state transitions (added, started, finished).
 *
 * All broadcasts are sent via [LocalBroadcastManager].
 */
class FileUploadBroadcastManager(private val broadcastManager: LocalBroadcastManager) {

    companion object {
        private const val TAG = "📣" + "FileUploadBroadcastManager"

        const val UPLOAD_ADDED = "UPLOAD_ADDED"
        const val UPLOAD_STARTED = "UPLOAD_STARTED"
        const val UPLOAD_FINISHED = "UPLOAD_FINISHED"
    }

    /**
     * Sends a broadcast to indicate that an upload has been added to the database.
     *
     * ### Triggered when
     * - [UploadFileOperation] added
     *
     *  ### Observed by
     *  - [com.owncloud.android.ui.activity.UploadListActivity.UploadFinishReceiver]
     *
     */
    fun sendAdded(context: Context) {
        Log_OC.d(TAG, "upload added broadcast sent")
        val intent = Intent(UPLOAD_ADDED).apply {
            setPackage(context.packageName)
        }
        broadcastManager.sendBroadcast(intent)
    }

    /**
     * Sends a broadcast indicating that an upload started
     *
     * ### Triggered when
     * - [UploadFileOperation] started
     *
     *  ### Observed by
     *  - [com.owncloud.android.ui.activity.UploadListActivity.UploadFinishReceiver]
     *
     */
    fun sendStarted(upload: UploadFileOperation, context: Context) {
        Log_OC.d(TAG, "upload started broadcast sent")
        val intent = Intent(UPLOAD_STARTED).apply {
            putExtra(FileUploadWorker.EXTRA_REMOTE_PATH, upload.remotePath) // real remote
            putExtra(FileUploadWorker.EXTRA_OLD_FILE_PATH, upload.originalStoragePath)
            putExtra(FileUploadWorker.ACCOUNT_NAME, upload.user.accountName)
            setPackage(context.packageName)
        }
        broadcastManager.sendBroadcast(intent)
    }

    /**
     * Sends a broadcast indicating that an upload has finished, either
     * successfully or with an error.
     *
     * ### Triggered when
     * - [UploadFileOperation] completes execution
     *
     *  ### Observed by
     *  - [com.owncloud.android.ui.activity.FileDisplayActivity.UploadFinishReceiver]
     *  - [com.owncloud.android.ui.activity.UploadListActivity.UploadFinishReceiver]
     *  - [com.owncloud.android.ui.preview.PreviewImageActivity.UploadFinishReceiver]
     *
     */
    fun sendFinished(
        upload: UploadFileOperation,
        uploadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?,
        context: Context
    ) {
        Log_OC.d(TAG, "upload finished broadcast sent")
        val intent = Intent(UPLOAD_FINISHED).apply {
            // real remote path, after possible automatic renaming
            putExtra(FileUploadWorker.EXTRA_REMOTE_PATH, upload.remotePath)
            if (upload.wasRenamed()) {
                upload.oldFile?.let {
                    putExtra(FileUploadWorker.EXTRA_OLD_REMOTE_PATH, it.remotePath)
                }
            }
            putExtra(FileUploadWorker.EXTRA_OLD_FILE_PATH, upload.originalStoragePath)
            putExtra(FileUploadWorker.ACCOUNT_NAME, upload.user.accountName)
            putExtra(FileUploadWorker.EXTRA_UPLOAD_RESULT, uploadResult.isSuccess)
            if (unlinkedFromRemotePath != null) {
                putExtra(FileUploadWorker.EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
            }
            setPackage(context.packageName)
        }
        broadcastManager.sendBroadcast(intent)
    }
}
