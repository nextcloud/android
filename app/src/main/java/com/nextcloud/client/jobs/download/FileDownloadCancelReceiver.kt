/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.owncloud.android.lib.common.utils.Log_OC

class FileDownloadCancelReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "FileDownloadCancelReceiver"
        private const val INVALID_FILE_ID = -1L

        const val ACCOUNT_NAME = "ACCOUNT_NAME"
        const val FILE_ID = "FILE_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val accountName = intent.getStringExtra(ACCOUNT_NAME)
        if (accountName.isNullOrEmpty()) {
            return
        }

        val fileId = intent.getLongExtra(FILE_ID, INVALID_FILE_ID)
        if (fileId == INVALID_FILE_ID) {
            return
        }

        Log_OC.d(TAG, "cancelling download of file: $fileId")
        FileDownloadWorker.cancelOperation(accountName, fileId)
    }
}
