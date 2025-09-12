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
import androidx.work.WorkManager
import com.nextcloud.client.jobs.BackgroundJobManagerImpl

class SyncWorkerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tag = BackgroundJobManagerImpl.JOB_SYNC_FOLDER
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }
}
