/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2018 Mario Danic
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.jobs

import android.content.ContentResolver
import android.content.Context
import android.os.PowerManager.WakeLock
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
class OfflineSyncWork constructor(
    private val context: Context,
    params: WorkerParameters,
    private val contentResolver: ContentResolver,
    private val userAccountManager: UserAccountManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService
) : Worker(context, params) {

    companion object {
        const val TAG = "OfflineSyncJob"
        private const val WAKELOCK_TAG_SEPARATION = ":"
        private const val WAKELOCK_ACQUISITION_TIMEOUT_MS = 10L * 60L * 1000L
    }

    override fun doWork(): Result {
        val wakeLock: WakeLock? = null
        if (!powerManagementService.isPowerSavingEnabled && !connectivityService.isInternetWalled) {
            val users = userAccountManager.allUsers
            for (user in users) {
                val storageManager = FileDataStorageManager(user.toPlatformAccount(), contentResolver)
                val ocRoot = storageManager.getFileByPath(OCFile.ROOT_PATH)
                if (ocRoot.storagePath == null) {
                    break
                }
                recursive(File(ocRoot.storagePath), storageManager, user)
            }
            wakeLock?.release()
        }
        return Result.success()
    }

    @Suppress("ReturnCount", "ComplexMethod") // legacy code
    private fun recursive(folder: File, storageManager: FileDataStorageManager, user: User) {
        val downloadFolder = FileStorageUtils.getSavePath(user.accountName)
        val folderName = folder.absolutePath.replaceFirst(downloadFolder.toRegex(), "") + OCFile.PATH_SEPARATOR
        Log_OC.d(TAG, "$folderName: enter")
        // exit
        if (folder.listFiles() == null) {
            return
        }
        val ocFolder = storageManager.getFileByPath(folderName)
        Log_OC.d(TAG, folderName + ": currentEtag: " + ocFolder.etag)
        // check for etag change, if false, skip
        val checkEtagOperation = CheckEtagRemoteOperation(
            ocFolder.remotePath,
            ocFolder.etagOnServer
        )
        val result = checkEtagOperation.execute(user.toPlatformAccount(), context)
        when (result.code) {
            ResultCode.ETAG_UNCHANGED -> {
                Log_OC.d(TAG, "$folderName: eTag unchanged")
                return
            }
            ResultCode.FILE_NOT_FOUND -> {
                val removalResult = storageManager.removeFolder(ocFolder, true, true)
                if (!removalResult) {
                    Log_OC.e(TAG, "removal of " + ocFolder.storagePath + " failed: file not found")
                }
                return
            }
            ResultCode.ETAG_CHANGED -> Log_OC.d(TAG, "$folderName: eTag changed")
            else -> Log_OC.d(TAG, "$folderName: eTag changed")
        }
        // iterate over downloaded files
        val files = folder.listFiles { obj: File -> obj.isFile }
        if (files != null) {
            for (file in files) {
                val ocFile = storageManager.getFileByLocalPath(file.path)
                val synchronizeFileOperation = SynchronizeFileOperation(
                    ocFile.remotePath,
                    user,
                    true,
                    context,
                    storageManager
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
            val updatedEtag = result.data[0] as String
            ocFolder.etagOnServer = updatedEtag
            storageManager.saveFile(ocFolder)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Failed to update etag on " + folder.absolutePath, e)
        }
    }
}
