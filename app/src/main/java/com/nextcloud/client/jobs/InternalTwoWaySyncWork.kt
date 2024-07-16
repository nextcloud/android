/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.SynchronizeFolderOperation

class InternalTwoWaySyncWork(
    private val context: Context,
    params: WorkerParameters,
    private val user: User
) : Worker(context, params) {
    override fun doWork(): Result {
        val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
        
        val folders = fileDataStorageManager.getInternalTwoWaySyncFolders(user)
        
        var result = true
        
        for (folder in folders) {
            Log_OC.d(TAG, "Starting with folder ${folder.remotePath}")
            val success = SynchronizeFolderOperation(context, folder.remotePath, user, fileDataStorageManager)
                .execute(context)
                .isSuccess
            
            if (!success) {
                Log_OC.d(TAG, "Folder ${folder.remotePath} failed!")
                result = false
            }
        }
        
        return if (result) {
            Result.success()
        } else {
            Result.failure()
        }
    }
    
    companion object {
        const val TAG = "InternalTwoWaySyncWork"
    }
}
