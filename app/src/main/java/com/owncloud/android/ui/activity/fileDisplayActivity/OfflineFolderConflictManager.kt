/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity.fileDisplayActivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextcloud.client.jobs.offlineOperations.OfflineOperationsNotificationManager
import com.owncloud.android.ui.activity.FileDisplayActivity

class OfflineFolderConflictManager(private val activity: FileDisplayActivity) {

    private val notificationManager = OfflineOperationsNotificationManager(activity, activity.viewThemeUtils)

    fun registerRefreshSearchEventReceiver() {
        val filter = IntentFilter(FileDisplayActivity.FOLDER_SYNC_CONFLICT)
        LocalBroadcastManager.getInstance(activity).registerReceiver(folderSyncConflictEventReceiver, filter)
    }

    private val folderSyncConflictEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.run {
                val remoteIds = getStringArrayListExtra(FileDisplayActivity.FOLDER_SYNC_CONFLICT_NEW_FILES)
                val offlineOperationsPaths =
                    getStringArrayListExtra(FileDisplayActivity.FOLDER_SYNC_CONFLICT_OFFLINE_OPERATION_PATHS)

                if (!remoteIds.isNullOrEmpty() && !offlineOperationsPaths.isNullOrEmpty()) {
                    showFolderSyncConflictNotifications(remoteIds, offlineOperationsPaths)
                }
            }
        }
    }

    private fun showFolderSyncConflictNotifications(remoteIds: List<String>, offlineOperationsPaths: List<String>) {
        remoteIds.mapNotNull { activity.storageManager.getFileByRemoteId(it) }
            .forEach { file ->
                offlineOperationsPaths.forEach { path ->
                    notificationManager.showConflictResolveNotification(file, path)
                }
            }
    }
}
