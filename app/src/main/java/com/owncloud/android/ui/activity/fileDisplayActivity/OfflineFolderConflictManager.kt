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
import com.nextcloud.utils.extensions.getSerializableArgument
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
                @Suppress("UNCHECKED_CAST")
                val map = getSerializableArgument(
                    FileDisplayActivity.FOLDER_SYNC_CONFLICT_ARG_REMOTE_IDS_TO_OPERATION_PATHS,
                    HashMap::class.java
                ) as? HashMap<String, String>

                if (!map.isNullOrEmpty()) {
                    showFolderSyncConflictNotifications(map)
                }
            }
        }
    }

    private fun showFolderSyncConflictNotifications(remoteIdsToOperationPaths: HashMap<String, String>) {
        remoteIdsToOperationPaths.forEach { (remoteId, path) ->
            val file = activity.storageManager.getFileByRemoteId(remoteId)
            file?.let {
                val entity = activity.storageManager.offlineOperationDao.getByPath(path)
                if (activity.user.isPresent) {
                    notificationManager.showConflictResolveNotification(file, entity, activity.user.get())
                }
            }
        }
    }
}
