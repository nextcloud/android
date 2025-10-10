/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.utils.Log_OC
import javax.inject.Inject

class SyncWorkerReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "SyncWorkerReceiver"
        const val FOLDER_ID = "folder_id"
    }

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    override fun onReceive(context: Context, intent: Intent) {
        MainApp.getAppComponent().inject(this)

        val folderId = intent.getLongExtra(FOLDER_ID, -1L)
        if (folderId == -1L) {
            Log_OC.e(TAG, "folder id is -1, cant cancel job")
            return
        }

        backgroundJobManager.cancelSyncFolder(folderId)
    }
}
