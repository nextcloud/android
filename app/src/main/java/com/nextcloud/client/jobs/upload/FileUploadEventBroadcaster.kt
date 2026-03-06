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
 * This class is responsible for notifying components about upload
 * queue changes and upload state transitions (added, started, finished).
 *
 * All broadcasts are sent via [LocalBroadcastManager].
 */
class FileUploadEventBroadcaster(private val broadcastManager: LocalBroadcastManager) {

    companion object {
        private const val TAG = "📣" + "FileUploadBroadcastManager"

        private const val PREFIX = "com.nextcloud.client.upload."

        const val ACTION_UPLOAD_ENQUEUED = PREFIX + "ACTION_UPLOAD_ENQUEUED"
        const val ACTION_UPLOAD_STARTED = PREFIX + "ACTION_UPLOAD_STARTED"
        const val ACTION_UPLOAD_COMPLETED = PREFIX + "UPLOAD_FINISHED"

        const val EXTRA_REMOTE_PATH = PREFIX + "EXTRA_REMOTE_PATH"
        const val EXTRA_OLD_FILE_PATH = PREFIX + "EXTRA_OLD_FILE_PATH"
        const val EXTRA_ACCOUNT_NAME = PREFIX + "EXTRA_ACCOUNT_NAME"
        const val EXTRA_OLD_REMOTE_PATH = PREFIX + "EXTRA_OLD_REMOTE_PATH"
        const val EXTRA_UPLOAD_RESULT = PREFIX + "EXTRA_UPLOAD_RESULT"
        const val EXTRA_LINKED_TO_PATH = PREFIX + "EXTRA_LINKED_TO_PATH"
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
    fun sendUploadEnqueued(context: Context) {
        Log_OC.d(TAG, "Upload enqueued broadcast sent")

        val intent = Intent(ACTION_UPLOAD_ENQUEUED).apply {
            setPackage(context.packageName)
        }
        broadcastManager.sendBroadcast(intent)
    }

    /**
     * Sends a broadcast indicating that an upload started.
     *
     * ### Triggered when
     * - [UploadFileOperation] started
     *
     *  ### Observed by
     *  - [com.owncloud.android.ui.activity.UploadListActivity.UploadFinishReceiver]
     *
     */
    fun sendUploadStarted(upload: UploadFileOperation, context: Context) {
        Log_OC.d(TAG, "Upload started broadcast sent")

        val intent = Intent(ACTION_UPLOAD_STARTED).apply {
            putExtra(EXTRA_REMOTE_PATH, upload.remotePath) // real remote
            putExtra(EXTRA_OLD_FILE_PATH, upload.originalStoragePath)
            putExtra(EXTRA_ACCOUNT_NAME, upload.user.accountName)
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
     *  - [com.owncloud.android.ui.activity.FileDisplayActivity.FileUploadCompletedReceiver]
     *  - [com.owncloud.android.ui.activity.UploadListActivity.UploadFinishReceiver]
     *
     */
    fun sendUploadCompleted(
        upload: UploadFileOperation,
        uploadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?,
        context: Context
    ) {
        Log_OC.d(TAG, "Upload completed broadcast sent")

        val intent = Intent(ACTION_UPLOAD_COMPLETED).apply {
            // real remote path, after possible automatic renaming
            putExtra(EXTRA_REMOTE_PATH, upload.remotePath)
            if (upload.wasRenamed()) {
                upload.oldFile?.let {
                    putExtra(EXTRA_OLD_REMOTE_PATH, it.remotePath)
                }
            }
            putExtra(EXTRA_OLD_FILE_PATH, upload.originalStoragePath)
            putExtra(EXTRA_ACCOUNT_NAME, upload.user.accountName)
            putExtra(EXTRA_UPLOAD_RESULT, uploadResult.isSuccess)
            if (unlinkedFromRemotePath != null) {
                putExtra(EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
            }
            setPackage(context.packageName)
        }
        broadcastManager.sendBroadcast(intent)
    }
}
