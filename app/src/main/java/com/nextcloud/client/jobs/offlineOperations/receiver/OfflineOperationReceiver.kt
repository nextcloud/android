/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextcloud.client.jobs.offlineOperations.OfflineOperationsNotificationManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import javax.inject.Inject

class OfflineOperationReceiver : BroadcastReceiver() {
    companion object {
        const val ID = "id"
    }

    @Inject
    lateinit var storageManager: FileDataStorageManager

    override fun onReceive(context: Context, intent: Intent) {
        MainApp.getAppComponent().inject(this)

        val id = intent.getIntExtra(ID, -1)
        if (id == -1) {
            return
        }

        storageManager.offlineOperationDao.deleteById(id)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(
            OfflineOperationsNotificationManager.ERROR_ID
        )
    }
}
