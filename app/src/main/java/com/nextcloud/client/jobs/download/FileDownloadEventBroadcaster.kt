/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.download

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation

class FileDownloadEventBroadcaster(private val context: Context, private val broadcastManager: LocalBroadcastManager) {
    companion object {
        private const val TAG = "📣" + "FileDownloadBroadcastManager"

        private const val PREFIX = "com.nextcloud.client.download."

        const val ACTION_DOWNLOAD_ENQUEUED = PREFIX + "ACTION_DOWNLOAD_ENQUEUED"
        const val ACTION_DOWNLOAD_COMPLETED = PREFIX + "ACTION_DOWNLOAD_COMPLETED"

        const val EXTRA_DOWNLOAD_RESULT = PREFIX + "EXTRA_DOWNLOAD_RESULT"
        const val EXTRA_REMOTE_PATH = PREFIX + "EXTRA_REMOTE_PATH"
        const val EXTRA_LINKED_TO_PATH = PREFIX + "EXTRA_LINKED_TO_PATH"
        const val EXTRA_ACCOUNT_NAME = PREFIX + "EXTRA_ACCOUNT_NAME"
        const val EXTRA_CURRENT_DOWNLOAD_ACCOUNT_NAME = PREFIX + "EXTRA_CURRENT_DOWNLOAD_ACCOUNT_NAME"
        const val EXTRA_CURRENT_DOWNLOAD_FILE_ID = PREFIX + "EXTRA_CURRENT_DOWNLOAD_FILE_ID"
        const val EXTRA_PACKAGE_NAME = PREFIX + "PACKAGE_NAME"
        const val EXTRA_ACTIVITY_NAME = PREFIX + "ACTIVITY_NAME"
        const val EXTRA_DOWNLOAD_BEHAVIOUR = PREFIX + "DOWNLOAD_BEHAVIOUR"
    }

    fun sendDownloadEnqueued(
        accountName: String,
        remotePath: String,
        packageName: String,
        fileId: Long?,
        linkedToRemotePath: String?,
        currentDownloadAccountName: String?
    ) {
        Log_OC.d(TAG, "Download enqueued broadcast sent")

        val intent = Intent(ACTION_DOWNLOAD_ENQUEUED).apply {
            putExtra(EXTRA_ACCOUNT_NAME, accountName)
            putExtra(EXTRA_REMOTE_PATH, remotePath)

            fileId?.let {
                putExtra(EXTRA_CURRENT_DOWNLOAD_FILE_ID, fileId)
            }

            currentDownloadAccountName?.let {
                putExtra(EXTRA_CURRENT_DOWNLOAD_ACCOUNT_NAME, currentDownloadAccountName)
            }

            linkedToRemotePath?.let {
                putExtra(EXTRA_LINKED_TO_PATH, linkedToRemotePath)
            }
            setPackage(packageName)
        }

        broadcastManager.sendBroadcast(intent)
    }

    fun sendDownloadCompleted(
        download: DownloadFileOperation,
        downloadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?
    ) {
        Log_OC.d(TAG, "Download completed broadcast sent")

        val intent = Intent(ACTION_DOWNLOAD_COMPLETED).apply {
            putExtra(EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess)
            putExtra(EXTRA_ACCOUNT_NAME, download.user.accountName)
            putExtra(EXTRA_REMOTE_PATH, download.remotePath)
            putExtra(EXTRA_DOWNLOAD_BEHAVIOUR, download.behaviour)
            putExtra(EXTRA_ACTIVITY_NAME, download.activityName)
            putExtra(EXTRA_PACKAGE_NAME, download.packageName)
            if (unlinkedFromRemotePath != null) {
                putExtra(EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
            }
            setPackage(context.packageName)
        }

        broadcastManager.sendBroadcast(intent)
    }

    fun sendDownloadCompleted(accountName: String, remotePath: String?, packageName: String, success: Boolean) {
        Log_OC.d(TAG, "Download completed broadcast sent")

        val intent = Intent(ACTION_DOWNLOAD_COMPLETED).apply {
            putExtra(EXTRA_ACCOUNT_NAME, accountName)
            putExtra(EXTRA_REMOTE_PATH, remotePath)
            putExtra(EXTRA_DOWNLOAD_RESULT, success)
            setPackage(packageName)
        }

        broadcastManager.sendBroadcast(intent)
    }
}
