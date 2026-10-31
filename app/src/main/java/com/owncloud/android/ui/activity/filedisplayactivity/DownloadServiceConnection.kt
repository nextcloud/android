/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.filedisplayactivity

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.owncloud.android.lib.common.utils.Log_OC

class DownloadServiceConnection(private val context: Context) : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) = Unit

    override fun onServiceDisconnected(component: ComponentName) {
        if (component == ComponentName(context, FileDownloadWorker::class.java)) {
            Log_OC.d(TAG, "Download service disconnected")
        }
    }

    companion object {
        private const val TAG = "DownloadServiceConnection"
    }
}
