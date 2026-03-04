/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.folderDownload

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextcloud.client.jobs.download.FileDownloadEventBroadcaster
import com.owncloud.android.lib.common.utils.Log_OC

class FolderDownloadEventBroadcaster(
    private val context: Context,
    private val broadcastManager: LocalBroadcastManager
) {
    companion object {
        private const val TAG = "📣" + "FolderDownloadBroadcastManager"

        private const val ARG_PREFIX = "com.nextcloud.client.folderDownload."

        const val ACTION_DOWNLOAD_ENQUEUED = ARG_PREFIX + "ACTION_DOWNLOAD_ENQUEUED"
        const val ACTION_DOWNLOAD_COMPLETED = ARG_PREFIX + "ACTION_DOWNLOAD_COMPLETED"

        const val EXTRA_FILE_ID = ARG_PREFIX + "EXTRA_FILE_ID"
    }

    fun sendAdded(id: Long) {
        Log_OC.d(TAG, "Download enqueued broadcast sent")

        val intent = Intent(ACTION_DOWNLOAD_ENQUEUED).apply {
            putExtra(EXTRA_FILE_ID, id)
        }

        broadcastManager.sendBroadcast(intent)
    }

    fun sendFinished(id: Long) {
        Log_OC.d(TAG, "Download completed broadcast sent")

        val intent = Intent(ACTION_DOWNLOAD_COMPLETED).apply {
            putExtra(EXTRA_FILE_ID, id)
        }

        broadcastManager.sendBroadcast(intent)
    }
}
