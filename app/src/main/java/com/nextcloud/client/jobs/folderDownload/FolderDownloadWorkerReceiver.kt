/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.folderDownload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.MainApp
import javax.inject.Inject

class FolderDownloadWorkerReceiver : BroadcastReceiver() {
    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    override fun onReceive(context: Context, intent: Intent) {
        MainApp.getAppComponent().inject(this)
        backgroundJobManager.cancelFolderDownload()
    }
}
