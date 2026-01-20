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
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.fragment.OCFileListFragment

class FileDownloadBroadcastManager(
    private val context: Context,
    private val broadcastManager: LocalBroadcastManager
) {
    companion object {
        private const val TAG = "📣" + "FileDownloadBroadcastManager"

        const val DOWNLOAD_ADDED = "DOWNLOAD_ADDED"
        const val DOWNLOAD_FINISHED = "DOWNLOAD_FINISHED"
    }

    fun sendAdded(accountName: String, remotePath: String, packageName: String, linkedToRemotePath: String?) {
        Log_OC.d(TAG, "download added broadcast sent")

        val intent = Intent(DOWNLOAD_ADDED).apply {
            putExtra(FileDownloadWorker.EXTRA_ACCOUNT_NAME, accountName)
            putExtra(FileDownloadWorker.EXTRA_REMOTE_PATH, remotePath)

            linkedToRemotePath?.let {
                putExtra(FileDownloadWorker.EXTRA_LINKED_TO_PATH, linkedToRemotePath)
            }
            setPackage(packageName)
        }

        broadcastManager.sendBroadcast(intent)
    }

    fun sendFinished(
        download: DownloadFileOperation,
        downloadResult: RemoteOperationResult<*>,
        unlinkedFromRemotePath: String?
    ) {
        Log_OC.d(TAG, "download finish broadcast sent")

        val intent = Intent(DOWNLOAD_FINISHED).apply {
            putExtra(FileDownloadWorker.EXTRA_DOWNLOAD_RESULT, downloadResult.isSuccess)
            putExtra(FileDownloadWorker.EXTRA_ACCOUNT_NAME, download.user.accountName)
            putExtra(FileDownloadWorker.EXTRA_REMOTE_PATH, download.remotePath)
            putExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR, download.behaviour)
            putExtra(SendShareDialog.ACTIVITY_NAME, download.activityName)
            putExtra(SendShareDialog.PACKAGE_NAME, download.packageName)
            if (unlinkedFromRemotePath != null) {
                putExtra(FileDownloadWorker.EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath)
            }
            setPackage(context.packageName)
        }

        broadcastManager.sendBroadcast(intent)
    }

    fun sendFinished(
        accountName: String,
        remotePath: String?,
        packageName: String,
        success: Boolean
    ) {
        Log_OC.d(TAG, "download finish broadcast sent")

        val intent = Intent(DOWNLOAD_FINISHED).apply {
            putExtra(FileDownloadWorker.EXTRA_ACCOUNT_NAME, accountName)
            putExtra(FileDownloadWorker.EXTRA_REMOTE_PATH, remotePath)
            putExtra(FileDownloadWorker.EXTRA_DOWNLOAD_RESULT, success)
            setPackage(packageName)
        }

        broadcastManager.sendBroadcast(intent)
    }
}
