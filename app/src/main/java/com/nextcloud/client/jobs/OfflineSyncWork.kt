/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2018 Mario Danic
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.ContentResolver
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.CheckEtagRemoteOperation
import com.owncloud.android.operations.SynchronizeFileOperation
import com.owncloud.android.utils.FileStorageUtils
import java.io.File

@Suppress("LongParameterList") // Legacy code
class OfflineSyncWork(
    private val context: Context,
    params: WorkerParameters,
    private val contentResolver: ContentResolver,
    private val userAccountManager: UserAccountManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService
) : Worker(context, params) {

    companion object {
        const val TAG = "OfflineSyncJob"
    }

    override fun doWork(): Result {
        if (!powerManagementService.isPowerSavingEnabled) {
            val users = userAccountManager.allUsers
            for (user in users) {
                val storageManager = FileDataStorageManager(user, contentResolver)
                val ocRoot = storageManager.getFileByPath(OCFile.ROOT_PATH)
                if (ocRoot.storagePath == null) {
                    break
                }
                recursive(File(ocRoot.storagePath), storageManager, user)
            }
        }
        return Result.success()
    }

    private fun recursive(folder: File, storageManager: FileDataStorageManager, user: User) {
        val downloadFolder = FileStorageUtils.getSavePath(user.accountName)
        val folderName = folder.absolutePath.replaceFirst(downloadFolder.toRegex(), "") + OCFile.PATH_SEPARATOR
        Log_OC.d(TAG, "$folderName: enter")
        // exit
        if (folder.listFiles() == null) {
            return
        }

        val updatedEtag = checkETagChanged(folderName, storageManager, user) ?: return

        // iterate over downloaded files
        val files = folder.listFiles { obj: File -> obj.isFile }
        if (files != null) {
            for (file in files) {
                val ocFile = storageManager.getFileByLocalPath(file.path)
                val synchronizeFileOperation = SynchronizeFileOperation(
                    ocFile?.remotePath,
                    user,
                    true,
                    context,
                    storageManager,
                    true
                )
                synchronizeFileOperation.execute(context)
            }
        }
        // recursive into folder
        val subfolders = folder.listFiles { obj: File -> obj.isDirectory }
        if (subfolders != null) {
            for (subfolder in subfolders) {
                recursive(subfolder, storageManager, user)
            }
        }
        // update eTag
        @Suppress("TooGenericExceptionCaught") // legacy code
        try {
            val ocFolder = storageManager.getFileByPath(folderName)
            ocFolder.etagOnServer = updatedEtag
            storageManager.saveFile(ocFolder)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Failed to update etag on " + folder.absolutePath, e)
        }
    }

    /**
     * @return new eTag if changed, `null` otherwise
     */
    private fun checkETagChanged(folderName: String, storageManager: FileDataStorageManager, user: User): String? {
        val folder = storageManager.getFileByEncryptedRemotePath(folderName) ?: return null

        Log_OC.d(TAG, "$folderName: current eTag: ${folder.etag}")

        // check for etag change, if false, skip
        val operation = CheckEtagRemoteOperation(folder.remotePath, folder.etagOnServer)
        val result = operation.execute(user, context)

        return when (result.code) {
            ResultCode.ETAG_UNCHANGED -> {
                Log_OC.d(TAG, "$folderName: eTag unchanged")
                null
            }
            ResultCode.FILE_NOT_FOUND -> {
                val removalResult = storageManager.removeFolder(folder, true, true)
                if (!removalResult) {
                    Log_OC.e(TAG, "removal of " + folder.storagePath + " failed: file not found")
                }
                null
            }
            ResultCode.ETAG_CHANGED -> {
                Log_OC.d(TAG, "$folderName: eTag changed")
                result?.data?.get(0) as? String
            }
            else -> if (connectivityService.isInternetWalled) {
                Log_OC.d(TAG, "No connectivity, skipping sync")
                null
            } else {
                Log_OC.d(TAG, "$folderName: eTag changed")
                result?.data?.get(0) as? String
            }
        }
    }
}
