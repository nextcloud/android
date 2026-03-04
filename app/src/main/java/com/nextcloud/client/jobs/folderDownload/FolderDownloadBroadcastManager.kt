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
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.owncloud.android.lib.common.utils.Log_OC

class FolderDownloadBroadcastManager(
    private val context: Context,
    private val broadcastManager: LocalBroadcastManager
) {
    companion object {
        private const val TAG = "📣" + "FolderDownloadBroadcastManager"

        const val DOWNLOAD_ADDED = "DOWNLOAD_ADDED"
        const val DOWNLOAD_FINISHED = "DOWNLOAD_FINISHED"

        const val EXTRA_FILE_ID = "EXTRA_FILE_ID"
    }

    fun sendAdded(id: Long) {
        Log_OC.d(TAG, "download added broadcast sent")

        val intent = Intent(DOWNLOAD_ADDED).apply {
            putExtra(EXTRA_FILE_ID, id)
        }

        broadcastManager.sendBroadcast(intent)
    }

    fun sendFinished(id: Long) {
        Log_OC.d(TAG, "download finished broadcast sent")

        val intent = Intent(DOWNLOAD_FINISHED).apply {
            putExtra(EXTRA_FILE_ID, id)
        }

        broadcastManager.sendBroadcast(intent)
    }
}
